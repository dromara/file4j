package org.dromara.x.file.storage.core.platform;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.StrUtil;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.dromara.x.file.storage.core.*;
import org.dromara.x.file.storage.core.UploadPretreatment;
import org.dromara.x.file.storage.core.copy.CopyPretreatment;
import org.dromara.x.file.storage.core.exception.Check;
import org.dromara.x.file.storage.core.exception.ExceptionFactory;
import org.dromara.x.file.storage.core.file.FileWrapper;
import org.dromara.x.file.storage.core.get.*;
import org.dromara.x.file.storage.core.move.MovePretreatment;
import org.dromara.x.file.storage.core.presigned.GeneratePresignedUrlPretreatment;
import org.dromara.x.file.storage.core.presigned.GeneratePresignedUrlResult;
import org.dromara.x.file.storage.core.upload.*;
import org.dromara.x.file.storage.core.util.Tools;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

/**
 * Amazon S3 存储<br/>
 * 适用于AWS SDK for Java 2.x，根据Amazon公告，AWS SDK for Java 1.x 自 2024 年 7 月 31 日起将进入维护模式，并于2025 年 12 月 31 日终止支持<br/>
 * 公告链接地址：<a href="https://aws.amazon.com/blogs/developer/the-aws-sdk-for-java-1-x-is-in-maintenance-mode-effective-july-31-2024/">The AWS SDK for Java 1.x is in maintenance mode, effective July 31, 2024</a>
 * @author zhangxin
 * @date 2024-12-08
 */
@Getter
@Setter
@NoArgsConstructor
public class AmazonS3V2FileStorage implements FileStorage {
    private String platform;
    private String bucketName;
    private String domain;
    private String basePath;
    private String defaultAcl;
    private int multipartThreshold;
    private int multipartPartSize;
    private FileStorageClientFactory<S3Client> clientFactory;

    public AmazonS3V2FileStorage(
            FileStorageProperties.AmazonS3V2Config config, FileStorageClientFactory<S3Client> clientFactory) {
        platform = config.getPlatform();
        bucketName = config.getBucketName();
        domain = config.getDomain();
        basePath = config.getBasePath();
        defaultAcl = config.getDefaultAcl();
        multipartThreshold = config.getMultipartThreshold();
        multipartPartSize = config.getMultipartPartSize();
        this.clientFactory = clientFactory;
    }

    public S3Client getClient() {
        return clientFactory.getClient();
    }

    @Override
    public void close() {
        clientFactory.close();
    }

    @Override
    public boolean save(FileInfo fileInfo, UploadPretreatment pre) {
        fileInfo.setBasePath(basePath);
        String newFileKey = getFileKey(fileInfo);
        fileInfo.setUrl(domain + newFileKey);
        ObjectCannedACL fileAcl = getAcl(fileInfo.getFileAcl());
        Map<String, String> metadata = getObjectMetadata(fileInfo); // 使用 Map 代替 ObjectMetadata
        ProgressListener listener = pre.getProgressListener();
        S3Client client = getClient(); // 使用 S3Client 替代 AmazonS3

        boolean useMultipartUpload = fileInfo.getSize() == null || fileInfo.getSize() >= multipartThreshold;
        String uploadId = null;
        try (InputStreamPlus in = pre.getInputStreamPlus(false)) {
            if (useMultipartUpload) { // 分片上传
                CreateMultipartUploadRequest createRequest = CreateMultipartUploadRequest.builder()
                        .bucket(bucketName)
                        .key(newFileKey)
                        .metadata(metadata)
                        .build();
                CreateMultipartUploadResponse createResponse = client.createMultipartUpload(createRequest);
                uploadId = createResponse.uploadId();
                List<CompletedPart> partList = new ArrayList<>();
                int i = 0;
                AtomicLong progressSize = new AtomicLong();
                if (listener != null) listener.start();
                while (true) {
                    byte[] bytes = IoUtil.readBytes(in, multipartPartSize);
                    if (bytes == null || bytes.length == 0) {
                        break;
                    }

                    // 创建分片上传请求
                    UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
                            .bucket(bucketName)
                            .key(newFileKey)
                            .uploadId(uploadId)
                            .partNumber(
                                    ++i) // 设置分片号。每一个上传的分片都有一个分片号，取值范围是1~10000，如果超出此范围，AmazonS3将返回InvalidArgument错误码。
                            .contentLength((long) bytes.length)
                            .build();
                    // 使用 InputStreamPlus 自动监听进度，无需在这里单独设置监听
                    UploadPartResponse uploadResponse =
                            client.uploadPart(uploadPartRequest, RequestBody.fromBytes(bytes));

                    // 更新进度
                    if (listener != null) {
                        listener.progress(progressSize.addAndGet(bytes.length), fileInfo.getSize());
                    }

                    // 记录分片的 ETag 信息
                    partList.add(CompletedPart.builder()
                            .partNumber(i)
                            .eTag(uploadResponse.eTag())
                            .build());
                }
                // 完成分片上传
                CompleteMultipartUploadRequest completeRequest = CompleteMultipartUploadRequest.builder()
                        .bucket(bucketName)
                        .key(newFileKey)
                        .uploadId(uploadId)
                        .multipartUpload(CompletedMultipartUpload.builder()
                                .parts(partList)
                                .build())
                        .build();
                client.completeMultipartUpload(completeRequest);

                // 设置 ACL（如果指定了）
                if (fileAcl != null) {
                    PutObjectAclRequest aclRequest = PutObjectAclRequest.builder()
                            .bucket(bucketName)
                            .key(newFileKey)
                            .acl(fileAcl)
                            .build();
                    client.putObjectAcl(aclRequest);
                }

                if (listener != null) {
                    listener.finish();
                }
            } else { // 普通上传
                BufferedInputStream bin = new BufferedInputStream(in);
                PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(newFileKey)
                        .metadata(metadata)
                        .acl(fileAcl)
                        .build();
                client.putObject(putObjectRequest, RequestBody.fromInputStream(bin, fileInfo.getSize()));
                if (listener != null) {
                    AtomicLong progressSize = new AtomicLong();
                    listener.start();
                    long bytesRead;
                    while ((bytesRead = bin.read()) != -1) {
                        listener.progress(progressSize.addAndGet(bytesRead), fileInfo.getSize());
                    }
                    listener.finish();
                }
            }
            if (fileInfo.getSize() == null) fileInfo.setSize(in.getProgressSize());
            byte[] thumbnailBytes = pre.getThumbnailBytes();
            if (thumbnailBytes != null) { // 上传缩略图
                String newThFileKey = getThFileKey(fileInfo);
                fileInfo.setThUrl(domain + newThFileKey);
                PutObjectRequest thRequest = PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(newThFileKey)
                        .metadata(getThObjectMetadata(fileInfo))
                        .acl(fileAcl)
                        .build();
                client.putObject(thRequest, RequestBody.fromBytes(thumbnailBytes));
            }
            return true;
        } catch (Exception e) {
            try {
                if (useMultipartUpload) {
                    AbortMultipartUploadRequest abortRequest = AbortMultipartUploadRequest.builder()
                            .bucket(bucketName)
                            .key(newFileKey)
                            .uploadId(uploadId)
                            .build();
                    client.abortMultipartUpload(abortRequest);
                } else {
                    DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                            .bucket(bucketName)
                            .key(newFileKey)
                            .build();
                    client.deleteObject(deleteRequest);
                }
            } catch (Exception ignored) {
            }
            throw ExceptionFactory.upload(fileInfo, platform, e);
        }
    }

    @Override
    public MultipartUploadSupportInfo isSupportMultipartUpload() {
        return MultipartUploadSupportInfo.supportAll();
    }

    @Override
    public void initiateMultipartUpload(FileInfo fileInfo, InitiateMultipartUploadPretreatment pre) {
        fileInfo.setBasePath(basePath);
        String newFileKey = getFileKey(fileInfo);
        fileInfo.setUrl(domain + newFileKey);
        Map<String, String> metadata = getObjectMetadata(fileInfo);
        S3Client client = getClient();
        try {
            String uploadId = client.createMultipartUpload(CreateMultipartUploadRequest.builder()
                            .bucket(bucketName)
                            .key(newFileKey)
                            .metadata(metadata)
                            .acl(getAcl(fileInfo.getFileAcl()))
                            .build())
                    .uploadId();

            fileInfo.setUploadId(uploadId);
        } catch (Exception e) {
            throw ExceptionFactory.initiateMultipartUpload(fileInfo, platform, e);
        }
    }

    @Override
    public FilePartInfo uploadPart(UploadPartPretreatment pre) {
        FileInfo fileInfo = pre.getFileInfo();
        String newFileKey = getFileKey(fileInfo);
        S3Client client = getClient();
        FileWrapper partFileWrapper = pre.getPartFileWrapper();
        Long partSize = partFileWrapper.getSize();
        try (InputStreamPlus in = pre.getInputStreamPlus()) {
            // Amazon S3 比较特殊，上传分片必须传入分片大小，这里强制获取，可能会占用大量内存
            if (partSize == null) partSize = partFileWrapper.getInputStreamMaskResetReturn(Tools::getSize);
            UploadPartRequest partRequest = UploadPartRequest.builder()
                    .bucket(bucketName)
                    .key(newFileKey)
                    .uploadId(fileInfo.getUploadId())
                    .partNumber(pre.getPartNumber())
                    .contentLength(partSize)
                    .build();
            UploadPartResponse partResponse = client.uploadPart(partRequest, RequestBody.fromInputStream(in, partSize));
            FilePartInfo filePartInfo = new FilePartInfo(fileInfo);
            filePartInfo.setETag(partResponse.eTag());
            filePartInfo.setPartNumber(pre.getPartNumber());
            filePartInfo.setPartSize(in.getProgressSize());
            filePartInfo.setCreateTime(new Date());
            return filePartInfo;
        } catch (Exception e) {
            throw ExceptionFactory.uploadPart(fileInfo, platform, e);
        }
    }

    @Override
    public void completeMultipartUpload(CompleteMultipartUploadPretreatment pre) {
        FileInfo fileInfo = pre.getFileInfo();
        String newFileKey = getFileKey(fileInfo);
        ObjectCannedACL fileAcl = getAcl(fileInfo.getFileAcl()); // 替换为 ObjectCannedACL
        S3Client client = getClient();
        try {
            // 收集分片的 Part 信息
            List<CompletedPart> completedParts = pre.getPartInfoList().stream()
                    .map(part -> CompletedPart.builder()
                            .partNumber(part.getPartNumber())
                            .eTag(part.getETag())
                            .build())
                    .collect(Collectors.toList());

            // 开始进度监听
            ProgressListener.quickStart(pre.getProgressListener(), fileInfo.getSize());

            // 完成分片上传
            CompleteMultipartUploadRequest completeRequest = CompleteMultipartUploadRequest.builder()
                    .bucket(bucketName)
                    .key(newFileKey)
                    .uploadId(fileInfo.getUploadId())
                    .multipartUpload(CompletedMultipartUpload.builder()
                            .parts(completedParts)
                            .build())
                    .build();
            client.completeMultipartUpload(completeRequest);

            // 设置 ACL（如果指定了）
            if (fileAcl != null) {
                PutObjectAclRequest aclRequest = PutObjectAclRequest.builder()
                        .bucket(bucketName)
                        .key(newFileKey)
                        .acl(fileAcl)
                        .build();
                client.putObjectAcl(aclRequest);
            }

            // 完成进度监听
            ProgressListener.quickFinish(pre.getProgressListener(), fileInfo.getSize());
        } catch (Exception e) {
            throw ExceptionFactory.completeMultipartUpload(fileInfo, platform, e);
        }
    }

    @Override
    public void abortMultipartUpload(AbortMultipartUploadPretreatment pre) {
        FileInfo fileInfo = pre.getFileInfo();
        String newFileKey = getFileKey(fileInfo);
        S3Client client = getClient();
        try {
            AbortMultipartUploadRequest abortRequest = AbortMultipartUploadRequest.builder()
                    .bucket(bucketName)
                    .key(newFileKey)
                    .uploadId(fileInfo.getUploadId())
                    .build();
            client.abortMultipartUpload(abortRequest);
        } catch (Exception e) {
            throw ExceptionFactory.abortMultipartUpload(fileInfo, platform, e);
        }
    }

    @Override
    public FilePartInfoList listParts(ListPartsPretreatment pre) {
        FileInfo fileInfo = pre.getFileInfo();
        String newFileKey = getFileKey(fileInfo);
        S3Client client = getClient();
        try {
            ListPartsRequest request = ListPartsRequest.builder()
                    .bucket(bucketName)
                    .key(newFileKey)
                    .uploadId(fileInfo.getUploadId())
                    .maxParts(pre.getMaxParts())
                    .partNumberMarker(pre.getPartNumberMarker())
                    .build();
            ListPartsResponse result = client.listParts(request);

            FilePartInfoList list = new FilePartInfoList();
            list.setFileInfo(fileInfo);
            list.setList(result.parts().stream()
                    .map(p -> {
                        FilePartInfo filePartInfo = new FilePartInfo(fileInfo);
                        filePartInfo.setETag(p.eTag());
                        filePartInfo.setPartNumber(p.partNumber());
                        filePartInfo.setPartSize(p.size());
                        filePartInfo.setLastModified(Date.from(p.lastModified()));
                        return filePartInfo;
                    })
                    .collect(Collectors.toList()));
            list.setMaxParts(result.maxParts());
            list.setIsTruncated(result.isTruncated());
            list.setPartNumberMarker(result.partNumberMarker());
            list.setNextPartNumberMarker(result.nextPartNumberMarker());
            return list;
        } catch (Exception e) {
            throw ExceptionFactory.listParts(fileInfo, platform, e);
        }
    }

    @Override
    public ListFilesSupportInfo isSupportListFiles() {
        return ListFilesSupportInfo.supportAll();
    }

    @Override
    public ListFilesResult listFiles(ListFilesPretreatment pre) {
        S3Client client = getClient();
        try {
            ListObjectsV2Request request = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .maxKeys(pre.getMaxFiles())
                    .startAfter(pre.getMarker()) // 使用 startAfter 替代 marker
                    .delimiter("/")
                    .prefix(basePath + pre.getPath() + pre.getFilenamePrefix())
                    .build();
            ListObjectsV2Response result = client.listObjectsV2(request);
            ListFilesResult list = new ListFilesResult();

            list.setDirList(result.commonPrefixes().stream()
                    .map(item -> {
                        RemoteDirInfo dir = new RemoteDirInfo();
                        dir.setPlatform(pre.getPlatform());
                        dir.setBasePath(basePath);
                        dir.setPath(pre.getPath());
                        dir.setName(FileNameUtil.getName(String.valueOf(item)));
                        dir.setOriginal(item);
                        return dir;
                    })
                    .collect(Collectors.toList()));

            list.setFileList(result.contents().stream()
                    .map(item -> {
                        RemoteFileInfo info = new RemoteFileInfo();
                        info.setPlatform(pre.getPlatform());
                        info.setBasePath(basePath);
                        info.setPath(pre.getPath());
                        info.setFilename(FileNameUtil.getName(item.key()));
                        info.setUrl(domain + getFileKey(new FileInfo(basePath, info.getPath(), info.getFilename())));
                        info.setSize(item.size());
                        info.setExt(FileNameUtil.extName(info.getFilename()));
                        info.setETag(item.eTag());
                        info.setLastModified(Date.from(item.lastModified()));
                        info.setOriginal(item);
                        return info;
                    })
                    .collect(Collectors.toList()));
            list.setPlatform(pre.getPlatform());
            list.setBasePath(basePath);
            list.setPath(pre.getPath());
            list.setFilenamePrefix(pre.getFilenamePrefix());
            list.setMaxFiles(result.maxKeys());
            list.setIsTruncated(result.isTruncated());
            list.setMarker(pre.getMarker());
            list.setNextMarker(result.nextContinuationToken());
            return list;
        } catch (Exception e) {
            throw ExceptionFactory.listFiles(pre, basePath, e);
        }
    }

    @Override
    public RemoteFileInfo getFile(GetFilePretreatment pre) {
        String fileKey = getFileKey(new FileInfo(basePath, pre.getPath(), pre.getFilename()));
        S3Client client = getClient();
        try {
            HeadObjectResponse metadata;
            try {
                metadata = client.headObject(HeadObjectRequest.builder()
                        .bucket(bucketName)
                        .key(fileKey)
                        .build());
            } catch (Exception e) {
                return null;
            }
            if (metadata == null) return null;

            RemoteFileInfo info = new RemoteFileInfo();
            info.setPlatform(pre.getPlatform());
            info.setBasePath(basePath);
            info.setPath(pre.getPath());
            info.setFilename(FileNameUtil.getName(fileKey));
            info.setUrl(domain + fileKey);
            info.setSize(metadata.contentLength());
            info.setExt(FileNameUtil.extName(info.getFilename()));
            info.setETag(metadata.eTag());
            info.setContentDisposition(metadata.contentDisposition());
            info.setContentType(metadata.contentType());
            info.setContentMd5(metadata.metadata().get("Content-MD5"));
            info.setLastModified(Date.from(metadata.lastModified()));
            // AWS SDK 2.x 不区分 RawMetadata 和 UserMetadata，因此全部使用 metadata()
            if (metadata.metadata() != null) {
                info.setMetadata(new HashMap<>(metadata.metadata()));
                info.setUserMetadata(new HashMap<>(metadata.metadata()));
            }
            info.setOriginal(metadata);
            return info;
        } catch (Exception e) {
            throw ExceptionFactory.getFile(pre, basePath, e);
        }
    }

    /**
     * 获取文件的访问控制列表
     */
    public ObjectCannedACL getAcl(Object acl) {
        if (acl instanceof ObjectCannedACL) {
            return (ObjectCannedACL) acl;
        } else if (acl instanceof String || acl == null) {
            String sAcl = (String) acl;
            if (StrUtil.isEmpty(sAcl)) sAcl = defaultAcl; // 如果为空则使用默认 ACL
            for (ObjectCannedACL item : ObjectCannedACL.values()) {
                if (item.toString().equalsIgnoreCase(sAcl)) { // 忽略大小写比较
                    return item;
                }
            }
            return null; // 如果未匹配到，返回 null
        } else {
            throw ExceptionFactory.unrecognizedAcl(acl, platform);
        }
    }

    /**
     * 获取对象的元数据
     */
    public Map<String, String> getObjectMetadata(FileInfo fileInfo) {
        Map<String, String> metadata = new HashMap<>();

        // 设置文件大小
        if (fileInfo.getSize() != null) metadata.put("Content-Length", String.valueOf(fileInfo.getSize()));

        // 设置文件类型
        if (fileInfo.getContentType() != null) metadata.put("Content-Type", fileInfo.getContentType());

        // 设置用户元数据
        if (fileInfo.getUserMetadata() != null) {
            metadata.putAll(fileInfo.getUserMetadata());
        }

        // 设置其他元数据
        if (CollUtil.isNotEmpty(fileInfo.getMetadata())) {
            metadata.putAll(fileInfo.getMetadata());
        }

        return metadata;
    }

    /**
     * 获取缩略图对象的元数据
     */
    public Map<String, String> getThObjectMetadata(FileInfo fileInfo) {
        Map<String, String> metadata = new HashMap<>();

        // 设置缩略图的文件大小
        if (fileInfo.getThSize() != null) {
            metadata.put("Content-Length", String.valueOf(fileInfo.getThSize()));
        }

        // 设置缩略图的内容类型
        if (fileInfo.getThContentType() != null) {
            metadata.put("Content-Type", fileInfo.getThContentType());
        }

        // 设置缩略图的用户元数据
        if (fileInfo.getThUserMetadata() != null) {
            metadata.putAll(fileInfo.getThUserMetadata());
        }

        // 设置其他缩略图的元数据
        if (CollUtil.isNotEmpty(fileInfo.getThMetadata())) {
            metadata.putAll(fileInfo.getThMetadata());
        }

        return metadata;
    }

    @Override
    public boolean isSupportPresignedUrl() {
        return true;
    }

    @Override
    public GeneratePresignedUrlResult generatePresignedUrl(GeneratePresignedUrlPretreatment pre) {
        try (S3Presigner s3Presigner = S3Presigner.create()) { // 使用 try-with-resources 管理 s3Presigner
            String fileKey = getFileKey(new FileInfo(basePath, pre.getPath(), pre.getFilename()));

            // 构建 GetObjectRequest
            GetObjectRequest.Builder getObjectRequestBuilder =
                    GetObjectRequest.builder().bucket(bucketName).key(fileKey);

            // 构建查询参数，用于覆盖响应头
            Map<String, String> queryParams = new HashMap<>();
            pre.getResponseHeaders().forEach((key, value) -> queryParams.put("response-" + key.toLowerCase(), value));

            // 合并用户元数据和请求头
            Map<String, String> headers = new HashMap<>(pre.getHeaders());
            pre.getUserMetadata()
                    .forEach((key, value) ->
                            headers.put(key.startsWith("x-amz-meta-") ? key : "x-amz-meta-" + key, value));

            // 将用户元数据转换为查询参数（AWS SDK 不直接支持请求头）
            queryParams.putAll(headers);

            // 将过期时间转换为 Duration
            Duration duration = Duration.ofMillis(pre.getExpiration().getTime() - System.currentTimeMillis());

            // 构建预签名请求
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .getObjectRequest(getObjectRequestBuilder.build())
                    .signatureDuration(duration)
                    .build();

            // 生成预签名 URL
            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);

            // 将查询参数拼接到 URL
            String url = appendQueryParams(presignedRequest.url().toString(), queryParams);

            // 将结果封装到 GeneratePresignedUrlResult 中
            GeneratePresignedUrlResult result = new GeneratePresignedUrlResult(platform, basePath, pre);
            result.setUrl(url);
            result.setHeaders(headers);
            return result;
        } catch (Exception e) {
            throw ExceptionFactory.generatePresignedUrl(pre, e);
        }
    }

    /**
     * 将查询参数附加到 URL
     */
    private String appendQueryParams(String url, Map<String, String> queryParams) {
        if (queryParams.isEmpty()) {
            return url;
        }
        StringBuilder sb = new StringBuilder(url);
        if (!url.contains("?")) {
            sb.append("?");
        } else if (!url.endsWith("&")) {
            sb.append("&");
        }
        queryParams.forEach(
                (key, value) -> sb.append(key).append("=").append(value).append("&"));
        // 移除末尾多余的 "&"
        sb.setLength(sb.length() - 1);
        return sb.toString();
    }

    public boolean isSupportAcl() {
        return true;
    }

    @Override
    public boolean setFileAcl(FileInfo fileInfo, Object acl) {
        ObjectCannedACL oAcl = getAcl(acl);
        try {
            if (oAcl == null) {
                return false;
            } else {
                PutObjectAclRequest aclRequest = PutObjectAclRequest.builder()
                        .bucket(bucketName)
                        .key(getFileKey(fileInfo))
                        .acl(oAcl)
                        .build();
                getClient().putObjectAcl(aclRequest);
                return true;
            }
        } catch (Exception e) {
            throw ExceptionFactory.setFileAcl(fileInfo, oAcl, platform, e);
        }
    }

    @Override
    public boolean setThFileAcl(FileInfo fileInfo, Object acl) {
        return setFileAcl(fileInfo, acl);
    }

    @Override
    public boolean isSupportMetadata() {
        return true;
    }

    @Override
    public boolean delete(FileInfo fileInfo) {
        S3Client client = getClient();
        try {
            if (fileInfo.getThFilename() != null) { // 删除缩略图
                DeleteObjectRequest deleteThumbnailRequest = DeleteObjectRequest.builder()
                        .bucket(bucketName)
                        .key(getThFileKey(fileInfo))
                        .build();
                client.deleteObject(deleteThumbnailRequest);
            }

            DeleteObjectRequest deleteFileRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(getFileKey(fileInfo))
                    .build();
            client.deleteObject(deleteFileRequest);

            return true;
        } catch (Exception e) {
            throw ExceptionFactory.delete(fileInfo, platform, e);
        }
    }

    @Override
    public boolean exists(FileInfo fileInfo) {
        try {
            HeadObjectRequest request = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(getFileKey(fileInfo))
                    .build();

            // 如果文件存在，headObject 不会抛出异常
            getClient().headObject(request);
            return true;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return false; // 如果状态码是 404，则表示文件不存在
            }
            throw ExceptionFactory.exists(fileInfo, platform, e);
        } catch (Exception e) {
            throw ExceptionFactory.exists(fileInfo, platform, e);
        }
    }

    @Override
    public void download(FileInfo fileInfo, Consumer<InputStream> consumer) {
        S3Client client = getClient();
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(getFileKey(fileInfo))
                    .build();

            // 下载文件，获取对象的 InputStream
            try (InputStream in = client.getObject(request)) {
                consumer.accept(in);
            }
        } catch (Exception e) {
            throw ExceptionFactory.download(fileInfo, platform, e);
        }
    }

    @Override
    public void downloadTh(FileInfo fileInfo, Consumer<InputStream> consumer) {
        Check.downloadThBlankThFilename(platform, fileInfo);

        S3Client client = getClient();
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(getThFileKey(fileInfo))
                    .build();

            // 下载文件，获取对象的 InputStream
            try (InputStream in = client.getObject(request)) {
                consumer.accept(in);
            }
        } catch (Exception e) {
            throw ExceptionFactory.download(fileInfo, platform, e);
        }
    }

    @Override
    public boolean isSupportSameCopy() {
        return true;
    }

    @Override
    public void sameCopy(FileInfo srcFileInfo, FileInfo destFileInfo, CopyPretreatment pre) {
        Check.sameCopyBasePath(platform, basePath, srcFileInfo, destFileInfo);

        S3Client client = getClient();

        // 获取远程文件信息
        String srcFileKey = getFileKey(srcFileInfo);
        HeadObjectRequest headRequest =
                HeadObjectRequest.builder().bucket(bucketName).key(srcFileKey).build();
        HeadObjectResponse srcFile;
        try {
            srcFile = client.headObject(headRequest);
        } catch (Exception e) {
            throw ExceptionFactory.sameCopyNotFound(srcFileInfo, destFileInfo, platform, e);
        }

        // 复制缩略图文件
        String destThFileKey = null;
        if (StrUtil.isNotBlank(srcFileInfo.getThFilename())) {
            destThFileKey = getThFileKey(destFileInfo);
            destFileInfo.setThUrl(domain + destThFileKey);
            CopyObjectRequest copyThumbnailRequest = CopyObjectRequest.builder()
                    .sourceBucket(bucketName)
                    .sourceKey(getThFileKey(srcFileInfo))
                    .destinationBucket(bucketName)
                    .destinationKey(destThFileKey)
                    .acl(getAcl(destFileInfo.getThFileAcl()))
                    .build();
            try {
                client.copyObject(copyThumbnailRequest);
            } catch (Exception e) {
                throw ExceptionFactory.sameCopyTh(srcFileInfo, destFileInfo, platform, e);
            }
        }

        // 复制文件
        String destFileKey = getFileKey(destFileInfo);
        destFileInfo.setUrl(domain + destFileKey);
        long fileSize = srcFile.contentLength();
        boolean useMultipartCopy = fileSize >= 1024 * 1024 * 1024; // 小于 1GB，走小文件复制
        String uploadId = null;

        try {
            if (useMultipartCopy) { // 大文件复制
                CreateMultipartUploadRequest multipartUploadRequest = CreateMultipartUploadRequest.builder()
                        .bucket(bucketName)
                        .key(destFileKey)
                        .acl(getAcl(destFileInfo.getFileAcl()))
                        .build();
                CreateMultipartUploadResponse multipartUploadResponse =
                        client.createMultipartUpload(multipartUploadRequest);
                uploadId = multipartUploadResponse.uploadId();

                ProgressListener.quickStart(pre.getProgressListener(), fileSize);
                ArrayList<CompletedPart> partList = new ArrayList<>();
                long progressSize = 0;
                int i = 0;
                while (progressSize < fileSize) {
                    long partSize = Math.min(256 * 1024 * 1024, fileSize - progressSize);
                    UploadPartCopyRequest partCopyRequest = UploadPartCopyRequest.builder()
                            .sourceBucket(bucketName)
                            .sourceKey(srcFileKey)
                            .destinationBucket(bucketName)
                            .destinationKey(destFileKey)
                            .uploadId(uploadId)
                            .partNumber(++i)
                            .copySourceRange("bytes=" + progressSize + "-" + (progressSize + partSize - 1))
                            .build();
                    UploadPartCopyResponse partCopyResponse = client.uploadPartCopy(partCopyRequest);

                    partList.add(CompletedPart.builder()
                            .partNumber(i)
                            .eTag(partCopyResponse.copyPartResult().eTag())
                            .build());
                    ProgressListener.quickProgress(pre.getProgressListener(), progressSize += partSize, fileSize);
                }

                CompleteMultipartUploadRequest completeRequest = CompleteMultipartUploadRequest.builder()
                        .bucket(bucketName)
                        .key(destFileKey)
                        .uploadId(uploadId)
                        .multipartUpload(CompletedMultipartUpload.builder()
                                .parts(partList)
                                .build())
                        .build();
                client.completeMultipartUpload(completeRequest);
                ProgressListener.quickFinish(pre.getProgressListener());
            } else { // 小文件复制
                CopyObjectRequest copyFileRequest = CopyObjectRequest.builder()
                        .sourceBucket(bucketName)
                        .sourceKey(srcFileKey)
                        .destinationBucket(bucketName)
                        .destinationKey(destFileKey)
                        .acl(getAcl(destFileInfo.getFileAcl()))
                        .build();
                ProgressListener.quickStart(pre.getProgressListener(), fileSize);
                client.copyObject(copyFileRequest);
                ProgressListener.quickFinish(pre.getProgressListener(), fileSize);
            }
        } catch (Exception e) {
            if (destThFileKey != null) {
                try {
                    client.deleteObject(DeleteObjectRequest.builder()
                            .bucket(bucketName)
                            .key(destThFileKey)
                            .build());
                } catch (Exception ignored) {
                }
            }
            try {
                if (useMultipartCopy) {
                    client.abortMultipartUpload(AbortMultipartUploadRequest.builder()
                            .bucket(bucketName)
                            .key(destFileKey)
                            .uploadId(uploadId)
                            .build());
                } else {
                    client.deleteObject(DeleteObjectRequest.builder()
                            .bucket(bucketName)
                            .key(destFileKey)
                            .build());
                }
            } catch (Exception ignored) {
            }
            throw ExceptionFactory.sameCopy(srcFileInfo, destFileInfo, platform, e);
        }
    }

    @Override
    public boolean isSupportSameMove() {
        return true;
    }

    /**
     * 同存储平台移动文件，在 Amazon S3 中，移动对象并没有专门的 API 操作。故该原理如下：
     * 1、复制对象：使用 CopyObject 操作将对象从源位置复制到目标位置。
     * 2、删除源对象：在成功复制后，删除源对象以完成移动操作。
     * @param srcFileInfo   源对象信息
     * @param destFileInfo  目标对象信息
     * @param pre   移动预处理
     */
    @Override
    public void sameMove(FileInfo srcFileInfo, FileInfo destFileInfo, MovePretreatment pre) {
        Check.sameCopyBasePath(platform, basePath, srcFileInfo, destFileInfo);

        S3Client client = getClient();

        // 获取源文件和目标文件的 Key
        String srcFileKey = getFileKey(srcFileInfo);
        String destFileKey = getFileKey(destFileInfo);

        try {
            // 复制文件到目标位置
            CopyObjectRequest copyRequest = CopyObjectRequest.builder()
                    .sourceBucket(bucketName)
                    .sourceKey(srcFileKey)
                    .destinationBucket(bucketName)
                    .destinationKey(destFileKey)
                    .acl(getAcl(destFileInfo.getFileAcl())) // 设置目标文件的 ACL
                    .build();

            client.copyObject(copyRequest);

            // 设置目标文件的 URL
            destFileInfo.setUrl(domain + destFileKey);

            // 删除源文件
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(srcFileKey)
                    .build();

            client.deleteObject(deleteRequest);

            // 如果源文件有缩略图，处理缩略图
            if (StrUtil.isNotBlank(srcFileInfo.getThFilename())) {
                String srcThFileKey = getThFileKey(srcFileInfo);
                String destThFileKey = getThFileKey(destFileInfo);

                // 复制缩略图
                CopyObjectRequest copyThumbnailRequest = CopyObjectRequest.builder()
                        .sourceBucket(bucketName)
                        .sourceKey(srcThFileKey)
                        .destinationBucket(bucketName)
                        .destinationKey(destThFileKey)
                        .acl(getAcl(destFileInfo.getThFileAcl())) // 设置目标缩略图的 ACL
                        .build();

                client.copyObject(copyThumbnailRequest);

                // 设置目标缩略图的 URL
                destFileInfo.setThUrl(domain + destThFileKey);

                // 删除源缩略图
                DeleteObjectRequest deleteThumbnailRequest = DeleteObjectRequest.builder()
                        .bucket(bucketName)
                        .key(srcThFileKey)
                        .build();

                client.deleteObject(deleteThumbnailRequest);
            }
        } catch (Exception e) {
            throw ExceptionFactory.sameMove(srcFileInfo, destFileInfo, platform, e);
        }
    }
}

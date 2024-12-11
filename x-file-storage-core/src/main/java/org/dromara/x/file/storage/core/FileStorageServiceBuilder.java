package org.dromara.x.file.storage.core;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.extra.ftp.Ftp;
import cn.hutool.extra.ssh.Sftp;
import com.aliyun.oss.OSS;
import com.amazonaws.services.s3.AmazonS3;
import com.baidubce.services.bos.BosClient;
import com.github.sardine.Sardine;
import com.google.cloud.storage.Storage;
import com.obs.services.ObsClient;
import com.qcloud.cos.COSClient;
import com.upyun.RestManager;
import io.minio.MinioClient;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.csource.fastdfs.StorageClient;
import org.dromara.x.file.storage.core.FileStorageProperties.*;
import org.dromara.x.file.storage.core.aspect.FileStorageAspect;
import org.dromara.x.file.storage.core.exception.FileStorageRuntimeException;
import org.dromara.x.file.storage.core.file.*;
import org.dromara.x.file.storage.core.platform.*;
import org.dromara.x.file.storage.core.platform.AzureBlobStorageFileStorageClientFactory.AzureBlobStorageClient;
import org.dromara.x.file.storage.core.platform.MongoGridFsFileStorageClientFactory.MongoGridFsClient;
import org.dromara.x.file.storage.core.platform.QiniuKodoFileStorageClientFactory.QiniuKodoClient;
import org.dromara.x.file.storage.core.recorder.DefaultFileRecorder;
import org.dromara.x.file.storage.core.recorder.FileRecorder;
import org.dromara.x.file.storage.core.tika.ContentTypeDetect;
import org.dromara.x.file.storage.core.tika.DefaultTikaFactory;
import org.dromara.x.file.storage.core.tika.TikaContentTypeDetect;
import org.dromara.x.file.storage.core.tika.TikaFactory;
import org.dromara.x.file.storage.core.util.Tools;
import software.amazon.awssdk.services.s3.S3Client;

@Slf4j
@Getter
@Setter
@Accessors(chain = true)
public class FileStorageServiceBuilder {
    /**
     * 配置参数
     */
    private FileStorageProperties properties;
    /**
     * 文件记录记录者
     */
    private FileRecorder fileRecorder;
    /**
     * Tika 工厂类
     */
    private TikaFactory tikaFactory;
    /**
     * 识别文件的 MIME 类型
     */
    private ContentTypeDetect contentTypeDetect;
    /**
     * 切面
     */
    private List<FileStorageAspect> aspectList = new ArrayList<>();
    /**
     * 文件包装适配器
     */
    private List<FileWrapperAdapter> fileWrapperAdapterList = new ArrayList<>();
    /**
     * Client 工厂
     */
    private List<List<FileStorageClientFactory<?>>> clientFactoryList = new ArrayList<>();
    /**
     * 存储平台
     */
    private List<FileStorage> fileStorageList = new ArrayList<>();

    public FileStorageServiceBuilder(FileStorageProperties properties) {
        this.properties = properties;
    }

    /**
     * 设置默认的文件记录者
     */
    public FileStorageServiceBuilder setDefaultFileRecorder() {
        fileRecorder = new DefaultFileRecorder();
        return this;
    }

    /**
     * 设置默认的 Tika 工厂类
     */
    public FileStorageServiceBuilder setDefaultTikaFactory() {
        tikaFactory = new DefaultTikaFactory();
        return this;
    }

    /**
     * 设置基于 Tika 识别文件的 MIME 类型
     */
    public FileStorageServiceBuilder setTikaContentTypeDetect() {
        if (tikaFactory == null) throw new FileStorageRuntimeException("请先设置 TikaFactory");
        contentTypeDetect = new TikaContentTypeDetect(tikaFactory);
        return this;
    }

    /**
     * 添加切面
     */
    public FileStorageServiceBuilder addAspect(FileStorageAspect aspect) {
        aspectList.add(aspect);
        return this;
    }

    /**
     * 添加文件包装适配器
     */
    public FileStorageServiceBuilder addFileWrapperAdapter(FileWrapperAdapter adapter) {
        fileWrapperAdapterList.add(adapter);
        return this;
    }

    /**
     * 添加 byte[] 文件包装适配器
     */
    public FileStorageServiceBuilder addByteFileWrapperAdapter() {
        if (contentTypeDetect == null) throw new FileStorageRuntimeException("请先设置 TikaFactory 和 ContentTypeDetect");
        fileWrapperAdapterList.add(new ByteFileWrapperAdapter(contentTypeDetect));
        return this;
    }

    /**
     * 添加 InputStream 文件包装适配器
     */
    public FileStorageServiceBuilder addInputStreamFileWrapperAdapter() {
        if (contentTypeDetect == null) throw new FileStorageRuntimeException("请先设置 TikaFactory 和 ContentTypeDetect");
        fileWrapperAdapterList.add(new InputStreamFileWrapperAdapter(contentTypeDetect));
        return this;
    }

    /**
     * 添加本地文件包装适配器
     */
    public FileStorageServiceBuilder addLocalFileWrapperAdapter() {
        if (contentTypeDetect == null) throw new FileStorageRuntimeException("请先设置 TikaFactory 和 ContentTypeDetect");
        fileWrapperAdapterList.add(new LocalFileWrapperAdapter(contentTypeDetect));
        return this;
    }

    /**
     * 添加 URI 文件包装适配器
     */
    public FileStorageServiceBuilder addUriFileWrapperAdapter() {
        if (contentTypeDetect == null) throw new FileStorageRuntimeException("请先设置 TikaFactory 和 ContentTypeDetect");
        fileWrapperAdapterList.add(new UriFileWrapperAdapter(contentTypeDetect));
        return this;
    }

    /**
     * 添加 HttpServletRequest 文件包装适配器
     */
    public FileStorageServiceBuilder addHttpServletRequestFileWrapperAdapter() {
        if (!doesNotExistClass("javax.servlet.http.HttpServletRequest")) {
            fileWrapperAdapterList.add(new JavaxHttpServletRequestFileWrapperAdapter());
        }
        if (!doesNotExistClass("jakarta.servlet.http.HttpServletRequest")) {
            fileWrapperAdapterList.add(new JakartaHttpServletRequestFileWrapperAdapter());
        }
        return this;
    }

    /**
     * 添加全部的文件包装适配器
     */
    public FileStorageServiceBuilder addAllFileWrapperAdapter() {
        addByteFileWrapperAdapter();
        addInputStreamFileWrapperAdapter();
        addLocalFileWrapperAdapter();
        addUriFileWrapperAdapter();
        addHttpServletRequestFileWrapperAdapter();
        return this;
    }

    /**
     * 添加 Client 工厂
     */
    public FileStorageServiceBuilder addFileStorageClientFactory(List<FileStorageClientFactory<?>> list) {
        clientFactoryList.add(list);
        return this;
    }

    /**
     * 添加 Client 工厂
     */
    public FileStorageServiceBuilder addFileStorageClientFactory(FileStorageClientFactory<?> factory) {
        clientFactoryList.add(Collections.singletonList(factory));
        return this;
    }

    /**
     * 添加存储平台
     */
    public FileStorageServiceBuilder addFileStorage(List<? extends FileStorage> storageList) {
        fileStorageList.addAll(storageList);
        return this;
    }

    /**
     * 添加存储平台
     */
    public FileStorageServiceBuilder addFileStorage(FileStorage storage) {
        fileStorageList.add(storage);
        return this;
    }

    /**
     * 使用默认配置
     */
    public FileStorageServiceBuilder useDefault() {
        setDefaultFileRecorder();
        setDefaultTikaFactory();
        setTikaContentTypeDetect();
        addAllFileWrapperAdapter();
        return this;
    }

    /**
     * 创建
     */
    public FileStorageService build() {
        if (properties == null) throw new FileStorageRuntimeException("properties 不能为 null");

        // 初始化各个存储平台
        fileStorageList.addAll(buildLocalFileStorage(properties.getLocal()));
        fileStorageList.addAll(buildLocalPlusFileStorage(properties.getLocalPlus()));
        fileStorageList.addAll(buildHuaweiObsFileStorage(properties.getHuaweiObs(), clientFactoryList));
        fileStorageList.addAll(buildAliyunOssFileStorage(properties.getAliyunOss(), clientFactoryList));
        fileStorageList.addAll(buildQiniuKodoFileStorage(properties.getQiniuKodo(), clientFactoryList));
        fileStorageList.addAll(buildTencentCosFileStorage(properties.getTencentCos(), clientFactoryList));
        fileStorageList.addAll(buildBaiduBosFileStorage(properties.getBaiduBos(), clientFactoryList));
        fileStorageList.addAll(buildUpyunUssFileStorage(properties.getUpyunUss(), clientFactoryList));
        fileStorageList.addAll(buildMinioFileStorage(properties.getMinio(), clientFactoryList));
        fileStorageList.addAll(buildAmazonS3FileStorage(properties.getAmazonS3(), clientFactoryList));
        fileStorageList.addAll(buildAmazonS3V2FileStorage(properties.getAmazonS3V2(), clientFactoryList));
        fileStorageList.addAll(buildFtpFileStorage(properties.getFtp(), clientFactoryList));
        fileStorageList.addAll(buildSftpFileStorage(properties.getSftp(), clientFactoryList));
        fileStorageList.addAll(buildWebDavFileStorage(properties.getWebdav(), clientFactoryList));
        fileStorageList.addAll(
                buildGoogleCloudStorageFileStorage(properties.getGoogleCloudStorage(), clientFactoryList));
        fileStorageList.addAll(buildFastDfsFileStorage(properties.getFastdfs(), clientFactoryList));
        fileStorageList.addAll(buildAzureBlobFileStorage(properties.getAzureBlob(), clientFactoryList));
        fileStorageList.addAll(buildMongoGridFsStorage(properties.getMongoGridFs(), clientFactoryList));
        fileStorageList.addAll(buildGoFastDfsStorage(properties.getGoFastdfs()));

        // 本体
        FileStorageService service = new FileStorageService();
        service.setSelf(service);
        service.setProperties(properties);
        service.setFileStorageList(new CopyOnWriteArrayList<>(fileStorageList));
        service.setFileRecorder(fileRecorder);
        service.setAspectList(new CopyOnWriteArrayList<>(aspectList));
        service.setFileWrapperAdapterList(new CopyOnWriteArrayList<>(fileWrapperAdapterList));
        service.setContentTypeDetect(contentTypeDetect);

        return service;
    }

    /**
     * 创建一个 FileStorageService 的构造器
     */
    public static FileStorageServiceBuilder create(FileStorageProperties properties) {
        return new FileStorageServiceBuilder(properties);
    }

    /**
     * 根据配置文件创建本地文件存储平台
     */
    public static List<LocalFileStorage> buildLocalFileStorage(List<? extends LocalConfig> list) {
        if (CollUtil.isEmpty(list)) return Collections.emptyList();
        return list.stream()
                .map(config -> {
                    log.info("加载本地存储平台：{}，此存储平台已不推荐使用，新项目请使用 本地升级版存储平台（LocalPlusFileStorage）", config.getPlatform());
                    return new LocalFileStorage(config);
                })
                .collect(Collectors.toList());
    }

    /**
     * 根据配置文件创建本地文件升级版存储平台
     */
    public static List<LocalPlusFileStorage> buildLocalPlusFileStorage(List<? extends LocalPlusConfig> list) {
        if (CollUtil.isEmpty(list)) return Collections.emptyList();
        return list.stream()
                .map(config -> {
                    log.info("加载本地升级版存储平台：{}", config.getPlatform());
                    return new LocalPlusFileStorage(config);
                })
                .collect(Collectors.toList());
    }

    /**
     * 根据配置文件创建华为云 OBS 存储平台
     */
    public static List<HuaweiObsFileStorage> buildHuaweiObsFileStorage(
            List<? extends HuaweiObsConfig> list, List<List<FileStorageClientFactory<?>>> clientFactoryList) {
        if (CollUtil.isEmpty(list)) return Collections.emptyList();
        buildFileStorageDetect(list, "华为云 OBS", "com.obs.services.ObsClient");
        return list.stream()
                .map(config -> {
                    log.info("加载华为云 OBS 存储平台：{}", config.getPlatform());
                    FileStorageClientFactory<ObsClient> clientFactory = getFactory(
                            config.getPlatform(),
                            clientFactoryList,
                            () -> new HuaweiObsFileStorageClientFactory(config));
                    return new HuaweiObsFileStorage(config, clientFactory);
                })
                .collect(Collectors.toList());
    }

    /**
     * 根据配置文件创建阿里云 OSS 存储平台
     */
    public static List<AliyunOssFileStorage> buildAliyunOssFileStorage(
            List<? extends AliyunOssConfig> list, List<List<FileStorageClientFactory<?>>> clientFactoryList) {
        if (CollUtil.isEmpty(list)) return Collections.emptyList();
        buildFileStorageDetect(list, "阿里云 OSS", "com.aliyun.oss.OSS");
        return list.stream()
                .map(config -> {
                    log.info("加载阿里云 OSS 存储平台：{}", config.getPlatform());
                    FileStorageClientFactory<OSS> clientFactory = getFactory(
                            config.getPlatform(),
                            clientFactoryList,
                            () -> new AliyunOssFileStorageClientFactory(config));
                    return new AliyunOssFileStorage(config, clientFactory);
                })
                .collect(Collectors.toList());
    }

    /**
     * 根据配置文件创建七牛云 Kodo 存储平台
     */
    public static List<QiniuKodoFileStorage> buildQiniuKodoFileStorage(
            List<? extends QiniuKodoConfig> list, List<List<FileStorageClientFactory<?>>> clientFactoryList) {
        if (CollUtil.isEmpty(list)) return Collections.emptyList();
        buildFileStorageDetect(list, "七牛云 Kodo", "com.qiniu.storage.UploadManager");
        return list.stream()
                .map(config -> {
                    log.info("加载七牛云 Kodo 存储平台：{}", config.getPlatform());
                    FileStorageClientFactory<QiniuKodoClient> clientFactory = getFactory(
                            config.getPlatform(),
                            clientFactoryList,
                            () -> new QiniuKodoFileStorageClientFactory(config));
                    return new QiniuKodoFileStorage(config, clientFactory);
                })
                .collect(Collectors.toList());
    }

    /**
     * 根据配置文件创建腾讯云 COS 存储平台
     */
    public static List<TencentCosFileStorage> buildTencentCosFileStorage(
            List<? extends TencentCosConfig> list, List<List<FileStorageClientFactory<?>>> clientFactoryList) {
        if (CollUtil.isEmpty(list)) return Collections.emptyList();
        buildFileStorageDetect(list, "腾讯云 COS", "com.qcloud.cos.COSClient");
        return list.stream()
                .map(config -> {
                    log.info("加载腾讯云 COS 存储平台：{}", config.getPlatform());
                    FileStorageClientFactory<COSClient> clientFactory = getFactory(
                            config.getPlatform(),
                            clientFactoryList,
                            () -> new TencentCosFileStorageClientFactory(config));
                    return new TencentCosFileStorage(config, clientFactory);
                })
                .collect(Collectors.toList());
    }

    /**
     * 根据配置文件创建百度云 BOS 存储平台
     */
    public static List<BaiduBosFileStorage> buildBaiduBosFileStorage(
            List<? extends BaiduBosConfig> list, List<List<FileStorageClientFactory<?>>> clientFactoryList) {
        if (CollUtil.isEmpty(list)) return Collections.emptyList();
        buildFileStorageDetect(list, "百度云 BOS", "com.baidubce.services.bos.BosClient");
        return list.stream()
                .map(config -> {
                    log.info("加载百度云 BOS 存储平台：{}", config.getPlatform());
                    FileStorageClientFactory<BosClient> clientFactory = getFactory(
                            config.getPlatform(),
                            clientFactoryList,
                            () -> new BaiduBosFileStorageClientFactory(config));
                    return new BaiduBosFileStorage(config, clientFactory);
                })
                .collect(Collectors.toList());
    }

    /**
     * 根据配置文件创建又拍云 USS 存储平台
     */
    public static List<UpyunUssFileStorage> buildUpyunUssFileStorage(
            List<? extends UpyunUssConfig> list, List<List<FileStorageClientFactory<?>>> clientFactoryList) {
        if (CollUtil.isEmpty(list)) return Collections.emptyList();
        buildFileStorageDetect(list, "又拍云 USS", "com.upyun.RestManager");
        return list.stream()
                .map(config -> {
                    log.info("加载又拍云 USS 存储平台：{}", config.getPlatform());
                    FileStorageClientFactory<RestManager> clientFactory = getFactory(
                            config.getPlatform(),
                            clientFactoryList,
                            () -> new UpyunUssFileStorageClientFactory(config));
                    return new UpyunUssFileStorage(config, clientFactory);
                })
                .collect(Collectors.toList());
    }

    /**
     * 根据配置文件创建 MinIO 存储平台
     */
    public static List<MinioFileStorage> buildMinioFileStorage(
            List<? extends MinioConfig> list, List<List<FileStorageClientFactory<?>>> clientFactoryList) {
        if (CollUtil.isEmpty(list)) return Collections.emptyList();
        buildFileStorageDetect(list, "MinIO", "io.minio.MinioClient");
        return list.stream()
                .map(config -> {
                    log.info("加载 MinIO 存储平台：{}", config.getPlatform());
                    FileStorageClientFactory<MinioClient> clientFactory = getFactory(
                            config.getPlatform(), clientFactoryList, () -> new MinioFileStorageClientFactory(config));
                    return new MinioFileStorage(config, clientFactory);
                })
                .collect(Collectors.toList());
    }

    /**
     * 根据配置文件创建又 Amazon S3 存储平台
     */
    public static List<AmazonS3FileStorage> buildAmazonS3FileStorage(
            List<? extends AmazonS3Config> list, List<List<FileStorageClientFactory<?>>> clientFactoryList) {
        if (CollUtil.isEmpty(list)) return Collections.emptyList();
        buildFileStorageDetect(list, "Amazon S3", "com.amazonaws.services.s3.AmazonS3");
        return list.stream()
                .map(config -> {
                    log.info("加载 Amazon S3 存储平台：{}", config.getPlatform());
                    FileStorageClientFactory<AmazonS3> clientFactory = getFactory(
                            config.getPlatform(),
                            clientFactoryList,
                            () -> new AmazonS3FileStorageClientFactory(config));
                    return new AmazonS3FileStorage(config, clientFactory);
                })
                .collect(Collectors.toList());
    }

    /**
     * 根据配置文件创建 Amazon S3 存储平台，使用v2SDK
     */
    public static List<AmazonS3V2FileStorage> buildAmazonS3V2FileStorage(
            List<? extends AmazonS3V2Config> list, List<List<FileStorageClientFactory<?>>> clientFactoryList) {
        if (CollUtil.isEmpty(list)) return Collections.emptyList();
        buildFileStorageDetect(list, "Amazon S3 v2", "software.amazon.awssdk.services.s3.S3Client");
        return list.stream()
                .map(config -> {
                    log.info("加载 Amazon S3 v2 存储平台：{}", config.getPlatform());
                    FileStorageClientFactory<S3Client> clientFactory = getFactory(
                            config.getPlatform(),
                            clientFactoryList,
                            () -> new AmazonS3V2FileStorageClientFactory(config));
                    return new AmazonS3V2FileStorage(config, clientFactory);
                })
                .collect(Collectors.toList());
    }

    /**
     * 根据配置文件创建 FTP 存储平台
     */
    public static List<FtpFileStorage> buildFtpFileStorage(
            List<? extends FtpConfig> list, List<List<FileStorageClientFactory<?>>> clientFactoryList) {
        if (CollUtil.isEmpty(list)) return Collections.emptyList();
        buildFileStorageDetect(
                list,
                "FTP",
                "org.apache.commons.net.ftp.FTPClient",
                "cn.hutool.extra.ftp.Ftp",
                "org.apache.commons.pool2.impl.GenericObjectPool");
        return list.stream()
                .map(config -> {
                    log.info("加载 FTP 存储平台：{}", config.getPlatform());
                    FileStorageClientFactory<Ftp> clientFactory = getFactory(
                            config.getPlatform(), clientFactoryList, () -> new FtpFileStorageClientFactory(config));
                    return new FtpFileStorage(config, clientFactory);
                })
                .collect(Collectors.toList());
    }

    /**
     * 根据配置文件创建 SFTP 存储平台
     */
    public static List<SftpFileStorage> buildSftpFileStorage(
            List<? extends SftpConfig> list, List<List<FileStorageClientFactory<?>>> clientFactoryList) {
        if (CollUtil.isEmpty(list)) return Collections.emptyList();
        buildFileStorageDetect(
                list,
                "SFTP",
                "com.jcraft.jsch.ChannelSftp",
                "cn.hutool.extra.ftp.Ftp",
                "org.apache.commons.pool2.impl.GenericObjectPool");
        return list.stream()
                .map(config -> {
                    log.info("加载 SFTP 存储平台：{}", config.getPlatform());
                    FileStorageClientFactory<Sftp> clientFactory = getFactory(
                            config.getPlatform(), clientFactoryList, () -> new SftpFileStorageClientFactory(config));
                    return new SftpFileStorage(config, clientFactory);
                })
                .collect(Collectors.toList());
    }

    /**
     * 根据配置文件创建 WebDAV 存储平台
     */
    public static List<WebDavFileStorage> buildWebDavFileStorage(
            List<? extends WebDavConfig> list, List<List<FileStorageClientFactory<?>>> clientFactoryList) {
        if (CollUtil.isEmpty(list)) return Collections.emptyList();
        buildFileStorageDetect(list, "WebDAV", "com.github.sardine.Sardine");
        return list.stream()
                .map(config -> {
                    log.info("加载 WebDAV 存储平台：{}", config.getPlatform());
                    FileStorageClientFactory<Sardine> clientFactory = getFactory(
                            config.getPlatform(), clientFactoryList, () -> new WebDavFileStorageClientFactory(config));
                    return new WebDavFileStorage(config, clientFactory);
                })
                .collect(Collectors.toList());
    }

    /**
     * 根据配置文件创建 GoogleCloud Storage 存储平台
     */
    public static List<GoogleCloudStorageFileStorage> buildGoogleCloudStorageFileStorage(
            List<? extends GoogleCloudStorageConfig> list, List<List<FileStorageClientFactory<?>>> clientFactoryList) {
        if (CollUtil.isEmpty(list)) return Collections.emptyList();
        buildFileStorageDetect(list, "GoogleCloud Storage ", "com.google.cloud.storage.Storage");
        return list.stream()
                .map(config -> {
                    log.info("加载 GoogleCloud Storage 存储平台：{}", config.getPlatform());
                    FileStorageClientFactory<Storage> clientFactory = getFactory(
                            config.getPlatform(),
                            clientFactoryList,
                            () -> new GoogleCloudStorageFileStorageClientFactory(config));
                    return new GoogleCloudStorageFileStorage(config, clientFactory);
                })
                .collect(Collectors.toList());
    }

    /**
     * 构建 FastDFS 客户端
     * @param fastdfs FastDFS 配置列表
     * @param clientFactoryList 客户端工厂
     * @return {@link Collection}<{@link ?} {@link extends} {@link FileStorage}>
     */
    private Collection<? extends FileStorage> buildFastDfsFileStorage(
            List<? extends FastDfsConfig> fastdfs, List<List<FileStorageClientFactory<?>>> clientFactoryList) {
        if (CollUtil.isEmpty(fastdfs)) {
            return Collections.emptyList();
        }

        buildFileStorageDetect(fastdfs, "FastDFS", "org.csource.fastdfs.StorageClient");

        return fastdfs.stream()
                .map(config -> {
                    log.info("加载 FastDFS 存储平台：{}", config.getPlatform());
                    FileStorageClientFactory<StorageClient> clientFactory = getFactory(
                            config.getPlatform(), clientFactoryList, () -> new FastDfsFileStorageClientFactory(config));
                    return new FastDfsFileStorage(config, clientFactory);
                })
                .collect(Collectors.toList());
    }

    /**
     * 根据配置文件创建 Azure Blob Storage 存储平台
     */
    public static List<AzureBlobStorageFileStorage> buildAzureBlobFileStorage(
            List<? extends AzureBlobStorageConfig> list, List<List<FileStorageClientFactory<?>>> clientFactoryList) {
        if (CollUtil.isEmpty(list)) return Collections.emptyList();
        buildFileStorageDetect(list, "microsoft azure blob ", "com.azure.storage.blob.BlobServiceClient");
        return list.stream()
                .map(config -> {
                    log.info("加载 microsoft azure blob 存储平台：{}", config.getPlatform());
                    FileStorageClientFactory<AzureBlobStorageClient> clientFactory = getFactory(
                            config.getPlatform(),
                            clientFactoryList,
                            () -> new AzureBlobStorageFileStorageClientFactory(config));
                    return new AzureBlobStorageFileStorage(config, clientFactory);
                })
                .collect(Collectors.toList());
    }

    /**
     * 根据配置文件创建 Mongo GridFS 存储平台
     */
    public static List<MongoGridFsFileStorage> buildMongoGridFsStorage(
            List<? extends MongoGridFsConfig> list, List<List<FileStorageClientFactory<?>>> clientFactoryList) {
        if (CollUtil.isEmpty(list)) return Collections.emptyList();
        buildFileStorageDetect(list, "Mongo GridFS", "com.mongodb.client.MongoClient");
        return list.stream()
                .map(config -> {
                    log.info("加载 Mongo GridFS 存储平台：{}", config.getPlatform());
                    FileStorageClientFactory<MongoGridFsClient> clientFactory = getFactory(
                            config.getPlatform(),
                            clientFactoryList,
                            () -> new MongoGridFsFileStorageClientFactory(config));
                    return new MongoGridFsFileStorage(config, clientFactory);
                })
                .collect(Collectors.toList());
    }

    /**
     * 根据配置文件创建goFastDfs存储平台
     */
    public static List<GoFastDfsFileStorage> buildGoFastDfsStorage(List<? extends GoFastDfsConfig> list) {
        if (CollUtil.isEmpty(list)) return Collections.emptyList();
        return list.stream()
                .map(config -> {
                    log.info("加载GoFastDfs存储平台：{}", config.getPlatform());
                    return new GoFastDfsFileStorage(config);
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取或创建指定存储平台的 Client 工厂对象
     */
    public static <Client> FileStorageClientFactory<Client> getFactory(
            String platform,
            List<List<FileStorageClientFactory<?>>> list,
            Supplier<FileStorageClientFactory<Client>> defaultSupplier) {
        if (list != null) {
            for (List<FileStorageClientFactory<?>> factoryList : list) {
                for (FileStorageClientFactory<?> factory : factoryList) {
                    if (Objects.equals(platform, factory.getPlatform())) {
                        try {
                            return Tools.cast(factory);
                        } catch (Exception e) {
                            throw new FileStorageRuntimeException(
                                    "获取 FileStorageClientFactory 失败，类型不匹配，platform：" + platform, e);
                        }
                    }
                }
            }
        }
        return defaultSupplier.get();
    }

    /**
     * 判断是否没有引入指定 Class
     */
    public static boolean doesNotExistClass(String name) {
        try {
            Class.forName(name);
            return false;
        } catch (ClassNotFoundException e) {
            return true;
        }
    }

    /**
     * 创建存储平台时的依赖检查
     */
    public static void buildFileStorageDetect(List<?> list, String platformName, String... classNames) {
        if (CollUtil.isEmpty(list)) return;
        for (String className : classNames) {
            if (doesNotExistClass(className)) {
                throw new FileStorageRuntimeException(
                        "检测到【" + platformName + "】配置，但是没有找到对应的依赖类：【" + className
                                + "】，所以无法加载此存储平台！配置参考地址：https://x-file-storage.xuyanwu.cn/2.2.1/#/%E5%BF%AB%E9%80%9F%E5%85%A5%E9%97%A8");
            }
        }
    }
}

package org.dromara.x.file.storage.solon;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.dromara.x.file.storage.core.FileStorageProperties;
import org.dromara.x.file.storage.core.FileStorageProperties.*;

/**
 * @author link2fun
 */
@Data
@Accessors(chain = true)
public class SolonFileStorageProperties {

    /**
     * 默认存储平台
     */
    private String defaultPlatform = "local";
    /**
     * 缩略图后缀，例如【.min.jpg】【.png】
     */
    private String thumbnailSuffix = ".min.jpg";
    /**
     * 上传时不支持元数据时抛出异常
     */
    private Boolean uploadNotSupportMetadataThrowException = true;
    /**
     * 上传时不支持 ACL 时抛出异常
     */
    private Boolean uploadNotSupportAclThrowException = true;
    /**
     * 复制时不支持元数据时抛出异常
     */
    private Boolean copyNotSupportMetadataThrowException = true;
    /**
     * 复制时不支持 ACL 时抛出异常
     */
    private Boolean copyNotSupportAclThrowException = true;
    /**
     * 移动时不支持元数据时抛出异常
     */
    private Boolean moveNotSupportMetadataThrowException = true;
    /**
     * 移动时不支持 ACL 时抛出异常
     */
    private Boolean moveNotSupportAclThrowException = true;
    /**
     * 启用 byte[] 文件包装适配器
     */
    private Boolean enableByteFileWrapper = true;
    /**
     * 启用 URI 文件包装适配器，包含 URL 和 String
     */
    private Boolean enableUriFileWrapper = true;
    /**
     * 启用 InputStream 文件包装适配器
     */
    private Boolean enableInputStreamFileWrapper = true;
    /**
     * 启用本地文件包装适配器
     */
    private Boolean enableLocalFileWrapper = true;
    /**
     * 启用 HttpServletRequest 文件包装适配器
     */
    private Boolean enableHttpServletRequestFileWrapper = true;
    /**
     * 启用 MultipartFile 文件包装适配器
     */
    private Boolean enableMultipartFileWrapper = true;
    /**
     * 启用 Solon UploadedFile 文件包装适配器
     */
    private Boolean enableUploadedFileWrapper = true;
    /**
     * 本地存储
     */
    @Deprecated
    private List<? extends SolonLocalConfig> local = new ArrayList<>();
    /**
     * 本地存储
     */
    private List<? extends SolonLocalPlusConfig> localPlus = new ArrayList<>();
    /**
     * 华为云 OBS
     */
    private List<? extends SolonHuaweiObsConfig> huaweiObs = new ArrayList<>();
    /**
     * 阿里云 OSS
     */
    private List<? extends SolonAliyunOssConfig> aliyunOss = new ArrayList<>();
    /**
     * 七牛云 Kodo
     */
    private List<? extends SolonQiniuKodoConfig> qiniuKodo = new ArrayList<>();
    /**
     * 腾讯云 COS
     */
    private List<? extends SolonTencentCosConfig> tencentCos = new ArrayList<>();
    /**
     * 百度云 BOS
     */
    private List<? extends SolonBaiduBosConfig> baiduBos = new ArrayList<>();
    /**
     * 又拍云 USS
     */
    private List<? extends SolonUpyunUssConfig> upyunUss = new ArrayList<>();
    /**
     * MinIO USS
     */
    private List<? extends SolonMinioConfig> minio = new ArrayList<>();

    /**
     * Amazon S3
     */
    private List<? extends SolonAmazonS3Config> amazonS3 = new ArrayList<>();

    /**
     * Amazon S3 V2
     */
    private List<? extends SpringAmazonS3V2Config> amazonS3V2 = new ArrayList<>();

    /**
     * FTP
     */
    private List<? extends SolonFtpConfig> ftp = new ArrayList<>();

    /**
     * FTP
     */
    private List<? extends SolonSftpConfig> sftp = new ArrayList<>();

    /**
     * WebDAV
     */
    private List<? extends SolonWebDavConfig> webdav = new ArrayList<>();

    /**
     * GoogleCloud Storage
     */
    private List<? extends SolonGoogleCloudStorageConfig> googleCloudStorage = new ArrayList<>();

    /**
     * FastDFS
     */
    private List<? extends SolonFastDfsConfig> fastdfs = new ArrayList<>();

    /**
     * Azure Blob Storage
     */
    private List<? extends SolonAzureBlobStorageConfig> azureBlob = new ArrayList<>();

    /**
     * Mongo GridFS
     */
    private List<? extends SolonMongoGridFsConfig> mongoGridFs = new ArrayList<>();

    /**
     * GoFastDFS
     */
    private List<? extends SolonGoFastDfsConfig> goFastdfs = new ArrayList<>();

    /**
     * 转换成 FileStorageProperties ，并过滤掉没有启用的存储平台
     */
    public FileStorageProperties toFileStorageProperties() {
        FileStorageProperties properties = new FileStorageProperties();
        properties.setDefaultPlatform(defaultPlatform);
        properties.setThumbnailSuffix(thumbnailSuffix);
        properties.setUploadNotSupportMetadataThrowException(uploadNotSupportMetadataThrowException);
        properties.setUploadNotSupportAclThrowException(uploadNotSupportAclThrowException);
        properties.setCopyNotSupportMetadataThrowException(copyNotSupportMetadataThrowException);
        properties.setCopyNotSupportAclThrowException(copyNotSupportAclThrowException);
        properties.setMoveNotSupportMetadataThrowException(moveNotSupportMetadataThrowException);
        properties.setMoveNotSupportAclThrowException(moveNotSupportAclThrowException);
        properties.setLocal(
                local.stream().filter(SolonLocalConfig::getEnableStorage).collect(Collectors.toList()));
        properties.setLocalPlus(localPlus.stream()
                .filter(SolonLocalPlusConfig::getEnableStorage)
                .collect(Collectors.toList()));
        properties.setHuaweiObs(huaweiObs.stream()
                .filter(SolonHuaweiObsConfig::getEnableStorage)
                .collect(Collectors.toList()));
        properties.setAliyunOss(aliyunOss.stream()
                .filter(SolonAliyunOssConfig::getEnableStorage)
                .collect(Collectors.toList()));
        properties.setQiniuKodo(qiniuKodo.stream()
                .filter(SolonQiniuKodoConfig::getEnableStorage)
                .collect(Collectors.toList()));
        properties.setTencentCos(tencentCos.stream()
                .filter(SolonTencentCosConfig::getEnableStorage)
                .collect(Collectors.toList()));
        properties.setBaiduBos(
                baiduBos.stream().filter(SolonBaiduBosConfig::getEnableStorage).collect(Collectors.toList()));
        properties.setUpyunUss(
                upyunUss.stream().filter(SolonUpyunUssConfig::getEnableStorage).collect(Collectors.toList()));
        properties.setMinio(
                minio.stream().filter(SolonMinioConfig::getEnableStorage).collect(Collectors.toList()));
        properties.setAmazonS3(
                amazonS3.stream().filter(SpringAmazonS3Config::getEnableStorage).collect(Collectors.toList()));
        properties.setAmazonS3V2(amazonS3V2.stream()
                .filter(SpringAmazonS3V2Config::getEnableStorage)
                .collect(Collectors.toList()));
        properties.setFtp(ftp.stream().filter(SpringFtpConfig::getEnableStorage).collect(Collectors.toList()));
        properties.setSftp(
                sftp.stream().filter(SolonSftpConfig::getEnableStorage).collect(Collectors.toList()));
        properties.setWebdav(
                webdav.stream().filter(SolonWebDavConfig::getEnableStorage).collect(Collectors.toList()));
        properties.setGoogleCloudStorage(googleCloudStorage.stream()
                .filter(SolonGoogleCloudStorageConfig::getEnableStorage)
                .collect(Collectors.toList()));
        properties.setFastdfs(
                fastdfs.stream().filter(SolonFastDfsConfig::getEnableStorage).collect(Collectors.toList()));
        properties.setAzureBlob(azureBlob.stream()
                .filter(SolonAzureBlobStorageConfig::getEnableStorage)
                .collect(Collectors.toList()));
        properties.setMongoGridFs(mongoGridFs.stream()
                .filter(SolonMongoGridFsConfig::getEnableStorage)
                .collect(Collectors.toList()));
        properties.setGoFastdfs(goFastdfs.stream()
                .filter(SolonGoFastDfsConfig::getEnableStorage)
                .collect(Collectors.toList()));

        return properties;
    }

    /**
     * 本地存储
     */
    @Deprecated
    @Data
    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    public static class SolonLocalConfig extends LocalConfig {
        /**
         * 本地存储访问路径
         */
        private String pathPatterns;
        /**
         * 启用本地存储
         */
        private Boolean enableStorage = false;
        /**
         * 启用本地访问
         */
        private Boolean enableAccess = false;
    }

    /**
     * 本地存储升级版
     */
    @Data
    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    public static class SolonLocalPlusConfig extends LocalPlusConfig {
        /**
         * 本地存储访问路径
         */
        private String pathPatterns;
        /**
         * 启用本地存储
         */
        private Boolean enableStorage = false;
        /**
         * 启用本地访问
         */
        private Boolean enableAccess = false;
    }

    /**
     * 华为云 OBS
     */
    @Data
    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    public static class SolonHuaweiObsConfig extends HuaweiObsConfig {
        /**
         * 启用存储
         */
        private Boolean enableStorage = false;
    }

    /**
     * 阿里云 OSS
     */
    @Data
    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    public static class SolonAliyunOssConfig extends AliyunOssConfig {
        /**
         * 启用存储
         */
        private Boolean enableStorage = false;
    }

    /**
     * 七牛云 Kodo
     */
    @Data
    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    public static class SolonQiniuKodoConfig extends QiniuKodoConfig {
        /**
         * 启用存储
         */
        private Boolean enableStorage = false;
    }

    /**
     * 腾讯云 COS
     */
    @Data
    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    public static class SolonTencentCosConfig extends TencentCosConfig {
        /**
         * 启用存储
         */
        private Boolean enableStorage = false;
    }

    /**
     * 百度云 BOS
     */
    @Data
    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    public static class SolonBaiduBosConfig extends BaiduBosConfig {
        /**
         * 启用存储
         */
        private Boolean enableStorage = false;
    }

    /**
     * 又拍云 USS
     */
    @Data
    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    public static class SolonUpyunUssConfig extends UpyunUssConfig {
        /**
         * 启用存储
         */
        private Boolean enableStorage = false;
    }

    /**
     * MinIO
     */
    @Data
    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    public static class SolonMinioConfig extends MinioConfig {
        /**
         * 启用存储
         */
        private Boolean enableStorage = false;
    }

    /**
     * Amazon S3
     */
    @Data
    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    public static class SolonAmazonS3Config extends AmazonS3Config {
        /**
         * 启用存储
         */
        private Boolean enableStorage = false;
    }

    /**
     * Amazon S3 V2
     */
    @Data
    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    public static class SpringAmazonS3V2Config extends AmazonS3V2Config {
        /**
         * 启用存储
         */
        private Boolean enableStorage = false;
    }

    /**
     * FTP
     */
    @Data
    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    public static class SolonFtpConfig extends FtpConfig {
        /**
         * 启用存储
         */
        private Boolean enableStorage = false;
    }

    /**
     * SFTP
     */
    @Data
    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    public static class SolonSftpConfig extends SftpConfig {
        /**
         * 启用存储
         */
        private Boolean enableStorage = false;
    }

    /**
     * WebDAV
     */
    @Data
    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    public static class SolonWebDavConfig extends WebDavConfig {
        /**
         * 启用存储
         */
        private Boolean enableStorage = false;
    }

    /**
     * GoogleCloud Storage
     */
    @Data
    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    public static class SolonGoogleCloudStorageConfig extends GoogleCloudStorageConfig {
        /**
         * 启用存储
         */
        private Boolean enableStorage = false;
    }

    /**
     * FastDFS Storage
     *
     * @author XS <wanghaiqi@beeplay123.com>
     * @date 2023/10/23
     */
    @Data
    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    public static class SolonFastDfsConfig extends FastDfsConfig {
        /**
         * 启用存储
         */
        private Boolean enableStorage = false;
    }

    /**
     * AzureBlob Storage
     */
    @Data
    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    public static class SolonAzureBlobStorageConfig extends AzureBlobStorageConfig {
        /**
         * 启用存储
         */
        private Boolean enableStorage = false;
    }

    /**
     * Mongo GridFS
     */
    @Data
    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    public static class SolonMongoGridFsConfig extends MongoGridFsConfig {
        /**
         * 启用存储
         */
        private Boolean enableStorage = false;
    }

    /**
     * GoFastDFS
     */
    @Data
    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    public static class SolonGoFastDfsConfig extends GoFastDfsConfig {
        /**
         * 启用存储
         */
        private Boolean enableStorage = false;
    }
}

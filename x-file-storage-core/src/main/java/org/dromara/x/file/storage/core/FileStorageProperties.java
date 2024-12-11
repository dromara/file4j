package org.dromara.x.file.storage.core;

import cn.hutool.core.map.MapBuilder;
import cn.hutool.core.util.StrUtil;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.dromara.x.file.storage.core.constant.Constant;

@Data
@Accessors(chain = true)
public class FileStorageProperties {

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
     * 本地存储
     */
    private List<? extends LocalConfig> local = new ArrayList<>();

    /**
     * 本地存储
     */
    private List<? extends LocalPlusConfig> localPlus = new ArrayList<>();

    /**
     * 华为云 OBS
     */
    private List<? extends HuaweiObsConfig> huaweiObs = new ArrayList<>();

    /**
     * 阿里云 OSS
     */
    private List<? extends AliyunOssConfig> aliyunOss = new ArrayList<>();

    /**
     * 七牛云 Kodo
     */
    private List<? extends QiniuKodoConfig> qiniuKodo = new ArrayList<>();

    /**
     * 腾讯云 COS
     */
    private List<? extends TencentCosConfig> tencentCos = new ArrayList<>();

    /**
     * 百度云 BOS
     */
    private List<? extends BaiduBosConfig> baiduBos = new ArrayList<>();

    /**
     * 又拍云 USS
     */
    private List<? extends UpyunUssConfig> upyunUss = new ArrayList<>();

    /**
     * MinIO USS
     */
    private List<? extends MinioConfig> minio = new ArrayList<>();

    /**
     * Amazon S3
     */
    private List<? extends AmazonS3Config> amazonS3 = new ArrayList<>();

    /**
     * Amazon S3 V2
     */
    private List<? extends AmazonS3V2Config> amazonS3V2 = new ArrayList<>();

    /**
     * FTP
     */
    private List<? extends FtpConfig> ftp = new ArrayList<>();

    /**
     * FTP
     */
    private List<? extends SftpConfig> sftp = new ArrayList<>();

    /**
     * WebDAV
     */
    private List<? extends WebDavConfig> webdav = new ArrayList<>();

    /**
     * 谷歌云存储
     */
    private List<? extends GoogleCloudStorageConfig> googleCloudStorage = new ArrayList<>();

    /**
     * FastDFS
     */
    private List<? extends FastDfsConfig> fastdfs = new ArrayList<>();

    /**
     * Azure Blob Storage
     */
    private List<? extends AzureBlobStorageConfig> azureBlob = new ArrayList<>();

    /**
     * Mongo GridFS
     */
    private List<? extends MongoGridFsConfig> mongoGridFs = new ArrayList<>();

    /**
     * GoFastDFS
     */
    private List<? extends GoFastDfsConfig> goFastdfs = new ArrayList<>();

    /**
     * 基本的存储平台配置
     */
    @Data
    @Accessors(chain = true)
    public static class BaseConfig {

        /**
         * 存储平台
         */
        private String platform = "";
    }

    /**
     * 本地存储
     */
    @Deprecated
    @Data
    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    public static class LocalConfig extends BaseConfig {

        /**
         * 本地存储路径
         */
        private String basePath = "";

        /**
         * 访问域名
         */
        private String domain = "";

        /**
         * 其它自定义配置
         */
        private Map<String, Object> attr = new LinkedHashMap<>();
    }

    /**
     * 本地存储升级版
     */
    @Data
    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    public static class LocalPlusConfig extends BaseConfig {

        /**
         * 基础路径
         */
        private String basePath = "";

        /**
         * 存储路径，上传的文件都会存储在这个路径下面，默认“/”，注意“/”结尾
         */
        private String storagePath = "/";

        /**
         * 访问域名
         */
        private String domain = "";

        /**
         * 其它自定义配置
         */
        private Map<String, Object> attr = new LinkedHashMap<>();
    }

    /**
     * 华为云 OBS
     */
    @Data
    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    public static class HuaweiObsConfig extends BaseConfig {

        private String accessKey;

        private String secretKey;

        private String endPoint;

        private String bucketName;

        /**
         * 访问域名
         */
        private String domain = "";

        /**
         * 基础路径
         */
        private String basePath = "";

        /**
         * 默认的 ACL，详情 {@link Constant.HuaweiObsACL}
         */
        private String defaultAcl;

        /**
         * 自动分片上传阈值，达到此大小则使用分片上传，默认 128MB
         */
        private int multipartThreshold = 128 * 1024 * 1024;

        /**
         * 自动分片上传时每个分片大小，默认 32MB
         */
        private int multipartPartSize = 32 * 1024 * 1024;

        /**
         * 其它自定义配置
         */
        private Map<String, Object> attr = new LinkedHashMap<>();
    }

    /**
     * 阿里云 OSS
     */
    @Data
    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    public static class AliyunOssConfig extends BaseConfig {

        private String accessKey;

        private String secretKey;

        private String endPoint;

        private String bucketName;

        /**
         * 访问域名
         */
        private String domain = "";

        /**
         * 基础路径
         */
        private String basePath = "";

        /**
         * 默认的 ACL，详情 {@link Constant.AliyunOssACL}
         */
        private String defaultAcl;

        /**
         * 自动分片上传阈值，达到此大小则使用分片上传，默认 128MB
         */
        private int multipartThreshold = 128 * 1024 * 1024;

        /**
         * 自动分片上传时每个分片大小，默认 32MB
         */
        private int multipartPartSize = 32 * 1024 * 1024;

        /**
         * 其它自定义配置
         */
        private Map<String, Object> attr = new LinkedHashMap<>();
    }

    /**
     * 七牛云 Kodo
     */
    @Data
    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    public static class QiniuKodoConfig extends BaseConfig {

        private String accessKey;

        private String secretKey;

        private String bucketName;

        /**
         * 访问域名
         */
        private String domain = "";

        /**
         * 基础路径
         */
        private String basePath = "";

        /**
         * 其它自定义配置
         */
        private Map<String, Object> attr = new LinkedHashMap<>();
    }

    /**
     * 腾讯云 COS
     */
    @Data
    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    public static class TencentCosConfig extends BaseConfig {

        private String secretId;

        private String secretKey;

        private String region;

        private String bucketName;

        /**
         * 访问域名
         */
        private String domain = "";

        /**
         * 基础路径
         */
        private String basePath = "";

        /**
         * 默认的 ACL，详情 {@link Constant.TencentCosACL}
         */
        private String defaultAcl;

        /**
         * 自动分片上传阈值，达到此大小则使用分片上传，默认 128MB
         */
        private int multipartThreshold = 128 * 1024 * 1024;

        /**
         * 自动分片上传时每个分片大小，默认 32MB
         */
        private int multipartPartSize = 32 * 1024 * 1024;

        /**
         * 其它自定义配置
         */
        private Map<String, Object> attr = new LinkedHashMap<>();
    }

    /**
     * 百度云 BOS
     */
    @Data
    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    public static class BaiduBosConfig extends BaseConfig {

        private String accessKey;

        private String secretKey;

        private String endPoint;

        private String bucketName;

        /**
         * 访问域名
         */
        private String domain = "";

        /**
         * 基础路径
         */
        private String basePath = "";

        /**
         * 默认的 ACL，详情 {@link Constant.BaiduBosACL}
         */
        private String defaultAcl;

        /**
         * 自动分片上传阈值，达到此大小则使用分片上传，默认 128MB
         */
        private int multipartThreshold = 128 * 1024 * 1024;

        /**
         * 自动分片上传时每个分片大小，默认 32MB
         */
        private int multipartPartSize = 32 * 1024 * 1024;

        /**
         * 其它自定义配置
         */
        private Map<String, Object> attr = new LinkedHashMap<>();
    }

    /**
     * 又拍云 USS
     */
    @Data
    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    public static class UpyunUssConfig extends BaseConfig {

        private String username;

        private String password;

        private String bucketName;

        /**
         * 访问域名
         */
        private String domain = "";

        /**
         * 基础路径
         */
        private String basePath = "";

        /**
         * 手动分片上传时，每个分片大小，单位字节，最小 1MB，最大 50MB，必须是 1MB 的整数倍，默认 1MB。
         * 又拍云 USS 比较特殊，必须提前传入分片大小（最后一个分片可以小于此大小，但不能超过）
         * 你可以在初始化文件时使用 putMetadata("X-Upyun-Multi-Part-Size", "1048576") 方法传入分片大小
         */
        private Integer multipartUploadPartSize = 1024 * 1024;

        /**
         * 其它自定义配置
         */
        private Map<String, Object> attr = new LinkedHashMap<>();
    }

    /**
     * MinIO
     */
    @Data
    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    public static class MinioConfig extends BaseConfig {

        private String accessKey;

        private String secretKey;

        private String endPoint;

        private String bucketName;

        /**
         * 访问域名
         */
        private String domain = "";

        /**
         * 基础路径
         */
        private String basePath = "";

        /**
         * 自动分片上传阈值，达到此大小则使用分片上传，默认 128MB。
         * 在获取不到文件大小或达到这个阈值的情况下，会使用这里提供的分片大小，否则 MinIO 会自动分片大小
         */
        private int multipartThreshold = 128 * 1024 * 1024;
        /**
         * 自动分片上传时每个分片大小，默认 32MB
         */
        private int multipartPartSize = 32 * 1024 * 1024;
        /**
         * 其它自定义配置
         */
        private Map<String, Object> attr = new LinkedHashMap<>();
    }

    /**
     * Amazon S3
     */
    @Data
    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    public static class AmazonS3Config extends BaseConfig {

        private String accessKey;

        private String secretKey;

        private String region;

        private String endPoint;

        private String bucketName;

        /**
         * 访问域名
         */
        private String domain = "";

        /**
         * 基础路径
         */
        private String basePath = "";

        /**
         * 默认的 ACL，详情 {@link Constant.AwsS3ACL}
         */
        private String defaultAcl;

        /**
         * 自动分片上传阈值，达到此大小则使用分片上传，默认 128MB
         */
        private int multipartThreshold = 128 * 1024 * 1024;

        /**
         * 自动分片上传时每个分片大小，默认 32MB
         */
        private int multipartPartSize = 32 * 1024 * 1024;

        /**
         * 其它自定义配置
         */
        private Map<String, Object> attr = new LinkedHashMap<>();
    }

    /**
     * Amazon S3 V2
     */
    @Data
    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    public static class AmazonS3V2Config extends BaseConfig {

        private String accessKey;

        private String secretKey;

        private String region;

        private String endPoint;

        private String bucketName;

        /**
         * 访问域名
         */
        private String domain = "";

        /**
         * 基础路径
         */
        private String basePath = "";

        /**
         * 默认的 ACL，详情 {@link Constant.AwsS3ACL}
         */
        private String defaultAcl;

        /**
         * 自动分片上传阈值，达到此大小则使用分片上传，默认 128MB
         */
        private int multipartThreshold = 128 * 1024 * 1024;

        /**
         * 自动分片上传时每个分片大小，默认 32MB
         */
        private int multipartPartSize = 32 * 1024 * 1024;

        /**
         * 其它自定义配置
         */
        private Map<String, Object> attr = new LinkedHashMap<>();
    }

    /**
     * FTP
     */
    @Data
    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    public static class FtpConfig extends BaseConfig {

        /**
         * 主机
         */
        private String host;

        /**
         * 端口，默认21
         */
        private int port = 21;

        /**
         * 用户名，默认 anonymous（匿名）
         */
        private String user = "anonymous";

        /**
         * 密码，默认空
         */
        private String password = "";

        /**
         * 编码，默认UTF-8
         */
        private Charset charset = StandardCharsets.UTF_8;

        /**
         * 连接超时时长，单位毫秒，默认10秒 {@link org.apache.commons.net.SocketClient#setConnectTimeout(int)}
         */
        private long connectionTimeout = 10 * 1000;

        /**
         * Socket连接超时时长，单位毫秒，默认10秒 {@link org.apache.commons.net.SocketClient#setSoTimeout(int)}
         */
        private long soTimeout = 10 * 1000;

        /**
         * 设置服务器语言，默认空，{@link org.apache.commons.net.ftp.FTPClientConfig#setServerLanguageCode(String)}
         */
        private String serverLanguageCode;

        /**
         * 服务器标识，默认空，{@link org.apache.commons.net.ftp.FTPClientConfig#FTPClientConfig(String)}
         * 例如：org.apache.commons.net.ftp.FTPClientConfig.SYST_NT
         */
        private String systemKey;

        /**
         * 是否主动模式，默认被动模式
         */
        private Boolean isActive = false;

        /**
         * 访问域名
         */
        private String domain = "";

        /**
         * 基础路径
         */
        private String basePath = "";

        /**
         * 存储路径，上传的文件都会存储在这个路径下面，默认“/”，注意“/”结尾
         */
        private String storagePath = "/";

        /**
         * Client 对象池配置
         */
        private CommonClientPoolConfig pool = new CommonClientPoolConfig();

        /**
         * 其它自定义配置
         */
        private Map<String, Object> attr = new LinkedHashMap<>();
    }

    /**
     * SFTP
     */
    @Data
    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    public static class SftpConfig extends BaseConfig {

        /**
         * 主机
         */
        private String host;

        /**
         * 端口，默认22
         */
        private int port = 22;

        /**
         * 用户名
         */
        private String user;

        /**
         * 密码
         */
        private String password;

        /**
         * 私钥路径
         */
        private String privateKeyPath;

        /**
         * 编码，默认UTF-8
         */
        private Charset charset = StandardCharsets.UTF_8;

        /**
         * 连接超时时长，单位毫秒，默认10秒
         */
        private int connectionTimeout = 10 * 1000;

        /**
         * 访问域名
         */
        private String domain = "";

        /**
         * 基础路径
         */
        private String basePath = "";

        /**
         * 存储路径，上传的文件都会存储在这个路径下面，默认“/”，注意“/”结尾
         */
        private String storagePath = "/";

        /**
         * Client 对象池配置
         */
        private CommonClientPoolConfig pool = new CommonClientPoolConfig();

        /**
         * 其它自定义配置
         */
        private Map<String, Object> attr = new LinkedHashMap<>();
    }

    /**
     * WebDAV
     */
    @Data
    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    public static class WebDavConfig extends BaseConfig {

        /**
         * 服务器地址，注意“/”结尾，例如：http://192.168.1.105:8405/
         */
        private String server;

        /**
         * 用户名
         */
        private String user;

        /**
         * 密码
         */
        private String password;

        /**
         * 访问域名
         */
        private String domain = "";

        /**
         * 基础路径
         */
        private String basePath = "";

        /**
         * 存储路径，上传的文件都会存储在这个路径下面，默认“/”，注意“/”结尾
         */
        private String storagePath = "/";

        /**
         * 其它自定义配置
         */
        private Map<String, Object> attr = new LinkedHashMap<>();
    }

    @Data
    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    public static class GoogleCloudStorageConfig extends BaseConfig {

        private String projectId;

        /**
         * 证书路径，兼容Spring的ClassPath路径、文件路径、HTTP路径等
         */
        private String credentialsPath;

        private String bucketName;

        /**
         * 访问域名
         */
        private String domain = "";

        /**
         * 基础路径
         */
        private String basePath = "";

        /**
         * 默认的 ACL，详情 {@link Constant.GoogleCloudStorageACL}
         */
        private String defaultAcl;

        /**
         * 其它自定义配置
         */
        private Map<String, Object> attr = new LinkedHashMap<>();
    }

    /**
     * FastDFS
     * 兼容性说明：https://x-file-storage.xuyanwu.cn/2.2.1/#/%E5%AD%98%E5%82%A8%E5%B9%B3%E5%8F%B0?id=OCI_FastDFS
     */
    @Data
    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    public static class FastDfsConfig extends BaseConfig {
        /**
         * 运行模式，由于 FastDFS 比较特殊，不支持自定义文件名及路径，所以使用运行模式来解决这个问题。
         * 详情请查看：https://x-file-storage.xuyanwu.cn/2.2.1/#/%E5%AD%98%E5%82%A8%E5%B9%B3%E5%8F%B0?id=OCI_FastDFS
         */
        private RunMod runMod = RunMod.COVER;

        /**
         * Tracker Server 配置
         */
        private FastDfsTrackerServer trackerServer;

        /**
         * Storage Server 配置（当不使用 Tracker Server 时使用）
         */
        private FastDfsStorageServer storageServer;

        /**
         * 额外扩展配置
         */
        private FastDfsExtra extra;

        /**
         * 访问域名
         */
        private String domain = "";

        /**
         * 基础路径，强烈建议留空
         * 仅在上传成功时和获取文件时原样传到 FileInfo 及 RemoteFileInfo 中，可以用来保存到数据库中使用，
         * 实际上作用也不大，还会破坏 url 约定（url：实际上就是 domain + basePath + path + filename），
         * 约定详情见文档 https://x-file-storage.xuyanwu.cn/2.2.1/#/%E5%B8%B8%E8%A7%81%E9%97%AE%E9%A2%98?id=%E9%85%8D%E7%BD%AE%E6%96%87%E4%BB%B6%E5%8F%8A-fileinfo-%E4%B8%AD%E5%90%84%E7%A7%8D%E8%B7%AF%E5%BE%84%EF%BC%88path%EF%BC%89%E7%9A%84%E5%8C%BA%E5%88%AB%EF%BC%9F
         * FastDFS 兼容性说明：https://x-file-storage.xuyanwu.cn/2.2.1/#/%E5%AD%98%E5%82%A8%E5%B9%B3%E5%8F%B0?id=OCI_FastDFS
         */
        private String basePath = "";

        /**
         * 自动分片上传阈值，达到此大小则使用分片上传，默认 128MB
         */
        private int multipartThreshold = 128 * 1024 * 1024;

        /**
         * 自动分片上传时每个分片大小，默认 32MB
         */
        private int multipartPartSize = 32 * 1024 * 1024;

        /**
         * 其它自定义配置
         */
        private Map<String, Object> attr = new LinkedHashMap<>();

        public String getGroupName() {
            return Optional.ofNullable(extra).map(FastDfsExtra::getGroupName).orElse(StrUtil.EMPTY);
        }

        /**
         * 运行模式
         */
        public enum RunMod {
            /**
             * 覆盖模式，强制用 FastDFS 返回的路径及文件名覆盖 FileInfo 中的 path 及 filename。
             * 详情请查看：https://x-file-storage.xuyanwu.cn/2.2.1/#/%E5%AD%98%E5%82%A8%E5%B9%B3%E5%8F%B0?id=OCI_FastDFS
             */
            COVER,
            /**
             * URL模式，不覆盖 FileInfo 中的 path 及 filename。通过 url 解析 FastDFS 支持的路径及文件名
             * 详情请查看：https://x-file-storage.xuyanwu.cn/2.2.1/#/%E5%AD%98%E5%82%A8%E5%B9%B3%E5%8F%B0?id=OCI_FastDFS
             */
            URL;
        }

        @Data
        @Accessors(chain = true)
        @EqualsAndHashCode
        public static class FastDfsTrackerServer {

            /**
             * Tracker Server 地址（IP:PORT），多个用英文逗号隔开
             */
            private String serverAddr;

            /**
             * HTTP端口，默认：80
             */
            private Integer httpPort = 80;
        }

        @Data
        @Accessors(chain = true)
        @EqualsAndHashCode
        public static class FastDfsStorageServer {

            /**
             * Storage Server 地址:IP:PORT
             */
            private String serverAddr;

            /**
             * Store path，默认 0
             */
            private Integer storePath = 0;
        }

        @Data
        @Accessors(chain = true)
        @EqualsAndHashCode
        public static class FastDfsExtra {

            /**
             * 组名，可以为空
             */
            private String groupName = "";

            /**
             * 连接超时，单位：秒，默认：5s
             */
            private Integer connectTimeoutInSeconds = 5;

            /**
             * 套接字超时，单位：秒，默认：30s
             */
            private Integer networkTimeoutInSeconds = 30;

            /**
             * 字符编码，默认：UTF-8
             */
            private Charset charset = StandardCharsets.UTF_8;

            /**
             * token 防盗链 默认：false
             */
            private Boolean httpAntiStealToken = false;

            /**
             * 安全密钥，默认：FastDFS1234567890
             */
            private String httpSecretKey = "FastDFS1234567890";

            /**
             * 是否启用连接池。默认：true
             */
            private Boolean connectionPoolEnabled = true;

            /**
             * #每一个IP:Port的最大连接数，0为没有限制，默认：100
             */
            private Integer connectionPoolMaxCountPerEntry = 100;

            /**
             * 连接池最大空闲时间。单位：秒，默认：3600
             */
            private Integer connectionPoolMaxIdleTime = 3600;

            /**
             * 连接池最大等待时间。单位：毫秒，默认：1000
             */
            private Integer connectionPoolMaxWaitTimeInMs = 1000;
        }
    }

    @Data
    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    public static class AzureBlobStorageConfig extends BaseConfig {

        /**
         * 终结点 AzureBlob控制台-设置-终结点-主终结点-Blob服务
         */
        private String endPoint;

        /**
         * 访问域名，注意“/”结尾，与 end-point 保持一致
         */
        private String domain = "";

        /**
         * 容器名称，类似于 s3 的 bucketName，AzureBlob控制台-数据存储-容器
         */
        private String containerName;

        /**
         * 基础路径
         */
        private String basePath = "";

        /**
         * 默认的 ACL，详情 {@link Constant.AzureBlobStorageACL}
         */
        private String defaultAcl;

        /**
         * 连接字符串，AzureBlob控制台-安全性和网络-访问秘钥-连接字符串
         */
        private String connectionString;

        /**
         * 自动分片上传阈值，超过此大小则使用分片上传，默认值256M
         */
        private long multipartThreshold = 256 * 1024 * 1024L;
        /**
         * 自动分片上传时每个分片大小，默认 4MB
         */
        private long multipartPartSize = 4 * 1024 * 1024L;

        /**
         * 最大上传并行度
         * 分片后 同时进行上传的 数量
         * 数量太大会占用大量缓冲区
         * 默认 8
         */
        private int maxConcurrency = 8;

        /**
         * 预签名 URL 时，传入的 HTTP method 与 Azure Blob Storage 中的 SAS 权限映射表，
         * 目前默认支持 GET （获取），PUT（上传），DELETE（删除），
         * 其它可以自行扩展，例如你想自定义一个 ALL 的 method，赋予所有权限，可以写为 .put("ALL", "racwdxytlmei")
         * {@link com.azure.storage.blob.sas.BlobSasPermission}
         */
        private Map<String, String> methodToPermissionMap = MapBuilder.create(new HashMap<String, String>())
                .put(Constant.GeneratePresignedUrl.Method.GET, "r") // 获取
                .put(Constant.GeneratePresignedUrl.Method.PUT, "w") // 上传
                .put(Constant.GeneratePresignedUrl.Method.DELETE, "d") // 删除
                // .put("ALL", "racwdxytlmei")    //自定义一个名为 ALL 的 method，赋予所有权限
                .build();

        /**
         * 其它自定义配置
         */
        private Map<String, Object> attr = new LinkedHashMap<>();
    }

    /**
     * Mongo GridFS
     */
    @Data
    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    public static class MongoGridFsConfig extends BaseConfig {
        /**
         * 链接字符串
         */
        private String connectionString;
        /**
         * 数据库名称
         */
        private String database;
        /**
         * 存储桶名称
         */
        private String bucketName;
        /**
         * 访问域名
         */
        private String domain = "";
        /**
         * 基础路径
         */
        private String basePath = "";
        /**
         * 其它自定义配置
         */
        private Map<String, Object> attr = new LinkedHashMap<>();
    }

    /**
     * GoFastDFS
     */
    @Data
    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    public static class GoFastDfsConfig extends BaseConfig {

        /**
         * http://172.24.5.163:8080
         */
        private String server;

        /**
         * 服务器组名
         */
        private String group;

        /**
         * 服务器场景
         */
        private String scene;

        /**
         * 超时时间
         */
        private Integer timeOut;

        /**
         * domain
         */
        private String domain;

        /**
         * 上传时候base路径
         */
        private String basePath;
        /**
         * 其它自定义配置
         */
        private Map<String, Object> attr = new LinkedHashMap<>();
    }

    /**
     * 通用的 Client 对象池配置，详情见 {@link org.apache.commons.pool2.impl.GenericObjectPoolConfig}
     */
    @Data
    @Accessors(chain = true)
    public static class CommonClientPoolConfig {

        /**
         * 取出对象前进行校验，默认开启
         */
        private Boolean testOnBorrow = true;

        /**
         * 空闲检测，默认开启
         */
        private Boolean testWhileIdle = true;

        /**
         * 最大总数量，超过此数量会进行阻塞等待，默认 16
         */
        private Integer maxTotal = 16;

        /**
         * 最大空闲数量，默认 4
         */
        private Integer maxIdle = 4;

        /**
         * 最小空闲数量，默认 1
         */
        private Integer minIdle = 1;

        /**
         * 空闲对象逐出（销毁）运行间隔时间，默认 30 秒
         */
        private Duration timeBetweenEvictionRuns = Duration.ofSeconds(30);

        /**
         * 对象空闲超过此时间将逐出（销毁），为负数则关闭此功能，默认 -1
         */
        private Duration minEvictableIdleDuration = Duration.ofMillis(-1);

        /**
         * 对象空闲超过此时间且当前对象池的空闲对象数大于最小空闲数量，将逐出（销毁），为负数则关闭此功能，默认 30 分钟
         */
        private Duration softMinEvictableIdleDuration = Duration.ofMillis(30);

        public <T> GenericObjectPoolConfig<T> toGenericObjectPoolConfig() {
            GenericObjectPoolConfig<T> config = new GenericObjectPoolConfig<>();
            config.setTestOnBorrow(testOnBorrow);
            config.setTestWhileIdle(testWhileIdle);
            config.setMaxTotal(maxTotal);
            config.setMinIdle(minIdle);
            config.setMaxIdle(maxIdle);
            config.setTimeBetweenEvictionRuns(timeBetweenEvictionRuns);
            config.setMinEvictableIdleTime(minEvictableIdleDuration);
            config.setSoftMinEvictableIdleTime(softMinEvictableIdleDuration);
            return config;
        }
    }
}

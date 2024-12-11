package org.dromara.x.file.storage.core.platform;

import cn.hutool.core.util.StrUtil;
import java.net.URI;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.dromara.x.file.storage.core.FileStorageProperties.AmazonS3V2Config;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

/**
 * Amazon S3 存储平台的 Client 工厂<br/>
 * 适用于AWS SDK for Java 2.x，根据Amazon公告，AWS SDK for Java 1.x 自 2024 年 7 月 31 日起将进入维护模式，并于2025 年 12 月 31 日终止支持<br/>
 * 公告链接地址：<a href="https://aws.amazon.com/blogs/developer/the-aws-sdk-for-java-1-x-is-in-maintenance-mode-effective-july-31-2024/">The AWS SDK for Java 1.x is in maintenance mode, effective July 31, 2024</a>
 * @author zhangxin
 * @date 2024-12-08
 */
@Getter
@Setter
@NoArgsConstructor
public class AmazonS3V2FileStorageClientFactory implements FileStorageClientFactory<S3Client> {
    private String platform;
    private String accessKey;
    private String secretKey;
    private String region;
    private String endPoint;
    private volatile S3Client client;

    public AmazonS3V2FileStorageClientFactory(AmazonS3V2Config config) {
        platform = config.getPlatform();
        accessKey = config.getAccessKey();
        secretKey = config.getSecretKey();
        region = config.getRegion();
        endPoint = config.getEndPoint();
    }

    @Override
    public S3Client getClient() {
        if (client == null) {
            synchronized (this) {
                if (client == null) {
                    S3ClientBuilder builder = S3Client.builder()
                            .credentialsProvider(
                                    StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                            .region(Region.of(region));
                    if (StrUtil.isNotBlank(endPoint)) {
                        builder.endpointOverride(URI.create(endPoint));
                    }
                    client = builder.build();
                }
            }
        }
        return client;
    }

    @Override
    public void close() {
        if (client != null) {
            client.close();
            client = null;
        }
    }
}

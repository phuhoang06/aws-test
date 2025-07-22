

package com.mm.image_aws.config;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.transfer.s3.S3TransferManager;

@Configuration
@EnableConfigurationProperties(AwsProperties.class)
public class AppConfig {

    @Bean
    public CloseableHttpClient httpClient() {
        return HttpClientBuilder.create()
                .setUserAgent("Mozilla/5.0")
                .setMaxConnTotal(200)
                .setMaxConnPerRoute(100)
                .build();
    }

    @Bean
    public S3AsyncClient s3AsyncClient(AwsProperties awsProperties) {
        // Sử dụng trình xây dựng CRT để có hiệu suất tối ưu.
        // Lưu ý: Cần thêm dependency 'software.amazon.awssdk.crt:aws-crt' vào project.
        return S3AsyncClient.crtBuilder()
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(awsProperties.getAccessKey(), awsProperties.getSecretKey())
                        )
                )
                .region(Region.of(awsProperties.getRegion()))
                // Bạn có thể tinh chỉnh thêm các thông số khác tại đây nếu cần
                // .targetThroughputInGbps(20.0)
                // .minimumPartSizeInBytes(8 * 1024 * 1024L)
                .build();
    }

    @Bean
    public S3TransferManager s3TransferManager(S3AsyncClient s3AsyncClient) {
        return S3TransferManager.builder()
                .s3Client(s3AsyncClient)
                .build();
    }
}
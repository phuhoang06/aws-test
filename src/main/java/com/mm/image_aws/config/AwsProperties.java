package com.mm.image_aws.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aws")
@Data
public class AwsProperties {
    private String accessKey;
    private String secretKey;
    private String region;
    private String bucket;
    private String cdnDomain;
    private long maxFileSize = 50000* 1024 * 1024; // Mặc định là 5MB
}
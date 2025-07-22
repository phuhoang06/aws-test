package com.mm.image_aws.service;

import com.mm.image_aws.config.AwsProperties;
import com.mm.image_aws.service.transformer.UrlTransformer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.Upload;
import software.amazon.awssdk.transfer.s3.model.UploadRequest;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UploadService {

    private final AwsProperties config;
    private final S3TransferManager transferManager;
    private final HttpClient httpClient;
    private final List<UrlTransformer> urlTransformers;

    private static final Map<String, String> CONTENT_TYPE_TO_EXTENSION_MAP = Map.ofEntries(
            Map.entry("image/jpeg", ".jpg"),
            Map.entry("image/png", ".png")
            // ... có thể thêm các định dạng khác nếu cần
    );

    public List<String> uploadImagesFromUrls(List<String> imageUrls) {
        List<CompletableFuture<String>> uploadFutures = imageUrls.stream()
                .map(this::uploadImageFromUrlAsync)
                .collect(Collectors.toList());

        return uploadFutures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
    }

    private CompletableFuture<String> uploadImageFromUrlAsync(String imageUrl) {
        return CompletableFuture.supplyAsync(() -> {
            validateImageUrl(imageUrl);
            String directImageUrl = normalizeUrlForDirectDownload(imageUrl);

            try {
                HttpGet request = new HttpGet(directImageUrl);
                HttpResponse response = httpClient.execute(request);
                HttpEntity entity = response.getEntity();

                String contentType = entity.getContentType().getValue();
                long contentLength = entity.getContentLength();

                if (!contentType.startsWith("image/")) {
                    throw new IllegalArgumentException("URL không phải là hình ảnh: " + imageUrl);
                }
                if (contentLength > config.getMaxFileSize()) {
                    throw new IllegalArgumentException("Kích thước tệp vượt quá giới hạn cho phép");
                }

                String extension = getExtensionFromContentType(contentType);
                String fileName = UUID.randomUUID() + extension;

                try (InputStream inputStream = entity.getContent()) {
                    // **FIX:** Đọc InputStream vào một mảng byte
                    byte[] contentBytes = inputStream.readAllBytes();

                    UploadRequest uploadRequest = UploadRequest.builder()
                            .putObjectRequest(req -> req.bucket(config.getBucket())
                                    .key(fileName)
                                    .contentType(contentType)
                                    .build())
                            // **FIX:** Sử dụng fromBytes để tạo AsyncRequestBody
                            .requestBody(AsyncRequestBody.fromBytes(contentBytes))
                            .build();

                    Upload upload = transferManager.upload(uploadRequest);

                    log.info("Bắt đầu tải tệp {} từ URL: {}", fileName, imageUrl);

                    // Chờ cho đến khi quá trình tải lên hoàn tất và trả về URL công khai
                    return upload.completionFuture()
                            .thenApply(completedUpload -> {
                                log.info("Hoàn tất tải tệp {}", fileName);
                                return buildS3PublicUrl(fileName);
                            }).join();
                }

            } catch (IOException e) {
                log.error("Lỗi khi tải từ URL: {}", directImageUrl, e);
                throw new RuntimeException("Lỗi khi tải từ URL: " + directImageUrl, e);
            }
        });
    }

    private String normalizeUrlForDirectDownload(String url) {
        return urlTransformers.stream()
                .map(transformer -> transformer.transform(url))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst()
                .orElse(url);
    }

    private String getExtensionFromContentType(String contentType) {
        if (contentType == null) return ".jpg";
        return CONTENT_TYPE_TO_EXTENSION_MAP.getOrDefault(contentType.toLowerCase(), ".jpg");
    }

    private void validateImageUrl(String url) {
        if (url == null || !url.matches("^https?://.*")) {
            throw new IllegalArgumentException("URL không hợp lệ: " + url);
        }
    }

    private String buildS3PublicUrl(String fileName) {
        if (config.getCdnDomain() != null && !config.getCdnDomain().isEmpty()) {
            return config.getCdnDomain() + "/" + fileName;
        }
        return "https://" + config.getBucket() + ".s3." + config.getRegion() + ".amazonaws.com/" + fileName;
    }
}
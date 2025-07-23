package com.mm.image_aws.service;

import com.mm.image_aws.config.AwsProperties;
import com.mm.image_aws.dto.JobStatus;
import com.mm.image_aws.dto.UploadJob;
import com.mm.image_aws.repo.JobRepository;
import com.mm.image_aws.service.transformer.UrlTransformer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.Upload;
import software.amazon.awssdk.transfer.s3.model.UploadRequest;

import java.net.URI;
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
    private final List<UrlTransformer> urlTransformers;
    private final JobRepository jobRepository;
    private final CloseableHttpAsyncClient httpAsyncClient; // <-- Sử dụng client mới

    private static final Map<String, String> CONTENT_TYPE_TO_EXTENSION_MAP = Map.ofEntries(
            Map.entry("image/jpeg", ".jpg"),
            Map.entry("image/png", ".png")
    );

    // (Các phương thức submitUploadJob, getJobStatus, processImagesInBackground giữ nguyên)
    public UploadJob submitUploadJob(List<String> imageUrls) {
        String jobId = UUID.randomUUID().toString();
        UploadJob job = new UploadJob(jobId, imageUrls.size());
        jobRepository.save(job);
        processImagesInBackground(job, imageUrls);
        return job;
    }

    public Optional<UploadJob> getJobStatus(String jobId) {
        return jobRepository.findById(jobId);
    }

    @Async("taskExecutor")
    public void processImagesInBackground(UploadJob job, List<String> imageUrls) {
        job.setStatus(JobStatus.PROCESSING);
        jobRepository.save(job);

        try {
            List<CompletableFuture<Void>> uploadFutures = imageUrls.stream()
                    .map(url -> uploadImageFromUrlAsync(url)
                            .thenAccept(cdnUrl -> {
                                if (cdnUrl != null) {
                                    job.addCdnUrl(cdnUrl);
                                }
                                job.incrementProcessedCount();
                                jobRepository.save(job);
                            })
                            .exceptionally(ex -> {
                                log.error("Không thể xử lý URL {}: {}", url, ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage());
                                job.incrementProcessedCount();
                                jobRepository.save(job);
                                return null;
                            }))
                    .collect(Collectors.toList());

            CompletableFuture.allOf(uploadFutures.toArray(new CompletableFuture[0])).join();
            job.setStatus(JobStatus.COMPLETED);
        } catch (Exception e) {
            log.error("Lỗi khi xử lý job {}", job.getJobId(), e);
            job.setStatus(JobStatus.FAILED);
            job.setErrorMessage(e.getMessage());
        } finally {
            jobRepository.save(job);
        }
    }


    // THAY ĐỔI LỚN: Viết lại phương thức này để dùng HttpAsyncClient
    private CompletableFuture<String> uploadImageFromUrlAsync(String imageUrl) {
        CompletableFuture<String> overallFuture = new CompletableFuture<>();
        try {
            validateImageUrl(imageUrl);
            String directImageUrl = normalizeUrlForDirectDownload(imageUrl);

            final SimpleHttpRequest request = SimpleHttpRequest.create("GET", URI.create(directImageUrl));
            // Không cần timeout ở đây vì connection manager đã quản lý

            httpAsyncClient.execute(request, new FutureCallback<SimpleHttpResponse>() {
                @Override
                public void completed(SimpleHttpResponse response) {
                    try {
                        if (response.getCode() != 200) {
                            throw new RuntimeException("Server returned status " + response.getCode());
                        }

                        String contentType = Optional.ofNullable(response.getContentType()).map(Object::toString).orElse("application/octet-stream");
                        if (!contentType.startsWith("image/")) {
                            throw new IllegalArgumentException("URL is not an image: " + contentType);
                        }

                        byte[] contentBytes = response.getBodyBytes();
                        if (contentBytes == null || contentBytes.length == 0) {
                            throw new IllegalArgumentException("Response body is empty.");
                        }

                        long contentLength = contentBytes.length;

                        if (contentLength > config.getMaxFileSize()) {
                            throw new IllegalArgumentException("File size exceeds limit");
                        }

                        String extension = getExtensionFromContentType(contentType);
                        final String fileName = UUID.randomUUID() + extension;

                        UploadRequest uploadRequest = UploadRequest.builder()
                                .putObjectRequest(req -> req.bucket(config.getBucket())
                                        .key(fileName)
                                        .contentType(contentType))
                                .requestBody(AsyncRequestBody.fromBytes(contentBytes))
                                .build();

                        log.info("Đang tải file {} từ URL: {}", fileName, imageUrl);
                        Upload upload = transferManager.upload(uploadRequest);

                        // Nối chuỗi CompletableFuture
                        upload.completionFuture()
                                .thenApply(completedUpload -> buildS3PublicUrl(fileName))
                                .whenComplete((cdnUrl, error) -> {
                                    if (error != null) {
                                        overallFuture.completeExceptionally(error);
                                    } else {
                                        overallFuture.complete(cdnUrl);
                                    }
                                });

                    } catch (Exception e) {
                        overallFuture.completeExceptionally(e);
                    }
                }

                @Override
                public void failed(Exception ex) {
                    overallFuture.completeExceptionally(ex);
                }

                @Override
                public void cancelled() {
                    overallFuture.cancel(true);
                }
            });

        } catch (Exception e) {
            overallFuture.completeExceptionally(e);
        }
        return overallFuture;
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
            throw new IllegalArgumentException("Invalid URL: " + url);
        }
    }

    private String buildS3PublicUrl(String fileName) {
        if (config.getCdnDomain() != null && !config.getCdnDomain().isEmpty()) {
            String cdnDomain = config.getCdnDomain();
            String prefix = cdnDomain.startsWith("http") ? "" : "https://";
            return prefix + cdnDomain + "/" + fileName;
        }
        return "https://" + config.getBucket() + ".s3." + config.getRegion() + ".amazonaws.com/" + fileName;
    }
}
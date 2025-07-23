package com.mm.image_aws.service;

import com.mm.image_aws.config.AwsProperties;
import com.mm.image_aws.dto.JobStatus;
import com.mm.image_aws.dto.UploadJob;
import com.mm.image_aws.service.transformer.UrlTransformer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.async.AsyncRequestBody;
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
    private final JobRepository jobRepository;

    private static final Map<String, String> CONTENT_TYPE_TO_EXTENSION_MAP = Map.ofEntries(
            Map.entry("image/jpeg", ".jpg"),
            Map.entry("image/png", ".png")
            // ... add more types as needed
    );

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
                                job.addCdnUrl(cdnUrl);
                                job.incrementProcessedCount();
                                jobRepository.save(job);
                            })
                            .exceptionally(ex -> {
                                log.error("Failed to process URL {}: {}", url, ex.getMessage());
                                job.incrementProcessedCount();
                                jobRepository.save(job);
                                return null;
                            })
                    )
                    .collect(Collectors.toList());

            CompletableFuture.allOf(uploadFutures.toArray(new CompletableFuture[0])).join();
            job.setStatus(JobStatus.COMPLETED);
        } catch (Exception e) {
            log.error("Error processing job {}", job.getJobId(), e);
            job.setStatus(JobStatus.FAILED);
            job.setErrorMessage(e.getMessage());
        } finally {
            jobRepository.save(job);
        }
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
                    throw new IllegalArgumentException("URL is not an image: " + imageUrl);
                }
                if (contentLength > config.getMaxFileSize()) {
                    throw new IllegalArgumentException("File size exceeds limit");
                }

                String extension = getExtensionFromContentType(contentType);
                String fileName = UUID.randomUUID() + extension;

                try (InputStream inputStream = entity.getContent()) {
                    byte[] contentBytes = inputStream.readAllBytes();

                    UploadRequest uploadRequest = UploadRequest.builder()
                            .putObjectRequest(req -> req.bucket(config.getBucket())
                                    .key(fileName)
                                    .contentType(contentType)
                                    .build())
                            .requestBody(AsyncRequestBody.fromBytes(contentBytes))
                            .build();

                    Upload upload = transferManager.upload(uploadRequest);

                    log.info("Uploading file {} from URL: {}", fileName, imageUrl);

                    return upload.completionFuture()
                            .thenApply(completedUpload -> {
                                log.info("Completed upload for file {}", fileName);
                                return buildS3PublicUrl(fileName);
                            }).join();
                }

            } catch (IOException e) {
                log.error("Error downloading from URL: {}", directImageUrl, e);
                throw new RuntimeException("Error downloading from URL: " + directImageUrl, e);
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
            throw new IllegalArgumentException("Invalid URL: " + url);
        }
    }

    private String buildS3PublicUrl(String fileName) {
        if (config.getCdnDomain() != null && !config.getCdnDomain().isEmpty()) {
            return config.getCdnDomain() + "/" + fileName;
        }
        return "https://" + config.getBucket() + ".s3." + config.getRegion() + ".amazonaws.com/" + fileName;
    }
}
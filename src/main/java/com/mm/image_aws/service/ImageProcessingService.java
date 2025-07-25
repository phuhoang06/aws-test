package com.mm.image_aws.service;

import com.mm.image_aws.dto.DownloadedImage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class ImageProcessingService {

    private final ImageDownloaderService downloaderService;
    private final S3StorageService s3StorageService;
    private final MetadataService metadataService;
    private final UploadJobService uploadJobService;

    public ImageProcessingService(ImageDownloaderService downloaderService,
                                  S3StorageService s3StorageService,
                                  MetadataService metadataService,
                                  @Lazy UploadJobService uploadJobService) { // <-- Thêm @Lazy ở đây
        this.downloaderService = downloaderService;
        this.s3StorageService = s3StorageService;
        this.metadataService = metadataService;
        this.uploadJobService = uploadJobService;
    }

    private static final Map<String, String> CONTENT_TYPE_TO_EXTENSION_MAP = Map.ofEntries(
            Map.entry("image/jpeg", ".jpg"),
            Map.entry("image/png", ".png"),
            Map.entry("image/gif", ".gif")
    );

    @Async("taskExecutor")
    // Bỏ @Transactional ở đây để mỗi service con tự quản lý transaction của nó
    public void processImage(Long jobId, String imageUrl) {
        final byte[][] imageBytesHolder = {null};

        downloaderService.downloadImage(imageUrl)
                .thenCompose(downloadedImage -> {
                    imageBytesHolder[0] = downloadedImage.getContent();
                    String contentType = downloadedImage.getContentType();
                    String extension = CONTENT_TYPE_TO_EXTENSION_MAP.getOrDefault(contentType, ".jpg");
                    String fileName = UUID.randomUUID().toString() + extension;

                    return s3StorageService.upload(fileName, contentType, downloadedImage.getContent())
                            .thenApply(completedUpload -> {
                                String cdnUrl = s3StorageService.buildS3PublicUrl(fileName);
                                // Khi thành công, gọi metadata service với đầy đủ thông tin
                                metadataService.extractAndSaveMetadata(jobId, imageUrl, cdnUrl, downloadedImage.getContent(), null);
                                return cdnUrl;
                            });
                })
                .whenComplete((cdnUrl, ex) -> {
                    if (ex != null) {
                        log.error("Xử lý thất bại cho URL {}: {}", imageUrl, ex.getMessage());
                        // Khi thất bại, vẫn gọi metadata service để ghi nhận lỗi
                        byte[] failedImageBytes = imageBytesHolder[0]; // Có thể là null nếu tải ảnh đã thất bại
                        metadataService.extractAndSaveMetadata(jobId, imageUrl, null, failedImageBytes, ex.getMessage());
                    }

                    // Luôn gọi service chuyên dụng để cập nhật job trong một transaction mới
                    uploadJobService.updateJobAfterProcessing(jobId);
                });
    }
}

package com.mm.image_aws.service;

import com.mm.image_aws.dto.JobStatus;
import com.mm.image_aws.dto.JobStatusResponse;
import com.mm.image_aws.dto.UploadRequest;
import com.mm.image_aws.entity.ImageMetadata;
import com.mm.image_aws.entity.UploadJob;
import com.mm.image_aws.entity.User;
import com.mm.image_aws.repo.UploadJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UploadJobService {
    private final UploadJobRepository uploadJobRepository;
    private final ImageProcessingService imageProcessingService;

    public Long createJob(UploadRequest uploadRequest, User user) {
        // Logic hiện tại chỉ xử lý URL đầu tiên trong danh sách
        String url = uploadRequest.getUrls() != null && !uploadRequest.getUrls().isEmpty() ? uploadRequest.getUrls().get(0) : null;
        if (url == null) {
            throw new IllegalArgumentException("No URL provided in upload request");
        }
        UploadJob job = new UploadJob();
        job.setImageUrl(url);
        job.setStatus(JobStatus.PENDING);
        job.setUser(user);
        // FIX: Khởi tạo tổng số và số lượng đã xử lý
        job.setTotalUrls(1);
        job.setProcessedUrls(0);
        UploadJob savedJob = uploadJobRepository.save(job);
        log.info("Created job with ID: {} for user: {}", savedJob.getJobId(), user.getUsername());
        imageProcessingService.processImage(savedJob.getJobId(), url);
        return savedJob.getJobId();
    }

    public JobStatusResponse getJobStatus(String jobId) {
        Long numericJobId;
        try {
            numericJobId = Long.parseLong(jobId);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid job ID format: " + jobId);
        }
        UploadJob job = uploadJobRepository.findById(numericJobId)
                .orElseThrow(() -> new RuntimeException("Job not found with id: " + jobId));
        return JobStatusResponse.fromJob(job);
    }

    // === PHẦN SỬA LỖI: Thêm phương thức bị thiếu ===
    /**
     * Cập nhật trạng thái của một job sau khi quá trình xử lý ảnh hoàn tất.
     * Phương thức này được gọi bởi ImageProcessingService.
     * Nó chạy trong một transaction mới để đảm bảo tính nhất quán của dữ liệu.
     * @param jobId ID của job cần cập nhật.
     */
    @Transactional
    public void updateJobAfterProcessing(Long jobId) {
        UploadJob job = uploadJobRepository.findById(jobId)
                .orElseThrow(() -> {
                    log.error("Attempted to update a non-existent job with ID: {}", jobId);
                    return new RuntimeException("Job not found with id: " + jobId);
                });

        // Đánh dấu là đã xử lý xong 1 URL
        job.setProcessedUrls(1);

        // Logic hiện tại chỉ xử lý 1 URL mỗi job, nên ta có thể kiểm tra metadata duy nhất
        List<ImageMetadata> metadataList = job.getImageMetadataList();

        if (metadataList != null && !metadataList.isEmpty()) {
            ImageMetadata metadata = metadataList.get(0); // Lấy metadata của ảnh đã xử lý
            if (metadata.getCdnUrl() != null && !metadata.getCdnUrl().isEmpty()) {
                job.setStatus(JobStatus.COMPLETED);
                job.setS3Url(metadata.getCdnUrl());
                job.setErrorMessage(null); // Xóa thông báo lỗi nếu thành công
            } else {
                job.setStatus(JobStatus.FAILED);
                job.setErrorMessage(metadata.getErrorMessage());
            }
        } else {
            // Trường hợp không tìm thấy metadata nào, đánh dấu là lỗi
            job.setStatus(JobStatus.FAILED);
            job.setErrorMessage("Không tìm thấy metadata của ảnh đã xử lý.");
        }

        uploadJobRepository.save(job);
        log.info("Updated job {} status to {}", jobId, job.getStatus());
    }
}

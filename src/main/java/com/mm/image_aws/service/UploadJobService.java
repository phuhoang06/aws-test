package com.mm.image_aws.service;

import com.mm.image_aws.dto.JobStatus;
import com.mm.image_aws.entity.UploadJob;
import com.mm.image_aws.entity.User;
import com.mm.image_aws.repo.UploadJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UploadJobService {

    private final UploadJobRepository uploadJobRepository;
    private final ImageProcessingService imageProcessingService;

    @Transactional
    // --- THAY ĐỔI: Nhận trực tiếp đối tượng User đã được xác thực ---
    public UploadJob submitUploadJob(User user, List<String> imageUrls) {
        UploadJob job = new UploadJob(user, imageUrls.size());
        UploadJob savedJob = uploadJobRepository.save(job);

        // Không cần set status là PROCESSING ngay, vì có thể xử lý xong rất nhanh
        // Trạng thái sẽ được cập nhật trong updateJobAfterProcessing

        for (String url : imageUrls) {
            imageProcessingService.processImage(savedJob.getId(), url);
        }

        return savedJob; // Trả về job với trạng thái ban đầu là PENDING
    }

    //... các phương thức khác giữ nguyên
    @Transactional
    public void updateJobAfterProcessing(Long jobId) {
        UploadJob job = uploadJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy job để cập nhật: " + jobId));

        // Bắt đầu xử lý thì chuyển sang PROCESSING
        if (job.getStatus() == JobStatus.PENDING) {
            job.setStatus(JobStatus.PROCESSING);
        }

        job.incrementProcessedCount();

        if (job.getProcessedUrls() >= job.getTotalUrls()) {
            job.setStatus(JobStatus.COMPLETED);
        }

        uploadJobRepository.save(job);
    }

    public Optional<UploadJob> getJobStatus(String jobId) {
        return uploadJobRepository.findByJobId(jobId);
    }

    public List<UploadJob> getAllJobs() {
        return uploadJobRepository.findAll();
    }
}
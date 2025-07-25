package com.mm.image_aws.entity;

import com.mm.image_aws.dto.JobStatus;
import jakarta.persistence.*;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "TB_UPLOAD_JOB")
@NoArgsConstructor
// LƯU Ý: Đã xóa @Getter và @Setter để thêm thủ công bên dưới
public class UploadJob {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "upload_job_seq")
    @SequenceGenerator(name = "upload_job_seq", sequenceName = "UPLOAD_JOB_SEQ", allocationSize = 1)
    @Column(name = "N_ID")
    private Long jobId;

    @Column(name = "S_IMAGE_URL", length = 2048)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "S_STATUS")
    private JobStatus status;

    @Column(name = "S_S3_URL", length = 2048)
    private String s3Url;

    @Column(name = "S_FAILURE_REASON", length = 1024)
    private String failureReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "N_USER_ID")
    private User user;

    @Column(name = "D_CREATE", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "D_UPDATE")
    private LocalDateTime updatedAt;

    // Số lượng URL đã xử lý thành công
    @Column(name = "N_PROCESSED_URLS")
    private Integer processedUrls;

    // Tổng số URL cần xử lý
    @Column(name = "N_TOTAL_URLS")
    private Integer totalUrls;

    // Danh sách metadata ảnh liên kết với job này
    @OneToMany(mappedBy = "uploadJob", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private java.util.List<ImageMetadata> imageMetadataList;

    // Lý do lỗi nếu job thất bại
    @Column(name = "S_ERROR_MESSAGE", length = 1024)
    private String errorMessage;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // FIX: Thêm các phương thức Getter và Setter thủ công để giải quyết lỗi "Cannot resolve method"
    public Long getJobId() {
        return jobId;
    }
    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public String getS3Url() {
        return s3Url;
    }

    public void setS3Url(String s3Url) {
        this.s3Url = s3Url;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getProcessedUrls() {
        return processedUrls;
    }
    public void setProcessedUrls(Integer processedUrls) {
        this.processedUrls = processedUrls;
    }
    public Integer getTotalUrls() {
        return totalUrls;
    }
    public void setTotalUrls(Integer totalUrls) {
        this.totalUrls = totalUrls;
    }
    public java.util.List<ImageMetadata> getImageMetadataList() {
        return imageMetadataList;
    }
    public void setImageMetadataList(java.util.List<ImageMetadata> imageMetadataList) {
        this.imageMetadataList = imageMetadataList;
    }
    public String getErrorMessage() {
        return errorMessage;
    }
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}

package com.mm.image_aws.entity;

import com.mm.image_aws.dto.JobStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "UPLOAD_JOB")
@Data
@NoArgsConstructor
public class UploadJob {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "upload_job_seq")
    @SequenceGenerator(name = "upload_job_seq", sequenceName = "UPLOAD_JOB_SEQ", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    // --- THÊM MỐI QUAN HỆ MANY-TO-ONE VỚI USER ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false) // Khóa ngoại tới bảng TB_USER
    private User user;
    // ---------------------------------------------

    @Column(name = "JOB_ID", unique = true, nullable = false)
    private String jobId;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false)
    private JobStatus status;

    @Column(name = "TOTAL_URLS")
    private int totalUrls;

    @Column(name = "PROCESSED_URLS")
    private int processedUrls;

    @Column(name = "ERROR_MESSAGE")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "CREATED_AT", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "uploadJob", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ImageMetadata> imageMetadataList = new ArrayList<>();

    // Cập nhật constructor để nhận User
    public UploadJob(User user, int totalUrls) {
        this.user = user;
        this.jobId = UUID.randomUUID().toString();
        this.status = JobStatus.PENDING;
        this.totalUrls = totalUrls;
        this.processedUrls = 0;
    }

    public synchronized void incrementProcessedCount() {
        this.processedUrls++;
    }
}

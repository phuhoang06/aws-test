package com.mm.image_aws.dto;

import lombok.Data;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Data
public class UploadJob {
    private String jobId;
    private JobStatus status;
    private int totalUrls;
    private int processedUrls;
    private final List<String> cdnUrls = new CopyOnWriteArrayList<>(); // Thread-safe list
    private String errorMessage;

    public UploadJob(String jobId, int totalUrls) {
        this.jobId = jobId;
        this.totalUrls = totalUrls;
        this.status = JobStatus.PENDING;
        this.processedUrls = 0;
    }

    // Phương thức thread-safe để tăng số lượng đã xử lý
    public synchronized void incrementProcessedCount() {
        this.processedUrls++;
    }
    
    // Phương thức để thêm URL một cách an toàn
    public void addCdnUrl(String url) {
        this.cdnUrls.add(url);
    }
}
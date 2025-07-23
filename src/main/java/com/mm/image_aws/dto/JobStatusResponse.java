package com.mm.image_aws.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL) // Bỏ qua các trường null khi serialize
public class JobStatusResponse {
    private String jobId;
    private JobStatus status;
    private String progress;
    private List<String> cdnUrls;
    private String errorMessage;

    public static JobStatusResponse fromJob(UploadJob job) {
        JobStatusResponse response = new JobStatusResponse();
        response.setJobId(job.getJobId());
        response.setStatus(job.getStatus());

        if (job.getStatus() == JobStatus.PROCESSING) {
            response.setProgress(job.getProcessedUrls() + "/" + job.getTotalUrls());
        }

        if (job.getStatus() == JobStatus.COMPLETED) {
            response.setCdnUrls(job.getCdnUrls());
        }

        if (job.getStatus() == JobStatus.FAILED) {
            response.setErrorMessage(job.getErrorMessage());
        }

        return response;
    }
}
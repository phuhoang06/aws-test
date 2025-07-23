package com.mm.image_aws.controller;

import com.mm.image_aws.dto.JobStatusResponse;
import com.mm.image_aws.dto.JobSubmissionResponse;
import com.mm.image_aws.dto.UploadJob;
import com.mm.image_aws.dto.UploadRequest;
import com.mm.image_aws.service.UploadService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/upload")
public class UploadController {

    private final UploadService uploadService;

    public UploadController(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    @PostMapping
    public ResponseEntity<JobSubmissionResponse> submitUploadJob(@RequestBody UploadRequest request) {
        UploadJob job = uploadService.submitUploadJob(request.getUrls());

        String statusUrl = "/upload/status/" + job.getJobId();

        return ResponseEntity.accepted().body(new JobSubmissionResponse(job.getJobId(), statusUrl));
    }

    @GetMapping("/status/{jobId}")
    public ResponseEntity<JobStatusResponse> getJobStatus(@PathVariable String jobId) {
        return uploadService.getJobStatus(jobId)
                .map(job -> ResponseEntity.ok(JobStatusResponse.fromJob(job)))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));
    }
}
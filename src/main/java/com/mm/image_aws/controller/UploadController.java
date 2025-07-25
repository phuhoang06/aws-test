package com.mm.image_aws.controller;

import com.mm.image_aws.dto.JobStatusResponse;
import com.mm.image_aws.dto.JobSubmissionResponse;
import com.mm.image_aws.dto.UploadRequest;
import com.mm.image_aws.entity.User;
import com.mm.image_aws.service.UploadJobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UploadController {
    private final UploadJobService uploadJobService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadImage(@Valid @RequestBody UploadRequest uploadRequest, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return new ResponseEntity<>("User not authenticated", HttpStatus.UNAUTHORIZED);
        }

        User user = (User) authentication.getPrincipal();
        Long jobId = uploadJobService.createJob(uploadRequest, user);

        // === PHẦN SỬA LỖI LOGIC ===
        // Tạo URL đầy đủ để kiểm tra trạng thái job thay vì chỉ trả về "PENDING"
        String statusUrl = "/api/jobs/" + jobId;
        return ResponseEntity.ok(new JobSubmissionResponse(String.valueOf(jobId), statusUrl));
    }

    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<JobStatusResponse> getJobStatus(@PathVariable String jobId) {
        JobStatusResponse status = uploadJobService.getJobStatus(jobId);
        return ResponseEntity.ok(status);
    }
}

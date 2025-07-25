package com.mm.image_aws.controller;

import com.mm.image_aws.dto.JobStatusResponse;
import com.mm.image_aws.dto.JobSubmissionResponse;
import com.mm.image_aws.dto.UploadRequest;
import com.mm.image_aws.entity.User;
import com.mm.image_aws.repo.UserRepository;
import com.mm.image_aws.service.UploadJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/upload") // Đổi thành /api/upload để nhất quán
@RequiredArgsConstructor
public class UploadController {

    private final UploadJobService uploadJobService;
    private final UserRepository userRepository; // Cần để lấy User entity từ UserDetails

    @PostMapping
    public ResponseEntity<?> submitUploadJob(
            // --- THAY ĐỔI: Lấy thông tin người dùng đã xác thực từ Security Context ---
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody UploadRequest request) {

        // Spring Security đã xác thực token và cung cấp UserDetails
        // Chúng ta cần lấy User entity đầy đủ từ database
        User authenticatedUser = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể tìm thấy thông tin người dùng đã xác thực."));

        var job = uploadJobService.submitUploadJob(authenticatedUser, request.getUrls());

        // --- THAY ĐỔI: Cập nhật URL cho đúng với prefix mới ---
        String statusUrl = "/api/upload/status/" + job.getJobId();
        return ResponseEntity.accepted().body(new JobSubmissionResponse(job.getJobId(), statusUrl));
    }

    @GetMapping("/status/{jobId}")
    public ResponseEntity<JobStatusResponse> getJobStatus(@PathVariable String jobId) {
        return uploadJobService.getJobStatus(jobId)
                .map(JobStatusResponse::fromJob)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy job với ID: " + jobId));
    }

    @GetMapping("/jobs")
    public ResponseEntity<List<JobStatusResponse>> getAllJobs() {
        List<JobStatusResponse> jobResponses = uploadJobService.getAllJobs().stream()
                .map(JobStatusResponse::fromJob)
                .collect(Collectors.toList());
        return ResponseEntity.ok(jobResponses);
    }
}
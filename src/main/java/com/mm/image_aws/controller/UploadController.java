package com.mm.image_aws.controller;

import com.mm.image_aws.dto.UploadRequest;
import com.mm.image_aws.dto.UploadResponse;
import com.mm.image_aws.service.UploadService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/upload")
public class UploadController {

    private final UploadService uploadService;

    public UploadController(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    @PostMapping
    public ResponseEntity<UploadResponse> upload(@RequestBody UploadRequest request) {
        // 1. Gọi phương thức mới trong service để xử lý danh sách URLs
        List<String> cdnUrls = uploadService.uploadImagesFromUrls(request.getUrls());

        // 2. Trả về danh sách các link CDN trong response
        return ResponseEntity.ok(new UploadResponse(cdnUrls));
    }
}
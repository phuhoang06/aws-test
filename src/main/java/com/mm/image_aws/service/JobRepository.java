package com.mm.image_aws.service;

import com.mm.image_aws.dto.UploadJob;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Repository
public class JobRepository {
    private final Map<String, UploadJob> jobs = new ConcurrentHashMap<>();

    public void save(UploadJob job) {
        jobs.put(job.getJobId(), job);
    }

    public Optional<UploadJob> findById(String jobId) {
        return Optional.ofNullable(jobs.get(jobId));
    }
}
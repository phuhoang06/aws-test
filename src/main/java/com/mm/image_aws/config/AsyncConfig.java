package com.mm.image_aws.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // Số lượng thread cốt lõi, luôn sẵn sàng
        executor.setCorePoolSize(10); 
        // Số lượng thread tối đa có thể tạo ra
        executor.setMaxPoolSize(50);
        // Số lượng tác vụ có thể nằm trong hàng đợi trước khi bị từ chối
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("UrlUpload-");
        executor.initialize();
        return executor;
    }
}
package com.mm.image_aws.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Service to handle rate limiting logic using Redis.
 */
@Service
public class RateLimitingService {

    private final StringRedisTemplate redisTemplate;
    private static final String RATE_LIMIT_PREFIX = "rate_limit:";
    private static final int PERMIT_COUNT = 1; // Number of permits allowed
    private static final Duration TIME_WINDOW = Duration.ofSeconds(1); // Time window for the permits

    @Autowired
    public RateLimitingService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Attempts to acquire a permit for a given user ID.
     *
     * @param userId The ID of the user trying to make a request.
     * @return true if the permit was acquired, false otherwise.
     */
    public boolean tryAcquire(String userId) {
        String key = RATE_LIMIT_PREFIX + userId;
        ValueOperations<String, String> ops = redisTemplate.opsForValue();

        // Increment the key and check the value
        Long currentCount = ops.increment(key);

        // If this is the first request in the time window, set the expiration
        if (currentCount != null && currentCount == 1) {
            redisTemplate.expire(key, TIME_WINDOW);
        }

        // Allow the request if the count is within the permitted limit
        return currentCount != null && currentCount <= PERMIT_COUNT;
    }
}

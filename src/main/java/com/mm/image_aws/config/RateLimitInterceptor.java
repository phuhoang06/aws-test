package com.mm.image_aws.config;

import com.mm.image_aws.entity.User;
import com.mm.image_aws.service.RateLimitingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor to check for rate limits on incoming requests.
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitingService rateLimitingService;

    @Autowired
    public RateLimitInterceptor(RateLimitingService rateLimitingService) {
        this.rateLimitingService = rateLimitingService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            // Should be handled by Spring Security, but as a safeguard
            return true;
        }

        User user = (User) authentication.getPrincipal();
        String userId = user.getS_id(); // Assuming your User entity has a getId() or getS_id() method

        if (!rateLimitingService.tryAcquire(userId)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("You have exceeded the request limit. Please try again later.");
            return false; // Block the request
        }

        return true; // Proceed with the request
    }
}

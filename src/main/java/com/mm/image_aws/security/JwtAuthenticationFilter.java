// File: src/main/java/com/mm/image_aws/security/JwtAuthenticationFilter.java
// NỘI DUNG ĐẦY ĐỦ CỦA FILE ĐÃ SỬA LỖI
package com.mm.image_aws.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        // ======================== PHẦN SỬA LỖI ========================
        // Nếu request path là một trong các endpoint công khai (/api/auth/**),
        // chúng ta sẽ bỏ qua hoàn toàn việc kiểm tra token và cho request đi tiếp.
        if (request.getServletPath().contains("/api/auth")) {
            filterChain.doFilter(request, response);
            return; // Rất quan trọng: return để kết thúc filter tại đây.
        }
        // =============================================================

        try {
            String jwt = getJwtFromRequest(request);

            // Chỉ xử lý token nếu nó tồn tại và người dùng chưa được xác thực trong context
            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt) && SecurityContextHolder.getContext().getAuthentication() == null) {
                String userEmail = tokenProvider.getUserEmailFromJWT(jwt);

                UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Set thông tin xác thực vào SecurityContext
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ex) {
            // Lỗi này sẽ được chuyển đến JwtAuthenticationEntryPoint để xử lý
            logger.error("Không thể set user authentication trong security context", ex);
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
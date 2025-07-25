package com.mm.image_aws.config;

import com.mm.image_aws.security.JwtAuthenticationEntryPoint;
import com.mm.image_aws.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint unauthorizedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Vô hiệu hóa CSRF vì chúng ta dùng JWT (stateless)
                .csrf(AbstractHttpConfigurer::disable)

                // Cấu hình xử lý exception, đặc biệt là lỗi 401 Unauthorized
                .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))

                // Cấu hình session management là STATELESS, vì chúng ta không dùng session
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Cấu hình quyền truy cập cho các request
                .authorizeHttpRequests(authorize -> authorize
                        // Cho phép tất cả các request đến /api/auth/** (đăng ký, đăng nhập)
                        .requestMatchers("/api/auth/**").permitAll()
                        // Tất cả các request khác đều yêu cầu xác thực
                        .anyRequest().authenticated()
                );

        // Thêm filter JWT của chúng ta vào trước filter UsernamePasswordAuthenticationFilter
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Sử dụng BCrypt để mã hóa mật khẩu, đây là tiêu chuẩn hiện nay
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        // Bean này cần thiết cho việc xác thực ở AuthController
        return authenticationConfiguration.getAuthenticationManager();
    }
}
package com.mm.image_aws.security;

import com.mm.image_aws.entity.User;
import com.mm.image_aws.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        // --- THAY ĐỔI: Tìm kiếm bằng cả username và email ---
        User user = userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng với username hoặc email: " + usernameOrEmail));

        // Spring Security sẽ dùng thông tin này để so sánh mật khẩu và quản lý quyền
        // Trả về UserDetails với username là email (hoặc username, tùy bạn chọn, email thường nhất quán hơn)
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                new ArrayList<>() // << QUAN TRỌNG: Không có role, danh sách quyền rỗng.
        );
    }
}
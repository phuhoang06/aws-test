package com.mm.image_aws.repo;

import com.mm.image_aws.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    // --- THAY ĐỔI: Tìm user bằng email hoặc username ---
    Optional<User> findByUsernameOrEmail(String username, String email);

    // --- THÊM MỚI: Các phương thức kiểm tra sự tồn tại ---
    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);

    // --- GIỮ LẠI: Vẫn cần để lấy User entity từ UserDetails ---
    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);
}
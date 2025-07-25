package com.mm.image_aws.config;

import com.github.javafaker.Faker;
import com.mm.image_aws.entity.User;
import com.mm.image_aws.repo.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;

/**
 * Lớp cấu hình này chịu trách nhiệm tạo dữ liệu mẫu cho ứng dụng khi khởi động.
 */
@Configuration
public class DataSeedingConfig {

    private static final Logger logger = LoggerFactory.getLogger(DataSeedingConfig.class);

    @Bean
    public CommandLineRunner seedDatabase(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.count() == 0) {
                logger.info("Cơ sở dữ liệu trống. Bắt đầu tạo 100 người dùng mẫu...");
                Faker faker = new Faker();
                List<User> users = new ArrayList<>();

                String commonPassword = passwordEncoder.encode("password123");

                for (int i = 0; i < 100; i++) {
                    User user = new User();
                    String firstName = faker.name().firstName();
                    String lastName = faker.name().lastName();
                    String username = firstName.toLowerCase() + "." + lastName.toLowerCase() + i;
                    String email = username + "@example.com";

                    user.setUsername(username);
                    user.setEmail(email);
                    user.setPassword(commonPassword);

                    // === SỬA LỖI Ở ĐÂY: Gán trạng thái 'active' một cách tường minh ===
                    user.setState("active");
                    // =============================================================

                    users.add(user);
                }

                userRepository.saveAll(users);
                logger.info("Đã tạo và lưu thành công 100 người dùng mẫu.");
            } else {
                logger.info("Cơ sở dữ liệu đã có dữ liệu người dùng. Bỏ qua việc tạo dữ liệu mẫu.");
            }
        };
    }
}

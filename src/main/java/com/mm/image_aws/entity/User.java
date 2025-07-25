package com.mm.image_aws.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "TB_USER", uniqueConstraints = {
        @UniqueConstraint(columnNames = "S_USERNAME"),
        @UniqueConstraint(columnNames = "S_EMAIL")
})
public class User {

    @Id
    @Column(name = "S_ID", length = 32, nullable = false)
    private String id;

    // --- THÊM MỚI: Cột username ---
    @Column(name = "S_USERNAME", length = 50, nullable = false, unique = true)
    private String username;

    @Column(name = "S_NAME", length = 256)
    private String name;

    @Column(name = "S_EMAIL", length = 255, nullable = false, unique = true)
    private String email;

    @JsonIgnore
    @Column(name = "S_PASSWORD", length = 100, nullable = false)
    private String password;

    @Column(name = "S_STATE", length = 32, nullable = false)
    private String state;

    @CreationTimestamp
    @Column(name = "D_CREATE", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "D_UPDATE")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString().replace("-", "");
        }
        if (this.state == null) {
            this.state = "approved";
        }
    }
}
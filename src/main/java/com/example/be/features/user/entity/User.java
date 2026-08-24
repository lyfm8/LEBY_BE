package com.example.be.features.user.entity;
import com.example.be.features.user.entity.LearningPath;

import com.example.be.features.user.enums.ELearnerType;
import com.example.be.features.user.enums.ERole;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Đại diện cho người dùng trong hệ thống.
 * Student và Admin đều được lưu trong bảng này, phân biệt qua role.
 * Rule 32: Actor Student/Admin không tạo Entity riêng vì Class Diagram chỉ có User.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(length = 150)
    private String fullName;

    private LocalDate dob;

    @Column(length = 50)
    private String avatar;

    @Column(nullable = false)
    private Boolean isActive = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ERole role;

    @Enumerated(EnumType.STRING)
    private ELearnerType learnerType;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /**
     * Phiên bản token của user. Được nhúng vào mọi JWT khi cấp phát.
     * JwtAuthenticationFilter so sánh giá trị này với version trong token payload.
     * Khi logout hoặc đổi mật khẩu: tăng 1 → vô hiệu hóa toàn bộ token cũ ngay lập tức.
     * Không cần Redis Blacklist.
     */
    @Column(nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer tokenVersion = 0;

    /**
     * User follows 0..1 LearningPath.
     * FK nằm ở User vì User là owner của LearningPath theo diagram.
     * Rule 9: OneToOne - User sở hữu LearningPath.
     * Nullable = true vì cardinality là 0..1 (User có thể chưa có LearningPath).
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "learning_path_id", nullable = true)
    private LearningPath learningPath;
}

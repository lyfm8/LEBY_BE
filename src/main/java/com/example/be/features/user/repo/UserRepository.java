package com.example.be.features.user.repo;

import com.example.be.features.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    /**
     * Tăng tokenVersion thêm 1 trực tiếp trong DB mà không cần load entity lên.
     * Dùng khi logout hoặc đổi mật khẩu để vô hiệu hóa toàn bộ token cũ.
     */
    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.tokenVersion = u.tokenVersion + 1 WHERE u.id = :id")
    void incrementTokenVersion(@Param("id") Long id);
}

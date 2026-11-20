package com.example.be.repository.ability;

import com.example.be.entity.ability.UserAbility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository truy vấn dữ liệu năng lực học viên (UserAbility).
 */
@Repository
public interface UserAbilityRepository extends JpaRepository<UserAbility, Long> {

    /**
     * Lấy toàn bộ bản ghi ma trận năng lực của một người dùng,
     * đồng thời nạp sẵn Ability và Part qua JOIN FETCH để tối ưu hiệu năng và triệt tiêu N+1 Query.
     *
     * @param userId ID định danh của người dùng
     * @return Danh sách UserAbility sắp xếp theo Part (1-7) và tên Ability
     */
    @Query("SELECT ua FROM UserAbility ua " +
           "JOIN FETCH ua.ability a " +
           "JOIN FETCH ua.part p " +
           "WHERE ua.user.id = :userId " +
           "ORDER BY p.id ASC, a.name ASC")
    List<UserAbility> findByUserIdWithDetails(@Param("userId") Long userId);
}

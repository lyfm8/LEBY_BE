package com.example.be.repository.content;

import com.example.be.entity.content.Module;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository quản lý dữ liệu Module học tập.
 */
@Repository
public interface ModuleRepository extends JpaRepository<Module, Long> {

    /**
     * Lấy toàn bộ danh sách Module theo thứ tự hiển thị sư phạm từ nhỏ đến lớn.
     *
     * @return Danh sách modules sắp xếp theo sequence tăng dần
     */
    List<Module> findAllByOrderBySequenceAsc();

    List<Module> findByPartIdOrderBySequenceAsc(Long partId);

    @Query("SELECT DISTINCT m FROM Module m JOIN m.moduleAbilities ma WHERE ma.ability.id = :abilityId ORDER BY m.sequence ASC")
    List<Module> findByAbilityId(@Param("abilityId") Long abilityId);

    /**
     * Tìm giá trị sequence lớn nhất hiện tại để gợi ý sequence tiếp theo khi tạo Module mới.
     *
     * @return Sequence lớn nhất hiện tại
     */
    @Query("SELECT MAX(m.sequence) FROM Module m")
    Optional<Integer> findMaxSequence();
}

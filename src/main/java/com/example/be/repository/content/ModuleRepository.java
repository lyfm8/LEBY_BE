package com.example.be.repository.content;

import com.example.be.entity.content.Module;
import com.example.be.enums.content.EModuleStatus;
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

    /**
     * Lấy toàn bộ Module kèm Abilities và Part trong 1 query (triệt tiêu N+1).
     * NOTE: Tách riêng lessons query để tránh MultipleBagFetchException (2 List collections).
     *
     * @return Danh sách modules đã JOIN FETCH moduleAbilities, ability, part
     */
    @Query("SELECT DISTINCT m FROM Module m " +
            "LEFT JOIN FETCH m.moduleAbilities ma " +
            "LEFT JOIN FETCH ma.ability " +
            "LEFT JOIN FETCH m.part " +
            "ORDER BY m.sequence ASC")
    List<Module> findAllWithLessonsAndAbilities();

    List<Module> findByPartIdOrderBySequenceAsc(Long partId);

    /**
     * Lấy danh sách Module đã xuất bản (PUBLISHED) thuộc một Part theo thứ tự sư phạm sequence tăng dần
     * để thuật toán sinh lộ trình không bao giờ bốc nhầm Module bản nháp (DRAFT).
     *
     * @param partId ID của Part
     * @param status Trạng thái Module hợp lệ (EModuleStatus.PUBLISHED)
     * @return Danh sách Module đã xuất bản sắp xếp theo sequence tăng dần
     */
    List<Module> findByPartIdAndStatusOrderBySequenceAsc(Long partId, EModuleStatus status);

    @Query("SELECT DISTINCT m FROM Module m JOIN m.moduleAbilities ma WHERE ma.ability.id = :abilityId ORDER BY m.sequence ASC")
    List<Module> findByAbilityId(@Param("abilityId") Long abilityId);

    /**
     * Lấy danh sách Module đã xuất bản (PUBLISHED) phụ trách một năng lực Ability cụ thể theo thứ tự sư phạm.
     *
     * @param abilityId ID của Ability yếu cần bổ trợ
     * @param status    Trạng thái Module hợp lệ (EModuleStatus.PUBLISHED)
     * @return Danh sách Module đã xuất bản sắp xếp theo sequence tăng dần
     */
    @Query("SELECT DISTINCT m FROM Module m JOIN m.moduleAbilities ma " +
           "WHERE ma.ability.id = :abilityId AND m.status = :status " +
           "ORDER BY m.sequence ASC")
    List<Module> findByAbilityIdAndStatus(@Param("abilityId") Long abilityId, @Param("status") EModuleStatus status);

    /**
     * Lấy toàn bộ Module theo trạng thái xuất bản (PUBLISHED) kèm Part và Abilities trong 1 query duy nhất.
     * Tách riêng danh sách Lesson để tránh lỗi MultipleBagFetchException của Hibernate.
     *
     * @param status Trạng thái Module cần lọc (thường là EModuleStatus.PUBLISHED cho học viên)
     * @return Danh sách Module đã xuất bản sắp xếp theo thứ tự sư phạm sequence tăng dần
     */
    @Query("SELECT DISTINCT m FROM Module m " +
            "LEFT JOIN FETCH m.part p " +
            "LEFT JOIN FETCH m.moduleAbilities ma " +
            "LEFT JOIN FETCH ma.ability a " +
            "WHERE m.status = :status " +
            "ORDER BY m.sequence ASC")
    List<Module> findByStatusWithPartAndAbilitiesOrderBySequenceAsc(@Param("status") EModuleStatus status);

    /**
     * Nạp chi tiết một Module theo ID và trạng thái xuất bản kèm Part và Abilities trong 1 query
     * phục vụ truy cập chế độ Học tự do (FREE_STUDY) từ Thư viện khóa học (UC17 Catalog).
     *
     * @param id     ID của Module cần tìm
     * @param status Trạng thái Module hợp lệ (EModuleStatus.PUBLISHED)
     * @return Optional chứa Module nếu tồn tại và đã xuất bản
     */
    @Query("SELECT DISTINCT m FROM Module m " +
            "LEFT JOIN FETCH m.part p " +
            "LEFT JOIN FETCH m.moduleAbilities ma " +
            "LEFT JOIN FETCH ma.ability a " +
            "WHERE m.id = :id AND m.status = :status")
    Optional<Module> findByIdAndStatusWithPartAndAbilities(
            @Param("id") Long id,
            @Param("status") EModuleStatus status
    );

    /**
     * Tìm giá trị sequence lớn nhất hiện tại để gợi ý sequence tiếp theo khi tạo Module mới.
     *
     * @return Sequence lớn nhất hiện tại
     */
    @Query("SELECT MAX(m.sequence) FROM Module m")
    Optional<Integer> findMaxSequence();

    /**
     * Nạp chi tiết Module kèm Part và danh sách Ability trong 1 truy vấn duy nhất (UC13 - UC14).
     */
    @Query("SELECT DISTINCT m FROM Module m " +
           "LEFT JOIN FETCH m.part p " +
           "LEFT JOIN FETCH m.moduleAbilities ma " +
           "LEFT JOIN FETCH ma.ability a " +
           "WHERE m.id = :moduleId")
    Optional<Module> findByIdWithPartAndAbilities(@Param("moduleId") Long moduleId);
}

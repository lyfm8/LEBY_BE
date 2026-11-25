package com.example.be.repository.user;

import com.example.be.entity.user.TargetProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TargetProfileRepository extends JpaRepository<TargetProfile, Long> {

    List<TargetProfile> findAllByOrderByTargetTotalScoreAsc();

    /**
     * Lấy danh sách các mốc mục tiêu AIM đang kích hoạt, sắp xếp điểm tăng dần (UC05).
     *
     * @return Danh sách TargetProfile đang hoạt động
     */
    List<TargetProfile> findByStatusTrueOrderByTargetTotalScoreAsc();
}

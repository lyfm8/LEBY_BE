package com.example.be.service.exam.util;

import com.example.be.entity.content.LearningPath;
import com.example.be.entity.content.LearningPathItem;
import com.example.be.entity.content.Module;
import com.example.be.enums.content.ELearningPathStatus;
import com.example.be.enums.content.ELessonStatus;
import com.example.be.enums.content.EModuleStatus;
import com.example.be.exception.BaseException;
import com.example.be.exception.ErrorCode;
import com.example.be.repository.content.LearningPathItemRepository;
import com.example.be.repository.content.LearningPathRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Helper dùng chung xác thực quyền truy cập chặng học trên Lộ trình cá nhân (BR10 & chống IDOR)
 * cho Phòng luyện tập (UC12) và Phòng thi vượt ải (UC13-UC14).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserExamAccessHelper {

    private static final List<ELearningPathStatus> CURRENT_PATH_STATUSES = List.of(
            ELearningPathStatus.ACTIVE,
            ELearningPathStatus.COMPLETED
    );

    private final LearningPathRepository learningPathRepository;
    private final LearningPathItemRepository learningPathItemRepository;

    /**
     * Lấy lộ trình học hiện hành của học viên (ưu tiên ACTIVE, fallback sang COMPLETED mới nhất).
     */
    public LearningPath requireCurrentLearningPath(Long userId) {
        List<LearningPath> candidates = learningPathRepository
                .findByUserIdAndStatusInOrderByVersionDescCreatedAtDesc(userId, CURRENT_PATH_STATUSES);

        if (candidates == null || candidates.isEmpty()) {
            throw new BaseException(
                    ErrorCode.DIAGNOSTIC_NOT_COMPLETED,
                    "Bạn cần hoàn thành bài kiểm tra chẩn đoán trước khi truy cập nội dung học tập"
            );
        }

        return candidates.stream()
                .filter(path -> path.getStatus() == ELearningPathStatus.ACTIVE)
                .findFirst()
                .orElse(candidates.get(0));
    }

    /**
     * Xác thực mô-đun nằm trong lộ trình của học viên và đã được mở khóa theo quy tắc tuần tự BR10.
     *
     * @param userId   ID học viên đang đăng nhập
     * @param moduleId ID mô-đun cần truy cập
     * @return {@link LearningPathItem} đã xác thực quyền truy cập
     */
    public LearningPathItem requireUnlockedPathItem(Long userId, Long moduleId) {
        LearningPath learningPath = requireCurrentLearningPath(userId);
        LearningPathItem pathItem = learningPathItemRepository
                .findByLearningPathIdAndModuleIdWithDetails(learningPath.getId(), moduleId)
                .orElseThrow(() -> new BaseException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Nội dung mô-đun không tồn tại trong lộ trình học của bạn"
                ));

        if (pathItem.getStatus() == ELessonStatus.LOCKED) {
            log.warn("[BR10] Học viên userId={} cố truy cập moduleId={} đang bị khóa", userId, moduleId);
            throw new BaseException(
                    ErrorCode.CONTENT_LOCKED,
                    "Mô-đun đang bị khóa. Bạn phải hoàn thành các mô-đun phía trước"
            );
        }

        return pathItem;
    }

    /**
     * Xác thực quyền vào học / luyện tập (UC11, UC12) cho cả mô-đun trong lộ trình lẫn chế độ Học tự do (UC17 Catalog):
     * - Bắt buộc học viên đã hoàn thành chẩn đoán và có lộ trình hiện hành.
     * - Nếu mô-đun nằm trong lộ trình cá nhân: chặn cứng trạng thái LOCKED theo BR10 và trả về {@link LearningPathItem}.
     * - Nếu mô-đun nằm ngoài lộ trình: chỉ cho phép truy cập chế độ Học tự do (trả {@code null}) khi mô-đun đã PUBLISHED.
     *
     * @param userId ID học viên đang đăng nhập
     * @param module Thực thể mô-đun cần truy cập
     * @return {@link LearningPathItem} nếu thuộc lộ trình, hoặc {@code null} nếu là mô-đun Học tự do
     */
    public LearningPathItem requireAccessibleModuleForStudy(Long userId, Module module) {
        if (module == null || module.getId() == null) {
            throw new BaseException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    "Nội dung mô-đun không tồn tại hoặc đã được gỡ bỏ"
            );
        }

        LearningPath learningPath = requireCurrentLearningPath(userId);
        Optional<LearningPathItem> pathItemOpt = learningPathItemRepository
                .findByLearningPathIdAndModuleIdWithDetails(learningPath.getId(), module.getId());

        if (pathItemOpt.isEmpty()) {
            if (module.getStatus() != EModuleStatus.PUBLISHED) {
                throw new BaseException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Bài học thuộc mô-đun hiện chưa được xuất bản"
                );
            }
            return null;
        }

        LearningPathItem pathItem = pathItemOpt.get();
        if (pathItem.getStatus() == ELessonStatus.LOCKED) {
            log.warn("[BR10] Học viên userId={} cố truy cập moduleId={} đang bị khóa", userId, module.getId());
            throw new BaseException(
                    ErrorCode.CONTENT_LOCKED,
                    "Mô-đun đang bị khóa. Bạn phải hoàn thành các mô-đun phía trước"
            );
        }

        return pathItem;
    }
}

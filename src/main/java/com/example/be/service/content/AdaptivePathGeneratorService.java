package com.example.be.service.content;

import com.example.be.entity.content.LearningPath;
import com.example.be.entity.diagnostic.DiagnosticAttempt;

/**
 * Service sinh lộ trình học thích ứng 2 tầng dựa trên kết quả bài thi chẩn đoán (Diagnostic).
 */
public interface AdaptivePathGeneratorService {

    /**
     * Sinh lộ trình học cá nhân hóa theo AIM và kết quả làm bài chẩn đoán.
     *
     * @param attempt Lượt thi chẩn đoán của học viên
     * @param targetProfileId ID của mục tiêu điểm TOEIC đã chọn (TargetProfile)
     * @return Lộ trình học (LearningPath) đã được tối ưu và sắp xếp theo chuẩn sư phạm
     */
    LearningPath generateAdaptivePath(DiagnosticAttempt attempt, Long targetProfileId);
}

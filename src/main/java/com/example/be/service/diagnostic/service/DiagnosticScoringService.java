package com.example.be.service.diagnostic.service;

import com.example.be.dto.request.diagnostic.StartDiagnosticRequest;
import com.example.be.dto.request.diagnostic.SubmitDiagnosticRequest;
import com.example.be.dto.response.diagnostic.DiagnosticResultResponse;
import com.example.be.dto.response.diagnostic.StartDiagnosticResponse;

/**
 * Service xử lý luồng làm bài và chấm điểm 2 tầng bài kiểm tra chẩn đoán.
 */
public interface DiagnosticScoringService {

    /**
     * Bắt đầu một lượt làm bài mới cho học viên.
     * Tạo bản ghi DiagnosticAttempt với trạng thái IN_PROGRESS.
     *
     * @param userId  ID học viên đang đăng nhập
     * @param request Thông tin đề thi muốn làm
     * @return Thông tin lượt thi vừa khởi tạo
     */
    StartDiagnosticResponse startDiagnostic(Long userId, StartDiagnosticRequest request);

    /**
     * Nộp bài và thực hiện chấm điểm 2 tầng hoàn chỉnh.
     * <p>
     * Tầng 1: Tính điểm % từng Part → so sánh TargetPartThreshold → xác định directive
     * (PASS / CONFIRM / ABILITY_DIAGNOSTIC / FULL_PART).
     * Tầng 2: Với CONFIRM và ABILITY_DIAGNOSTIC, tính accuracyRate từng Ability →
     * so sánh AbilityEvaluationRule → phân loại WEAK / STABLE → lưu UserAbility.
     * Sau đó gọi AdaptivePathGeneratorService để sinh lộ trình cá nhân hóa.
     *
     * @param userId  ID học viên đang đăng nhập (chống IDOR)
     * @param request Dữ liệu nộp bài kèm đáp án và mục tiêu AIM
     * @return Kết quả chấm điểm và ID lộ trình vừa được sinh
     */
    DiagnosticResultResponse submitDiagnostic(Long userId, SubmitDiagnosticRequest request);
}

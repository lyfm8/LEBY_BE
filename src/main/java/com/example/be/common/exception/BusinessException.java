package com.example.be.common.exception;

import org.springframework.http.HttpStatus;

/**
 * HTTP 422 - Unprocessable Entity.
 * Dùng cho các business rule không hợp lệ - request hợp lệ về mặt kỹ thuật
 * nhưng vi phạm quy tắc nghiệp vụ của hệ thống LEBY.
 *
 * Ví dụ:
 * - Level/Module chưa được unlock mà user cố truy cập.
 * - User chưa đủ điều kiện làm test (chưa hoàn thành lesson).
 * - User đã hoàn thành test rồi, không thể làm lại.
 * - Không thể chuyển sang level tiếp theo khi chưa pass level hiện tại.
 * - DiagnosticTest chưa được hoàn thành mà cố tạo LearningPath.
 */
public class BusinessException extends BaseException {

    public BusinessException(String message) {
        super(HttpStatus.UNPROCESSABLE_CONTENT, message);
    }
}

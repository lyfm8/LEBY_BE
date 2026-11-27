package com.example.be.dto.response.exam;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Phương án trả lời trắc nghiệm dùng chung cho Phòng luyện tập (UC12) và Phòng thi vượt ải (UC13-UC14).
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamOptionResponse {

    /**
     * Ký hiệu phương án hiển thị (A, B, C, D).
     */
    private String key;

    /**
     * Nội dung văn bản của phương án lựa chọn.
     */
    private String text;
}

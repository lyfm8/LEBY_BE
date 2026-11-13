package com.example.be.dto.response.setting;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * DTO phản hồi kết quả kiểm tra tính hợp lệ của bộ ngưỡng hoặc quy tắc.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThresholdValidationResponse {

    private boolean valid;
    private List<String> errors;
}


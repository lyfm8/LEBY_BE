package com.example.be.dto.response.exam;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

/**
 * Dữ liệu đáp án và cờ đánh dấu đã lưu nháp trên máy chủ để khôi phục phiên thi vượt ải (UC13 Luồng 7a).
 */
@Getter
@Builder
public class ModuleTestDraftAnswerResponse {

    private Integer orderNo;
    private Long questionId;
    private String selectedOption;

    @JsonProperty("isFlagged")
    private Boolean isFlagged;
}

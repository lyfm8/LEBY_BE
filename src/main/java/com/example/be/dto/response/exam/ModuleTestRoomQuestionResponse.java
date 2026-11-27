package com.example.be.dto.response.exam;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Dữ liệu câu hỏi trong Phòng thi vượt ải mô-đun (UC13).
 * Tuyệt đối ẩn đáp án đúng và lời giải chi tiết trong quá trình đang làm bài.
 */
@Getter
@Builder
public class ModuleTestRoomQuestionResponse {

    private Long moduleTestQuestionId;
    private Long questionId;
    private Integer orderNo;
    private Long parentId;
    private Long partId;
    private Integer partNo;
    private String partName;
    private String section;
    private String title;
    private String instruction;
    private String passage;
    private String audioUrl;
    private String imageUrl;

    @JsonProperty("isGroupQuestion")
    private Boolean isGroupQuestion;

    private List<ExamOptionResponse> options;
}

package com.example.be.dto.response.exam;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Dữ liệu đầy đủ của Phòng luyện tập bài thực hành (UC12).
 */
@Getter
@Builder
public class PracticeRoomResponse {

    private Long lessonId;
    private String lessonTitle;
    private Long moduleId;
    private String moduleTitle;
    private String partName;
    private String section;
    private String abilityName;
    private Integer totalQuestions;

    @JsonProperty("isEmpty")
    private Boolean isEmpty;

    private List<PracticeRoomQuestionResponse> questions;
}

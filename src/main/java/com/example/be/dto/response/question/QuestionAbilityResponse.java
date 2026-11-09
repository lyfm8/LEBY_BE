package com.example.be.dto.response.question;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Phản hồi thông tin kỹ năng năng lực (Ability) gán vào câu hỏi.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionAbilityResponse {

    private Long abilityId;
    private String abilityName;
    private Integer orderNo;
    private Integer points;
}


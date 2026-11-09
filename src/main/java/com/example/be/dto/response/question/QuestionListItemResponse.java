package com.example.be.dto.response.question;

import com.example.be.enums.ability.ESection;
import com.example.be.enums.media.EMediaKind;
import com.example.be.enums.question.EQuestionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Phản hồi tóm tắt cho từng hàng trong bảng danh sách Ngân hàng câu hỏi.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionListItemResponse {

    private Long id;
    private String name;
    private String summary;
    private EQuestionType type;
    private Long partId;
    private String partName;
    private ESection section;
    private Integer difficulty;
    private boolean isGroup;
    private int itemCount;
    private List<String> abilityNames;
    private List<EMediaKind> mediaKinds;
    private LocalDateTime updatedAt;
}


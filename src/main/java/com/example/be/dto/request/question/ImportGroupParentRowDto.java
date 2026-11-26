package com.example.be.dto.request.question;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO trung gian chứa thông tin ngữ cảnh chung của một dòng loại CHÙM trong sheet "Chùm câu hỏi".
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportGroupParentRowDto {

    private int rowNumber;
    private String groupRowId;
    private String name;
    private String rawPartId;
    private Long partId;
    private String instruction;
    private String passage;
    private String transcript;
    private String translation;
    private String rawMediaIds;
    private List<Long> mediaIds;
    private String descriptions;
}

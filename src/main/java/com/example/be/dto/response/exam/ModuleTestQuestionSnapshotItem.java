package com.example.be.dto.response.exam;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Bản ghi snapshot lưu vị trí câu hỏi và ánh xạ xáo trộn phương án A-B-C-D cho một lượt thi Module Test (BR05).
 * Trong {@code optionMap}: key là ký hiệu hiển thị trên phòng thi (A/B/C/D), value là key gốc trong bảng questions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModuleTestQuestionSnapshotItem {

    private Integer orderNo;
    private Long questionId;
    private Long moduleTestQuestionId;
    private Map<String, String> optionMap;
}

package com.example.be.dto.response.dashboard;

import com.example.be.enums.ability.ESection;
import com.example.be.enums.diagnostic.EPartDirective;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO đánh giá năng lực theo từng Phần thi TOEIC (Part 1 - Part 7) trên Dashboard học viên (UC08).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDashboardPartEvalDto {

    private Long partId;
    private int partNo;
    private String name;
    private ESection section;
    private int scorePercent;
    private EPartDirective directive;

    /** Nhãn Tiếng Việt tự nhiên hiển thị trực tiếp lên UI thay cho mã enum kỹ thuật. */
    private String directiveLabel;
}

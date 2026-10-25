package com.example.be.entity.setting;

import com.example.be.entity.base.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Bảng quy đổi điểm chuẩn TOEIC (10 - 990) dựa theo số câu trả lời đúng (Phụ lục 2).
 */
@Entity
@Table(name = "toeic_score_conversions")
@Getter
@Setter
@NoArgsConstructor
public class ToeicScoreConversion extends AbstractEntity {

    /**
     * Số câu trả lời đúng (1 - 100 câu).
     */
    @Column(nullable = false, unique = true)
    private Integer correctCount;

    /**
     * Điểm quy đổi phần thi Nghe (Listening 5 - 495).
     */
    @Column(nullable = false)
    private Integer listeningScore;

    /**
     * Điểm quy đổi phần thi Đọc (Reading 5 - 495).
     */
    @Column(nullable = false)
    private Integer readingScore;
}

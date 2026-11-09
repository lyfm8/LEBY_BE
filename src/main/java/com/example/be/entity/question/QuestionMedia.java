package com.example.be.entity.question;

import com.example.be.entity.media.Media;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Thực thể liên kết giữa Câu hỏi (Question) và Tệp đa phương tiện (Media).
 * Cho phép một câu hỏi liên kết nhiều media (ví dụ: Part 1 có cả ảnh và audio).
 */
@Entity
@Table(
        name = "question_medias",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_question_media", columnNames = {"question_id", "media_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "media_id", nullable = false)
    private Media media;

    /**
     * Thứ tự hiển thị của tệp media trong câu hỏi.
     */
    @Column(name = "order_no", nullable = false)
    private Integer orderNo;
}


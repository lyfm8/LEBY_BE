package com.example.be.entity.question;

import com.example.be.entity.base.AbstractEntity;
import com.example.be.entity.content.Lesson;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Thực thể trung gian (Association Entity) gắn các câu hỏi trong Ngân hàng câu hỏi (Question)
 * vào một bài học luyện tập (Lesson loại PRACTICE).
 * Kế thừa AbstractEntity để tự động quản lý khóa chính id cùng dấu thời gian createdAt và updatedAt.
 */
@Entity
@Table(
        name = "practice_questions",
        uniqueConstraints = {
                // Chặn gắn trùng cùng 1 câu hỏi 2 lần vào cùng một bài luyện tập
                @UniqueConstraint(name = "uk_practice_lesson_question", columnNames = {"lesson_id", "question_id"})
        },
        indexes = {
                // Đánh index khóa ngoại lesson_id để tăng tốc truy vấn danh sách câu hỏi luyện tập theo bài học
                @Index(name = "idx_practice_questions_lesson_id", columnList = "lesson_id"),
                // Đánh index khóa ngoại question_id để kiểm tra nhanh câu hỏi có đang được gắn vào bài Practice hay không
                @Index(name = "idx_practice_questions_question_id", columnList = "question_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class PracticeQuestion extends AbstractEntity {

    /**
     * Thứ tự hiển thị câu hỏi trong bài luyện tập (1, 2, 3...).
     */
    @Column(name = "order_no", nullable = false)
    private Integer orderNo;

    /**
     * Điểm số phân bổ cho câu hỏi này trong bài luyện tập.
     */
    @Column(nullable = false)
    private Integer points;

    /**
     * Bài học luyện tập (Lesson) sở hữu câu hỏi này.
     * Dùng FetchType.LAZY để tránh tải thừa dữ liệu bài học khi chỉ cần thông tin câu hỏi.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    /**
     * Câu hỏi gốc tham chiếu từ Ngân hàng câu hỏi (Question).
     * Dùng FetchType.LAZY để tối ưu hiệu năng truy vấn danh sách.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;
}

package com.example.be.entity.question;

import com.example.be.entity.base.AbstractEntity;
import com.example.be.entity.content.Part;
import com.example.be.enums.ability.ESection;
import com.example.be.enums.question.EQuestionType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Thực thể quản lý câu hỏi trong Ngân hàng câu hỏi của hệ thống LEBY.
 * Hỗ trợ cả câu hỏi đơn lập (Part 1, 2, 5) và chùm câu hỏi chung ngữ cảnh (Part 3, 4, 6, 7).
 */
@Entity
@Table(
        name = "questions",
        indexes = {
                @Index(name = "idx_questions_part_id", columnList = "part_id"),
                @Index(name = "idx_questions_parent_id", columnList = "parent_id"),
                @Index(name = "idx_questions_type", columnList = "type"),
                @Index(name = "idx_questions_difficulty", columnList = "difficulty")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Question extends AbstractEntity {

    /**
     * Hình thức tương tác của câu hỏi (mặc định SINGLE_CHOICE cho đề thi TOEIC).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EQuestionType type = EQuestionType.SINGLE_CHOICE;

    /**
     * Tên gợi nhớ / tóm tắt câu hỏi (hiển thị trên danh sách quản trị).
     */
    @Column(nullable = false, length = 200)
    private String name;

    /**
     * Thứ tự câu hỏi trong chùm (ví dụ câu thứ 1, 2, 3 trong chùm Part 3).
     */
    @Column(name = "order_no")
    private Integer orderNo;

    /**
     * Nội dung câu hỏi và các phương án lựa chọn A, B, C, D (hoặc ngữ cảnh đoạn văn bài đọc) dưới dạng chuỗi JSON.
     */
    @Column(name = "question_data", columnDefinition = "JSON")
    private String questionData;

    /**
     * Đáp án đúng của câu hỏi dưới dạng chuỗi JSON (ví dụ: {"key": "A"}).
     */
    @Column(name = "correct_answer", columnDefinition = "JSON")
    private String correctAnswer;

    /**
     * Độ khó của câu hỏi (thang điểm từ 1 đến 5).
     */
    private Integer difficulty;

    /**
     * Phần thi tương ứng (LISTENING / READING).
     * Được đồng bộ tự động theo Part.sections.
     */
    @Enumerated(EnumType.STRING)
    private ESection sections;

    /**
     * Mô tả thêm hoặc ghi chú nội bộ cho giáo viên / biên soạn đề.
     */
    @Column(columnDefinition = "TEXT")
    private String descriptions;

    /**
     * Phần thi Part mà câu hỏi thuộc về (Part 1 đến Part 7).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "part_id", nullable = true)
    private Part part;

    /**
     * Giải thích chi tiết câu hỏi (transcript, translation, clue, analysis, vocabulary) dưới dạng chuỗi JSON.
     */
    @Column(name = "explanation", columnDefinition = "JSON")
    private String explanation;

    /**
     * Câu hỏi / Ngữ cảnh cha chứa bài đọc (Part 6, 7) hoặc audio chung (Part 3, 4).
     * Nếu là câu hỏi độc lập (Part 1, 2, 5) hoặc chính bản ghi cha thì parent = null.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", nullable = true)
    private Question parent;

    /**
     * Danh sách các câu hỏi con thuộc về chùm câu hỏi này.
     */
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("orderNo ASC")
    private List<Question> subQuestions = new ArrayList<>();

    /**
     * Danh sách các tệp đa phương tiện (Audio, Image, Video) gắn với câu hỏi.
     */
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("orderNo ASC")
    private List<QuestionMedia> questionMedias = new ArrayList<>();

    /**
     * Danh sách các kỹ năng năng lực (Ability) gắn với câu hỏi này.
     */
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<QuestionAbility> questionAbilities = new ArrayList<>();
}

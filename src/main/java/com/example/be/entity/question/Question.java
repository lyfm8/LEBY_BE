package com.example.be.entity.question;

import com.example.be.entity.ability.Ability;
import com.example.be.entity.base.AbstractEntity;
import com.example.be.entity.content.Part;
import com.example.be.enums.ability.ESection;
import com.example.be.enums.question.EQuestionType;
import tools.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

/**
 * Question - base entity cho tất cả câu hỏi trong hệ thống ngân hàng đề thi.
 * Hỗ trợ các liên kết media độc lập: audioUrl, videoUrl, imageUrl.
 */
@Entity
@Table(name = "questions")
@Getter
@Setter
@NoArgsConstructor
public class Question extends AbstractEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EQuestionType type;

    @Column(nullable = false, length = 200)
    private String name;

    /**
     * Đường dẫn file âm thanh câu hỏi / bài nghe (Audio clip / Listening audio).
     */
    @Column(name = "audio_url", length = 500)
    private String audioUrl;

    /**
     * Đường dẫn video liên quan đến câu hỏi / giải thích (Video clip).
     */
    @Column(name = "video_url", length = 500)
    private String videoUrl;

    /**
     * Đường dẫn hình ảnh câu hỏi / bài đọc / biểu đồ minh họa (Photo / Diagram / Reading scan).
     */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /**
     * questionData: JSON - nội dung câu hỏi (text, các lựa chọn A/B/C/D...)
     * Rule 17: Dùng JsonNode + @JdbcTypeCode(SqlTypes.JSON), không dùng String.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSON")
    private JsonNode questionData;

    /**
     * correctAnswer: JSON - đáp án đúng
     * Rule 17: Dùng JsonNode.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSON")
    private JsonNode correctAnswer;

    /**
     * difficulty: Int - độ khó (1-5)
     */
    private Integer difficulty;

    /**
     * sections: ESection - thuộc section nào (LISTENING/READING)
     */
    @Enumerated(EnumType.STRING)
    private ESection sections;

    @Column(columnDefinition = "TEXT")
    private String descriptions;

    /**
     * Question "references" Part - câu hỏi thuộc Part nào.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "part_id", nullable = true)
    private Part part;

    /**
     * Giải thích chi tiết câu hỏi (transcript, translation, clue, analysis, vocabulary) theo Phụ lục 1.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSON")
    private JsonNode explanation;

    /**
     * Câu hỏi cha chứa nội dung bài đọc (Part 6, 7) hoặc audio chung (Part 3, 4).
     * Nếu là câu hỏi độc lập (Part 1, 2, 5) thì parent = null.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", nullable = true)
    private Question parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Question> subQuestions = new ArrayList<>();

    /**
     * Question 1 ---- * QuestionAbility (M-M với Ability qua entity trung gian).
     * Rule 12: Entity trung gian vì QuestionAbility có thuộc tính riêng.
     */
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<QuestionAbility> questionAbilities = new ArrayList<>();
}

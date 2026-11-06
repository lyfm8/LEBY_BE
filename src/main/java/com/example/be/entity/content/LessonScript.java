package com.example.be.entity.content;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity lưu trữ từng bản ghi phụ đề / kịch bản lời thoại (Script record) của bài nghe (Listening Lesson).
 * Hỗ trợ đồng bộ tua nhanh phát âm thanh/video theo timestamp.
 */
@Entity
@Table(name = "lesson_scripts")
@Getter
@Setter
@NoArgsConstructor
public class LessonScript {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Thứ tự hiển thị của câu thoại trong bài học.
     */
    @Column(name = "order_no", nullable = false)
    private Integer orderNo;

    /**
     * Thời điểm bắt đầu của câu thoại (định dạng mm:ss hoặc số giây dạng chuỗi, ví dụ: "00:00" hoặc "0.0").
     */
    @Column(name = "start_time", nullable = false, length = 20)
    private String startTime;

    /**
     * Thời điểm kết thúc của câu thoại (định dạng mm:ss hoặc số giây dạng chuỗi, ví dụ: "00:03" hoặc "3.5").
     */
    @Column(name = "end_time", nullable = false, length = 20)
    private String endTime;

    /**
     * Tên nhân vật hoặc người nói (ví dụ: "Anna", "John", "Speaker A").
     */
    @Column(length = 100)
    private String speaker;

    /**
     * Nội dung câu thoại tiếng Anh (bắt buộc).
     */
    @Column(name = "content_en", nullable = false, columnDefinition = "TEXT")
    private String contentEn;

    /**
     * Bản dịch tiếng Việt của câu thoại (không bắt buộc).
     */
    @Column(name = "content_vi", columnDefinition = "TEXT")
    private String contentVi;

    /**
     * Bài học (Lesson) sở hữu bản ghi Script này.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;
}


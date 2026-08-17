package com.example.be.features.content.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Lesson dạng video.
 * Extends Lesson (JOINED table strategy).
 * Rule 20: Phải dùng Java inheritance, không tạo Entity riêng độc lập.
 */
@Entity
@Table(name = "video_lessons")
@Getter
@Setter
@NoArgsConstructor
public class VideoLesson extends Lesson {

    /**
     * uri: String - URL của video
     */
    @Column(nullable = false, length = 500)
    private String uri;

    /**
     * duration: Int - thời lượng video (giây hoặc phút)
     */
    private Integer duration;
}

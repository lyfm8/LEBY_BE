package com.example.be.entity.content;

import com.example.be.entity.ability.Ability;
import com.example.be.entity.base.AbstractEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity đại diện cho bài học (Lesson) trong một Module học tập.
 * Hỗ trợ các liên kết media độc lập: audioUrl, videoUrl, imageUrl.
 */
@Entity
@Table(name = "lessons")
@Getter
@Setter
@NoArgsConstructor
public class Lesson extends AbstractEntity {

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false)
    private Integer orderNo;

    @Column(columnDefinition = "TEXT")
    private String descriptions;

    /**
     * Đường dẫn file âm thanh bài học (Listening / Audio guide).
     */
    @Column(name = "audio_url", length = 500)
    private String audioUrl;

    /**
     * Đường dẫn video bài giảng (Video lecture).
     */
    @Column(name = "video_url", length = 500)
    private String videoUrl;

    /**
     * Đường dẫn hình ảnh minh họa / sơ đồ / tài liệu trực quan (Picture / Diagram).
     */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /**
     * Cờ đánh dấu bài học miễn phí hay yêu cầu trả phí (Freemium). Mặc định true.
     */
    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean isFree = true;

    /**
     * Lesson * ---- 1 Module.
     * FK nằm ở bảng lessons.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id", nullable = false)
    private Module module;

    /**
     * Lesson "references" Ability - lesson này train ability nào.
     * Đây là aggregation, không cascade delete.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ability_id", nullable = true)
    private Ability ability;
}

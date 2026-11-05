package com.example.be.entity.content;

import com.example.be.entity.ability.Ability;
import com.example.be.entity.base.AbstractEntity;
import com.example.be.enums.content.EModuleStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Entity đại diện cho bài học (Lesson) trong một Module học tập.
 * Hỗ trợ các liên kết media độc lập: audioUrl, videoUrl, imageUrl,
 * nội dung rich text descriptions, và kịch bản phụ đề (LessonScript).
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EModuleStatus status = EModuleStatus.DRAFT;

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
     * Lesson thuộc về 1 Module cụ thể.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id", nullable = false)
    private Module module;

    /**
     * Lesson rèn luyện Năng lực (Ability) nào.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ability_id", nullable = true)
    private Ability ability;

    /**
     * Danh sách các bản ghi kịch bản lời thoại (Script) phục vụ bài nghe Listening.
     */
    @OneToMany(
            mappedBy = "lesson",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<LessonScript> scripts = new ArrayList<>();
}

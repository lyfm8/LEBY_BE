package com.example.be.entity.media;

import com.example.be.entity.base.AbstractEntity;
import com.example.be.entity.user.User;
import com.example.be.enums.media.EMediaResourceType;
import com.example.be.enums.media.EMediaStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Thực thể quản lý các tệp tin media (ảnh, video, audio) được lưu trữ trên Cloudinary.
 * Đóng vai trò là nguồn dữ liệu trung tâm phục vụ cho câu hỏi TOEIC, bài giảng bài học và hồ sơ người dùng.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "medias", uniqueConstraints = {
        @UniqueConstraint(name = "uk_medias_public_id", columnNames = "public_id")
})
public class Media extends AbstractEntity {

    /**
     * Mã định danh duy nhất của tài nguyên trên Cloudinary (dùng để quản lý/xóa file).
     */
    @Column(name = "public_id", nullable = false)
    private String publicId;

    /**
     * Đường dẫn an toàn HTTPS truy cập tệp tin trên Cloudinary.
     */
    @Column(name = "secure_url", nullable = false, columnDefinition = "TEXT")
    private String secureUrl;

    /**
     * Phân loại tài nguyên (IMAGE, VIDEO, RAW, AUTO).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false)
    private EMediaResourceType resourceType;

    /**
     * Định dạng tệp tin (png, jpg, mp3, mp4, webp...).
     */
    @Column(name = "format", nullable = false, length = 50)
    private String format;

    /**
     * Kích thước dung lượng của tệp tin tính theo Byte.
     */
    @Column(name = "bytes", nullable = false)
    private Long bytes;

    /**
     * Chiều rộng hình ảnh/video (tính theo pixel, có thể null với audio/raw).
     */
    @Column(name = "width")
    private Integer width;

    /**
     * Chiều cao hình ảnh/video (tính theo pixel, có thể null với audio/raw).
     */
    @Column(name = "height")
    private Integer height;

    /**
     * Trạng thái vòng đời của media (PENDING hoặc ACTIVE).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private EMediaStatus status;

    /**
     * Thời điểm hết hạn của media ở trạng thái PENDING.
     * Khi chuyển sang ACTIVE thì trường này được đặt về null.
     */
    @Column(name = "pending_expires_at")
    private LocalDateTime pendingExpiresAt;

    /**
     * Người dùng (Admin/Teacher/Student) đã thực hiện tải tệp tin này lên hệ thống.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;
}

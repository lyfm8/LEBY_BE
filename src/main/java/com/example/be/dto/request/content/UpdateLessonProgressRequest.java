package com.example.be.dto.request.content;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO nhận nhịp đồng bộ tiến độ xem bài giảng Video từ trình phát phía Frontend (UC11).
 * Áp dụng nghiêm ngặt các giới hạn @Min/@Max và @DecimalMin/@DecimalMax để chống giả mạo payload.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateLessonProgressRequest {

    /**
     * Số giây xem thực tế tăng thêm kể từ lần đồng bộ trước (tối đa 300 giây/nhịp, Backend sẽ kiểm chứng lại theo thời gian thực).
     */
    @NotNull(message = "Thời lượng xem tăng thêm không được để trống")
    @Min(value = 0, message = "Thời lượng xem tăng thêm không được âm")
    @Max(value = 300, message = "Thời lượng xem tăng thêm mỗi lần đồng bộ tối đa là 300 giây")
    private Integer watchedSecondsDelta;

    /**
     * Mốc thời gian hiện tại trên trình phát (giây) để lưu vị trí tạm dừng phục vụ tính năng Xem tiếp (Resume).
     */
    @NotNull(message = "Vị trí hiện tại của video không được để trống")
    @Min(value = 0, message = "Vị trí hiện tại không được âm")
    @Max(value = 86400, message = "Vị trí hiện tại không vượt quá 24 giờ")
    private Integer lastPositionSeconds;

    /**
     * Thời lượng thực tế trình phát đọc được từ tệp video (chỉ dùng fallback khi DB chưa ghi nhận durationSeconds > 0).
     */
    @Min(value = 0, message = "Thời lượng video không được âm")
    @Max(value = 86400, message = "Thời lượng video không vượt quá 24 giờ")
    private Integer reportedDurationSeconds;

    /**
     * Tốc độ phát video hiện tại (0.5x - 2.0x) để Backend tính biên độ dung sai chống gian lận hợp lệ.
     */
    @DecimalMin(value = "0.5", message = "Tốc độ phát tối thiểu là 0.5x")
    @DecimalMax(value = "2.0", message = "Tốc độ phát tối đa là 2.0x")
    private Double playbackRate;
}

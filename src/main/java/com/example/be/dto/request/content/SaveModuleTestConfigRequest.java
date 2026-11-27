package com.example.be.dto.request.content;

import com.example.be.enums.content.EModuleTestSelectionMode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO lưu cấu hình Bài thi vượt ải mô-đun (Use Case A07).
 * Áp dụng Bean Validation chống Payload DoS (@Size) và chống dữ liệu số âm/tràn số (@Min, @Max).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveModuleTestConfigRequest {

    /**
     * Thời gian làm bài thi vượt ải (tính bằng phút, mẫu 25 phút).
     * Giữ đúng chuẩn tên trường durationMinutes theo quy chuẩn leby-anticonflict.
     */
    @NotNull(message = "Thời gian làm bài không được để trống")
    @Min(value = 1, message = "Số lượng câu hỏi và thời gian làm bài phải lớn hơn 0")
    @Max(value = 180, message = "Thời gian làm bài không được vượt quá 180 phút")
    private Integer durationMinutes;

    /**
     * Số lượng câu hỏi trong bài thi vượt ải (mẫu 25 câu, linh hoạt 1-100).
     */
    @NotNull(message = "Số lượng câu hỏi không được để trống")
    @Min(value = 1, message = "Số lượng câu hỏi và thời gian làm bài phải lớn hơn 0")
    @Max(value = 100, message = "Số lượng câu hỏi không được vượt quá 100 câu")
    private Integer questionCount;

    /**
     * Cờ kích hoạt tự động xáo trộn câu hỏi và đáp án mỗi lần thi (BR05).
     */
    @NotNull(message = "Trạng thái xáo trộn câu hỏi không được để trống")
    private Boolean shuffleQuestions;

    /**
     * Chế độ chọn câu hỏi: MANUAL (chọn thủ công) hoặc RANDOM_BY_ABILITY (sinh ngẫu nhiên từ kho Ability).
     */
    @NotNull(message = "Phương thức chọn câu hỏi không được để trống")
    private EModuleTestSelectionMode selectionMode;

    /**
     * Danh sách ID câu hỏi gán vào bài thi (dùng khi selectionMode = MANUAL).
     * SECURITY: Giới hạn tối đa 100 phần tử/request để chống JSON Payload DoS.
     */
    @Size(max = 100, message = "Danh sách câu hỏi không được vượt quá 100 câu")
    private List<Long> questionIds;

    /**
     * Cờ xác nhận của Quản trị viên khi cố tình đưa câu hỏi ngoài Ability của mô-đun vào đề (Luồng ngoại lệ 6b).
     */
    private Boolean confirmMismatchAbility;
}

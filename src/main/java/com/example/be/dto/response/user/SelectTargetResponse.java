package com.example.be.dto.response.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Phản hồi sau khi học viên xác nhận chọn hoặc đổi mục tiêu AIM (UC05).
 * Chứa chỉ dẫn điều hướng (redirectAction) để Frontend đưa học viên tới phòng thi chẩn đoán
 * hoặc thẳng sang lộ trình học tập mới vừa tái sinh.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SelectTargetResponse {

    public static final String ACTION_GO_DIAGNOSTIC = "GO_DIAGNOSTIC";
    public static final String ACTION_GO_LEARNING_PATH = "GO_LEARNING_PATH";

    private Long userId;

    private Long targetProfileId;

    private Integer targetTotalScore;

    /**
     * Chỉ dẫn điều hướng sau khi chọn/đổi AIM:
     * - GO_DIAGNOSTIC: Chuyển sang phòng thi chẩn đoán (lần đầu hoặc chưa có dữ liệu chẩn đoán).
     * - GO_LEARNING_PATH: Lộ trình mới đã được tái sinh (đổi AIM khi đã có kết quả chẩn đoán).
     */
    private String redirectAction;

    /**
     * ID lộ trình học mới được sinh (chỉ có giá trị khi redirectAction = GO_LEARNING_PATH).
     */
    private Long learningPathId;
}

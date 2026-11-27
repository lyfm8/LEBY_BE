package com.example.be.enums.content;

/**
 * Chế độ thiết lập bộ câu hỏi cho Bài thi vượt ải mô-đun (Use Case A07).
 * - MANUAL: Quản trị viên chọn thủ công từng câu hỏi từ Ngân hàng câu hỏi.
 * - RANDOM_BY_ABILITY: Hệ thống tự động bốc ngẫu nhiên theo số lượng từ kho câu hỏi thuộc Ability của mô-đun (Luồng 6a).
 */
public enum EModuleTestSelectionMode {
    MANUAL,
    RANDOM_BY_ABILITY
}

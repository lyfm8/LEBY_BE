package com.example.be.enums.ability;

public enum EAbilityStatus {
    WEAK,        // Yếu, cần bốc module học bổ trợ
    DEVELOPING,  // Đang phát triển / Trung bình
    STABLE,      // Thành thạo / Ổn định
    UNCERTAIN    // Chưa đủ độ tin cậy (chưa đủ minEvidenceCount câu hỏi)
}

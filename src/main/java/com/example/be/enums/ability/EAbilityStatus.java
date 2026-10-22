package com.example.be.enums.ability;

public enum EAbilityStatus {
    LEVEL_1_WEAK,        // Mức 1 (Dưới level1): Yếu, cần cải thiện
    LEVEL_2_DEVELOPING,  // Mức 2 (Từ level1 -> level2): Đang phát triển / Trung bình
    LEVEL_3_STABLE,      // Mức 3 (Trên level2): Thành thạo / Ổn định
    UNCERTAIN            // Chưa xác định (chưa làm đủ số câu minEvidenceCount)
}

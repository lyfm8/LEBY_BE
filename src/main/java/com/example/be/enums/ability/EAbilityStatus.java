package com.example.be.enums.ability;

public enum EAbilityStatus {
    WEAK,        // Yếu, cần bốc module học bổ trợ
    DEVELOPING,  // Đang phát triển (tương thích dữ liệu seed data / lịch sử)
    STABLE,      // Thành thạo / Ổn định (đạt chuẩn stableMin)
    UNCERTAIN    // Chưa đủ độ tin cậy (chưa đủ số câu hỏi bằng chứng)
}

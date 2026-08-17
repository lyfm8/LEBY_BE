package com.example.be.features.diagnostic.enums;

/**
 * Loại bài test Diagnostic.
 * Dùng để phân biệt bài test Tầng 1 (đánh giá Part) và Tầng 2 (đánh giá Ability).
 */
public enum ETestType {
    TIER_1_PART,      // Bài test tổng hợp để lọc ra Part yếu
    TIER_2_ABILITY    // Bài test chuyên sâu đánh giá Ability của Part yếu
}

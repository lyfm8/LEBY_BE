package com.example.be.enums.diagnostic;

public enum EPartDirective {
    LEVEL_1_FULL_PART,          // Mức 1 (Dưới level1 - Thấp nhất): Yếu toàn diện, phải học lại cả Part
    LEVEL_2_ABILITY_DIAGNOSTIC,  // Mức 2 (Từ level1 -> level2): Cần làm bài chẩn đoán sâu từng kỹ năng
    LEVEL_3_CONFIRM,             // Mức 3 (Từ level2 -> level3): Đạt chuẩn, chỉ cần luyện tập củng cố
    LEVEL_4_PASS                 // Mức 4 (Trên level3/level4 - Cao nhất): Quá giỏi, bỏ qua không cần học Part này
}

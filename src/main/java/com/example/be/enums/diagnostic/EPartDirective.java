package com.example.be.enums.diagnostic;

/**
 * Chỉ thị điều hướng 4 nhánh Tầng 1 cho từng Part sau bài thi chẩn đoán.
 */
public enum EPartDirective {
    PASS,                 // Nhánh 1: Đạt chuẩn AIM -> Miễn học Part này (0 module)
    CONFIRM,              // Nhánh 2: Cận chuẩn -> Xuống Tầng 2 lọc khắt khe (developingThreshold ~50%)
    ABILITY_DIAGNOSTIC,   // Nhánh 3: Dưới chuẩn -> Xuống Tầng 2 lọc an toàn (stableThreshold ~75%)
    FULL_PART,            // Nhánh 4: Mất gốc (< fullPartThreshold) -> Bốc toàn bộ 100% module của Part

}

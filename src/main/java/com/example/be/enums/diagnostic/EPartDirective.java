package com.example.be.enums.diagnostic;

/**
 * Chỉ thị điều hướng 3 nhánh Tầng 1 cho từng Part sau bài thi chẩn đoán (Diagnostic Test).
 *
 * Sơ đồ phân nhánh:
 *   - PASS: actualScore >= passScore -> Miễn học Part này (0 module)
 *   - CONFIRM: confirmingScore <= actualScore < passScore -> Cận chuẩn, xuống Tầng 2 lọc theo Ability
 *   - WEAK: actualScore < confirmingScore -> Dưới chuẩn / mất gốc, bốc toàn bộ 100% module của Part
 */
public enum EPartDirective {
    /**
     * Nhánh 1: Đạt chuẩn AIM -> Miễn học Part này (0 module).
     */
    PASS,

    /**
     * Nhánh 2: Cận chuẩn -> Xuống Tầng 2 xét từng Ability (accuracyRate < stableMin).
     */
    CONFIRM,

    /**
     * Nhánh 3: Dưới chuẩn / Mất gốc (< confirmingScore) -> Bốc toàn bộ 100% module của Part.
     */
    WEAK
}

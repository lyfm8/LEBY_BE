package com.example.be.enums.ability;

/**
 * ESection đại diện cho section TOEIC (Listening/Reading).
 * Được dùng trong Ability.sections và Part.source.
 *
 * DECISION: Values dựa trên context TOEIC của hệ thống.
 * Cần xác nhận lại nếu diagram có thêm giá trị khác.
 */
public enum ESection {
    LISTENING,
    READING
}

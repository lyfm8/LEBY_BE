package com.example.be.enums.content;

/**
 * Trạng thái của lộ trình học tập (LearningPath).
 */
public enum ELearningPathStatus {
    ACTIVE,     // Đang trong quá trình học
    ARCHIVED,   // Đã lưu trữ (khi học viên đổi AIM hoặc làm lại bài chẩn đoán)
    COMPLETED   // Đã hoàn thành toàn bộ lộ trình
}

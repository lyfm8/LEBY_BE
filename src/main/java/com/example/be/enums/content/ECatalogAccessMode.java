package com.example.be.enums.content;

/**
 * Chế độ quyền truy cập của một Mô-đun trên màn hình Thư viện khóa học 7 Part (UC17 Catalog).
 * Quy tắc nhất quán với BR10 (mở khóa tuần tự lộ trình) và BR13 (xem trước đề cương):
 * - {@link #ROADMAP_OPEN}: Mô-đun thuộc lộ trình cá nhân và đã mở khóa (UNLOCKED, IN_PROGRESS, COMPLETED).
 * - {@link #ROADMAP_LOCKED}: Mô-đun thuộc lộ trình cá nhân nhưng đang khóa tuần tự (LOCKED theo BR10).
 * - {@link #FREE_STUDY}: Mô-đun ngoài lộ trình cá nhân của học viên đã có lộ trình — mở tự do để ôn tập bổ trợ.
 * - {@link #PREVIEW_ONLY}: Học viên chưa hoàn thành bài thi chẩn đoán (chưa có lộ trình) — chỉ xem trước đề cương.
 */
public enum ECatalogAccessMode {
    ROADMAP_OPEN,
    ROADMAP_LOCKED,
    FREE_STUDY,
    PREVIEW_ONLY
}

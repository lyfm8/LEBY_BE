package com.example.be.dto.response.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DTO biểu diễn một mục hoạt động học tập / khảo thí gần đây của học viên (UC08).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDashboardActivityDto {

    /** Khóa định danh duy nhất kết hợp loại sự kiện và ID bản ghi để tránh trùng key trên UI. */
    private String id;

    /** Loại hoạt động: VIDEO | PRACTICE | MODULE_TEST | DIAGNOSTIC | MOCK_EXAM. */
    private String type;

    private String title;
    private String subtitle;
    private String timeAgo;
    private LocalDateTime timestamp;

    /** Đường dẫn nội bộ phía Frontend để học viên bấm xem lại nhanh. */
    private String targetUrl;
}

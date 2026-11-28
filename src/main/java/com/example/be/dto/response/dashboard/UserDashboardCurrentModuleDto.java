package com.example.be.dto.response.dashboard;

import com.example.be.enums.content.ELessonType;
import com.example.be.enums.ability.ESection;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * DTO làm nổi bật Mô-đun đang học tiếp theo trên lộ trình tuần tự kèm bài học gần nhất (UC08 Bước 4, 5, 7 & Ngoại lệ 7a).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDashboardCurrentModuleDto {

    private Long moduleId;
    private Long learningPathItemId;
    private Integer orderNo;
    private String title;
    private String description;
    private String partName;
    private ESection section;
    private String abilityName;
    private String reason;
    private int progressPercent;
    private int requiredCompletionRate;
    private int completedLessons;
    private int totalLessons;

    @JsonProperty("isModuleTestUnlocked")
    private Boolean isModuleTestUnlocked;

    /** Cờ báo hiệu mô-đun đang bị Quản trị viên tạm khóa/chuyển DRAFT để bảo trì học liệu (UC08 Ngoại lệ 7a). */
    @JsonProperty("isUnderMaintenance")
    private Boolean isUnderMaintenance;

    /** Thông báo thân thiện khi mô-đun đang bảo trì học liệu. */
    private String maintenanceMessage;

    /** ID bài học được đề xuất bấm "Tiếp tục bài học" trực tiếp (null nếu chưa có bài hoặc đang bảo trì). */
    private Long continueLessonId;
    private String continueLessonTitle;
    private ELessonType continueLessonType;

    /** Danh sách tối đa 4 bài học gần nhất cần hoàn thành trong mô-đun hiện tại. */
    private List<UserDashboardLessonItemDto> upcomingLessons;
}

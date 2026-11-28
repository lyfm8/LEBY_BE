package com.example.be.dto.response.dashboard;

import com.example.be.enums.content.EDashboardState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * DTO phản hồi tổng hợp toàn diện cho màn hình Dashboard học tập cá nhân của học viên (UC08).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDashboardSummaryResponse {

    /** Trạng thái vòng đời học tập hiện tại giúp FE hiển thị đúng màn hình tương ứng. */
    private EDashboardState dashboardState;

    /** Thông tin hồ sơ cơ bản và mốc điểm TOEIC mục tiêu (AIM) hiện tại. */
    private UserDashboardProfileDto user;

    /** Tình trạng bài thi chẩn đoán gần nhất và điểm TOEIC ước lượng. */
    private UserDashboardDiagnosticDto diagnostic;

    /** Bộ 4 chỉ số KPI tổng hợp từ dữ liệu thật trong cơ sở dữ liệu. */
    private UserDashboardStatsDto stats;

    /** Mô-đun đang mở khóa hiện tại cần học tiếp kèm danh sách bài học (null nếu chưa học hoặc đã xong 100%). */
    private UserDashboardCurrentModuleDto currentModule;

    /** Danh sách tối đa 4 chặng học lân cận trên lộ trình cá nhân để xem nhanh bối cảnh. */
    private List<UserDashboardModuleItemDto> upcomingModules;

    /** Bảng đánh giá 7 Part TOEIC từ bài thi chẩn đoán gần nhất. */
    private List<UserDashboardPartEvalDto> partEvaluations;

    /** Dữ liệu biểu đồ tiến độ học tập 7 ngày gần nhất (từ 6 ngày trước đến hôm nay). */
    private List<UserDashboardWeeklyDayDto> weeklyProgress;

    /** Danh sách tối đa 6 hoạt động học tập/thi cử mới nhất của học viên. */
    private List<UserDashboardActivityDto> recentActivities;
}

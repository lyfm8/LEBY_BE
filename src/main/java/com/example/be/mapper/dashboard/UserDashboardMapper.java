package com.example.be.mapper.dashboard;

import com.example.be.dto.response.dashboard.UserDashboardActivityDto;
import com.example.be.dto.response.dashboard.UserDashboardCurrentModuleDto;
import com.example.be.dto.response.dashboard.UserDashboardLessonItemDto;
import com.example.be.dto.response.dashboard.UserDashboardModuleItemDto;
import com.example.be.dto.response.dashboard.UserDashboardPartEvalDto;
import com.example.be.dto.response.dashboard.UserDashboardProfileDto;
import com.example.be.entity.content.LearningPathItem;
import com.example.be.entity.content.Lesson;
import com.example.be.entity.content.LessonProgress;
import com.example.be.entity.content.Module;
import com.example.be.entity.content.ModuleTestAttempt;
import com.example.be.entity.content.Part;
import com.example.be.entity.diagnostic.DiagnosticAttempt;
import com.example.be.entity.diagnostic.PartDiagnosticResult;
import com.example.be.entity.user.TargetProfile;
import com.example.be.entity.user.User;
import com.example.be.enums.content.ELessonStatus;
import com.example.be.enums.content.ELessonType;
import com.example.be.enums.content.EModuleStatus;
import com.example.be.enums.ability.ESection;
import com.example.be.enums.diagnostic.EPartDirective;
import com.example.be.enums.diagnostic.ETestResult;
import com.example.be.enums.diagnostic.ETestType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Mapper Component chuyên biệt chuyển đổi Entity sang DTO cho màn hình Dashboard học tập cá nhân (UC08).
 * Tái sử dụng {@link AdminDashboardMapper#formatTimeAgo(LocalDateTime, LocalDateTime)} theo nguyên tắc DRY
 * và Việt hóa toàn bộ nhãn hiển thị để không lộ mã enum kỹ thuật lên giao diện người dùng.
 */
@Component
@RequiredArgsConstructor
public class UserDashboardMapper {

    private static final int MAX_PERCENT = 100;
    private static final Pattern PART_NUMBER_PATTERN = Pattern.compile("Part\\s*(\\d+)", Pattern.CASE_INSENSITIVE);

    private final AdminDashboardMapper adminDashboardMapper;

    /**
     * Ánh xạ thông tin hồ sơ học viên và mục tiêu AIM hiện hành sang {@link UserDashboardProfileDto}.
     *
     * @param user          Thực thể người dùng đang đăng nhập
     * @param targetProfile Mục tiêu điểm TOEIC hiệu lực (ưu tiên từ lộ trình hiện hành hoặc user.targetProfile)
     * @return DTO thông tin cá nhân hiển thị ở header Dashboard
     */
    public UserDashboardProfileDto toProfileDto(User user, TargetProfile targetProfile) {
        // Fallback sang email nếu học viên chưa cập nhật họ tên đầy đủ
        String fullName = (user.getFullName() != null && !user.getFullName().isBlank())
                ? user.getFullName().trim()
                : user.getEmail();

        return UserDashboardProfileDto.builder()
                .fullName(fullName)
                .avatar(user.getAvatar())
                .role(user.getRole() != null ? user.getRole().name() : "STUDENT")
                .targetProfileId(targetProfile != null ? targetProfile.getId() : null)
                .targetScore(targetProfile != null ? targetProfile.getTargetTotalScore() : null)
                .targetDescription(targetProfile != null ? targetProfile.getDescription() : null)
                .build();
    }

    /**
     * Ánh xạ kết quả chẩn đoán 1 Phần thi TOEIC sang {@link UserDashboardPartEvalDto} kèm nhãn Tiếng Việt chuẩn.
     *
     * @param result Kết quả chẩn đoán của từng Part
     * @return DTO đánh giá Part hiển thị thanh tiến độ trên Dashboard
     */
    public UserDashboardPartEvalDto toPartEvalDto(PartDiagnosticResult result) {
        Part part = result.getPart();
        int partNo = resolvePartNumber(part);
        String partName = (part != null && part.getName() != null && !part.getName().isBlank())
                ? part.getName()
                : "Part " + partNo;
        ESection section = (part != null && part.getSections() != null)
                ? part.getSections()
                : (partNo <= 4 ? ESection.LISTENING : ESection.READING);

        // Chặn giá trị phần trăm nằm gọn trong khoảng [0, 100]
        int rawPercent = result.getScore() != null ? Math.round(result.getScore()) : 0;
        int scorePercent = Math.max(0, Math.min(MAX_PERCENT, rawPercent));

        EPartDirective directive = result.getDirective();
        return UserDashboardPartEvalDto.builder()
                .partId(part != null ? part.getId() : (long) partNo)
                .partNo(partNo)
                .name(partName)
                .section(section)
                .scorePercent(scorePercent)
                .directive(directive)
                .directiveLabel(resolveDirectiveLabel(directive))
                .build();
    }

    /**
     * Ánh xạ một chặng mô-đun trên lộ trình sang {@link UserDashboardModuleItemDto} cho cột phải Dashboard.
     *
     * @param item Chặng học trong bảng learning_path_items đã nạp sẵn Module và Part
     * @return DTO mô-đun tóm tắt kèm trạng thái mở/khóa theo BR10
     */
    public UserDashboardModuleItemDto toModuleItemDto(LearningPathItem item) {
        Module module = item.getModule();
        Part part = module != null ? module.getPart() : null;
        ELessonStatus status = item.getStatus() != null ? item.getStatus() : ELessonStatus.LOCKED;
        boolean isUnderMaintenance = module != null && module.getStatus() != EModuleStatus.PUBLISHED;
        int progress = item.getProgressPercent() != null
                ? Math.max(0, Math.min(MAX_PERCENT, item.getProgressPercent()))
                : 0;

        return UserDashboardModuleItemDto.builder()
                .moduleId(module != null ? module.getId() : null)
                .learningPathItemId(item.getId())
                .orderNo(item.getOrderNo())
                .sequence(module != null ? module.getSequence() : item.getOrderNo())
                .title(module != null ? module.getTitle() : "Mô-đun học tập")
                .description(module != null ? module.getDescriptions() : null)
                .partName(part != null ? part.getName() : null)
                .section(part != null ? part.getSections() : null)
                .status(status)
                .statusLabel(resolveModuleStatusLabel(status))
                .progressPercent(progress)
                .isLocked(status == ELessonStatus.LOCKED)
                .isUnderMaintenance(isUnderMaintenance)
                .build();
    }

    /**
     * Ánh xạ một bài học trong mô-đun đang học sang {@link UserDashboardLessonItemDto}.
     *
     * @param lesson            Thực thể bài học đã xuất bản
     * @param progress          Bản ghi tiến độ cá nhân (có thể null nếu học viên chưa mở bài)
     * @param questionCount     Số câu hỏi gắn trong practice_questions (0 nếu không có)
     * @param isCompleted       Cờ hoàn thành đã tính cả bảo lưu BR06
     * @param isNextRecommended Cờ đánh dấu bài học được đề xuất bấm "Tiếp tục bài học"
     * @return DTO bài học hiển thị ngay trong khối mô-đun hiện tại
     */
    public UserDashboardLessonItemDto toLessonItemDto(
            Lesson lesson,
            LessonProgress progress,
            int questionCount,
            boolean isCompleted,
            boolean isNextRecommended
    ) {
        int watchPercent = isCompleted
                ? MAX_PERCENT
                : (progress != null && progress.getWatchPercent() != null ? progress.getWatchPercent() : 0);

        return UserDashboardLessonItemDto.builder()
                .id(lesson.getId())
                .orderNo(lesson.getOrderNo())
                .title(lesson.getTitle())
                .type(lesson.getLessonType() != null ? lesson.getLessonType() : ELessonType.VIDEO)
                .durationSeconds(lesson.getDurationSeconds() != null ? lesson.getDurationSeconds() : 0)
                .questionCount(Math.max(0, questionCount))
                .watchPercent(Math.max(0, Math.min(MAX_PERCENT, watchPercent)))
                .isCompleted(isCompleted)
                .isNextRecommended(isNextRecommended)
                .build();
    }

    /**
     * Ánh xạ thông tin mô-đun đang học hiện tại cùng danh sách bài học gợi ý sang {@link UserDashboardCurrentModuleDto}.
     *
     * @param item                   Chặng học đang mở khóa hiện tại
     * @param progressPercent        Tỷ lệ hoàn thành bài giảng (%) của mô-đun
     * @param requiredCompletionRate Ngưỡng mở khóa bài thi vượt ải theo BR03
     * @param completedLessons       Số bài học đã hoàn thành trong mô-đun
     * @param totalLessons           Tổng số bài học đã xuất bản trong mô-đun
     * @param isModuleTestUnlocked   Cờ đã đủ điều kiện thi vượt ải hay chưa
     * @param isUnderMaintenance     Cờ mô-đun đang bảo trì học liệu (Ngoại lệ 7a)
     * @param maintenanceMessage     Thông báo bảo trì nếu có
     * @param continueLesson         Bài học gợi ý tiếp tục (có thể null)
     * @param upcomingLessons        Danh sách tối đa 4 bài học sắp tới
     * @return DTO khối mô-đun trọng tâm trên Dashboard
     */
    public UserDashboardCurrentModuleDto toCurrentModuleDto(
            LearningPathItem item,
            int progressPercent,
            int requiredCompletionRate,
            int completedLessons,
            int totalLessons,
            boolean isModuleTestUnlocked,
            boolean isUnderMaintenance,
            String maintenanceMessage,
            Lesson continueLesson,
            List<UserDashboardLessonItemDto> upcomingLessons
    ) {
        Module module = item.getModule();
        Part part = module != null ? module.getPart() : null;

        return UserDashboardCurrentModuleDto.builder()
                .moduleId(module != null ? module.getId() : null)
                .learningPathItemId(item.getId())
                .orderNo(item.getOrderNo())
                .title(module != null ? module.getTitle() : "Mô-đun đang học")
                .description(module != null ? module.getDescriptions() : null)
                .partName(part != null ? part.getName() : null)
                .section(part != null ? part.getSections() : null)
                .abilityName(item.getAbility() != null ? item.getAbility().getName() : null)
                .reason(item.getReason())
                .progressPercent(Math.max(0, Math.min(MAX_PERCENT, progressPercent)))
                .requiredCompletionRate(requiredCompletionRate)
                .completedLessons(completedLessons)
                .totalLessons(totalLessons)
                .isModuleTestUnlocked(isModuleTestUnlocked)
                .isUnderMaintenance(isUnderMaintenance)
                .maintenanceMessage(maintenanceMessage)
                .continueLessonId(continueLesson != null ? continueLesson.getId() : null)
                .continueLessonTitle(continueLesson != null ? continueLesson.getTitle() : null)
                .continueLessonType(continueLesson != null ? continueLesson.getLessonType() : null)
                .upcomingLessons(upcomingLessons)
                .build();
    }

    /**
     * Chuyển đổi tiến độ học bài giảng sang mục hoạt động gần đây trên Dashboard.
     *
     * @param progress  Bản ghi tiến độ bài học (đã JOIN FETCH Lesson & Module)
     * @param timestamp Mốc thời gian tương tác gần nhất
     * @param now       Thời điểm hiện tại để tính chuỗi timeAgo
     * @return DTO hoạt động gần đây
     */
    public UserDashboardActivityDto toLessonActivityDto(
            LessonProgress progress,
            LocalDateTime timestamp,
            LocalDateTime now
    ) {
        Lesson lesson = progress.getLesson();
        Module module = lesson != null ? lesson.getModule() : null;
        boolean isPractice = lesson != null && lesson.getLessonType() == ELessonType.PRACTICE;
        boolean isCompleted = Boolean.TRUE.equals(progress.getIsCompleted());
        int watchPercent = progress.getWatchPercent() != null ? progress.getWatchPercent() : 0;

        String titlePrefix = isCompleted ? "Hoàn thành: " : "Đang học: ";
        String lessonTitle = lesson != null ? lesson.getTitle() : "Bài giảng";
        String moduleTitle = module != null ? module.getTitle() : "Mô-đun học tập";
        String subtitle = isCompleted
                ? moduleTitle + " • Đã hoàn thành bài học"
                : moduleTitle + " • Tiến độ " + watchPercent + "%";

        String targetUrl = null;
        if (module != null && lesson != null) {
            // Bài thực hành đi thẳng phòng luyện tập UC12, bài giảng đi phòng video UC11
            targetUrl = isPractice
                    ? "/modules/" + module.getId() + "/lessons/" + lesson.getId() + "/practice"
                    : "/modules/" + module.getId() + "/lessons/" + lesson.getId();
        }

        return UserDashboardActivityDto.builder()
                .id("LESSON_" + progress.getId())
                .type(isPractice ? "PRACTICE" : "VIDEO")
                .title(titlePrefix + lessonTitle)
                .subtitle(subtitle)
                .timeAgo(adminDashboardMapper.formatTimeAgo(timestamp, now))
                .timestamp(timestamp)
                .targetUrl(targetUrl)
                .build();
    }

    /**
     * Chuyển đổi lượt thi vượt ải mô-đun đã nộp sang mục hoạt động gần đây.
     *
     * @param attempt   Lượt thi vượt ải đã nộp (đã JOIN FETCH Module)
     * @param timestamp Thời điểm nộp bài
     * @param now       Thời điểm hiện tại
     * @return DTO hoạt động thi vượt ải
     */
    public UserDashboardActivityDto toModuleTestActivityDto(
            ModuleTestAttempt attempt,
            LocalDateTime timestamp,
            LocalDateTime now
    ) {
        Module module = attempt.getModule();
        String moduleTitle = module != null ? module.getTitle() : "Mô-đun học tập";
        boolean passed = attempt.getResult() == ETestResult.PASS;
        int score = attempt.getScore() != null ? attempt.getScore() : 0;

        String subtitle = String.format(
                "Điểm %d/100 • %s",
                score,
                passed ? "Vượt ải thành công" : "Chưa đạt điểm đỗ"
        );
        String targetUrl = module != null
                ? "/modules/" + module.getId() + "/test/result/" + attempt.getId()
                : "/results";

        return UserDashboardActivityDto.builder()
                .id("MODULE_TEST_" + attempt.getId())
                .type("MODULE_TEST")
                .title("Kiểm tra vượt ải: " + moduleTitle)
                .subtitle(subtitle)
                .timeAgo(adminDashboardMapper.formatTimeAgo(timestamp, now))
                .timestamp(timestamp)
                .targetUrl(targetUrl)
                .build();
    }

    /**
     * Chuyển đổi lượt thi chẩn đoán hoặc thi thử TOEIC 200 câu đã nộp sang mục hoạt động gần đây.
     *
     * @param attempt   Lượt thi đã hoàn thành (đã JOIN FETCH DiagnosticTest)
     * @param timestamp Thời điểm nộp bài
     * @param now       Thời điểm hiện tại
     * @return DTO hoạt động khảo thí
     */
    public UserDashboardActivityDto toDiagnosticActivityDto(
            DiagnosticAttempt attempt,
            LocalDateTime timestamp,
            LocalDateTime now
    ) {
        boolean isMockExam = attempt.getDiagnosticTest() != null
                && attempt.getDiagnosticTest().getTestType() == ETestType.FULL_MOCK_TEST;
        String testTitle = attempt.getDiagnosticTest() != null && attempt.getDiagnosticTest().getTitle() != null
                ? attempt.getDiagnosticTest().getTitle()
                : (isMockExam ? "Bài thi thử TOEIC 200 câu" : "Bài thi chẩn đoán đầu vào");
        int durationMinutes = attempt.getDurationSeconds() != null && attempt.getDurationSeconds() > 0
                ? Math.max(1, Math.round(attempt.getDurationSeconds() / 60.0f))
                : 0;

        String subtitle = durationMinutes > 0
                ? (isMockExam ? "Thi thử nghiệm thu đầu ra" : "Đánh giá năng lực 7 Part")
                  + " • Thời gian làm " + durationMinutes + " phút"
                : (isMockExam ? "Thi thử nghiệm thu đầu ra" : "Đánh giá năng lực 7 Part");

        String targetUrl = isMockExam
                ? "/mock-exam/report/" + attempt.getId()
                : "/diagnostic/results/" + attempt.getId();

        return UserDashboardActivityDto.builder()
                .id((isMockExam ? "MOCK_" : "DIAG_") + attempt.getId())
                .type(isMockExam ? "MOCK_EXAM" : "DIAGNOSTIC")
                .title("Hoàn thành: " + testTitle)
                .subtitle(subtitle)
                .timeAgo(adminDashboardMapper.formatTimeAgo(timestamp, now))
                .timestamp(timestamp)
                .targetUrl(targetUrl)
                .build();
    }

    /**
     * Trích xuất số thứ tự Phần thi TOEIC (1..7) từ tên Part hoặc ID dự phòng.
     *
     * @param part Thực thể Part
     * @return Số thứ tự Part từ 1 đến 7
     */
    public int resolvePartNumber(Part part) {
        if (part == null) {
            return 1;
        }
        if (part.getName() != null) {
            Matcher matcher = PART_NUMBER_PATTERN.matcher(part.getName());
            if (matcher.find()) {
                try {
                    return Integer.parseInt(matcher.group(1));
                } catch (NumberFormatException ignored) {
                    // Fallback về ID bên dưới nếu không chuyển được sang số nguyên
                }
            }
        }
        return part.getId() != null ? part.getId().intValue() : 1;
    }

    /**
     * Chuyển đổi thứ trong tuần sang nhãn ngắn gọn tiếng Việt cho biểu đồ tuần (T2..CN).
     *
     * @param dayOfWeek Thứ trong tuần của ngày tương ứng
     * @return Chuỗi nhãn ngắn gọn
     */
    public String resolveVietnameseDayLabel(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> "T2";
            case TUESDAY -> "T3";
            case WEDNESDAY -> "T4";
            case THURSDAY -> "T5";
            case FRIDAY -> "T6";
            case SATURDAY -> "T7";
            case SUNDAY -> "CN";
        };
    }

    /**
     * Dịch chỉ thị đánh giá Part Tầng 1 sang nhãn Tiếng Việt thân thiện chuẩn sản phẩm.
     */
    private String resolveDirectiveLabel(EPartDirective directive) {
        if (directive == null) {
            return "Đang cập nhật";
        }
        return switch (directive) {
            case PASS -> "Đạt chuẩn";
            case CONFIRM -> "Cận chuẩn";
            case ABILITY_DIAGNOSTIC -> "Cần cải thiện";
            case FULL_PART -> "Cần học nền tảng";
        };
    }

    /**
     * Dịch trạng thái chặng học sang nhãn Tiếng Việt thân thiện cho học viên.
     */
    private String resolveModuleStatusLabel(ELessonStatus status) {
        if (status == null) {
            return "Chưa mở khóa";
        }
        return switch (status) {
            case IN_PROGRESS -> "Đang học";
            case UNLOCKED -> "Sẵn sàng học";
            case COMPLETED -> "Đã hoàn thành";
            case LOCKED -> "Chưa mở khóa";
        };
    }
}

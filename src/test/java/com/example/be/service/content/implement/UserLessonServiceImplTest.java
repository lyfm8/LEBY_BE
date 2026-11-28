package com.example.be.service.content.implement;

import com.example.be.dto.request.content.UpdateLessonProgressRequest;
import com.example.be.dto.response.content.LessonProgressResponse;
import com.example.be.dto.response.content.UserLessonDetailResponse;
import com.example.be.entity.ability.Ability;
import com.example.be.entity.content.LearningPath;
import com.example.be.entity.content.LearningPathItem;
import com.example.be.entity.content.Lesson;
import com.example.be.entity.content.LessonProgress;
import com.example.be.entity.content.Module;
import com.example.be.entity.content.Part;
import com.example.be.entity.media.Media;
import com.example.be.entity.question.PracticeQuestion;
import com.example.be.entity.question.Question;
import com.example.be.entity.question.QuestionMedia;
import com.example.be.entity.user.User;
import com.example.be.enums.ability.ESection;
import com.example.be.enums.content.ELearningPathStatus;
import com.example.be.enums.content.ELessonStatus;
import com.example.be.enums.content.ELessonType;
import com.example.be.enums.content.EModuleStatus;
import com.example.be.enums.media.EMediaResourceType;
import com.example.be.exception.BaseException;
import com.example.be.exception.ErrorCode;
import com.example.be.mapper.content.UserLessonMapper;
import com.example.be.repository.content.LearningPathItemRepository;
import com.example.be.repository.content.LearningPathRepository;
import com.example.be.repository.content.LessonProgressRepository;
import com.example.be.repository.content.LessonRepository;
import com.example.be.repository.question.PracticeQuestionRepository;
import com.example.be.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit Tests kiểm thử toàn diện nghiệp vụ UC11 (Học bài giảng Video & Ghi nhận tiến độ học LessonProgress):
 * - Chống truy cập bài học thuộc Module bị khóa BR10 (422 CONTENT_LOCKED) & bài DRAFT/không tồn tại (404)
 * - Nạp lịch sử xem để Resume (Luồng 3) và bóc tách JSON câu hỏi ôn tập kèm câu hỏi cha & media (0 N+1)
 * - Giám sát xem chưa đủ ngưỡng 60% (Luồng 4a, 5a) vs đạt >= 60% tự hoàn thành & mở khóa Module Test (BR09 + BR03)
 * - Chống gian lận gửi delta thời gian xem dồn dập bằng kiểm soát wall-clock
 * - Bảo toàn trạng thái hoàn thành khi xem lại video đã hoàn thành (Luồng 4b)
 * - Chặn gọi API tiến độ video cho bài loại PRACTICE
 */
@ExtendWith(MockitoExtension.class)
class UserLessonServiceImplTest {

    private static final Long USER_ID = 10L;
    private static final Long PATH_ID = 100L;
    private static final Long MODULE_ID = 1L;
    private static final Long LESSON_1_ID = 101L;
    private static final Long LESSON_2_ID = 102L;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private LessonProgressRepository lessonProgressRepository;

    @Mock
    private LearningPathRepository learningPathRepository;

    @Mock
    private LearningPathItemRepository learningPathItemRepository;

    @Mock
    private PracticeQuestionRepository practiceQuestionRepository;

    @Mock
    private UserRepository userRepository;

    private UserLessonServiceImpl userLessonService;

    @BeforeEach
    void setUp() {
        UserLessonMapper mapper = new UserLessonMapper(new ObjectMapper());
        userLessonService = new UserLessonServiceImpl(
                lessonRepository,
                lessonProgressRepository,
                learningPathRepository,
                learningPathItemRepository,
                practiceQuestionRepository,
                userRepository,
                mapper
        );
    }

    @Test
    @DisplayName("UC11 - BR10: Ném 422 CONTENT_LOCKED khi truy cập bài giảng thuộc mô-đun đang bị khóa")
    void getLessonDetailForUser_whenModuleLocked_throwsContentLocked() {
        Module module = buildModule(MODULE_ID, "Part 1: Tranh mô tả người");
        Lesson lesson = buildLesson(LESSON_1_ID, module, "Bài 1", 1, ELessonType.VIDEO, 100);
        LearningPath path = buildLearningPath(PATH_ID, ELearningPathStatus.ACTIVE);
        LearningPathItem lockedItem = buildPathItem(11L, path, module, ELessonStatus.LOCKED, 0);

        when(lessonRepository.findByIdWithModulePartAndAbility(LESSON_1_ID)).thenReturn(Optional.of(lesson));
        when(learningPathRepository.findByUserIdAndStatusInOrderByVersionDescCreatedAtDesc(eq(USER_ID), anyCollection()))
                .thenReturn(List.of(path));
        when(learningPathItemRepository.findByLearningPathIdAndModuleIdWithDetails(PATH_ID, MODULE_ID))
                .thenReturn(Optional.of(lockedItem));

        assertThatThrownBy(() -> userLessonService.getLessonDetailForUser(USER_ID, LESSON_1_ID))
                .isInstanceOf(BaseException.class)
                .extracting(ex -> ((BaseException) ex).getErrorCode())
                .isEqualTo(ErrorCode.CONTENT_LOCKED);
    }

    @Test
    @DisplayName("UC11 - Ném 404 RESOURCE_NOT_FOUND khi bài học đang ở trạng thái DRAFT")
    void getLessonDetailForUser_whenLessonDraft_throwsResourceNotFound() {
        Module module = buildModule(MODULE_ID, "Part 5: Ngữ pháp");
        Lesson draftLesson = buildLesson(LESSON_1_ID, module, "Bài nháp", 1, ELessonType.VIDEO, 120);
        draftLesson.setStatus(EModuleStatus.DRAFT);

        when(lessonRepository.findByIdWithModulePartAndAbility(LESSON_1_ID)).thenReturn(Optional.of(draftLesson));

        assertThatThrownBy(() -> userLessonService.getLessonDetailForUser(USER_ID, LESSON_1_ID))
                .isInstanceOf(BaseException.class)
                .extracting(ex -> ((BaseException) ex).getErrorCode())
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    @DisplayName("UC11 - Luồng 3: Trả về vị trí Resume, bài tiếp theo và bóc tách câu hỏi ôn tập kèm câu hỏi cha + media")
    void getLessonDetailForUser_whenHasHistoryAndReviewQuestions_returnsResumeAndParsedQuestions() {
        Module module = buildModule(MODULE_ID, "Part 7: Đọc hiểu email");
        Lesson lesson1 = buildLesson(LESSON_1_ID, module, "Bài 1: Chiến thuật đọc lướt", 1, ELessonType.VIDEO, 600);
        Lesson lesson2 = buildLesson(LESSON_2_ID, module, "Bài 2: Luyện tập", 2, ELessonType.PRACTICE, 0);
        LearningPath path = buildLearningPath(PATH_ID, ELearningPathStatus.ACTIVE);
        LearningPathItem activeItem = buildPathItem(11L, path, module, ELessonStatus.IN_PROGRESS, 0);

        LessonProgress existingProgress = new LessonProgress();
        existingProgress.setLesson(lesson1);
        existingProgress.setWatchedSeconds(180);
        existingProgress.setLastPositionSeconds(215);
        existingProgress.setWatchPercent(30);
        existingProgress.setIsCompleted(false);

        Question parent = new Question();
        parent.setId(500L);
        parent.setQuestionData("{\"passage\":\"Dear team, the meeting is moved to Friday.\"}");

        Question child = new Question();
        child.setId(501L);
        child.setName("When is the meeting?");
        child.setParent(parent);
        child.setQuestionData("{\"text\":\"When is the meeting?\",\"options\":[{\"key\":\"A\",\"text\":\"Monday\"},{\"key\":\"B\",\"text\":\"Friday\"}]}");
        child.setCorrectAnswer("{\"key\":\"B\"}");
        child.setExplanation("{\"clue\":\"moved to Friday\",\"analysis\":\"Email ghi rõ lịch dời sang thứ Sáu.\",\"vocabulary\":[{\"word\":\"move\",\"type\":\"v\",\"meaning\":\"dời lịch\"}]}");

        PracticeQuestion pq = new PracticeQuestion();
        pq.setId(900L);
        pq.setOrderNo(1);
        pq.setPoints(10);
        pq.setLesson(lesson1);
        pq.setQuestion(child);

        Media parentImg = Media.builder()
                .secureUrl("https://res.cloudinary.com/demo/image/upload/passage.jpg")
                .resourceType(EMediaResourceType.IMAGE)
                .format("jpg")
                .build();
        QuestionMedia parentMedia = QuestionMedia.builder()
                .question(parent)
                .media(parentImg)
                .orderNo(1)
                .build();

        when(lessonRepository.findByIdWithModulePartAndAbility(LESSON_1_ID)).thenReturn(Optional.of(lesson1));
        when(learningPathRepository.findByUserIdAndStatusInOrderByVersionDescCreatedAtDesc(eq(USER_ID), anyCollection()))
                .thenReturn(List.of(path));
        when(learningPathItemRepository.findByLearningPathIdAndModuleIdWithDetails(PATH_ID, MODULE_ID))
                .thenReturn(Optional.of(activeItem));
        when(lessonRepository.findByModuleIdAndStatusWithAbilityOrderByOrderNoAsc(MODULE_ID, EModuleStatus.PUBLISHED))
                .thenReturn(List.of(lesson1, lesson2));
        when(lessonProgressRepository.findByUserIdAndLessonIdIn(USER_ID, List.of(LESSON_1_ID, LESSON_2_ID)))
                .thenReturn(List.of(existingProgress));
        when(practiceQuestionRepository.findAllWithQuestionAndPartByLessonId(LESSON_1_ID))
                .thenReturn(List.of(pq));
        when(lessonRepository.findQuestionMediasByQuestionIdIn(anyCollection()))
                .thenReturn(List.of(parentMedia));

        UserLessonDetailResponse result = userLessonService.getLessonDetailForUser(USER_ID, LESSON_1_ID);

        assertThat(result.getWatchedSeconds()).isEqualTo(180);
        assertThat(result.getLastPositionSeconds()).isEqualTo(215);
        assertThat(result.getWatchPercent()).isEqualTo(30);
        assertThat(result.getRequiredWatchPercent()).isEqualTo(60);
        assertThat(result.getIsCompleted()).isFalse();
        assertThat(result.getNextLessonId()).isEqualTo(LESSON_2_ID);
        assertThat(result.getNextLessonType()).isEqualTo(ELessonType.PRACTICE);
        assertThat(result.getModuleLessons()).hasSize(2);
        assertThat(result.getReviewQuestions()).hasSize(1);
        assertThat(result.getReviewQuestions().get(0).getParentContextText()).contains("moved to Friday");
        assertThat(result.getReviewQuestions().get(0).getParentImageUrl()).isEqualTo("https://res.cloudinary.com/demo/image/upload/passage.jpg");
        assertThat(result.getReviewQuestions().get(0).getCorrectAnswer()).isEqualTo("B");
        assertThat(result.getReviewQuestions().get(0).getExplanation().getVocabulary()).hasSize(1);
    }

    @Test
    @DisplayName("UC11 - Luồng 4a & 5a: Dừng xem trước mốc 60% chỉ lưu vị trí tạm dừng, chưa hoàn thành bài giảng")
    void updateLessonProgress_whenWatchedBelowThreshold_savesPositionWithoutCompleting() {
        Module module = buildModule(MODULE_ID, "Part 1: Tranh");
        Lesson lesson = buildLesson(LESSON_1_ID, module, "Bài 1", 1, ELessonType.VIDEO, 100);
        LearningPath path = buildLearningPath(PATH_ID, ELearningPathStatus.ACTIVE);
        LearningPathItem unlockedItem = buildPathItem(11L, path, module, ELessonStatus.UNLOCKED, 0);

        User userRef = new User();
        userRef.setId(USER_ID);

        when(lessonRepository.findByIdWithModulePartAndAbility(LESSON_1_ID)).thenReturn(Optional.of(lesson));
        when(learningPathRepository.findByUserIdAndStatusInOrderByVersionDescCreatedAtDesc(eq(USER_ID), anyCollection()))
                .thenReturn(List.of(path));
        when(learningPathItemRepository.findByLearningPathIdAndModuleIdWithDetails(PATH_ID, MODULE_ID))
                .thenReturn(Optional.of(unlockedItem));
        when(lessonProgressRepository.findByUserIdAndLessonId(USER_ID, LESSON_1_ID)).thenReturn(Optional.empty());
        when(userRepository.getReferenceById(USER_ID)).thenReturn(userRef);
        when(lessonProgressRepository.save(any(LessonProgress.class))).thenAnswer(inv -> inv.getArgument(0));
        when(lessonRepository.findByModuleIdAndStatusWithAbilityOrderByOrderNoAsc(MODULE_ID, EModuleStatus.PUBLISHED))
                .thenReturn(List.of(lesson));
        when(lessonProgressRepository.countCompletedLessonsByUserAndModuleAndType(
                USER_ID, MODULE_ID, EModuleStatus.PUBLISHED, ELessonType.VIDEO)).thenReturn(0L);

        // Giả lập học viên xem thực tế 25s nhưng tua thanh trượt tới giây 95
        UpdateLessonProgressRequest request = UpdateLessonProgressRequest.builder()
                .watchedSecondsDelta(25)
                .lastPositionSeconds(95)
                .playbackRate(1.0)
                .build();

        LessonProgressResponse response = userLessonService.updateLessonProgress(USER_ID, LESSON_1_ID, request);

        assertThat(response.getWatchedSeconds()).isEqualTo(25);
        assertThat(response.getLastPositionSeconds()).isEqualTo(95);
        assertThat(response.getWatchPercent()).isEqualTo(25);
        assertThat(response.getIsCompleted()).isFalse();
        assertThat(response.getJustCompleted()).isFalse();
        assertThat(response.getModuleTestUnlocked()).isFalse();
        // Đảm bảo LearningPathItem chuyển từ UNLOCKED sang IN_PROGRESS khi bắt đầu học
        assertThat(unlockedItem.getStatus()).isEqualTo(ELessonStatus.IN_PROGRESS);
        verify(learningPathItemRepository).save(unlockedItem);
    }

    @Test
    @DisplayName("UC11 - BR09 & BR03: Đạt >= 60% thời lượng tự động hoàn thành bài học, cập nhật tiến độ Module và mở khóa Module Test")
    void updateLessonProgress_whenWatchedReaches60Percent_marksCompletedAndUnlocksModuleTest() {
        Module module = buildModule(MODULE_ID, "Part 2: Hỏi đáp");
        module.setRequiredCompletionRate(80);
        Lesson lesson1 = buildLesson(LESSON_1_ID, module, "Bài 1", 1, ELessonType.VIDEO, 100);
        LearningPath path = buildLearningPath(PATH_ID, ELearningPathStatus.ACTIVE);
        LearningPathItem pathItem = buildPathItem(11L, path, module, ELessonStatus.IN_PROGRESS, 0);

        LessonProgress existing = new LessonProgress();
        existing.setLesson(lesson1);
        existing.setWatchedSeconds(45);
        existing.setLastPositionSeconds(45);
        existing.setWatchPercent(45);
        existing.setIsCompleted(false);
        existing.setLastSyncedAt(LocalDateTime.now().minusSeconds(20));

        when(lessonRepository.findByIdWithModulePartAndAbility(LESSON_1_ID)).thenReturn(Optional.of(lesson1));
        when(learningPathRepository.findByUserIdAndStatusInOrderByVersionDescCreatedAtDesc(eq(USER_ID), anyCollection()))
                .thenReturn(List.of(path));
        when(learningPathItemRepository.findByLearningPathIdAndModuleIdWithDetails(PATH_ID, MODULE_ID))
                .thenReturn(Optional.of(pathItem));
        when(lessonProgressRepository.findByUserIdAndLessonId(USER_ID, LESSON_1_ID)).thenReturn(Optional.of(existing));
        when(lessonProgressRepository.save(any(LessonProgress.class))).thenAnswer(inv -> inv.getArgument(0));
        when(lessonRepository.findByModuleIdAndStatusWithAbilityOrderByOrderNoAsc(MODULE_ID, EModuleStatus.PUBLISHED))
                .thenReturn(List.of(lesson1));
        when(lessonProgressRepository.countCompletedLessonsByUserAndModuleAndType(
                USER_ID, MODULE_ID, EModuleStatus.PUBLISHED, ELessonType.VIDEO)).thenReturn(1L);

        UpdateLessonProgressRequest request = UpdateLessonProgressRequest.builder()
                .watchedSecondsDelta(20)
                .lastPositionSeconds(65)
                .playbackRate(1.0)
                .build();

        LessonProgressResponse response = userLessonService.updateLessonProgress(USER_ID, LESSON_1_ID, request);

        assertThat(response.getWatchedSeconds()).isEqualTo(65);
        assertThat(response.getWatchPercent()).isEqualTo(65);
        assertThat(response.getIsCompleted()).isTrue();
        assertThat(response.getJustCompleted()).isTrue();
        assertThat(response.getCompletedAt()).isNotNull();
        assertThat(response.getModuleProgressPercent()).isEqualTo(100);
        assertThat(response.getModuleTestUnlocked()).isTrue();
        assertThat(response.getModuleTestJustUnlocked()).isTrue();
        assertThat(pathItem.getProgressPercent()).isEqualTo(100);
    }

    @Test
    @DisplayName("UC11 - SECURITY BR09: Giới hạn delta xem theo thời gian thực để chặn spam API hoàn thành gian lận")
    void updateLessonProgress_whenClientSpamsLargeDeltaTooFast_capsDeltaByWallClock() {
        Module module = buildModule(MODULE_ID, "Part 3: Hội thoại");
        Lesson lesson = buildLesson(LESSON_1_ID, module, "Bài 1", 1, ELessonType.VIDEO, 300);
        LearningPath path = buildLearningPath(PATH_ID, ELearningPathStatus.ACTIVE);
        LearningPathItem pathItem = buildPathItem(11L, path, module, ELessonStatus.IN_PROGRESS, 0);

        LessonProgress existing = new LessonProgress();
        existing.setLesson(lesson);
        existing.setWatchedSeconds(10);
        existing.setLastPositionSeconds(10);
        existing.setWatchPercent(3);
        existing.setIsCompleted(false);
        // Vừa đồng bộ cách đây 2 giây nhưng kẻ xấu gửi thẳng 250 giây
        existing.setLastSyncedAt(LocalDateTime.now().minusSeconds(2));

        when(lessonRepository.findByIdWithModulePartAndAbility(LESSON_1_ID)).thenReturn(Optional.of(lesson));
        when(learningPathRepository.findByUserIdAndStatusInOrderByVersionDescCreatedAtDesc(eq(USER_ID), anyCollection()))
                .thenReturn(List.of(path));
        when(learningPathItemRepository.findByLearningPathIdAndModuleIdWithDetails(PATH_ID, MODULE_ID))
                .thenReturn(Optional.of(pathItem));
        when(lessonProgressRepository.findByUserIdAndLessonId(USER_ID, LESSON_1_ID)).thenReturn(Optional.of(existing));
        when(lessonProgressRepository.save(any(LessonProgress.class))).thenAnswer(inv -> inv.getArgument(0));
        when(lessonRepository.findByModuleIdAndStatusWithAbilityOrderByOrderNoAsc(MODULE_ID, EModuleStatus.PUBLISHED))
                .thenReturn(List.of(lesson));
        when(lessonProgressRepository.countCompletedLessonsByUserAndModuleAndType(
                USER_ID, MODULE_ID, EModuleStatus.PUBLISHED, ELessonType.VIDEO)).thenReturn(0L);

        UpdateLessonProgressRequest spoofedRequest = UpdateLessonProgressRequest.builder()
                .watchedSecondsDelta(250)
                .lastPositionSeconds(280)
                .playbackRate(1.0)
                .build();

        LessonProgressResponse response = userLessonService.updateLessonProgress(USER_ID, LESSON_1_ID, spoofedRequest);

        // Chỉ chấp nhận tối đa vài giây tương ứng thời gian thực trôi qua, tuyệt đối không nhảy lên 260s
        assertThat(response.getWatchedSeconds()).isLessThan(30);
        assertThat(response.getIsCompleted()).isFalse();
    }

    @Test
    @DisplayName("UC11 - Luồng 4b: Xem lại bài giảng đã hoàn thành giữ nguyên isCompleted=true và completedAt")
    void updateLessonProgress_whenRewatchingCompletedLesson_preservesCompletedStatus() {
        Module module = buildModule(MODULE_ID, "Part 5: Ngữ pháp");
        Lesson lesson = buildLesson(LESSON_1_ID, module, "Bài 1", 1, ELessonType.VIDEO, 100);
        LearningPath path = buildLearningPath(PATH_ID, ELearningPathStatus.ACTIVE);
        LearningPathItem pathItem = buildPathItem(11L, path, module, ELessonStatus.IN_PROGRESS, 100);

        LocalDateTime originalCompletedAt = LocalDateTime.now().minusDays(2);
        LessonProgress existing = new LessonProgress();
        existing.setLesson(lesson);
        existing.setWatchedSeconds(80);
        existing.setLastPositionSeconds(80);
        existing.setWatchPercent(80);
        existing.setIsCompleted(true);
        existing.setCompletedAt(originalCompletedAt);
        existing.setLastSyncedAt(LocalDateTime.now().minusMinutes(5));

        when(lessonRepository.findByIdWithModulePartAndAbility(LESSON_1_ID)).thenReturn(Optional.of(lesson));
        when(learningPathRepository.findByUserIdAndStatusInOrderByVersionDescCreatedAtDesc(eq(USER_ID), anyCollection()))
                .thenReturn(List.of(path));
        when(learningPathItemRepository.findByLearningPathIdAndModuleIdWithDetails(PATH_ID, MODULE_ID))
                .thenReturn(Optional.of(pathItem));
        when(lessonProgressRepository.findByUserIdAndLessonId(USER_ID, LESSON_1_ID)).thenReturn(Optional.of(existing));
        when(lessonProgressRepository.save(any(LessonProgress.class))).thenAnswer(inv -> inv.getArgument(0));
        when(lessonRepository.findByModuleIdAndStatusWithAbilityOrderByOrderNoAsc(MODULE_ID, EModuleStatus.PUBLISHED))
                .thenReturn(List.of(lesson));
        when(lessonProgressRepository.countCompletedLessonsByUserAndModuleAndType(
                USER_ID, MODULE_ID, EModuleStatus.PUBLISHED, ELessonType.VIDEO)).thenReturn(1L);

        UpdateLessonProgressRequest request = UpdateLessonProgressRequest.builder()
                .watchedSecondsDelta(10)
                .lastPositionSeconds(15)
                .playbackRate(1.0)
                .build();

        LessonProgressResponse response = userLessonService.updateLessonProgress(USER_ID, LESSON_1_ID, request);

        ArgumentCaptor<LessonProgress> captor = ArgumentCaptor.forClass(LessonProgress.class);
        verify(lessonProgressRepository).save(captor.capture());

        assertThat(response.getIsCompleted()).isTrue();
        assertThat(response.getJustCompleted()).isFalse();
        assertThat(response.getCompletedAt()).isEqualTo(originalCompletedAt);
        assertThat(captor.getValue().getLastPositionSeconds()).isEqualTo(15);
    }

    @Test
    @DisplayName("UC11 - Ném 400 BAD_REQUEST khi gọi cập nhật tiến độ xem Video cho bài loại PRACTICE")
    void updateLessonProgress_whenLessonIsPracticeType_throwsBadRequest() {
        Module module = buildModule(MODULE_ID, "Part 5");
        Lesson practiceLesson = buildLesson(LESSON_2_ID, module, "Bài tập thực hành", 2, ELessonType.PRACTICE, 0);

        when(lessonRepository.findByIdWithModulePartAndAbility(LESSON_2_ID)).thenReturn(Optional.of(practiceLesson));

        UpdateLessonProgressRequest request = UpdateLessonProgressRequest.builder()
                .watchedSecondsDelta(10)
                .lastPositionSeconds(10)
                .build();

        assertThatThrownBy(() -> userLessonService.updateLessonProgress(USER_ID, LESSON_2_ID, request))
                .isInstanceOf(BaseException.class)
                .extracting(ex -> ((BaseException) ex).getErrorCode())
                .isEqualTo(ErrorCode.BAD_REQUEST);
    }

    @Test
    @DisplayName("UC17 - Học tự do: Cho phép xem & lưu LessonProgress cho bài Video ngoài lộ trình mà không sửa LearningPathItem")
    void freeStudyLesson_whenModuleOutsideRoadmapAndPublished_savesProgressWithoutTouchingRoadmap() {
        Module freeModule = buildModule(MODULE_ID, "Part 2 Học tự do");
        freeModule.setStatus(EModuleStatus.PUBLISHED);
        Lesson videoLesson = buildLesson(LESSON_1_ID, freeModule, "Bài giảng bổ trợ", 1, ELessonType.VIDEO, 100);
        LearningPath path = buildLearningPath(PATH_ID, ELearningPathStatus.ACTIVE);
        User userRef = new User();
        userRef.setId(USER_ID);

        when(lessonRepository.findByIdWithModulePartAndAbility(LESSON_1_ID)).thenReturn(Optional.of(videoLesson));
        when(learningPathRepository.findByUserIdAndStatusInOrderByVersionDescCreatedAtDesc(eq(USER_ID), anyCollection()))
                .thenReturn(List.of(path));
        when(learningPathItemRepository.findByLearningPathIdAndModuleIdWithDetails(PATH_ID, MODULE_ID))
                .thenReturn(Optional.empty());
        when(lessonRepository.findByModuleIdAndStatusWithAbilityOrderByOrderNoAsc(MODULE_ID, EModuleStatus.PUBLISHED))
                .thenReturn(List.of(videoLesson));
        when(lessonProgressRepository.findByUserIdAndLessonIdIn(USER_ID, List.of(LESSON_1_ID)))
                .thenReturn(List.of());
        when(practiceQuestionRepository.findAllWithQuestionAndPartByLessonId(LESSON_1_ID)).thenReturn(List.of());

        UserLessonDetailResponse detail = userLessonService.getLessonDetailForUser(USER_ID, LESSON_1_ID);
        assertThat(detail.getInRoadmap()).isFalse();
        assertThat(detail.getModuleTestUnlocked()).isFalse();

        // Cập nhật tiến độ đạt 60% -> vẫn lưu LessonProgress, nhưng không đụng vào LearningPathItem
        when(lessonProgressRepository.findByUserIdAndLessonId(USER_ID, LESSON_1_ID)).thenReturn(Optional.empty());
        when(userRepository.getReferenceById(USER_ID)).thenReturn(userRef);
        when(lessonProgressRepository.save(any(LessonProgress.class))).thenAnswer(inv -> inv.getArgument(0));
        when(lessonProgressRepository.countCompletedLessonsByUserAndModuleAndType(
                USER_ID, MODULE_ID, EModuleStatus.PUBLISHED, ELessonType.VIDEO)).thenReturn(1L);

        UpdateLessonProgressRequest req = UpdateLessonProgressRequest.builder()
                .watchedSecondsDelta(60)
                .lastPositionSeconds(60)
                .playbackRate(1.0)
                .build();

        LessonProgressResponse progressRes = userLessonService.updateLessonProgress(USER_ID, LESSON_1_ID, req);

        assertThat(progressRes.getIsCompleted()).isTrue();
        assertThat(progressRes.getJustCompleted()).isTrue();
        assertThat(progressRes.getModuleTestUnlocked()).isFalse();
        verify(learningPathItemRepository, never()).save(any(LearningPathItem.class));
    }

    private Module buildModule(Long id, String title) {
        Part part = new Part();
        part.setId(1L);
        part.setName("Part 1");
        part.setSections(ESection.LISTENING);

        Module module = new Module();
        module.setId(id);
        module.setTitle(title);
        module.setSequence(1);
        module.setStatus(EModuleStatus.PUBLISHED);
        module.setRequiredCompletionRate(80);
        module.setPart(part);
        return module;
    }

    private Lesson buildLesson(Long id, Module module, String title, int orderNo, ELessonType type, int durationSeconds) {
        Ability ability = new Ability();
        ability.setId(10L);
        ability.setName("Kỹ năng nhận diện hành động");

        Lesson lesson = new Lesson();
        lesson.setId(id);
        lesson.setModule(module);
        lesson.setAbility(ability);
        lesson.setTitle(title);
        lesson.setOrderNo(orderNo);
        lesson.setLessonType(type);
        lesson.setDurationSeconds(durationSeconds);
        lesson.setStatus(EModuleStatus.PUBLISHED);
        lesson.setIsFree(true);
        lesson.setVideoUrl("https://res.cloudinary.com/demo/video/upload/lesson.mp4");
        lesson.setDescriptions("Nội dung lý thuyết trọng tâm");
        return lesson;
    }

    private LearningPath buildLearningPath(Long id, ELearningPathStatus status) {
        LearningPath path = new LearningPath();
        path.setId(id);
        path.setVersion(1);
        path.setStatus(status);
        return path;
    }

    private LearningPathItem buildPathItem(
            Long id,
            LearningPath path,
            Module module,
            ELessonStatus status,
            int progressPercent
    ) {
        LearningPathItem item = new LearningPathItem();
        item.setId(id);
        item.setLearningPath(path);
        item.setModule(module);
        item.setOrderNo(1);
        item.setStatus(status);
        item.setProgressPercent(progressPercent);
        return item;
    }
}

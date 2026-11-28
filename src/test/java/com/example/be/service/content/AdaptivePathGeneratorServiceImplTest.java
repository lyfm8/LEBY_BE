package com.example.be.service.content;

import com.example.be.entity.content.LearningPath;
import com.example.be.entity.content.LearningPathItem;
import com.example.be.entity.content.Module;
import com.example.be.entity.content.Part;
import com.example.be.entity.diagnostic.DiagnosticAttempt;
import com.example.be.entity.diagnostic.PartDiagnosticResult;
import com.example.be.entity.setting.ModuleTargetThreshold;
import com.example.be.entity.setting.TargetPartThreshold;
import com.example.be.entity.user.TargetProfile;
import com.example.be.entity.user.User;
import com.example.be.enums.content.ELearningPathStatus;
import com.example.be.enums.content.ELessonStatus;
import com.example.be.enums.content.ELessonType;
import com.example.be.enums.content.EModuleStatus;
import com.example.be.enums.diagnostic.ETestResult;
import com.example.be.repository.content.LearningPathItemRepository;
import com.example.be.repository.content.LearningPathRepository;
import com.example.be.repository.content.LessonProgressRepository;
import com.example.be.repository.content.LessonRepository;
import com.example.be.repository.content.ModuleRepository;
import com.example.be.repository.content.ModuleTestAttemptRepository;
import com.example.be.repository.setting.AbilityEvaluationRuleRepository;
import com.example.be.repository.setting.ModuleTargetThresholdRepository;
import com.example.be.repository.setting.TargetPartThresholdRepository;
import com.example.be.repository.user.TargetProfileRepository;
import com.example.be.service.content.implement.AdaptivePathGeneratorServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test cho {@link AdaptivePathGeneratorServiceImpl}, tập trung vào version lộ trình,
 * quy tắc bảo lưu BR06 (bản cập nhật 25/11) và kế thừa tiến độ học thực tế từ bảng {@code lesson_progress}
 * khi học viên đổi mục tiêu AIM (UC05 & UC16 Bổ sung).
 */
@ExtendWith(MockitoExtension.class)
class AdaptivePathGeneratorServiceImplTest {

    @Mock private TargetPartThresholdRepository targetPartThresholdRepo;
    @Mock private AbilityEvaluationRuleRepository abilityRuleRepo;
    @Mock private ModuleRepository moduleRepo;
    @Mock private LessonRepository lessonRepo;
    @Mock private LessonProgressRepository lessonProgressRepo;
    @Mock private LearningPathRepository learningPathRepo;
    @Mock private LearningPathItemRepository learningPathItemRepo;
    @Mock private TargetProfileRepository targetProfileRepo;
    @Mock private ModuleTestAttemptRepository moduleTestAttemptRepo;
    @Mock private ModuleTargetThresholdRepository moduleTargetThresholdRepo;

    @InjectMocks
    private AdaptivePathGeneratorServiceImpl service;

    @Captor
    private ArgumentCaptor<List<LearningPathItem>> itemsCaptor;

    private User student;
    private TargetProfile profile550;
    private TargetProfile profile650;
    private TargetProfile profile850;
    private Part part1;
    private Module module1;
    private Module module2;
    private DiagnosticAttempt attempt;

    @BeforeEach
    void setUp() {
        student = new User();
        student.setId(1L);

        profile550 = new TargetProfile();
        profile550.setId(1L);
        profile550.setTargetTotalScore(550);

        profile650 = new TargetProfile();
        profile650.setId(2L);
        profile650.setTargetTotalScore(650);

        profile850 = new TargetProfile();
        profile850.setId(5L);
        profile850.setTargetTotalScore(850);

        part1 = new Part();
        part1.setId(1L);
        part1.setName("Part 1");

        module1 = new Module();
        module1.setId(10L);
        module1.setTitle("Nghe tranh cơ bản");
        module1.setSequence(1);
        module1.setDefaultPassScore(70);
        module1.setPart(part1);

        module2 = new Module();
        module2.setId(20L);
        module2.setTitle("Nghe tranh nâng cao");
        module2.setSequence(2);
        module2.setDefaultPassScore(70);
        module2.setPart(part1);

        PartDiagnosticResult partResult = new PartDiagnosticResult();
        partResult.setPart(part1);
        partResult.setScore(10.0f); // Rơi vào FULL_PART để bốc toàn bộ module 1 & 2

        attempt = new DiagnosticAttempt();
        attempt.setId(100L);
        attempt.setUser(student);
        attempt.setPartResults(List.of(partResult));
    }

    @Test
    void generateAdaptivePath_upgradeAim_preservesLessonProgressButRequiresModuleTestRetake() {
        // Đổi từ 650 -> 850 (nâng AIM), module1 đã từng PASS trước đó với điểm 80
        LearningPath oldPath = new LearningPath();
        oldPath.setId(50L);
        oldPath.setVersion(1);
        oldPath.setStatus(ELearningPathStatus.ACTIVE);
        oldPath.setTargetProfile(profile650);

        when(targetProfileRepo.findById(5L)).thenReturn(Optional.of(profile850));
        when(learningPathRepo.findWithTargetProfileByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(oldPath));
        when(learningPathRepo.save(any(LearningPath.class))).thenAnswer(inv -> {
            LearningPath lp = inv.getArgument(0);
            lp.setId(51L);
            return lp;
        });

        TargetPartThreshold threshold850 = new TargetPartThreshold();
        threshold850.setPassScore(90.0f);
        threshold850.setConfirmingScore(75.0f);
        threshold850.setFullPartThreshold(50.0f);
        when(targetPartThresholdRepo.findByTargetProfileIdAndPartId(5L, 1L)).thenReturn(Optional.of(threshold850));
        when(moduleRepo.findByPartIdAndStatusOrderBySequenceAsc(1L, EModuleStatus.PUBLISHED))
                .thenReturn(List.of(module1, module2));

        // Module 1 từng PASS 80 điểm
        when(moduleTestAttemptRepo.findPassedModuleMaxScoresByUserId(1L, ETestResult.PASS))
                .thenReturn(Collections.singletonList(new Object[]{10L, 80}));
        when(moduleTargetThresholdRepo.findByTargetProfileId(5L)).thenReturn(Collections.emptyList());

        LearningPath newPath = service.generateAdaptivePath(attempt, 5L);

        assertThat(oldPath.getStatus()).isEqualTo(ELearningPathStatus.ARCHIVED);
        assertThat(newPath.getVersion()).isEqualTo(2);

        verify(learningPathItemRepo).saveAll(itemsCaptor.capture());
        List<LearningPathItem> savedItems = itemsCaptor.getValue();
        assertThat(savedItems).hasSize(2);

        // Module 1: bảo lưu 100% bài học (không cần học lại lesson), nhưng isCorrect=false & IN_PROGRESS để thi lại module test
        LearningPathItem item1 = savedItems.get(0);
        assertThat(item1.getModule().getId()).isEqualTo(10L);
        assertThat(item1.getProgressPercent()).isEqualTo(100);
        assertThat(item1.getIsCorrect()).isFalse();
        assertThat(item1.getStatus()).isEqualTo(ELessonStatus.IN_PROGRESS);
        assertThat(item1.getReason()).contains("70/100");

        // Module 2: chưa pass bao giờ và chưa có LessonProgress -> progress 0%, LOCKED
        LearningPathItem item2 = savedItems.get(1);
        assertThat(item2.getModule().getId()).isEqualTo(20L);
        assertThat(item2.getProgressPercent()).isEqualTo(0);
        assertThat(item2.getIsCorrect()).isFalse();
        assertThat(item2.getStatus()).isEqualTo(ELessonStatus.LOCKED);
    }

    @Test
    void generateAdaptivePath_downgradeAimWithSufficientScore_marksModuleCompletedAndUnlocksNext() {
        // Hạ từ 650 -> 550, module1 từng PASS 80 >= threshold mới 65 -> bảo lưu COMPLETED, mở khóa module2
        LearningPath oldPath = new LearningPath();
        oldPath.setId(50L);
        oldPath.setVersion(2);
        oldPath.setStatus(ELearningPathStatus.ACTIVE);
        oldPath.setTargetProfile(profile650);

        when(targetProfileRepo.findById(1L)).thenReturn(Optional.of(profile550));
        when(learningPathRepo.findWithTargetProfileByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(oldPath));
        when(learningPathRepo.save(any(LearningPath.class))).thenAnswer(inv -> inv.getArgument(0));

        TargetPartThreshold threshold550 = new TargetPartThreshold();
        threshold550.setPassScore(65.0f);
        threshold550.setConfirmingScore(50.0f);
        threshold550.setFullPartThreshold(30.0f);
        when(targetPartThresholdRepo.findByTargetProfileIdAndPartId(1L, 1L)).thenReturn(Optional.of(threshold550));
        when(moduleRepo.findByPartIdAndStatusOrderBySequenceAsc(1L, EModuleStatus.PUBLISHED))
                .thenReturn(List.of(module1, module2));

        when(moduleTestAttemptRepo.findPassedModuleMaxScoresByUserId(1L, ETestResult.PASS))
                .thenReturn(Collections.singletonList(new Object[]{10L, 80}));

        ModuleTargetThreshold customThreshold550 = new ModuleTargetThreshold();
        customThreshold550.setModule(module1);
        customThreshold550.setPassScore(65);
        when(moduleTargetThresholdRepo.findByTargetProfileId(1L)).thenReturn(List.of(customThreshold550));

        LearningPath newPath = service.generateAdaptivePath(attempt, 1L);

        assertThat(newPath.getVersion()).isEqualTo(3);
        assertThat(newPath.getStatus()).isEqualTo(ELearningPathStatus.ACTIVE);

        verify(learningPathItemRepo).saveAll(itemsCaptor.capture());
        List<LearningPathItem> savedItems = itemsCaptor.getValue();

        LearningPathItem item1 = savedItems.get(0);
        assertThat(item1.getStatus()).isEqualTo(ELessonStatus.COMPLETED);
        assertThat(item1.getIsCorrect()).isTrue();
        assertThat(item1.getProgressPercent()).isEqualTo(100);

        LearningPathItem item2 = savedItems.get(1);
        assertThat(item2.getStatus()).isEqualTo(ELessonStatus.IN_PROGRESS);
    }

    @Test
    void generateAdaptivePath_whenUnpassedModuleHasLessonProgress_inheritsExactVideoProgressPercent() {
        // Học viên chưa PASS Module Test nào, nhưng đã học 2/3 video (67%) ở module1 và 3/3 video (100%) ở module2
        LearningPath oldPath = new LearningPath();
        oldPath.setId(50L);
        oldPath.setVersion(1);
        oldPath.setStatus(ELearningPathStatus.ACTIVE);
        oldPath.setTargetProfile(profile550);

        when(targetProfileRepo.findById(2L)).thenReturn(Optional.of(profile650));
        when(learningPathRepo.findWithTargetProfileByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(oldPath));
        when(learningPathRepo.save(any(LearningPath.class))).thenAnswer(inv -> {
            LearningPath lp = inv.getArgument(0);
            lp.setId(52L);
            return lp;
        });

        TargetPartThreshold threshold650 = new TargetPartThreshold();
        threshold650.setPassScore(80.0f);
        threshold650.setConfirmingScore(60.0f);
        threshold650.setFullPartThreshold(35.0f);
        when(targetPartThresholdRepo.findByTargetProfileIdAndPartId(2L, 1L)).thenReturn(Optional.of(threshold650));
        when(moduleRepo.findByPartIdAndStatusOrderBySequenceAsc(1L, EModuleStatus.PUBLISHED))
                .thenReturn(List.of(module1, module2));

        when(moduleTestAttemptRepo.findPassedModuleMaxScoresByUserId(1L, ETestResult.PASS))
                .thenReturn(Collections.emptyList());
        when(moduleTargetThresholdRepo.findByTargetProfileId(2L)).thenReturn(Collections.emptyList());

        when(lessonRepo.countByModuleIdInAndStatusAndLessonType(anyCollection(), eq(EModuleStatus.PUBLISHED), eq(ELessonType.VIDEO)))
                .thenReturn(List.of(
                        new Object[]{10L, 3L},
                        new Object[]{20L, 3L}
                ));
        when(lessonProgressRepo.countCompletedLessonsGroupedByModuleIdAndType(eq(1L), anyCollection(), eq(EModuleStatus.PUBLISHED), eq(ELessonType.VIDEO)))
                .thenReturn(List.of(
                        new Object[]{10L, 2L},
                        new Object[]{20L, 3L}
                ));

        service.generateAdaptivePath(attempt, 2L);

        verify(learningPathItemRepo).saveAll(itemsCaptor.capture());
        List<LearningPathItem> savedItems = itemsCaptor.getValue();

        LearningPathItem item1 = savedItems.get(0);
        assertThat(item1.getProgressPercent()).isEqualTo(67);
        assertThat(item1.getIsCorrect()).isFalse();
        assertThat(item1.getStatus()).isEqualTo(ELessonStatus.IN_PROGRESS);
        assertThat(item1.getReason()).contains("67%");

        LearningPathItem item2 = savedItems.get(1);
        assertThat(item2.getProgressPercent()).isEqualTo(100);
        assertThat(item2.getIsCorrect()).isFalse();
        assertThat(item2.getStatus()).isEqualTo(ELessonStatus.LOCKED);
        assertThat(item2.getReason()).contains("sẵn sàng làm bài kiểm tra vượt ải");

        // Không gọi truy vấn fallback vì cả 2 module đều có bài giảng VIDEO
        verify(lessonRepo, never()).countByModuleIdInAndStatus(anyCollection(), any());
    }

    @Test
    void generateAdaptivePath_whenModuleHasNoVideoLessons_fallsBackToPublishedLessonProgress() {
        // Module1 chỉ có 4 bài PRACTICE (0 bài VIDEO) và đã hoàn thành 2/4 bài -> fallback tính đúng 50%
        when(targetProfileRepo.findById(2L)).thenReturn(Optional.of(profile650));
        when(learningPathRepo.findWithTargetProfileByUserIdOrderByCreatedAtDesc(1L)).thenReturn(Collections.emptyList());
        when(learningPathRepo.save(any(LearningPath.class))).thenAnswer(inv -> inv.getArgument(0));

        TargetPartThreshold threshold650 = new TargetPartThreshold();
        threshold650.setPassScore(80.0f);
        threshold650.setConfirmingScore(60.0f);
        threshold650.setFullPartThreshold(35.0f);
        when(targetPartThresholdRepo.findByTargetProfileIdAndPartId(2L, 1L)).thenReturn(Optional.of(threshold650));
        when(moduleRepo.findByPartIdAndStatusOrderBySequenceAsc(1L, EModuleStatus.PUBLISHED))
                .thenReturn(List.of(module1));

        when(moduleTestAttemptRepo.findPassedModuleMaxScoresByUserId(1L, ETestResult.PASS))
                .thenReturn(Collections.emptyList());
        when(moduleTargetThresholdRepo.findByTargetProfileId(2L)).thenReturn(Collections.emptyList());

        when(lessonRepo.countByModuleIdInAndStatusAndLessonType(anyCollection(), eq(EModuleStatus.PUBLISHED), eq(ELessonType.VIDEO)))
                .thenReturn(Collections.emptyList());
        when(lessonProgressRepo.countCompletedLessonsGroupedByModuleIdAndType(eq(1L), anyCollection(), eq(EModuleStatus.PUBLISHED), eq(ELessonType.VIDEO)))
                .thenReturn(Collections.emptyList());
        when(lessonRepo.countByModuleIdInAndStatus(eq(List.of(10L)), eq(EModuleStatus.PUBLISHED)))
                .thenReturn(Collections.singletonList(new Object[]{10L, 4L}));
        when(lessonProgressRepo.countCompletedLessonsGroupedByModuleId(eq(1L), eq(List.of(10L)), eq(EModuleStatus.PUBLISHED)))
                .thenReturn(Collections.singletonList(new Object[]{10L, 2L}));

        service.generateAdaptivePath(attempt, 2L);

        verify(learningPathItemRepo).saveAll(itemsCaptor.capture());
        LearningPathItem item1 = itemsCaptor.getValue().get(0);
        assertThat(item1.getProgressPercent()).isEqualTo(50);
        assertThat(item1.getStatus()).isEqualTo(ELessonStatus.IN_PROGRESS);
    }

    @Test
    void generateAdaptivePath_whenPreviousPathWasCompleted_archivesOldCompletedPath() {
        // Học viên học xong 100% lộ trình AIM 550 (COMPLETED) rồi nâng lên AIM 650 -> bản COMPLETED cũ phải thành ARCHIVED
        LearningPath completedOldPath = new LearningPath();
        completedOldPath.setId(60L);
        completedOldPath.setVersion(1);
        completedOldPath.setStatus(ELearningPathStatus.COMPLETED);
        completedOldPath.setTargetProfile(profile550);

        when(targetProfileRepo.findById(2L)).thenReturn(Optional.of(profile650));
        when(learningPathRepo.findWithTargetProfileByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(completedOldPath));
        when(learningPathRepo.save(any(LearningPath.class))).thenAnswer(inv -> inv.getArgument(0));

        TargetPartThreshold threshold650 = new TargetPartThreshold();
        threshold650.setPassScore(80.0f);
        threshold650.setConfirmingScore(60.0f);
        threshold650.setFullPartThreshold(35.0f);
        when(targetPartThresholdRepo.findByTargetProfileIdAndPartId(2L, 1L)).thenReturn(Optional.of(threshold650));
        when(moduleRepo.findByPartIdAndStatusOrderBySequenceAsc(1L, EModuleStatus.PUBLISHED))
                .thenReturn(List.of(module1));
        when(moduleTestAttemptRepo.findPassedModuleMaxScoresByUserId(1L, ETestResult.PASS))
                .thenReturn(Collections.singletonList(new Object[]{10L, 90}));
        when(moduleTargetThresholdRepo.findByTargetProfileId(2L)).thenReturn(Collections.emptyList());

        LearningPath newPath = service.generateAdaptivePath(attempt, 2L);

        assertThat(completedOldPath.getStatus()).isEqualTo(ELearningPathStatus.ARCHIVED);
        assertThat(newPath.getVersion()).isEqualTo(2);

        // Vì nâng AIM (550 -> 650), module1 dù điểm cũ 90 vẫn phải thi lại theo BR06 (25/11)
        verify(learningPathItemRepo).saveAll(itemsCaptor.capture());
        LearningPathItem item1 = itemsCaptor.getValue().get(0);
        assertThat(item1.getStatus()).isEqualTo(ELessonStatus.IN_PROGRESS);
        assertThat(item1.getIsCorrect()).isFalse();
        assertThat(item1.getProgressPercent()).isEqualTo(100);
    }

    @Test
    void generateAdaptivePath_whenAllModulesInheritedCompletedOnDowngrade_marksNewPathCompleted() {
        // Hạ AIM từ 850 -> 550 và cả 2 module đều từng PASS >= 70 -> 100% chặng COMPLETED -> lộ trình mới tự chốt COMPLETED
        LearningPath oldPath = new LearningPath();
        oldPath.setId(70L);
        oldPath.setVersion(1);
        oldPath.setStatus(ELearningPathStatus.ACTIVE);
        oldPath.setTargetProfile(profile850);

        when(targetProfileRepo.findById(1L)).thenReturn(Optional.of(profile550));
        when(learningPathRepo.findWithTargetProfileByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(oldPath));
        when(learningPathRepo.save(any(LearningPath.class))).thenAnswer(inv -> inv.getArgument(0));

        TargetPartThreshold threshold550 = new TargetPartThreshold();
        threshold550.setPassScore(65.0f);
        threshold550.setConfirmingScore(50.0f);
        threshold550.setFullPartThreshold(30.0f);
        when(targetPartThresholdRepo.findByTargetProfileIdAndPartId(1L, 1L)).thenReturn(Optional.of(threshold550));
        when(moduleRepo.findByPartIdAndStatusOrderBySequenceAsc(1L, EModuleStatus.PUBLISHED))
                .thenReturn(List.of(module1, module2));
        when(moduleTestAttemptRepo.findPassedModuleMaxScoresByUserId(1L, ETestResult.PASS))
                .thenReturn(List.of(
                        new Object[]{10L, 85},
                        new Object[]{20L, 80}
                ));
        when(moduleTargetThresholdRepo.findByTargetProfileId(1L)).thenReturn(Collections.emptyList());

        LearningPath newPath = service.generateAdaptivePath(attempt, 1L);

        assertThat(newPath.getStatus()).isEqualTo(ELearningPathStatus.COMPLETED);
        verify(learningPathItemRepo).saveAll(itemsCaptor.capture());
        assertThat(itemsCaptor.getValue())
                .allSatisfy(item -> {
                    assertThat(item.getStatus()).isEqualTo(ELessonStatus.COMPLETED);
                    assertThat(item.getIsCorrect()).isTrue();
                    assertThat(item.getProgressPercent()).isEqualTo(100);
                });
    }
}

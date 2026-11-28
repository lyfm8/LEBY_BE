package com.example.be.service.user;

import com.example.be.dto.response.user.SelectTargetResponse;
import com.example.be.dto.response.user.UserTargetProfileResponse;
import com.example.be.entity.content.LearningPath;
import com.example.be.entity.diagnostic.DiagnosticAttempt;
import com.example.be.entity.diagnostic.PartDiagnosticResult;
import com.example.be.entity.user.TargetProfile;
import com.example.be.entity.user.User;
import com.example.be.enums.diagnostic.EAttemptStatus;
import com.example.be.enums.diagnostic.ETestType;
import com.example.be.exception.BaseException;
import com.example.be.exception.ErrorCode;
import com.example.be.mapper.user.TargetProfileMapper;
import com.example.be.repository.diagnostic.DiagnosticAttemptRepository;
import com.example.be.repository.user.TargetProfileRepository;
import com.example.be.repository.user.UserRepository;
import com.example.be.service.content.AdaptivePathGeneratorService;
import com.example.be.service.user.implement.UserTargetServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test cho {@link UserTargetServiceImpl} (UC05 - Chọn lần đầu & Đổi mục tiêu AIM).
 */
@ExtendWith(MockitoExtension.class)
class UserTargetServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private TargetProfileRepository targetProfileRepository;
    @Mock
    private DiagnosticAttemptRepository diagnosticAttemptRepository;
    @Mock
    private AdaptivePathGeneratorService adaptivePathGeneratorService;

    @Spy
    private TargetProfileMapper targetProfileMapper = new TargetProfileMapper();

    @InjectMocks
    private UserTargetServiceImpl userTargetService;

    private User student;
    private TargetProfile profile650;
    private TargetProfile profile850;

    @BeforeEach
    void setUp() {
        profile650 = new TargetProfile();
        profile650.setId(2L);
        profile650.setTargetTotalScore(650);
        profile650.setDescription("Mục tiêu 650");
        profile650.setStatus(true);

        profile850 = new TargetProfile();
        profile850.setId(5L);
        profile850.setTargetTotalScore(850);
        profile850.setDescription("Mục tiêu 850");
        profile850.setStatus(true);

        student = new User();
        student.setId(10L);
        student.setEmail("student@leby.edu.vn");
    }

    @Test
    void getAllActiveTargets_returnsRecommendedFlagFor650AndNoLevel() {
        when(targetProfileRepository.findByStatusTrueOrderByTargetTotalScoreAsc())
                .thenReturn(List.of(profile650, profile850));

        List<UserTargetProfileResponse> result = userTargetService.getAllActiveTargets();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTargetTotalScore()).isEqualTo(650);
        assertThat(result.get(0).getIsRecommended()).isTrue();
        assertThat(result.get(1).getTargetTotalScore()).isEqualTo(850);
        assertThat(result.get(1).getIsRecommended()).isFalse();
    }

    @Test
    void selectTarget_firstTime_savesUserTargetAndRedirectsToDiagnostic() {
        student.setTargetProfile(null);
        when(userRepository.findWithTargetProfileById(10L)).thenReturn(Optional.of(student));
        when(targetProfileRepository.findById(2L)).thenReturn(Optional.of(profile650));

        SelectTargetResponse resp = userTargetService.selectTarget(10L, 2L);

        assertThat(resp.getUserId()).isEqualTo(10L);
        assertThat(resp.getTargetProfileId()).isEqualTo(2L);
        assertThat(resp.getTargetTotalScore()).isEqualTo(650);
        assertThat(resp.getRedirectAction()).isEqualTo(SelectTargetResponse.ACTION_GO_DIAGNOSTIC);
        assertThat(resp.getLearningPathId()).isNull();
        assertThat(student.getTargetProfile()).isEqualTo(profile650);
        verify(userRepository).save(student);
        verify(adaptivePathGeneratorService, never()).generateAdaptivePath(any(), anyLong());
    }

    @Test
    void selectTarget_sameTarget_throwsTargetAlreadyActive() {
        student.setTargetProfile(profile650);
        when(userRepository.findWithTargetProfileById(10L)).thenReturn(Optional.of(student));
        when(targetProfileRepository.findById(2L)).thenReturn(Optional.of(profile650));

        assertThatThrownBy(() -> userTargetService.selectTarget(10L, 2L))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.TARGET_ALREADY_ACTIVE));

        verify(userRepository, never()).save(any());
    }

    @Test
    void selectTarget_targetNotFound_throwsTargetNotFound() {
        when(userRepository.findWithTargetProfileById(10L)).thenReturn(Optional.of(student));
        when(targetProfileRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userTargetService.selectTarget(10L, 999L))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.TARGET_NOT_FOUND));
    }

    @Test
    void selectTarget_inactiveTarget_throwsTargetProfileInactive() {
        profile850.setStatus(false);
        when(userRepository.findWithTargetProfileById(10L)).thenReturn(Optional.of(student));
        when(targetProfileRepository.findById(5L)).thenReturn(Optional.of(profile850));

        assertThatThrownBy(() -> userTargetService.selectTarget(10L, 5L))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.TARGET_PROFILE_INACTIVE));
    }

    @Test
    void selectTarget_changeAimWithSubmittedDiagnostic_regeneratesPathAndRedirectsToLearningPath() {
        student.setTargetProfile(profile650);

        DiagnosticAttempt attempt = new DiagnosticAttempt();
        attempt.setId(55L);
        attempt.setStatus(EAttemptStatus.SUBMITTED);
        attempt.setPartResults(List.of(new PartDiagnosticResult()));

        LearningPath regeneratedPath = new LearningPath();
        regeneratedPath.setId(77L);

        when(userRepository.findWithTargetProfileById(10L)).thenReturn(Optional.of(student));
        when(targetProfileRepository.findById(5L)).thenReturn(Optional.of(profile850));
        when(diagnosticAttemptRepository.findTopByUserIdAndDiagnosticTestTestTypeAndStatusInOrderByCompletedAtDesc(
                10L, ETestType.DIAGNOSTIC, List.of(EAttemptStatus.SUBMITTED, EAttemptStatus.TIMED_OUT)))
                .thenReturn(Optional.of(attempt));
        when(adaptivePathGeneratorService.generateAdaptivePath(attempt, 5L)).thenReturn(regeneratedPath);

        SelectTargetResponse resp = userTargetService.selectTarget(10L, 5L);

        assertThat(resp.getTargetProfileId()).isEqualTo(5L);
        assertThat(resp.getTargetTotalScore()).isEqualTo(850);
        assertThat(resp.getRedirectAction()).isEqualTo(SelectTargetResponse.ACTION_GO_LEARNING_PATH);
        assertThat(resp.getLearningPathId()).isEqualTo(77L);
        verify(userRepository).save(student);
        verify(adaptivePathGeneratorService).generateAdaptivePath(attempt, 5L);
    }

    @Test
    void selectTarget_changeAimWithTimedOutDiagnostic_reusesAttemptAndRegeneratesPath() {
        student.setTargetProfile(profile650);

        DiagnosticAttempt timedOutAttempt = new DiagnosticAttempt();
        timedOutAttempt.setId(56L);
        timedOutAttempt.setStatus(EAttemptStatus.TIMED_OUT);
        timedOutAttempt.setPartResults(List.of(new PartDiagnosticResult()));

        LearningPath regeneratedPath = new LearningPath();
        regeneratedPath.setId(78L);

        when(userRepository.findWithTargetProfileById(10L)).thenReturn(Optional.of(student));
        when(targetProfileRepository.findById(5L)).thenReturn(Optional.of(profile850));
        when(diagnosticAttemptRepository.findTopByUserIdAndDiagnosticTestTestTypeAndStatusInOrderByCompletedAtDesc(
                10L, ETestType.DIAGNOSTIC, List.of(EAttemptStatus.SUBMITTED, EAttemptStatus.TIMED_OUT)))
                .thenReturn(Optional.of(timedOutAttempt));
        when(adaptivePathGeneratorService.generateAdaptivePath(timedOutAttempt, 5L)).thenReturn(regeneratedPath);

        SelectTargetResponse resp = userTargetService.selectTarget(10L, 5L);

        assertThat(resp.getRedirectAction()).isEqualTo(SelectTargetResponse.ACTION_GO_LEARNING_PATH);
        assertThat(resp.getLearningPathId()).isEqualTo(78L);
        verify(adaptivePathGeneratorService).generateAdaptivePath(timedOutAttempt, 5L);
    }

    @Test
    void selectTarget_changeAimWithoutDiagnosticData_redirectsToDiagnostic() {
        student.setTargetProfile(profile650);
        when(userRepository.findWithTargetProfileById(10L)).thenReturn(Optional.of(student));
        when(targetProfileRepository.findById(5L)).thenReturn(Optional.of(profile850));
        when(diagnosticAttemptRepository.findTopByUserIdAndDiagnosticTestTestTypeAndStatusInOrderByCompletedAtDesc(
                10L, ETestType.DIAGNOSTIC, List.of(EAttemptStatus.SUBMITTED, EAttemptStatus.TIMED_OUT)))
                .thenReturn(Optional.empty());

        SelectTargetResponse resp = userTargetService.selectTarget(10L, 5L);

        assertThat(resp.getRedirectAction()).isEqualTo(SelectTargetResponse.ACTION_GO_DIAGNOSTIC);
        assertThat(resp.getLearningPathId()).isNull();
        verify(adaptivePathGeneratorService, never()).generateAdaptivePath(any(), anyLong());

        // Trường hợp có attempt nhưng partResults rỗng cũng phải điều hướng về chẩn đoán (Luồng 7b)
        DiagnosticAttempt emptyAttempt = new DiagnosticAttempt();
        emptyAttempt.setPartResults(Collections.emptyList());
        student.setTargetProfile(profile650);
        when(diagnosticAttemptRepository.findTopByUserIdAndDiagnosticTestTestTypeAndStatusInOrderByCompletedAtDesc(
                10L, ETestType.DIAGNOSTIC, List.of(EAttemptStatus.SUBMITTED, EAttemptStatus.TIMED_OUT)))
                .thenReturn(Optional.of(emptyAttempt));

        SelectTargetResponse resp2 = userTargetService.selectTarget(10L, 5L);
        assertThat(resp2.getRedirectAction()).isEqualTo(SelectTargetResponse.ACTION_GO_DIAGNOSTIC);
    }
}

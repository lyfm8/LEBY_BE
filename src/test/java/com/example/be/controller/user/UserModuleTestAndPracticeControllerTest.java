package com.example.be.controller.user;

import com.example.be.common.ApiResponse;
import com.example.be.dto.request.exam.SaveModuleTestDraftRequest;
import com.example.be.dto.request.exam.SubmitModuleTestRequest;
import com.example.be.dto.request.exam.SyncModuleTestTimerRequest;
import com.example.be.dto.response.exam.ModuleTestPerformanceReportResponse;
import com.example.be.dto.response.exam.ModuleTestReviewResponse;
import com.example.be.dto.response.exam.ModuleTestRoomResponse;
import com.example.be.dto.response.exam.PracticeRoomResponse;
import com.example.be.dto.response.exam.StartModuleTestResponse;
import com.example.be.dto.response.exam.SubmitModuleTestResponse;
import com.example.be.entity.user.User;
import com.example.be.service.exam.service.UserModuleTestService;
import com.example.be.service.exam.service.UserPracticeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test cho {@link UserModuleTestController} và {@link UserPracticeController}.
 */
@ExtendWith(MockitoExtension.class)
class UserModuleTestAndPracticeControllerTest {

    @Mock private UserModuleTestService userModuleTestService;
    @Mock private UserPracticeService userPracticeService;

    @InjectMocks private UserModuleTestController moduleTestController;
    @InjectMocks private UserPracticeController practiceController;

    @Test
    void mappings_matchExpectedApiUsersPrefixes() {
        RequestMapping modMapping = UserModuleTestController.class.getAnnotation(RequestMapping.class);
        RequestMapping pracMapping = UserPracticeController.class.getAnnotation(RequestMapping.class);

        assertThat(modMapping).isNotNull();
        assertThat(modMapping.value()).containsExactly("/api/users/modules/{moduleId}/test");
        assertThat(pracMapping).isNotNull();
        assertThat(pracMapping.value()).containsExactly("/api/users/practice");
    }

    @Test
    void practiceController_delegatesToService() {
        User user = new User();
        user.setId(7L);

        PracticeRoomResponse room = PracticeRoomResponse.builder().lessonId(3L).totalQuestions(6).build();
        when(userPracticeService.getPracticeRoom(7L, 3L)).thenReturn(room);

        ResponseEntity<ApiResponse<PracticeRoomResponse>> response = practiceController.getPracticeRoom(user, 3L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().getTotalQuestions()).isEqualTo(6);
    }

    @Test
    void moduleTestController_allEndpointsDelegateCorrectly() {
        User user = new User();
        user.setId(7L);

        StartModuleTestResponse startResp = StartModuleTestResponse.builder().attemptId(12L).moduleId(2L).build();
        ModuleTestRoomResponse roomResp = ModuleTestRoomResponse.builder().attemptId(12L).moduleId(2L).build();
        SubmitModuleTestResponse submitResp = SubmitModuleTestResponse.builder().attemptId(12L).isPassed(true).build();
        ModuleTestPerformanceReportResponse reportResp = ModuleTestPerformanceReportResponse.builder().attemptId(12L).score(85).build();
        ModuleTestReviewResponse reviewResp = ModuleTestReviewResponse.builder().attemptId(12L).score(85).build();

        SaveModuleTestDraftRequest draftReq = new SaveModuleTestDraftRequest(12L, List.of());
        SyncModuleTestTimerRequest syncReq = new SyncModuleTestTimerRequest(12L, 500);
        SubmitModuleTestRequest submitReq = new SubmitModuleTestRequest(12L, 400, false, List.of());

        when(userModuleTestService.startModuleTest(7L, 2L)).thenReturn(startResp);
        when(userModuleTestService.getActiveSession(7L, 2L)).thenReturn(roomResp);
        when(userModuleTestService.getExamRoom(7L, 2L, 12L)).thenReturn(roomResp);
        when(userModuleTestService.syncRemainingTime(7L, 2L, syncReq)).thenReturn(500L);
        when(userModuleTestService.submitModuleTest(7L, 2L, submitReq)).thenReturn(submitResp);
        when(userModuleTestService.getPerformanceReport(7L, 2L, 12L)).thenReturn(reportResp);
        when(userModuleTestService.getTestReview(7L, 2L, 12L)).thenReturn(reviewResp);

        assertThat(moduleTestController.startModuleTest(user, 2L).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(moduleTestController.resumeModuleTest(user, 2L).getBody().getData().getAttemptId()).isEqualTo(12L);
        assertThat(moduleTestController.getExamRoom(user, 2L, 12L).getBody().getData().getModuleId()).isEqualTo(2L);
        assertThat(moduleTestController.saveDraftAnswers(user, 2L, draftReq).getBody().isSuccess()).isTrue();
        assertThat(moduleTestController.syncTimer(user, 2L, syncReq).getBody().getData()).isEqualTo(500L);
        assertThat(moduleTestController.submitModuleTest(user, 2L, submitReq).getBody().getData().getIsPassed()).isTrue();
        assertThat(moduleTestController.getPerformanceReport(user, 2L, 12L).getBody().getData().getScore()).isEqualTo(85);
        assertThat(moduleTestController.getTestReview(user, 2L, 12L).getBody().getData().getAttemptId()).isEqualTo(12L);

        verify(userModuleTestService).saveDraftAnswers(7L, 2L, draftReq);
    }
}

package com.example.be.controller.user;

import com.example.be.common.ApiResponse;
import com.example.be.dto.request.mockexam.StartMockExamRequest;
import com.example.be.dto.request.mockexam.SyncMockExamTimerRequest;
import com.example.be.dto.response.diagnostic.StartDiagnosticResponse;
import com.example.be.dto.response.mockexam.MockExamListResponse;
import com.example.be.dto.response.mockexam.MockExamReportResponse;
import com.example.be.dto.response.mockexam.MockExamReviewResponse;
import com.example.be.dto.response.mockexam.MockExamRoomResponse;
import com.example.be.entity.user.User;
import com.example.be.enums.diagnostic.EExamTag;
import com.example.be.service.mockexam.service.UserMockExamService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test cho {@link UserMockExamController} xác minh các endpoint UC17 & UC18.
 */
@ExtendWith(MockitoExtension.class)
class UserMockExamControllerTest {

    @Mock
    private UserMockExamService userMockExamService;

    @InjectMocks
    private UserMockExamController controller;

    @Test
    void requestMapping_matchesApiUsersMockExam() {
        RequestMapping mapping = UserMockExamController.class.getAnnotation(RequestMapping.class);
        assertThat(mapping).isNotNull();
        assertThat(mapping.value()).containsExactly("/api/users/mock-exam");
    }

    @Test
    void startMockExam_returnsCreatedWrappedInApiResponse() {
        User user = new User();
        user.setId(7L);

        StartMockExamRequest req = new StartMockExamRequest();
        req.setDiagnosticTestId(2L);

        StartDiagnosticResponse startResp = StartDiagnosticResponse.builder()
                .attemptId(55L)
                .diagnosticTestId(2L)
                .durationMinutes(120)
                .build();

        when(userMockExamService.startMockExam(eq(7L), any(StartMockExamRequest.class)))
                .thenReturn(startResp);

        ResponseEntity<ApiResponse<StartDiagnosticResponse>> response = controller.startMockExam(user, req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData().getAttemptId()).isEqualTo(55L);
    }

    @Test
    void listRoomReportAndReview_delegateToService() {
        User user = new User();
        user.setId(7L);

        MockExamListResponse listResp = MockExamListResponse.builder()
                .isOutputExamUnlocked(true)
                .exams(Collections.emptyList())
                .history(Collections.emptyList())
                .build();
        MockExamRoomResponse roomResp = MockExamRoomResponse.builder()
                .attemptId(55L)
                .totalQuestions(200)
                .build();
        MockExamReportResponse reportResp = MockExamReportResponse.builder()
                .attemptId(55L)
                .examTag(EExamTag.OUTPUT)
                .totalToeicScore(815)
                .build();
        MockExamReviewResponse reviewResp = MockExamReviewResponse.builder()
                .attemptId(55L)
                .totalQuestions(200)
                .build();

        when(userMockExamService.listAvailableMockExams(7L)).thenReturn(listResp);
        when(userMockExamService.getActiveSession(7L)).thenReturn(roomResp);
        when(userMockExamService.getExamRoom(7L, 55L)).thenReturn(roomResp);
        when(userMockExamService.getReport(7L, 55L)).thenReturn(reportResp);
        when(userMockExamService.getLatestOutputReport(7L)).thenReturn(reportResp);
        when(userMockExamService.getReviewData(7L, 55L)).thenReturn(reviewResp);

        assertThat(controller.listAvailableMockExams(user).getBody().getData().getIsOutputExamUnlocked()).isTrue();
        assertThat(controller.resumeMockExam(user).getBody().getData().getAttemptId()).isEqualTo(55L);
        assertThat(controller.getExamRoom(user, 55L).getBody().getData().getTotalQuestions()).isEqualTo(200);
        assertThat(controller.getReport(user, 55L).getBody().getData().getTotalToeicScore()).isEqualTo(815);
        assertThat(controller.getLatestOutputReport(user).getBody().getData().getExamTag()).isEqualTo(EExamTag.OUTPUT);
        assertThat(controller.getReviewData(user, 55L).getBody().getData().getTotalQuestions()).isEqualTo(200);

        SyncMockExamTimerRequest timerReq = new SyncMockExamTimerRequest();
        timerReq.setAttemptId(55L);
        timerReq.setRemainingSeconds(3000);
        when(userMockExamService.syncRemainingTime(7L, timerReq)).thenReturn(3000L);
        assertThat(controller.syncTimer(user, timerReq).getBody().getData()).isEqualTo(3000L);

        verify(userMockExamService).listAvailableMockExams(7L);
        verify(userMockExamService).getExamRoom(7L, 55L);
        verify(userMockExamService).getReport(7L, 55L);
        verify(userMockExamService).getReviewData(7L, 55L);
    }
}

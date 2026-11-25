package com.example.be.controller.user;

import com.example.be.common.ApiResponse;
import com.example.be.dto.request.diagnostic.StartDiagnosticRequest;
import com.example.be.dto.response.diagnostic.DiagnosticResultDetailResponse;
import com.example.be.dto.response.diagnostic.DiagnosticRoomResponse;
import com.example.be.dto.response.diagnostic.StartDiagnosticResponse;
import com.example.be.entity.user.User;
import com.example.be.service.diagnostic.service.DiagnosticScoringService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test cho {@link UserDiagnosticController} xác minh các endpoint UC06 & UC07.
 */
@ExtendWith(MockitoExtension.class)
class UserDiagnosticControllerTest {

    @Mock
    private DiagnosticScoringService diagnosticScoringService;

    @InjectMocks
    private UserDiagnosticController controller;

    @Test
    void requestMapping_matchesApiUsersDiagnostic() {
        RequestMapping mapping = UserDiagnosticController.class.getAnnotation(RequestMapping.class);
        assertThat(mapping).isNotNull();
        assertThat(mapping.value()).containsExactly("/api/users/diagnostic");
    }

    @Test
    void startDiagnostic_acceptsNullBodyAndReturnsCreated() {
        User user = new User();
        user.setId(7L);

        StartDiagnosticResponse startResp = StartDiagnosticResponse.builder()
                .attemptId(12L)
                .diagnosticTestId(1L)
                .targetProfileId(3L)
                .durationMinutes(45)
                .build();

        when(diagnosticScoringService.startDiagnostic(eq(7L), any(StartDiagnosticRequest.class)))
                .thenReturn(startResp);

        ResponseEntity<ApiResponse<StartDiagnosticResponse>> response = controller.startDiagnostic(user, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData().getAttemptId()).isEqualTo(12L);
    }

    @Test
    void resumeAndGetExamAndResults_delegateToService() {
        User user = new User();
        user.setId(7L);

        DiagnosticRoomResponse roomResp = DiagnosticRoomResponse.builder().attemptId(12L).totalQuestions(20).build();
        DiagnosticResultDetailResponse resultResp = DiagnosticResultDetailResponse.builder().attemptId(12L).predictedScore(650).build();

        when(diagnosticScoringService.getActiveSession(7L)).thenReturn(roomResp);
        when(diagnosticScoringService.getExamForAttempt(7L, 12L)).thenReturn(roomResp);
        when(diagnosticScoringService.getLatestResultDetail(7L)).thenReturn(resultResp);
        when(diagnosticScoringService.getResultDetail(7L, 12L)).thenReturn(resultResp);

        assertThat(controller.resumeDiagnostic(user).getBody().getData().getAttemptId()).isEqualTo(12L);
        assertThat(controller.getExamForAttempt(user, 12L).getBody().getData().getTotalQuestions()).isEqualTo(20);
        assertThat(controller.getLatestResultDetail(user).getBody().getData().getPredictedScore()).isEqualTo(650);
        assertThat(controller.getResultDetail(user, 12L).getBody().getData().getAttemptId()).isEqualTo(12L);

        verify(diagnosticScoringService).getActiveSession(7L);
        verify(diagnosticScoringService).getExamForAttempt(7L, 12L);
        verify(diagnosticScoringService).getLatestResultDetail(7L);
        verify(diagnosticScoringService).getResultDetail(7L, 12L);
    }
}

package com.example.be.controller.user;

import com.example.be.common.ApiResponse;
import com.example.be.dto.request.user.SelectTargetRequest;
import com.example.be.dto.response.user.SelectTargetResponse;
import com.example.be.dto.response.user.UserTargetProfileResponse;
import com.example.be.entity.user.User;
import com.example.be.service.user.service.UserTargetService;
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
 * Unit test cho {@link UserTargetController}, xác minh mapping endpoint chuẩn (không tiền tố v1)
 * và định dạng ApiResponse trả về cho UC05.
 */
@ExtendWith(MockitoExtension.class)
class UserTargetControllerTest {

    @Mock
    private UserTargetService userTargetService;

    @InjectMocks
    private UserTargetController controller;

    @Test
    void requestMapping_hasNoV1Prefix() {
        RequestMapping mapping = UserTargetController.class.getAnnotation(RequestMapping.class);
        assertThat(mapping).isNotNull();
        assertThat(mapping.value()).containsExactly("/api/targets");
    }

    @Test
    void getTargets_returnsOkWithTargetList() {
        UserTargetProfileResponse target650 = UserTargetProfileResponse.builder()
                .id(2L)
                .targetTotalScore(650)
                .isRecommended(true)
                .status(true)
                .build();
        when(userTargetService.getAllActiveTargets()).thenReturn(List.of(target650));

        ResponseEntity<ApiResponse<List<UserTargetProfileResponse>>> response = controller.getTargets();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData()).hasSize(1);
        assertThat(response.getBody().getData().get(0).getIsRecommended()).isTrue();
    }

    @Test
    void selectTarget_delegatesToServiceWithAuthenticatedUserId() {
        User currentUser = new User();
        currentUser.setId(99L);

        SelectTargetRequest request = new SelectTargetRequest(5L);
        SelectTargetResponse serviceResult = SelectTargetResponse.builder()
                .userId(99L)
                .targetProfileId(5L)
                .targetTotalScore(850)
                .redirectAction(SelectTargetResponse.ACTION_GO_LEARNING_PATH)
                .learningPathId(123L)
                .build();

        when(userTargetService.selectTarget(99L, 5L)).thenReturn(serviceResult);

        ResponseEntity<ApiResponse<SelectTargetResponse>> response = controller.selectTarget(currentUser, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData().getRedirectAction())
                .isEqualTo(SelectTargetResponse.ACTION_GO_LEARNING_PATH);
        assertThat(response.getBody().getData().getLearningPathId()).isEqualTo(123L);
        verify(userTargetService).selectTarget(99L, 5L);
    }
}

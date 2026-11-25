package com.example.be.service.auth;

import com.example.be.dto.request.auth.RegisterRequest;
import com.example.be.dto.response.auth.AuthResponse;
import com.example.be.entity.user.User;
import com.example.be.enums.auth.OtpPurpose;
import com.example.be.enums.user.ELearnerType;
import com.example.be.enums.user.ERole;
import com.example.be.mapper.auth.AuthMapper;
import com.example.be.repository.user.UserRepository;
import com.example.be.security.CookieProperties;
import com.example.be.security.JwtTokenProvider;
import com.example.be.service.auth.implement.AuthServiceImpl;
import com.example.be.service.auth.service.MailService;
import com.example.be.service.auth.service.OtpService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test cho {@link AuthServiceImpl} khi gộp thông tin cá nhân (dob, learnerType) vào đăng ký (UC01).
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private CookieProperties cookieProperties;
    @Mock private OtpService otpService;
    @Mock private MailService mailService;

    @Spy
    private AuthMapper authMapper = new AuthMapper();

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void register_savesPersonalInfoDobAndLearnerTypeWithStudentRole() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("  NewUser@Example.COM ");
        request.setFullName("  Nguyễn Văn A  ");
        request.setDob(LocalDate.of(2003, 8, 15));
        request.setLearnerType(ELearnerType.UNIVERSITY_STUDENT);
        request.setPassword("Pass1234");
        request.setConfirmPassword("Pass1234");
        request.setOtp("123456");

        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Pass1234")).thenReturn("hashed_pass");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User saved = inv.getArgument(0);
            saved.setId(50L);
            return saved;
        });
        when(jwtTokenProvider.generateAccessToken(anyLong(), anyInt(), anyString())).thenReturn("acc-token");
        when(jwtTokenProvider.generateRefreshToken(anyLong(), anyInt())).thenReturn("ref-token");
        when(jwtTokenProvider.buildAccessTokenCookie("acc-token"))
                .thenReturn(ResponseCookie.from("accessToken", "acc-token").build());
        when(jwtTokenProvider.buildRefreshTokenCookie("ref-token"))
                .thenReturn(ResponseCookie.from("refreshToken", "ref-token").build());

        MockHttpServletResponse httpResponse = new MockHttpServletResponse();
        AuthResponse response = authService.register(request, httpResponse);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        verify(otpService).verify("newuser@example.com", OtpPurpose.REGISTER, "123456");

        User persisted = userCaptor.getValue();
        assertThat(persisted.getEmail()).isEqualTo("newuser@example.com");
        assertThat(persisted.getFullName()).isEqualTo("Nguyễn Văn A");
        assertThat(persisted.getDob()).isEqualTo(LocalDate.of(2003, 8, 15));
        assertThat(persisted.getLearnerType()).isEqualTo(ELearnerType.UNIVERSITY_STUDENT);
        assertThat(persisted.getRole()).isEqualTo(ERole.STUDENT);
        assertThat(persisted.getIsActive()).isTrue();

        assertThat(response.getId()).isEqualTo(50L);
        assertThat(response.getDob()).isEqualTo(LocalDate.of(2003, 8, 15));
        assertThat(response.getLearnerType()).isEqualTo(ELearnerType.UNIVERSITY_STUDENT);
        assertThat(response.isHasSelectedTarget()).isFalse();
    }
}

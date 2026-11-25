package com.example.be.mapper;

import com.example.be.dto.response.auth.AuthResponse;
import com.example.be.dto.response.user.UserTargetProfileResponse;
import com.example.be.entity.user.TargetProfile;
import com.example.be.entity.user.User;
import com.example.be.enums.user.ELearnerType;
import com.example.be.enums.user.ERole;
import com.example.be.mapper.auth.AuthMapper;
import com.example.be.mapper.user.TargetProfileMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test cho {@link AuthMapper} và {@link TargetProfileMapper} phục vụ UC05 & Onboarding Profile.
 */
class TargetAndAuthMapperTest {

    private final AuthMapper authMapper = new AuthMapper();
    private final TargetProfileMapper targetProfileMapper = new TargetProfileMapper();

    @Test
    void authMapper_mapsTargetAndProfileCompletionStateCorrectly() {
        User userWithoutTarget = new User();
        userWithoutTarget.setId(1L);
        userWithoutTarget.setEmail("new@leby.edu.vn");
        userWithoutTarget.setRole(ERole.STUDENT);

        AuthResponse resp1 = authMapper.toResponse(userWithoutTarget);
        assertThat(resp1.getLearnerType()).isNull();
        assertThat(resp1.isHasSelectedTarget()).isFalse();
        assertThat(resp1.getTargetProfileId()).isNull();
        assertThat(resp1.getTargetTotalScore()).isNull();

        userWithoutTarget.setDob(LocalDate.of(2003, 8, 15));
        userWithoutTarget.setLearnerType(ELearnerType.UNIVERSITY_STUDENT);

        TargetProfile target = new TargetProfile();
        target.setId(3L);
        target.setTargetTotalScore(650);
        userWithoutTarget.setTargetProfile(target);

        AuthResponse resp2 = authMapper.toResponse(userWithoutTarget);
        assertThat(resp2.getDob()).isEqualTo(LocalDate.of(2003, 8, 15));
        assertThat(resp2.getLearnerType()).isEqualTo(ELearnerType.UNIVERSITY_STUDENT);
        assertThat(resp2.isHasSelectedTarget()).isTrue();
        assertThat(resp2.getTargetProfileId()).isEqualTo(3L);
        assertThat(resp2.getTargetTotalScore()).isEqualTo(650);
    }

    @Test
    void targetProfileMapper_marksOnly650AsRecommended() {
        TargetProfile profile650 = new TargetProfile();
        profile650.setId(2L);
        profile650.setTargetTotalScore(650);
        profile650.setDescription("Mục tiêu 650");
        profile650.setStatus(true);

        TargetProfile profile750 = new TargetProfile();
        profile750.setId(4L);
        profile750.setTargetTotalScore(750);
        profile750.setDescription("Mục tiêu 750");
        profile750.setStatus(true);

        UserTargetProfileResponse dto650 = targetProfileMapper.toUserResponse(profile650);
        UserTargetProfileResponse dto750 = targetProfileMapper.toUserResponse(profile750);

        assertThat(dto650.getIsRecommended()).isTrue();
        assertThat(dto750.getIsRecommended()).isFalse();
    }
}

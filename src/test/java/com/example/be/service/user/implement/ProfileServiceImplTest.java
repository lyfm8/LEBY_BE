package com.example.be.service.user.implement;

import com.example.be.dto.request.user.UpdateProfileRequest;
import com.example.be.dto.response.user.ProfileResponse;
import com.example.be.dto.response.user.UserSkillStatResponse;
import com.example.be.entity.ability.Ability;
import com.example.be.entity.ability.UserAbility;
import com.example.be.entity.content.Part;
import com.example.be.entity.media.Media;
import com.example.be.entity.setting.AbilityEvaluationRule;
import com.example.be.entity.user.User;
import com.example.be.enums.ability.EAbilityStatus;
import com.example.be.enums.diagnostic.ETestType;
import com.example.be.enums.media.EMediaStatus;
import com.example.be.mapper.user.ProfileMapper;
import com.example.be.repository.ability.UserAbilityRepository;
import com.example.be.repository.content.UserMistakeQueryRepository;
import com.example.be.repository.media.MediaRepository;
import com.example.be.repository.setting.AbilityEvaluationRuleRepository;
import com.example.be.repository.user.UserRepository;
import com.example.be.service.media.service.MediaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileServiceImplTest {

    private static final Long USER_ID = 7L;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserAbilityRepository userAbilityRepository;

    @Mock
    private UserMistakeQueryRepository userMistakeQueryRepository;

    @Mock
    private AbilityEvaluationRuleRepository abilityEvaluationRuleRepository;

    @Mock
    private MediaRepository mediaRepository;

    @Mock
    private MediaService mediaService;

    @Mock
    private PasswordEncoder passwordEncoder;

    private ProfileServiceImpl profileService;

    @BeforeEach
    void setUp() {
        profileService = new ProfileServiceImpl(
                userRepository,
                userAbilityRepository,
                userMistakeQueryRepository,
                abilityEvaluationRuleRepository,
                mediaRepository,
                mediaService,
                passwordEncoder,
                new ProfileMapper()
        );
    }

    @Test
    void getProfile_whenUserAbilitiesTableIsEmpty_populatesFullMatrixFromLatestDiagnosticAttempt() {
        User user = buildUser(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userAbilityRepository.findByUserIdWithDetails(USER_ID)).thenReturn(Collections.emptyList());
        when(userMistakeQueryRepository.findLatestCompletedDiagnosticAttemptIds(
                eq(USER_ID), eq(ETestType.DIAGNOSTIC), anyCollection(), any(Pageable.class)))
                .thenReturn(List.of(99L));
        when(userMistakeQueryRepository.aggregateDiagnosticAbilityStatsByAttemptId(99L))
                .thenReturn(List.of(
                        new Object[]{1L, "Human Actions Identification", 1L, 3L, 0L},
                        new Object[]{2L, "Wh-Questions", 2L, 4L, 4L}
                ));
        when(userMistakeQueryRepository.aggregateModuleTestAbilityStatsByUserId(eq(USER_ID), anyCollection()))
                .thenReturn(Collections.emptyList());
        when(abilityEvaluationRuleRepository.findAllWithDetails())
                .thenReturn(List.of(buildRule(1L, 50.0f, 75.0f, 2), buildRule(2L, 50.0f, 75.0f, 2)));

        ProfileResponse response = profileService.getProfile(USER_ID);

        assertThat(response.getSkills()).hasSize(2);
        UserSkillStatResponse first = response.getSkills().get(0);
        assertThat(first.getAbilityName()).isEqualTo("Human Actions Identification");
        assertThat(first.getPartNo()).isEqualTo(1);
        assertThat(first.getAccuracyRate()).isEqualTo(0.0f);
        assertThat(first.getEvidenceCount()).isEqualTo(3);
        assertThat(first.getStatus()).isEqualTo(EAbilityStatus.WEAK);

        UserSkillStatResponse second = response.getSkills().get(1);
        assertThat(second.getAbilityName()).isEqualTo("Wh-Questions");
        assertThat(second.getAccuracyRate()).isEqualTo(100.0f);
        assertThat(second.getStatus()).isEqualTo(EAbilityStatus.STABLE);
    }

    @Test
    void getProfile_mergesPersistedUserAbilitiesWithMissingDiagnosticPartsAndModuleTestProgress() {
        User user = buildUser(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        UserAbility persistedWeak = buildPersistedAbility(1L, "Human Actions Identification", 1L, 25.0f, 4, EAbilityStatus.WEAK);
        when(userAbilityRepository.findByUserIdWithDetails(USER_ID)).thenReturn(List.of(persistedWeak));

        when(userMistakeQueryRepository.findLatestCompletedDiagnosticAttemptIds(
                eq(USER_ID), eq(ETestType.DIAGNOSTIC), anyCollection(), any(Pageable.class)))
                .thenReturn(List.of(100L));
        when(userMistakeQueryRepository.aggregateDiagnosticAbilityStatsByAttemptId(100L))
                .thenReturn(List.of(
                        new Object[]{1L, "Human Actions Identification", 1L, 4L, 1L},
                        new Object[]{5L, "Word Form", 5L, 4L, 2L}
                ));
        // Học viên làm bài thi vượt ải mô-đun Part 1 đúng 7/8 câu -> tổng cộng 8/12 (66.7% -> DEVELOPING)
        when(userMistakeQueryRepository.aggregateModuleTestAbilityStatsByUserId(eq(USER_ID), anyCollection()))
                .thenReturn(List.<Object[]>of(
                        new Object[]{1L, "Human Actions Identification", 1L, 8L, 7L}
                ));
        when(abilityEvaluationRuleRepository.findAllWithDetails())
                .thenReturn(List.of(buildRule(1L, 50.0f, 75.0f, 2), buildRule(5L, 50.0f, 75.0f, 2)));

        ProfileResponse response = profileService.getProfile(USER_ID);

        assertThat(response.getSkills()).hasSize(2);
        UserSkillStatResponse part1 = response.getSkills().get(0);
        assertThat(part1.getAbilityId()).isEqualTo(1L);
        assertThat(part1.getEvidenceCount()).isEqualTo(12);
        assertThat(Math.round(part1.getAccuracyRate())).isEqualTo(67);
        assertThat(part1.getStatus()).isEqualTo(EAbilityStatus.DEVELOPING);

        UserSkillStatResponse part5 = response.getSkills().get(1);
        assertThat(part5.getAbilityId()).isEqualTo(5L);
        assertThat(part5.getEvidenceCount()).isEqualTo(4);
        assertThat(part5.getAccuracyRate()).isEqualTo(50.0f);
        assertThat(part5.getStatus()).isEqualTo(EAbilityStatus.DEVELOPING);
    }

    @Test
    void updateProfile_confirmsNewPendingAvatarAndReleasesOldActiveAvatar() {
        String oldUrl = "https://res.cloudinary.com/leby/image/upload/old-avatar.png";
        String newUrl = "https://res.cloudinary.com/leby/image/upload/new-avatar.png";

        User user = buildUser(USER_ID);
        user.setAvatar(oldUrl);

        Media oldMedia = new Media();
        oldMedia.setPublicId("old-avatar");
        oldMedia.setSecureUrl(oldUrl);
        oldMedia.setStatus(EMediaStatus.ACTIVE);

        Media newMedia = new Media();
        newMedia.setPublicId("new-avatar");
        newMedia.setSecureUrl(newUrl);
        newMedia.setStatus(EMediaStatus.PENDING);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mediaRepository.findBySecureUrl(newUrl)).thenReturn(Optional.of(newMedia));
        when(mediaRepository.findBySecureUrl(oldUrl)).thenReturn(Optional.of(oldMedia));
        when(userAbilityRepository.findByUserIdWithDetails(USER_ID)).thenReturn(Collections.emptyList());
        when(userMistakeQueryRepository.findLatestCompletedDiagnosticAttemptIds(
                eq(USER_ID), eq(ETestType.DIAGNOSTIC), anyCollection(), any(Pageable.class)))
                .thenReturn(Collections.emptyList());
        when(userMistakeQueryRepository.aggregateModuleTestAbilityStatsByUserId(eq(USER_ID), anyCollection()))
                .thenReturn(Collections.emptyList());

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullName("Nguyễn Văn B");
        request.setAvatar(newUrl);

        ProfileResponse response = profileService.updateProfile(USER_ID, request);

        assertThat(response.getUser().getAvatar()).isEqualTo(newUrl);
        verify(mediaService).confirmMedia(newMedia);
        verify(mediaService).releaseMedia(oldMedia);
    }

    private User buildUser(Long id) {
        User user = new User();
        user.setId(id);
        user.setEmail("student@leby.vn");
        user.setFullName("Nguyễn Văn A");
        return user;
    }

    private UserAbility buildPersistedAbility(
            Long abilityId,
            String abilityName,
            Long partId,
            float accuracy,
            int evidence,
            EAbilityStatus status
    ) {
        Ability ability = new Ability();
        ability.setId(abilityId);
        ability.setName(abilityName);

        Part part = new Part();
        part.setId(partId);

        UserAbility ua = new UserAbility();
        ua.setAbility(ability);
        ua.setPart(part);
        ua.setAccuracyRate(accuracy);
        ua.setEvidenceCount(evidence);
        ua.setStatus(status);
        return ua;
    }

    private AbilityEvaluationRule buildRule(Long abilityId, float developing, float stable, int minEvidence) {
        Ability ability = new Ability();
        ability.setId(abilityId);

        AbilityEvaluationRule rule = new AbilityEvaluationRule();
        rule.setAbility(ability);
        rule.setDevelopingThreshold(developing);
        rule.setStableThreshold(stable);
        rule.setMinEvidenceCount(minEvidence);
        return rule;
    }
}

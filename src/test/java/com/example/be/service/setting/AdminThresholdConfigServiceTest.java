package com.example.be.service.setting;

import com.example.be.dto.request.setting.CopyThresholdsFromAimRequest;
import com.example.be.dto.request.setting.SaveAbilityRulesBatchRequest;
import com.example.be.dto.request.setting.SavePartThresholdsBatchRequest;
import com.example.be.dto.response.setting.AimAbilityRuleMatrixResponse;
import com.example.be.dto.response.setting.AimPartThresholdMatrixResponse;
import com.example.be.dto.response.setting.ThresholdValidationResponse;
import com.example.be.entity.ability.Ability;
import com.example.be.entity.content.Part;
import com.example.be.entity.setting.AbilityEvaluationRule;
import com.example.be.entity.setting.TargetPartThreshold;
import com.example.be.entity.user.TargetProfile;
import com.example.be.enums.ability.ESection;
import com.example.be.exception.BaseException;
import com.example.be.repository.ability.AbilityRepository;
import com.example.be.repository.content.PartRepository;
import com.example.be.repository.setting.AbilityEvaluationRuleRepository;
import com.example.be.repository.setting.TargetPartThresholdRepository;
import com.example.be.repository.user.TargetProfileRepository;
import com.example.be.service.setting.implement.AdminThresholdConfigServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminThresholdConfigServiceTest {

    @Mock
    private TargetPartThresholdRepository targetPartThresholdRepo;

    @Mock
    private AbilityEvaluationRuleRepository abilityRuleRepo;

    @Mock
    private TargetProfileRepository targetProfileRepo;

    @Mock
    private PartRepository partRepo;

    @Mock
    private AbilityRepository abilityRepo;

    @InjectMocks
    private AdminThresholdConfigServiceImpl thresholdConfigService;

    private TargetProfile sampleProfile;
    private Part samplePart;
    private Ability sampleAbility;

    @BeforeEach
    void setUp() {
        sampleProfile = new TargetProfile();
        sampleProfile.setId(2L);
        sampleProfile.setTargetTotalScore(650);
        sampleProfile.setDescription("Mục tiêu 650 TOEIC");

        samplePart = new Part();
        samplePart.setId(1L);
        samplePart.setName("Part 1: Photographs");
        samplePart.setSections(ESection.LISTENING);

        sampleAbility = new Ability();
        sampleAbility.setId(10L);
        sampleAbility.setName("Human Actions Identification");
        sampleAbility.setPart(samplePart);
        sampleAbility.setSections(ESection.LISTENING);
    }

    @Test
    @DisplayName("A08: Lấy ma trận ngưỡng 7 Part theo AIM thành công")
    void testGetPartThresholdMatrixSuccess() {
        when(targetProfileRepo.findById(2L)).thenReturn(Optional.of(sampleProfile));
        when(partRepo.findAll(any(Sort.class))).thenReturn(List.of(samplePart));

        TargetPartThreshold threshold = new TargetPartThreshold();
        threshold.setId(100L);
        threshold.setTargetProfile(sampleProfile);
        threshold.setPart(samplePart);
        threshold.setPassScore(80.0f);
        threshold.setConfirmingScore(60.0f);

        when(targetPartThresholdRepo.findByTargetProfileId(2L)).thenReturn(List.of(threshold));

        AimPartThresholdMatrixResponse response = thresholdConfigService.getPartThresholdMatrix(2L);

        assertNotNull(response);
        assertEquals(2L, response.getTargetProfileId());
        assertEquals(650, response.getAimScore());
        assertEquals(1, response.getRows().size());

        AimPartThresholdMatrixResponse.PartThresholdRowResponse row = response.getRows().get(0);
        assertEquals(80.0f, row.getPassScore());
        assertEquals(60.0f, row.getConfirmingScore());
    }

    @Test
    @DisplayName("A08: Kiểm tra tính hợp lệ - Báo lỗi khi confirmingScore >= passScore")
    void testValidatePartThresholdsDetectsInvalidOrder() {
        when(partRepo.findAll()).thenReturn(List.of(samplePart));

        SavePartThresholdsBatchRequest request = SavePartThresholdsBatchRequest.builder()
                .targetProfileId(2L)
                .thresholds(List.of(
                        SavePartThresholdsBatchRequest.PartThresholdItem.builder()
                                .partId(1L)
                                .confirmingScore(85.0f) // Lỗi: Cận chuẩn lớn hơn đạt chuẩn
                                .passScore(80.0f)
                                .build()
                ))
                .build();

        ThresholdValidationResponse response = thresholdConfigService.validatePartThresholds(request);

        assertNotNull(response);
        assertFalse(response.isValid());
        assertEquals(1, response.getErrors().size());
        assertTrue(response.getErrors().get(0).contains("nhỏ hơn Ngưỡng Đạt chuẩn"));
    }

    @Test
    @DisplayName("A08: Lưu hàng loạt ngưỡng 7 Part thành công khi logic hợp lệ")
    void testSavePartThresholdsBatchSuccess() {
        when(targetProfileRepo.findById(2L)).thenReturn(Optional.of(sampleProfile));
        when(partRepo.findAll()).thenReturn(List.of(samplePart));
        when(partRepo.findAll(any(Sort.class))).thenReturn(List.of(samplePart));
        when(targetPartThresholdRepo.findByTargetProfileIdAndPartId(2L, 1L)).thenReturn(Optional.empty());

        SavePartThresholdsBatchRequest request = SavePartThresholdsBatchRequest.builder()
                .targetProfileId(2L)
                .thresholds(List.of(
                        SavePartThresholdsBatchRequest.PartThresholdItem.builder()
                                .partId(1L)
                                .confirmingScore(60.0f)
                                .passScore(80.0f)
                                .build()
                ))
                .build();

        AimPartThresholdMatrixResponse response = thresholdConfigService.savePartThresholdsBatch(request);

        assertNotNull(response);
        verify(targetPartThresholdRepo).save(any(TargetPartThreshold.class));
    }

    @Test
    @DisplayName("A10: Lấy ma trận Năng lực theo AIM thành công")
    void testGetAbilityRuleMatrixSuccess() {
        when(targetProfileRepo.findById(2L)).thenReturn(Optional.of(sampleProfile));
        when(abilityRepo.findAll(any(Sort.class))).thenReturn(List.of(sampleAbility));

        AbilityEvaluationRule rule = new AbilityEvaluationRule();
        rule.setId(200L);
        rule.setTargetProfile(sampleProfile);
        rule.setAbility(sampleAbility);
        rule.setStableMin(75.0f);

        when(abilityRuleRepo.findByTargetProfileId(2L)).thenReturn(List.of(rule));

        AimAbilityRuleMatrixResponse response = thresholdConfigService.getAbilityRuleMatrix(2L);

        assertNotNull(response);
        assertEquals(1, response.getRows().size());
        assertEquals(75.0f, response.getRows().get(0).getStableMin());
    }

    @Test
    @DisplayName("A10: Lưu hàng loạt quy tắc Năng lực thành công")
    void testSaveAbilityRulesBatchSuccess() {
        when(targetProfileRepo.findById(2L)).thenReturn(Optional.of(sampleProfile));
        when(abilityRepo.findAll()).thenReturn(List.of(sampleAbility));
        when(abilityRepo.findAll(any(Sort.class))).thenReturn(List.of(sampleAbility));
        when(abilityRuleRepo.findByAbilityIdAndTargetProfileId(10L, 2L)).thenReturn(Optional.empty());

        SaveAbilityRulesBatchRequest request = SaveAbilityRulesBatchRequest.builder()
                .targetProfileId(2L)
                .rules(List.of(
                        SaveAbilityRulesBatchRequest.AbilityRuleItem.builder()
                                .abilityId(10L)
                                .stableMin(80.0f)
                                .build()
                ))
                .build();

        AimAbilityRuleMatrixResponse response = thresholdConfigService.saveAbilityRulesBatch(request);

        assertNotNull(response);
        verify(abilityRuleRepo).save(any(AbilityEvaluationRule.class));
    }

    @Test
    @DisplayName("Sao chép cấu hình: Báo lỗi nếu AIM nguồn và AIM đích trùng nhau")
    void testCopyThresholdsThrowsWhenSourceSameAsTarget() {
        CopyThresholdsFromAimRequest request = CopyThresholdsFromAimRequest.builder()
                .sourceProfileId(2L)
                .targetProfileId(2L)
                .build();

        BaseException ex = assertThrows(BaseException.class,
                () -> thresholdConfigService.copyThresholdsFromAim(request));
        assertTrue(ex.getMessage().contains("không được trùng nhau"));
    }

    @Test
    @DisplayName("Sao chép cấu hình: Sao chép cả Part Threshold và Ability Rule sang AIM đích")
    void testCopyThresholdsSuccess() {
        TargetProfile targetProfile = new TargetProfile();
        targetProfile.setId(3L);
        targetProfile.setTargetTotalScore(800);

        when(targetProfileRepo.findById(2L)).thenReturn(Optional.of(sampleProfile));
        when(targetProfileRepo.findById(3L)).thenReturn(Optional.of(targetProfile));

        TargetPartThreshold srcThreshold = new TargetPartThreshold();
        srcThreshold.setPart(samplePart);
        srcThreshold.setPassScore(85.0f);
        srcThreshold.setConfirmingScore(65.0f);

        when(targetPartThresholdRepo.findByTargetProfileId(2L)).thenReturn(List.of(srcThreshold));
        when(targetPartThresholdRepo.findByTargetProfileIdAndPartId(3L, 1L)).thenReturn(Optional.empty());
        when(abilityRuleRepo.findByTargetProfileId(2L)).thenReturn(Collections.emptyList());

        when(partRepo.findAll(any(Sort.class))).thenReturn(List.of(samplePart));

        CopyThresholdsFromAimRequest request = CopyThresholdsFromAimRequest.builder()
                .sourceProfileId(2L)
                .targetProfileId(3L)
                .build();

        AimPartThresholdMatrixResponse response = thresholdConfigService.copyThresholdsFromAim(request);

        assertNotNull(response);
        verify(targetPartThresholdRepo).save(any(TargetPartThreshold.class));
    }
}


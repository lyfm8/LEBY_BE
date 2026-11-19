package com.example.be.service.setting;

import com.example.be.dto.request.setting.BatchUpdateModuleThresholdRequest;
import com.example.be.dto.request.setting.CreateAbilityEvaluationRuleRequest;
import com.example.be.dto.request.setting.CreateTargetPartThresholdRequest;
import com.example.be.dto.request.setting.ModuleThresholdItemDto;
import com.example.be.dto.request.setting.UpdateAbilityEvaluationRuleRequest;
import com.example.be.dto.response.setting.AbilityEvaluationRuleResponse;
import com.example.be.dto.response.setting.ModuleTargetThresholdResponse;
import com.example.be.dto.response.setting.ModuleThresholdMatrixResponse;
import com.example.be.dto.response.setting.TargetPartThresholdResponse;
import com.example.be.entity.ability.Ability;
import com.example.be.entity.content.Module;
import com.example.be.entity.content.Part;
import com.example.be.entity.setting.AbilityEvaluationRule;
import com.example.be.entity.setting.ModuleTargetThreshold;
import com.example.be.entity.setting.TargetPartThreshold;
import com.example.be.entity.user.TargetProfile;
import com.example.be.exception.BaseException;
import com.example.be.mapper.setting.AbilityEvaluationRuleMapper;
import com.example.be.mapper.setting.ModuleTargetThresholdMapper;
import com.example.be.mapper.setting.TargetPartThresholdMapper;
import com.example.be.repository.ability.AbilityRepository;
import com.example.be.repository.content.ModuleRepository;
import com.example.be.repository.content.PartRepository;
import com.example.be.repository.setting.AbilityEvaluationRuleRepository;
import com.example.be.repository.setting.ModuleTargetThresholdRepository;
import com.example.be.repository.setting.TargetPartThresholdRepository;
import com.example.be.repository.user.TargetProfileRepository;
import com.example.be.service.setting.implement.AbilityEvaluationRuleServiceImpl;
import com.example.be.service.setting.implement.ModuleTargetThresholdServiceImpl;
import com.example.be.service.setting.implement.TargetPartThresholdServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

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
class SettingThresholdServiceTest {

    // --- Mocks cho ModuleTargetThresholdService ---
    @Mock
    private ModuleTargetThresholdRepository moduleThresholdRepo;
    @Mock
    private ModuleRepository moduleRepo;
    @Mock
    private TargetProfileRepository profileRepo;
    @Spy
    private ModuleTargetThresholdMapper moduleThresholdMapper = new ModuleTargetThresholdMapper();

    @InjectMocks
    private ModuleTargetThresholdServiceImpl moduleThresholdService;

    // --- Mocks cho AbilityEvaluationRuleService ---
    @Mock
    private AbilityEvaluationRuleRepository abilityRuleRepo;
    @Mock
    private AbilityRepository abilityRepo;
    @Spy
    private AbilityEvaluationRuleMapper abilityRuleMapper = new AbilityEvaluationRuleMapper();

    @InjectMocks
    private AbilityEvaluationRuleServiceImpl abilityRuleService;

    // --- Mocks cho TargetPartThresholdService ---
    @Mock
    private TargetPartThresholdRepository partThresholdRepo;
    @Mock
    private PartRepository partRepo;
    @Spy
    private TargetPartThresholdMapper partThresholdMapper = new TargetPartThresholdMapper();

    @InjectMocks
    private TargetPartThresholdServiceImpl partThresholdService;

    private TargetProfile profile550;
    private TargetProfile profile750;
    private Module module1;
    private Ability ability1;
    private Part part1;

    @BeforeEach
    void setUp() {
        profile550 = new TargetProfile();
        profile550.setId(1L);
        profile550.setTargetTotalScore(550);
        profile550.setDescription("Mục tiêu TOEIC 550");

        profile750 = new TargetProfile();
        profile750.setId(2L);
        profile750.setTargetTotalScore(750);
        profile750.setDescription("Mục tiêu TOEIC 750");

        part1 = new Part();
        part1.setId(1L);
        part1.setName("Part 1: Photographs");

        module1 = new Module();
        module1.setId(10L);
        module1.setTitle("Từ vựng Tranh Miêu tả Người");
        module1.setSequence(1);
        module1.setDefaultPassScore(65);
        module1.setPart(part1);

        ability1 = new Ability();
        ability1.setId(100L);
        ability1.setName("Nhận diện hành động trọng tâm tranh 1 người");
        ability1.setPart(part1);
    }

    // =========================================================================
    // 1. TESTS CHO ModuleTargetThresholdService
    // =========================================================================

    @Test
    @DisplayName("getMatrix: Lấy ma trận kết hợp giữa điểm tùy biến và điểm mặc định của Module")
    void testGetMatrix_Success() {
        // Given
        when(profileRepo.findAllByOrderByTargetTotalScoreAsc()).thenReturn(List.of(profile550, profile750));
        when(moduleRepo.findByPartIdOrderBySequenceAsc(1L)).thenReturn(List.of(module1));

        ModuleTargetThreshold custom550 = new ModuleTargetThreshold();
        custom550.setId(1001L);
        custom550.setModule(module1);
        custom550.setTargetProfile(profile550);
        custom550.setPassScore(60);

        when(moduleThresholdRepo.findByModuleIdIn(List.of(10L))).thenReturn(List.of(custom550));

        // When
        ModuleThresholdMatrixResponse matrix = moduleThresholdService.getMatrix(1L);

        // Then
        assertNotNull(matrix);
        assertEquals(2, matrix.getProfileIds().size());
        assertEquals(1, matrix.getRows().size());

        var row = matrix.getRows().get(0);
        assertEquals(10L, row.getModuleId());
        assertEquals(60, row.getThresholds().get("1"));
        assertTrue(row.getIsCustom().get("1"));

        // Profile 750 chưa có cấu hình tùy biến -> fallback về defaultPassScore của module (65)
        assertEquals(65, row.getThresholds().get("2"));
        assertFalse(row.getIsCustom().get("2"));
    }

    @Test
    @DisplayName("batchUpsert: Cập nhật nhiều cấu hình điểm đỗ thành công")
    void testBatchUpsert_Success() {
        // Given
        ModuleThresholdItemDto item1 = ModuleThresholdItemDto.builder()
                .moduleId(10L)
                .targetProfileId(1L)
                .passScore(70)
                .build();

        BatchUpdateModuleThresholdRequest request = BatchUpdateModuleThresholdRequest.builder()
                .items(List.of(item1))
                .build();

        when(moduleThresholdRepo.findByModuleIdAndTargetProfileId(10L, 1L)).thenReturn(Optional.empty());
        when(moduleRepo.findById(10L)).thenReturn(Optional.of(module1));
        when(profileRepo.findById(1L)).thenReturn(Optional.of(profile550));

        ModuleTargetThreshold savedEntity = new ModuleTargetThreshold();
        savedEntity.setId(500L);
        savedEntity.setModule(module1);
        savedEntity.setTargetProfile(profile550);
        savedEntity.setPassScore(70);

        when(moduleThresholdRepo.save(any(ModuleTargetThreshold.class))).thenReturn(savedEntity);

        // When
        List<ModuleTargetThresholdResponse> responses = moduleThresholdService.batchUpsert(request);

        // Then
        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(70, responses.get(0).getPassScore());
        assertEquals(10L, responses.get(0).getModuleId());
    }

    // =========================================================================
    // 2. TESTS CHO AbilityEvaluationRuleService
    // =========================================================================

    @Test
    @DisplayName("createRule: Thành công khi weakMax <= stableMin")
    void testCreateAbilityRule_Success() {
        // Given
        CreateAbilityEvaluationRuleRequest request = CreateAbilityEvaluationRuleRequest.builder()
                .abilityId(100L)
                .stableMin(75.0f)
                .weakMax(50.0f)
                .minEvidenceCount(3)
                .build();

        when(abilityRuleRepo.existsByAbilityId(100L)).thenReturn(false);
        when(abilityRepo.findById(100L)).thenReturn(Optional.of(ability1));

        AbilityEvaluationRule savedRule = new AbilityEvaluationRule();
        savedRule.setId(10L);
        savedRule.setAbility(ability1);
        savedRule.setStableMin(75.0f);
        savedRule.setWeakMax(50.0f);
        savedRule.setMinEvidenceCount(3);

        when(abilityRuleRepo.save(any(AbilityEvaluationRule.class))).thenReturn(savedRule);

        // When
        AbilityEvaluationRuleResponse response = abilityRuleService.createRule(request);

        // Then
        assertNotNull(response);
        assertEquals(75.0f, response.getStableMin());
        assertEquals(50.0f, response.getWeakMax());
        assertEquals(75.0f, response.getDevelopingMax());
        assertEquals(3, response.getMinEvidenceCount());
    }

    @Test
    @DisplayName("createRule: Thất bại khi weakMax > stableMin (vi phạm logic nghiệp vụ)")
    void testCreateAbilityRule_ThrowsWhenWeakMaxGreaterThanStableMin() {
        // Given: weakMax (80%) > stableMin (70%)
        CreateAbilityEvaluationRuleRequest invalidRequest = CreateAbilityEvaluationRuleRequest.builder()
                .abilityId(100L)
                .stableMin(70.0f)
                .weakMax(80.0f)
                .minEvidenceCount(3)
                .build();

        // When & Then
        assertThrows(BaseException.class, () -> abilityRuleService.createRule(invalidRequest));
    }

    @Test
    @DisplayName("updateRule: Cập nhật thành công ngưỡng đánh giá")
    void testUpdateAbilityRule_Success() {
        // Given
        UpdateAbilityEvaluationRuleRequest request = UpdateAbilityEvaluationRuleRequest.builder()
                .stableMin(80.0f)
                .weakMax(55.0f)
                .minEvidenceCount(5)
                .build();

        AbilityEvaluationRule existing = new AbilityEvaluationRule();
        existing.setId(10L);
        existing.setAbility(ability1);
        existing.setStableMin(75.0f);
        existing.setWeakMax(50.0f);
        existing.setMinEvidenceCount(3);

        when(abilityRuleRepo.findById(10L)).thenReturn(Optional.of(existing));
        when(abilityRuleRepo.save(any(AbilityEvaluationRule.class))).thenReturn(existing);

        // When
        AbilityEvaluationRuleResponse response = abilityRuleService.updateRule(10L, request);

        // Then
        assertNotNull(response);
        assertEquals(80.0f, response.getStableMin());
        assertEquals(55.0f, response.getWeakMax());
        assertEquals(5, response.getMinEvidenceCount());
    }

    // =========================================================================
    // 3. TESTS CHO TargetPartThresholdService
    // =========================================================================

    @Test
    @DisplayName("createThreshold: Thành công khi các ngưỡng tuân thủ thứ tự passScore >= confirmingScore >= fullPartThreshold")
    void testCreatePartThreshold_Success() {
        // Given
        CreateTargetPartThresholdRequest request = CreateTargetPartThresholdRequest.builder()
                .targetProfileId(1L)
                .partId(1L)
                .passScore(80.0f)
                .confirmingScore(60.0f)
                .fullPartThreshold(35.0f)
                .build();

        when(partThresholdRepo.existsByTargetProfileIdAndPartId(1L, 1L)).thenReturn(false);
        when(profileRepo.findById(1L)).thenReturn(Optional.of(profile550));
        when(partRepo.findById(1L)).thenReturn(Optional.of(part1));

        TargetPartThreshold savedEntity = new TargetPartThreshold();
        savedEntity.setId(20L);
        savedEntity.setTargetProfile(profile550);
        savedEntity.setPart(part1);
        savedEntity.setPassScore(80.0f);
        savedEntity.setConfirmingScore(60.0f);
        savedEntity.setFullPartThreshold(35.0f);

        when(partThresholdRepo.save(any(TargetPartThreshold.class))).thenReturn(savedEntity);

        // When
        TargetPartThresholdResponse response = partThresholdService.createThreshold(request);

        // Then
        assertNotNull(response);
        assertEquals(80.0f, response.getPassScore());
        assertEquals(60.0f, response.getConfirmingScore());
        assertEquals(35.0f, response.getFullPartThreshold());
    }

    @Test
    @DisplayName("createThreshold: Thất bại khi confirmingScore > passScore")
    void testCreatePartThreshold_ThrowsWhenConfirmingGreaterThanPass() {
        CreateTargetPartThresholdRequest invalidRequest = CreateTargetPartThresholdRequest.builder()
                .targetProfileId(1L)
                .partId(1L)
                .passScore(70.0f)
                .confirmingScore(85.0f) // Lớn hơn passScore
                .fullPartThreshold(35.0f)
                .build();

        assertThrows(BaseException.class, () -> partThresholdService.createThreshold(invalidRequest));
    }

    @Test
    @DisplayName("deleteThreshold: Xóa thành công khi ID tồn tại")
    void testDeletePartThreshold_Success() {
        when(partThresholdRepo.existsById(20L)).thenReturn(true);

        partThresholdService.deleteThreshold(20L);

        verify(partThresholdRepo).deleteById(20L);
    }
}

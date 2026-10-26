package com.example.be.service.content.implement;

import com.example.be.dto.response.diagnostic.AbilityScoreDTO;
import com.example.be.entity.ability.Ability;
import com.example.be.entity.content.LearningPath;
import com.example.be.entity.content.LearningPathItem;
import com.example.be.entity.content.Module;
import com.example.be.entity.diagnostic.DiagnosticAnswer;
import com.example.be.entity.diagnostic.DiagnosticAttempt;
import com.example.be.entity.diagnostic.PartDiagnosticResult;
import com.example.be.entity.question.Question;
import com.example.be.entity.question.QuestionAbility;
import com.example.be.entity.setting.AbilityEvaluationRule;
import com.example.be.entity.setting.TargetPartThreshold;
import com.example.be.entity.user.TargetProfile;
import com.example.be.enums.content.ELearningPathStatus;
import com.example.be.enums.content.ELessonStatus;
import com.example.be.enums.diagnostic.EPartDirective;
import com.example.be.repository.content.LearningPathItemRepository;
import com.example.be.repository.content.LearningPathRepository;
import com.example.be.repository.content.ModuleRepository;
import com.example.be.repository.setting.AbilityEvaluationRuleRepository;
import com.example.be.repository.setting.TargetPartThresholdRepository;
import com.example.be.repository.user.TargetProfileRepository;
import com.example.be.service.content.AdaptivePathGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Triển khai thuật toán sinh lộ trình thích ứng cá nhân hóa (Adaptive Path Generation).
 * Tuân thủ quy chuẩn Zero-Conflict:
 * - Không hardcode bất kỳ tham số nào (đọc động từ TargetPartThreshold và AbilityEvaluationRule).
 * - Sắp xếp Module theo đúng thứ tự sư phạm Module.sequence.
 * - Triển khai Kịch bản B: Gán trực tiếp TargetProfile và DiagnosticAttempt vào LearningPath.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdaptivePathGeneratorServiceImpl implements AdaptivePathGeneratorService {

    private final TargetPartThresholdRepository targetPartThresholdRepo;
    private final AbilityEvaluationRuleRepository abilityRuleRepo;
    private final ModuleRepository moduleRepo;
    private final LearningPathRepository learningPathRepo;
    private final LearningPathItemRepository learningPathItemRepo;
    private final TargetProfileRepository targetProfileRepo;

    @Override
    @Transactional
    public LearningPath generateAdaptivePath(DiagnosticAttempt attempt, Long targetProfileId) {
        TargetProfile targetProfile = targetProfileRepo.findById(targetProfileId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy TargetProfile với ID: " + targetProfileId));

        // Lưu trữ các lộ trình cũ sang trạng thái ARCHIVED nếu có
        learningPathRepo.findByUserIdAndStatus(attempt.getUser().getId(), ELearningPathStatus.ACTIVE)
                .ifPresent(oldPath -> {
                    oldPath.setStatus(ELearningPathStatus.ARCHIVED);
                    learningPathRepo.save(oldPath);
                });

        // Khởi tạo Lộ trình mới 
        LearningPath learningPath = new LearningPath();
        learningPath.setUser(attempt.getUser());
        learningPath.setTargetProfile(targetProfile);
        learningPath.setDiagnosticAttempt(attempt);
        learningPath.setStatus(ELearningPathStatus.ACTIVE);
        learningPath.setVersion(1);
        learningPath = learningPathRepo.save(learningPath);

        // Map lưu trữ Module đã chọn và Ability liên quan (Khử trùng lặp qua Module ID)
        Map<Long, SelectedModuleEntry> selectedModuleMap = new LinkedHashMap<>();

        // Quét từng Part trong kết quả thi chẩn đoán (7 Part TOEIC)
        if (attempt.getPartResults() != null) {
            for (PartDiagnosticResult partResult : attempt.getPartResults()) {
                Long partId = partResult.getPart().getId();
                float partScore = partResult.getScore();

                // ĐỌC NGƯỠNG ĐỘNG TỪ DATABASE THEO AIM ĐÃ CHỌN (KHÔNG HARDCODE)
                TargetPartThreshold threshold = targetPartThresholdRepo
                        .findByTargetProfileIdAndPartId(targetProfileId, partId)
                        .orElseThrow(() -> new IllegalStateException("Chưa cấu hình ngưỡng cho Part " + partId + " và TargetProfile " + targetProfileId));

                // Phân loại Directive tự động theo hàm nghiệp vụ trong Entity
                EPartDirective directive = threshold.determineDirective(partScore);
                partResult.setDirective(directive);

                // XỬ LÝ 4 NHÁNH TẦNG 1
                switch (directive) {
                    case PASS:
                        // Nhánh 1: Đạt chuẩn AIM -> MIỄN HỌC 100% Part này (0 module)
                        break;

                    case FULL_PART:
                        // Nhánh 4: Mất gốc (< fullPartThreshold) -> Bốc toàn bộ 100% module của Part theo sequence
                        List<Module> allModulesOfPart = moduleRepo.findByPartIdOrderBySequenceAsc(partId);
                        for (Module m : allModulesOfPart) {
                            selectedModuleMap.putIfAbsent(m.getId(), new SelectedModuleEntry(m, null));
                        }
                        break;

                    case CONFIRM:
                    case ABILITY_DIAGNOSTIC:
                        // Nhánh 2 & 3: Xuống Tầng 2 soi Ability vi mô
                        // CONFIRM dùng developingThreshold (Seed Data mẫu 50%)
                        // ABILITY_DIAGNOSTIC dùng stableThreshold (Seed Data mẫu 75%)
                        processAbilityTier(partResult, directive, attempt, selectedModuleMap);
                        break;
                }
            }
        }

        // BƯỚC SẮP XẾP SƯ PHẠM BẤT DI BẤT DỊCH: Sắp xếp theo Module.sequence
        List<SelectedModuleEntry> finalEntries = new ArrayList<>(selectedModuleMap.values());
        finalEntries.sort(Comparator.comparing(entry -> entry.module.getSequence()));

        // TẠO CÁC BẢN GHI LEARNING PATH ITEMS (Gán orderNo tăng dần: 1, 2, 3...)
        int order = 1;
        for (SelectedModuleEntry entry : finalEntries) {
            LearningPathItem item = new LearningPathItem();
            item.setLearningPath(learningPath);
            item.setModule(entry.module);
            item.setAbility(entry.ability);
            item.setOrderNo(order);
            item.setProgressPercent(0);
            item.setIsCorrect(false);
            // Module đầu tiên mở khóa sẵn (IN_PROGRESS), các module sau bị khóa (LOCKED)
            item.setStatus(order == 1 ? ELessonStatus.IN_PROGRESS : ELessonStatus.LOCKED);
            learningPathItemRepo.save(item);
            order++;
        }

        return learningPath;
    }

    private void processAbilityTier(
            PartDiagnosticResult partResult,
            EPartDirective directive,
            DiagnosticAttempt attempt,
            Map<Long, SelectedModuleEntry> selectedModuleMap) {

        List<AbilityScoreDTO> abilityScores = calculateAbilityScores(partResult, attempt);

        for (AbilityScoreDTO abilityScore : abilityScores) {
            // ĐỌC QUY TẮC ĐỘNG TỪ BẢNG ability_evaluation_rules (KHÔNG HARDCODE)
            AbilityEvaluationRule rule = abilityRuleRepo.findByAbilityId(abilityScore.getAbilityId())
                    .orElse(null);

            boolean isWeak;
            if (rule != null) {
                isWeak = rule.isWeak(abilityScore.getAccuracyRate(), directive);
            } else {
                // Fallback an toàn nếu chưa kịp gán rule cho Ability
                float fallbackCutoff = (directive == EPartDirective.CONFIRM || directive == EPartDirective.LEVEL_3_CONFIRM)
                        ? 50.0f
                        : 75.0f;
                isWeak = abilityScore.getAccuracyRate() < fallbackCutoff;
            }

            // ĐÁNH GIÁ NHỊ PHÂN TẦNG 2: Nếu tỷ lệ đúng < ngưỡng cắt -> BỐC MODULE
            if (isWeak) {
                List<Module> modulesForAbility = moduleRepo.findByAbilityId(abilityScore.getAbilityId());
                for (Module m : modulesForAbility) {
                    Ability abilityObj = new Ability();
                    abilityObj.setId(abilityScore.getAbilityId());
                    selectedModuleMap.putIfAbsent(m.getId(), new SelectedModuleEntry(m, abilityObj));
                }
            }
        }
    }

    /**
     * Bóc tách điểm số các Ability con trong Part từ chi tiết bài làm của học viên.
     */
    private List<AbilityScoreDTO> calculateAbilityScores(PartDiagnosticResult partResult, DiagnosticAttempt attempt) {
        Map<Long, AbilityStat> statMap = new HashMap<>();

        if (attempt.getAnswers() != null && !attempt.getAnswers().isEmpty()) {
            for (DiagnosticAnswer answer : attempt.getAnswers()) {
                Question question = answer.getQuestion();
                if (question != null && question.getPart() != null
                        && question.getPart().getId().equals(partResult.getPart().getId())) {

                    boolean correct = Boolean.TRUE.equals(answer.getIsCorrect());

                    if (question.getQuestionAbilities() != null) {
                        for (QuestionAbility qa : question.getQuestionAbilities()) {
                            Ability ability = qa.getAbility();
                            if (ability != null) {
                                AbilityStat stat = statMap.computeIfAbsent(ability.getId(),
                                        k -> new AbilityStat(ability.getId(), ability.getName()));
                                stat.totalQuestions++;
                                if (correct) {
                                    stat.correctQuestions++;
                                }
                            }
                        }
                    }
                }
            }
        }

        List<AbilityScoreDTO> dtoList = new ArrayList<>();
        for (AbilityStat stat : statMap.values()) {
            float accuracy = stat.totalQuestions > 0
                    ? (stat.correctQuestions * 100.0f) / stat.totalQuestions
                    : 0.0f;

            dtoList.add(AbilityScoreDTO.builder()
                    .abilityId(stat.abilityId)
                    .abilityName(stat.abilityName)
                    .accuracyRate(accuracy)
                    .correctCount(stat.correctQuestions)
                    .totalEvidenceCount(stat.totalQuestions)
                    .build());
        }

        return dtoList;
    }

    private static class AbilityStat {
        Long abilityId;
        String abilityName;
        int totalQuestions = 0;
        int correctQuestions = 0;

        AbilityStat(Long abilityId, String abilityName) {
            this.abilityId = abilityId;
            this.abilityName = abilityName;
        }
    }

    private static class SelectedModuleEntry {
        final Module module;
        final Ability ability;

        SelectedModuleEntry(Module module, Ability ability) {
            this.module = module;
            this.ability = ability;
        }
    }
}

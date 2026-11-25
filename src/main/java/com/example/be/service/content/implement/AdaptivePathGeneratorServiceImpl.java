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
import com.example.be.entity.setting.ModuleTargetThreshold;
import com.example.be.entity.setting.TargetPartThreshold;
import com.example.be.entity.user.TargetProfile;
import com.example.be.enums.content.ELearningPathStatus;
import com.example.be.enums.content.ELessonStatus;
import com.example.be.enums.diagnostic.EPartDirective;
import com.example.be.enums.diagnostic.ETestResult;
import com.example.be.exception.BaseException;
import com.example.be.exception.ErrorCode;
import com.example.be.repository.content.LearningPathItemRepository;
import com.example.be.repository.content.LearningPathRepository;
import com.example.be.repository.content.ModuleRepository;
import com.example.be.repository.content.ModuleTestAttemptRepository;
import com.example.be.repository.setting.AbilityEvaluationRuleRepository;
import com.example.be.repository.setting.ModuleTargetThresholdRepository;
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
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Triển khai thuật toán sinh lộ trình thích ứng cá nhân hóa (Adaptive Path Generation).
 * Tuân thủ quy chuẩn Zero-Conflict:
 * - Không hardcode bất kỳ tham số nào (đọc động từ TargetPartThreshold, AbilityEvaluationRule, ModuleTargetThreshold).
 * - Sắp xếp Module theo đúng thứ tự sư phạm Module.sequence.
 * - Hỗ trợ BR02 & BR06 khi đổi AIM: tính version tăng dần, lưu trữ lộ trình cũ và bảo lưu phần bài học / kết quả thi đỗ.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdaptivePathGeneratorServiceImpl implements AdaptivePathGeneratorService {

    private static final int FULL_PROGRESS_PERCENT = 100;
    private static final int ZERO_PROGRESS_PERCENT = 0;

    private final TargetPartThresholdRepository targetPartThresholdRepo;
    private final AbilityEvaluationRuleRepository abilityRuleRepo;
    private final ModuleRepository moduleRepo;
    private final LearningPathRepository learningPathRepo;
    private final LearningPathItemRepository learningPathItemRepo;
    private final TargetProfileRepository targetProfileRepo;
    private final ModuleTestAttemptRepository moduleTestAttemptRepo;
    private final ModuleTargetThresholdRepository moduleTargetThresholdRepo;

    @Override
    @Transactional
    public LearningPath generateAdaptivePath(DiagnosticAttempt attempt, Long targetProfileId) {
        // Bước 1: Kiểm tra tồn tại mốc mục tiêu AIM mới
        TargetProfile targetProfile = targetProfileRepo.findById(targetProfileId)
                .orElseThrow(() -> new BaseException(ErrorCode.TARGET_NOT_FOUND, "Không tìm thấy TargetProfile với ID: " + targetProfileId));

        Long userId = attempt.getUser().getId();

        // Bước 2: Lưu trữ toàn bộ lộ trình ACTIVE cũ (BR06), đồng thời xác định AIM trước đó & version tiếp theo
        PathHistoryMetadata historyMeta = resolveHistoryAndArchiveOldPaths(userId);

        // Bước 3: Khởi tạo và lưu bản ghi LearningPath mới
        LearningPath learningPath = new LearningPath();
        learningPath.setUser(attempt.getUser());
        learningPath.setTargetProfile(targetProfile);
        learningPath.setDiagnosticAttempt(attempt);
        learningPath.setStatus(ELearningPathStatus.ACTIVE);
        learningPath.setVersion(historyMeta.nextVersion);
        learningPath = learningPathRepo.save(learningPath);

        // Bước 4: Quét 7 Part trong kết quả chẩn đoán, phân nhánh Tầng 1 & soi Ability Tầng 2
        Map<Long, SelectedModuleEntry> selectedModuleMap = evaluateModulesFromDiagnostic(attempt, targetProfileId);

        // Bước 5: Sắp xếp cố định theo thứ tự sư phạm Module.sequence
        List<SelectedModuleEntry> finalEntries = new ArrayList<>(selectedModuleMap.values());
        finalEntries.sort(Comparator.comparing(entry -> entry.module.getSequence()));

        // Bước 6: Tạo các LearningPathItem kèm logic bảo lưu tiến độ bài học / vượt ải (BR06)
        createAndSavePathItems(learningPath, finalEntries, targetProfile, historyMeta.previousTargetScore);

        return learningPath;
    }

    /**
     * Đọc lịch sử lộ trình của học viên, xác định điểm AIM gần nhất & version kế tiếp,
     * sau đó chuyển toàn bộ lộ trình đang ACTIVE sang trạng thái ARCHIVED.
     */
    private PathHistoryMetadata resolveHistoryAndArchiveOldPaths(Long userId) {
        List<LearningPath> existingPaths = learningPathRepo.findWithTargetProfileByUserIdOrderByCreatedAtDesc(userId);

        Integer previousTargetScore = existingPaths.stream()
                .filter(lp -> lp.getStatus() == ELearningPathStatus.ACTIVE)
                .findFirst()
                .or(() -> existingPaths.stream().findFirst())
                .map(LearningPath::getTargetProfile)
                .map(TargetProfile::getTargetTotalScore)
                .orElse(null);

        int nextVersion = existingPaths.stream()
                .map(LearningPath::getVersion)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0) + 1;

        List<LearningPath> activePaths = existingPaths.stream()
                .filter(lp -> lp.getStatus() == ELearningPathStatus.ACTIVE)
                .peek(lp -> lp.setStatus(ELearningPathStatus.ARCHIVED))
                .toList();

        if (!activePaths.isEmpty()) {
            learningPathRepo.saveAll(activePaths);
        }

        return new PathHistoryMetadata(previousTargetScore, nextVersion);
    }

    /**
     * Đánh giá 2 tầng trên toàn bộ Part của lượt thi chẩn đoán để chọn tập Module cần học theo AIM mới.
     */
    private Map<Long, SelectedModuleEntry> evaluateModulesFromDiagnostic(DiagnosticAttempt attempt, Long targetProfileId) {
        Map<Long, SelectedModuleEntry> selectedModuleMap = new LinkedHashMap<>();

        if (attempt.getPartResults() == null) {
            return selectedModuleMap;
        }

        for (PartDiagnosticResult partResult : attempt.getPartResults()) {
            Long partId = partResult.getPart().getId();
            float partScore = partResult.getScore();

            // ĐỌC NGƯỠNG ĐỘNG TỪ DATABASE THEO AIM ĐÃ CHỌN (KHÔNG HARDCODE)
            TargetPartThreshold threshold = targetPartThresholdRepo
                    .findByTargetProfileIdAndPartId(targetProfileId, partId)
                    .orElseThrow(() -> new BaseException(
                            ErrorCode.BAD_REQUEST,
                            "Chưa cấu hình ngưỡng cho Part " + partId + " và TargetProfile " + targetProfileId
                    ));

            EPartDirective directive = threshold.determineDirective(partScore);
            partResult.setDirective(directive);

            switch (directive) {
                case PASS -> {
                    // Đạt chuẩn AIM -> Miễn học 100% Part này
                }
                case FULL_PART -> {
                    // Mất gốc -> Bốc toàn bộ module của Part theo thứ tự sequence
                    List<Module> allModulesOfPart = moduleRepo.findByPartIdOrderBySequenceAsc(partId);
                    for (Module m : allModulesOfPart) {
                        selectedModuleMap.putIfAbsent(m.getId(), new SelectedModuleEntry(m, null));
                    }
                }
                case CONFIRM, ABILITY_DIAGNOSTIC -> processAbilityTier(partResult, directive, attempt, selectedModuleMap);
            }
        }

        return selectedModuleMap;
    }

    /**
     * Khởi tạo và lưu hàng loạt các LearningPathItem cho lộ trình mới, áp dụng quy tắc bảo lưu BR06:
     * - Nếu module đã từng thi vượt ải thành công (PASS) ở quá khứ:
     *   + Khi tăng AIM lên mốc cao hơn (hoặc điểm cũ thấp hơn ngưỡng mới): giữ tiến độ bài giảng 100%
     *     (không phải học lại lesson), nhưng yêu cầu làm lại bài kiểm tra vượt ải (isCorrect = false).
     *   + Khi hạ/giữ nguyên AIM và điểm đạt chuẩn ngưỡng mới: bảo lưu hoàn toàn COMPLETED.
     * - Mở khóa (IN_PROGRESS) cho module đầu tiên chưa COMPLETED trong danh sách.
     */
    private void createAndSavePathItems(
            LearningPath learningPath,
            List<SelectedModuleEntry> finalEntries,
            TargetProfile newProfile,
            Integer previousTargetScore
    ) {
        if (finalEntries.isEmpty()) {
            return;
        }

        Long userId = learningPath.getUser().getId();
        Map<Long, Integer> passedModuleBestScores = loadPassedModuleBestScores(userId);
        Map<Long, Integer> newModulePassScores = loadModulePassScoresForTarget(newProfile.getId());

        boolean isHigherAim = previousTargetScore != null
                && newProfile.getTargetTotalScore() != null
                && newProfile.getTargetTotalScore() > previousTargetScore;

        List<LearningPathItem> itemsToSave = new ArrayList<>();
        int order = 1;

        for (SelectedModuleEntry entry : finalEntries) {
            LearningPathItem item = new LearningPathItem();
            item.setLearningPath(learningPath);
            item.setModule(entry.module);
            item.setAbility(entry.ability);
            item.setOrderNo(order++);

            Long moduleId = entry.module.getId();
            if (passedModuleBestScores.containsKey(moduleId)) {
                applyInheritedModuleState(item, entry.module, passedModuleBestScores.get(moduleId), newModulePassScores, isHigherAim);
            } else {
                item.setProgressPercent(ZERO_PROGRESS_PERCENT);
                item.setIsCorrect(false);
                item.setStatus(ELessonStatus.LOCKED);
            }

            itemsToSave.add(item);
        }

        // Mở khóa module đầu tiên chưa hoàn thành để học viên bắt đầu ngay
        itemsToSave.stream()
                .filter(i -> i.getStatus() != ELessonStatus.COMPLETED)
                .findFirst()
                .ifPresent(i -> i.setStatus(ELessonStatus.IN_PROGRESS));

        learningPathItemRepo.saveAll(itemsToSave);
    }

    /**
     * Áp dụng trạng thái kế thừa BR06 cho một Module mà học viên đã từng PASS trong quá khứ.
     */
    private void applyInheritedModuleState(
            LearningPathItem item,
            Module module,
            Integer bestPreviousScore,
            Map<Long, Integer> newModulePassScores,
            boolean isHigherAim
    ) {
        Integer configuredThreshold = newModulePassScores.get(module.getId());
        int requiredPassScore = configuredThreshold != null
                ? configuredThreshold
                : (module.getDefaultPassScore() != null ? module.getDefaultPassScore() : 0);

        // Nếu không phải nâng AIM lên cao hơn VÀ điểm cũ đạt ngưỡng AIM mới -> bảo lưu COMPLETED toàn phần
        if (!isHigherAim && bestPreviousScore != null && bestPreviousScore >= requiredPassScore) {
            item.setProgressPercent(FULL_PROGRESS_PERCENT);
            item.setIsCorrect(true);
            item.setStatus(ELessonStatus.COMPLETED);
            item.setReason("Đã bảo lưu kết quả vượt ải từ lộ trình trước");
            return;
        }

        // Trường hợp đổi lên AIM cao hơn (hoặc điểm cũ chưa đủ chuẩn mới):
        // Bảo lưu 100% bài học (không cần học lại lesson), nhưng bắt buộc làm lại bài Module Test theo ngưỡng AIM mới
        item.setProgressPercent(FULL_PROGRESS_PERCENT);
        item.setIsCorrect(false);
        item.setStatus(ELessonStatus.LOCKED);
        item.setReason("Đã hoàn thành bài học — cần làm lại bài kiểm tra vượt ải theo ngưỡng AIM mới");
    }

    /**
     * Truy vấn điểm cao nhất của từng Module mà học viên đã từng PASS trước đó.
     */
    private Map<Long, Integer> loadPassedModuleBestScores(Long userId) {
        List<Object[]> rows = moduleTestAttemptRepo.findPassedModuleMaxScoresByUserId(userId, ETestResult.PASS);
        Map<Long, Integer> result = new HashMap<>();
        if (rows == null) {
            return result;
        }
        for (Object[] row : rows) {
            if (row != null && row.length >= 2 && row[0] != null) {
                Long moduleId = ((Number) row[0]).longValue();
                Integer maxScore = row[1] != null ? ((Number) row[1]).intValue() : null;
                result.put(moduleId, maxScore);
            }
        }
        return result;
    }

    /**
     * Nạp map ngưỡng passScore tùy chỉnh của các Module theo TargetProfile mới (đọc động từ DB).
     */
    private Map<Long, Integer> loadModulePassScoresForTarget(Long targetProfileId) {
        List<ModuleTargetThreshold> thresholds = moduleTargetThresholdRepo.findByTargetProfileId(targetProfileId);
        if (thresholds == null || thresholds.isEmpty()) {
            return new HashMap<>();
        }
        return thresholds.stream()
                .filter(t -> t.getModule() != null && t.getPassScore() != null)
                .collect(Collectors.toMap(
                        t -> t.getModule().getId(),
                        ModuleTargetThreshold::getPassScore,
                        (first, second) -> first
                ));
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
                float fallbackCutoff = (directive == EPartDirective.CONFIRM)
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

    private record PathHistoryMetadata(Integer previousTargetScore, int nextVersion) {
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

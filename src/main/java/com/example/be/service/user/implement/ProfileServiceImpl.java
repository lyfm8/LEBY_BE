package com.example.be.service.user.implement;

import com.example.be.dto.request.user.ChangePasswordRequest;
import com.example.be.dto.request.user.UpdateProfileRequest;
import com.example.be.dto.response.user.ProfileResponse;
import com.example.be.dto.response.user.UserSkillStatResponse;
import com.example.be.entity.ability.UserAbility;
import com.example.be.entity.setting.AbilityEvaluationRule;
import com.example.be.entity.user.User;
import com.example.be.enums.ability.EAbilityStatus;
import com.example.be.enums.diagnostic.EAttemptStatus;
import com.example.be.enums.diagnostic.ETestType;
import com.example.be.enums.media.EMediaStatus;
import com.example.be.exception.BaseException;
import com.example.be.exception.ErrorCode;
import com.example.be.mapper.user.ProfileMapper;
import com.example.be.repository.ability.UserAbilityRepository;
import com.example.be.repository.content.UserMistakeQueryRepository;
import com.example.be.repository.media.MediaRepository;
import com.example.be.repository.setting.AbilityEvaluationRuleRepository;
import com.example.be.repository.user.UserRepository;
import com.example.be.service.media.service.MediaService;
import com.example.be.service.user.service.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Hiện thực hóa các nghiệp vụ xem hồ sơ, chỉnh sửa thông tin và đổi mật khẩu cho người dùng.
 * Tổng hợp Ma trận năng lực TOEIC đầy đủ cả 7 Part (bao gồm PASS/FULL_PART) và tích lũy bài thi mô-đun.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private static final List<EAttemptStatus> COMPLETED_ATTEMPT_STATUSES = List.of(
            EAttemptStatus.SUBMITTED,
            EAttemptStatus.TIMED_OUT
    );
    private static final float DEFAULT_DEVELOPING_THRESHOLD = 50.0f;
    private static final float DEFAULT_STABLE_THRESHOLD = 75.0f;
    private static final int DEFAULT_MIN_EVIDENCE_COUNT = 2;

    private final UserRepository userRepository;
    private final UserAbilityRepository userAbilityRepository;
    private final UserMistakeQueryRepository userMistakeQueryRepository;
    private final AbilityEvaluationRuleRepository abilityEvaluationRuleRepository;
    private final MediaRepository mediaRepository;
    private final MediaService mediaService;
    private final PasswordEncoder passwordEncoder;
    private final ProfileMapper profileMapper;

    /**
     * Lấy toàn bộ thông tin hồ sơ cá nhân của người dùng đang đăng nhập.
     *
     * @param userId ID định danh của người dùng
     * @return ProfileResponse DTO tổng hợp dữ liệu hồ sơ, mục tiêu và ma trận năng lực
     * @throws BaseException Khi không tìm thấy người dùng trong hệ thống
     */
    @Override
    @Transactional(readOnly = true)
    public ProfileResponse getProfile(Long userId) {
        // Bước 1: Truy vấn thông tin người dùng từ cơ sở dữ liệu
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy người dùng với ID: " + userId));

        // Bước 2: Tổng hợp Ma trận năng lực từ snapshot UserAbility + đủ 7 Part bài chẩn đoán + bài thi mô-đun
        List<UserSkillStatResponse> aggregatedSkills = buildComprehensiveSkillMatrix(userId);

        // Bước 3: Ánh xạ thực thể sang DTO thông qua ProfileMapper
        return profileMapper.toProfileResponseWithSkills(user, aggregatedSkills);
    }

    /**
     * Cập nhật thông tin hồ sơ người dùng (Họ tên, ngày sinh, ảnh đại diện, kiểu người học).
     *
     * @param userId ID định danh của người dùng
     * @param request DTO chứa các trường dữ liệu cần cập nhật
     * @return ProfileResponse DTO hồ sơ mới nhất sau khi lưu thành công
     * @throws BaseException Khi không tìm thấy người dùng
     */
    @Override
    @Transactional
    public ProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        // Bước 1: Kiểm tra sự tồn tại của người dùng
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy người dùng với ID: " + userId));

        // Bước 2: Cập nhật các trường dữ liệu được phép thay đổi và đồng bộ trạng thái Media ảnh đại diện
        String previousAvatar = user.getAvatar();
        user.setFullName(request.getFullName().trim());
        user.setDob(request.getDob());

        String nextAvatar = previousAvatar;
        if (request.getAvatar() != null) {
            nextAvatar = request.getAvatar().trim().isEmpty() ? null : request.getAvatar().trim();
            user.setAvatar(nextAvatar);
        }

        if (request.getLearnerType() != null) {
            user.setLearnerType(request.getLearnerType());
        }

        // Bước 3: Lưu thay đổi vào cơ sở dữ liệu
        userRepository.save(user);

        // Bước 3b: Kích hoạt Media Cloudinary mới (PENDING -> ACTIVE) và thu hồi ảnh cũ bị thay thế để tránh mất ảnh sau 12h
        syncAvatarMediaLifecycle(previousAvatar, nextAvatar);
        log.info("[PROFILE] User id={} updated profile successfully", userId);

        // Bước 4: Tải lại và trả về hồ sơ hoàn chỉnh sau khi cập nhật
        return getProfile(userId);
    }

    /**
     * Xử lý đổi mật khẩu tài khoản và vô hiệu hóa toàn bộ phiên đăng nhập cũ.
     *
     * @param userId ID định danh của người dùng
     * @param request DTO chứa mật khẩu hiện tại, mật khẩu mới và xác nhận mật khẩu
     * @throws BaseException Khi mật khẩu hiện tại không đúng, mật khẩu xác nhận không khớp, hoặc mật khẩu mới trùng mật khẩu cũ
     */
    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        // Bước 1: Kiểm tra sự tồn tại của người dùng
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy người dùng với ID: " + userId));

        // Bước 2: Xác minh mật khẩu hiện tại
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "Mật khẩu hiện tại không đúng. Vui lòng kiểm tra lại");
        }

        // Bước 3: Kiểm tra mật khẩu mới và xác nhận mật khẩu có trùng khớp không
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "Mật khẩu xác nhận không khớp với mật khẩu mới");
        }

        // Bước 4: Kiểm tra mật khẩu mới không được trùng với mật khẩu cũ
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "Mật khẩu mới không được trùng với mật khẩu đang sử dụng");
        }

        // Bước 5: Mã hóa mật khẩu mới và tăng tokenVersion để vô hiệu hóa toàn bộ token JWT cũ
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setTokenVersion(user.getTokenVersion() + 1);

        userRepository.save(user);
        log.info("[PROFILE] User id={} changed password successfully. Old tokens invalidated", userId);
    }

    /**
     * Đồng bộ vòng đời tệp ảnh đại diện trên Cloudinary:
     * - Chuyển ảnh mới tải lên từ PENDING sang ACTIVE để tránh bị PendingMediaCleanupScheduler xóa sau 12 giờ.
     * - Thu hồi ảnh đại diện cũ (ACTIVE -> PENDING) nếu người dùng đổi sang ảnh khác hoặc gỡ ảnh.
     */
    private void syncAvatarMediaLifecycle(String previousAvatar, String nextAvatar) {
        if (Objects.equals(previousAvatar, nextAvatar)) {
            return;
        }
        if (nextAvatar != null && !nextAvatar.isBlank()) {
            mediaRepository.findBySecureUrl(nextAvatar).ifPresent(media -> {
                if (media.getStatus() == EMediaStatus.PENDING) {
                    mediaService.confirmMedia(media);
                    log.info("[PROFILE] Confirmed avatar media publicId={}", media.getPublicId());
                }
            });
        }
        if (previousAvatar != null && !previousAvatar.isBlank()) {
            mediaRepository.findBySecureUrl(previousAvatar).ifPresent(oldMedia -> {
                if (oldMedia.getStatus() == EMediaStatus.ACTIVE) {
                    mediaService.releaseMedia(oldMedia);
                    log.info("[PROFILE] Released replaced avatar media publicId={}", oldMedia.getPublicId());
                }
            });
        }
    }

    /**
     * Tổng hợp Ma trận năng lực TOEIC đầy đủ cả 7 Part từ:
     * 1) Snapshot bảng user_abilities (nhánh CONFIRM / ABILITY_DIAGNOSTIC),
     * 2) Lượt thi chẩn đoán hoàn thành gần nhất (lấp đầy cả các Part PASS & FULL_PART),
     * 3) Tích lũy kết quả từ các bài thi vượt ải mô-đun đã nộp.
     */
    private List<UserSkillStatResponse> buildComprehensiveSkillMatrix(Long userId) {
        List<UserAbility> persistedList = userAbilityRepository.findByUserIdWithDetails(userId);
        Map<Long, UserAbility> persistedByAbilityId = new LinkedHashMap<>();
        if (persistedList != null) {
            for (UserAbility ua : persistedList) {
                if (ua != null && ua.getAbility() != null && ua.getAbility().getId() != null) {
                    persistedByAbilityId.putIfAbsent(ua.getAbility().getId(), ua);
                }
            }
        }

        // Nạp thống kê từ lượt chẩn đoán hoàn thành gần nhất (bao phủ đủ 7 Part kể cả PASS và FULL_PART)
        List<Long> latestDiagAttemptIds = userMistakeQueryRepository.findLatestCompletedDiagnosticAttemptIds(
                userId,
                ETestType.DIAGNOSTIC,
                COMPLETED_ATTEMPT_STATUSES,
                PageRequest.of(0, 1)
        );
        List<Object[]> diagnosticAbilityRows = (latestDiagAttemptIds != null && !latestDiagAttemptIds.isEmpty())
                ? userMistakeQueryRepository.aggregateDiagnosticAbilityStatsByAttemptId(latestDiagAttemptIds.get(0))
                : List.of();

        // Nạp thống kê tích lũy từ toàn bộ các lượt thi vượt ải mô-đun đã nộp
        List<Object[]> moduleAbilityRows = userMistakeQueryRepository.aggregateModuleTestAbilityStatsByUserId(
                userId,
                COMPLETED_ATTEMPT_STATUSES
        );

        if (persistedByAbilityId.isEmpty()
                && (diagnosticAbilityRows == null || diagnosticAbilityRows.isEmpty())
                && (moduleAbilityRows == null || moduleAbilityRows.isEmpty())) {
            return List.of();
        }

        Map<Long, MutableAbilityStat> aggregatedByAbilityId = new LinkedHashMap<>();
        mergeAbilityRows(aggregatedByAbilityId, diagnosticAbilityRows, false);
        mergeAbilityRows(aggregatedByAbilityId, moduleAbilityRows, true);

        // Chỉ truy vấn bảng quy tắc đánh giá 1 lần duy nhất nếu có kỹ năng cần chấm động
        boolean needsRuleEvaluation = aggregatedByAbilityId.entrySet().stream()
                .anyMatch(e -> !persistedByAbilityId.containsKey(e.getKey()) || e.getValue().hasModuleTestEvidence);
        Map<Long, AbilityEvaluationRule> ruleByAbilityId = needsRuleEvaluation
                ? loadRuleByAbilityId()
                : Map.of();

        Map<Long, UserSkillStatResponse> resultByAbilityId = new HashMap<>();

        for (Map.Entry<Long, MutableAbilityStat> entry : aggregatedByAbilityId.entrySet()) {
            Long abilityId = entry.getKey();
            MutableAbilityStat stat = entry.getValue();
            UserAbility persisted = persistedByAbilityId.get(abilityId);

            // Nếu đã có snapshot trong user_abilities và chưa có thêm bài thi mô-đun -> giữ nguyên đánh giá chuẩn của báo cáo chẩn đoán
            if (persisted != null && !stat.hasModuleTestEvidence) {
                resultByAbilityId.put(abilityId, profileMapper.toSkillStatResponse(persisted));
                continue;
            }

            int total = stat.totalQuestions;
            float accuracyRate = total > 0 ? (stat.correctQuestions * 100.0f) / total : 0.0f;
            EAbilityStatus status = evaluateAbilityStatus(accuracyRate, total, ruleByAbilityId.get(abilityId));

            resultByAbilityId.put(abilityId, UserSkillStatResponse.builder()
                    .abilityId(abilityId)
                    .abilityName(stat.abilityName)
                    .partNo(stat.partNo)
                    .accuracyRate(accuracyRate)
                    .evidenceCount(total)
                    .status(status)
                    .build());
        }

        // Bảo toàn các bản ghi user_abilities đã lưu trước đó (nếu chưa xuất hiện trong truy vấn tổng hợp)
        for (Map.Entry<Long, UserAbility> entry : persistedByAbilityId.entrySet()) {
            resultByAbilityId.putIfAbsent(entry.getKey(), profileMapper.toSkillStatResponse(entry.getValue()));
        }

        return resultByAbilityId.values().stream()
                .sorted(Comparator
                        .comparing(UserSkillStatResponse::getPartNo, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(UserSkillStatResponse::getAbilityName, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private void mergeAbilityRows(
            Map<Long, MutableAbilityStat> targetMap,
            List<Object[]> rows,
            boolean fromModuleTest
    ) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        for (Object[] row : rows) {
            if (row == null || row.length < 5 || !(row[0] instanceof Number abilityIdNum)) {
                continue;
            }
            Long abilityId = abilityIdNum.longValue();
            String abilityName = row[1] != null ? row[1].toString() : null;
            Integer partNo = row[2] instanceof Number partNum ? partNum.intValue() : null;
            int total = row[3] instanceof Number totalNum ? totalNum.intValue() : 0;
            int correct = row[4] instanceof Number correctNum ? correctNum.intValue() : 0;
            if (total <= 0) {
                continue;
            }

            MutableAbilityStat stat = targetMap.computeIfAbsent(
                    abilityId,
                    ignored -> new MutableAbilityStat(abilityName, partNo)
            );
            stat.totalQuestions += total;
            stat.correctQuestions += correct;
            if (fromModuleTest) {
                stat.hasModuleTestEvidence = true;
            }
        }
    }

    private Map<Long, AbilityEvaluationRule> loadRuleByAbilityId() {
        List<AbilityEvaluationRule> rules = abilityEvaluationRuleRepository.findAllWithDetails();
        if (rules == null || rules.isEmpty()) {
            return Map.of();
        }
        return rules.stream()
                .filter(rule -> rule != null && rule.getAbility() != null && rule.getAbility().getId() != null)
                .collect(Collectors.toMap(
                        rule -> rule.getAbility().getId(),
                        rule -> rule,
                        (first, second) -> first
                ));
    }

    private EAbilityStatus evaluateAbilityStatus(
            float accuracyRate,
            int totalQuestions,
            AbilityEvaluationRule rule
    ) {
        int minEvidence = (rule != null && rule.getMinEvidenceCount() != null)
                ? rule.getMinEvidenceCount()
                : DEFAULT_MIN_EVIDENCE_COUNT;
        if (totalQuestions < minEvidence) {
            return EAbilityStatus.UNCERTAIN;
        }

        float developingCutoff = (rule != null && rule.getDevelopingThreshold() != null)
                ? rule.getDevelopingThreshold()
                : DEFAULT_DEVELOPING_THRESHOLD;
        float stableCutoff = (rule != null && rule.getStableThreshold() != null)
                ? rule.getStableThreshold()
                : DEFAULT_STABLE_THRESHOLD;

        if (accuracyRate >= stableCutoff) {
            return EAbilityStatus.STABLE;
        }
        if (accuracyRate >= developingCutoff) {
            return EAbilityStatus.DEVELOPING;
        }
        return EAbilityStatus.WEAK;
    }

    private static final class MutableAbilityStat {
        private final String abilityName;
        private final Integer partNo;
        private int totalQuestions;
        private int correctQuestions;
        private boolean hasModuleTestEvidence;

        private MutableAbilityStat(String abilityName, Integer partNo) {
            this.abilityName = abilityName;
            this.partNo = partNo;
        }
    }
}

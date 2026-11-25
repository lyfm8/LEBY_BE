package com.example.be.service.user.implement;

import com.example.be.dto.response.user.SelectTargetResponse;
import com.example.be.dto.response.user.UserTargetProfileResponse;
import com.example.be.entity.content.LearningPath;
import com.example.be.entity.diagnostic.DiagnosticAttempt;
import com.example.be.entity.user.TargetProfile;
import com.example.be.entity.user.User;
import com.example.be.enums.diagnostic.EAttemptStatus;
import com.example.be.exception.BaseException;
import com.example.be.exception.ErrorCode;
import com.example.be.mapper.user.TargetProfileMapper;
import com.example.be.repository.diagnostic.DiagnosticAttemptRepository;
import com.example.be.repository.user.TargetProfileRepository;
import com.example.be.repository.user.UserRepository;
import com.example.be.service.content.AdaptivePathGeneratorService;
import com.example.be.service.user.service.UserTargetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Triển khai nghiệp vụ UC05 — Chọn lần đầu và Đổi mục tiêu điểm TOEIC (AIM).
 * Tuân thủ chặt chẽ BR01 (bắt buộc chọn AIM trước chẩn đoán), BR02 (tái sử dụng kết quả chẩn đoán khi đổi AIM)
 * và BR06 (lưu trữ lộ trình cũ + bảo lưu module đã học/đỗ).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserTargetServiceImpl implements UserTargetService {

    private final UserRepository userRepository;
    private final TargetProfileRepository targetProfileRepository;
    private final DiagnosticAttemptRepository diagnosticAttemptRepository;
    private final AdaptivePathGeneratorService adaptivePathGeneratorService;
    private final TargetProfileMapper targetProfileMapper;

    /**
     * Lấy danh sách các mốc mục tiêu AIM đang hoạt động, sắp xếp tăng dần theo điểm mục tiêu.
     *
     * @return Danh sách {@link UserTargetProfileResponse}
     */
    @Override
    @Transactional(readOnly = true)
    public List<UserTargetProfileResponse> getAllActiveTargets() {
        List<TargetProfile> activeProfiles = targetProfileRepository.findByStatusTrueOrderByTargetTotalScoreAsc();
        return targetProfileMapper.toUserResponseList(activeProfiles);
    }

    /**
     * Xử lý chọn mới hoặc thay đổi mục tiêu AIM cho học viên trong cùng một Transaction.
     *
     * @param userId          ID học viên đang đăng nhập
     * @param targetProfileId ID mục tiêu AIM học viên vừa xác nhận
     * @return {@link SelectTargetResponse} chứa chỉ dẫn điều hướng (GO_DIAGNOSTIC | GO_LEARNING_PATH)
     * @throws BaseException khi không tìm thấy user/mục tiêu, mục tiêu bị vô hiệu hóa, hoặc trùng AIM hiện tại
     */
    @Override
    @Transactional
    public SelectTargetResponse selectTarget(Long userId, Long targetProfileId) {
        // Bước 1: Kiểm tra tồn tại học viên và mục tiêu AIM hợp lệ
        User user = userRepository.findWithTargetProfileById(userId)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy tài khoản người dùng"));

        TargetProfile newProfile = targetProfileRepository.findById(targetProfileId)
                .orElseThrow(() -> new BaseException(ErrorCode.TARGET_NOT_FOUND));

        if (!Boolean.TRUE.equals(newProfile.getStatus())) {
            throw new BaseException(ErrorCode.TARGET_PROFILE_INACTIVE);
        }

        // Bước 2: Chặn chọn lại đúng mục tiêu đang học hiện tại (UC05 - Luồng ngoại lệ 3a)
        TargetProfile currentProfile = user.getTargetProfile();
        if (currentProfile != null && currentProfile.getId().equals(targetProfileId)) {
            throw new BaseException(ErrorCode.TARGET_ALREADY_ACTIVE);
        }

        boolean isFirstTime = (currentProfile == null);

        // Bước 3: Cập nhật mục tiêu AIM mới cho học viên
        user.setTargetProfile(newProfile);
        userRepository.save(user);

        // Bước 4a: Nếu là thiết lập mục tiêu lần đầu -> điều hướng sang phòng thi chẩn đoán (BR01)
        if (isFirstTime) {
            log.info("[UC05] Học viên userId={} chọn AIM lần đầu: score={}", userId, newProfile.getTargetTotalScore());
            return targetProfileMapper.toSelectTargetResponse(
                    user,
                    newProfile,
                    SelectTargetResponse.ACTION_GO_DIAGNOSTIC,
                    null
            );
        }

        // Bước 4b: Đổi mục tiêu AIM -> kiểm tra bài chẩn đoán gần nhất đã nộp (BR02 & Luồng 7b)
        Optional<DiagnosticAttempt> latestAttemptOpt = diagnosticAttemptRepository
                .findTopByUserIdAndStatusOrderByCompletedAtDesc(userId, EAttemptStatus.SUBMITTED);

        if (latestAttemptOpt.isEmpty() || !hasValidPartResults(latestAttemptOpt.get())) {
            log.info("[UC05] Học viên userId={} đổi AIM sang {} nhưng chưa có dữ liệu chẩn đoán hợp lệ -> GO_DIAGNOSTIC",
                    userId, newProfile.getTargetTotalScore());
            return targetProfileMapper.toSelectTargetResponse(
                    user,
                    newProfile,
                    SelectTargetResponse.ACTION_GO_DIAGNOSTIC,
                    null
            );
        }

        // Bước 5: Tái sử dụng câu trả lời chẩn đoán cũ, chạy lại phân tầng theo ngưỡng AIM mới và sinh lộ trình mới
        DiagnosticAttempt latestAttempt = latestAttemptOpt.get();
        LearningPath regeneratedPath = adaptivePathGeneratorService.generateAdaptivePath(latestAttempt, targetProfileId);

        log.info("[UC05] Học viên userId={} đổi AIM sang {} thành công, tái sinh learningPathId={}",
                userId, newProfile.getTargetTotalScore(), regeneratedPath.getId());

        return targetProfileMapper.toSelectTargetResponse(
                user,
                newProfile,
                SelectTargetResponse.ACTION_GO_LEARNING_PATH,
                regeneratedPath.getId()
        );
    }

    /**
     * Kiểm tra lượt thi chẩn đoán cũ có đầy đủ kết quả từng Part để tái sử dụng chạy thuật toán 2 tầng hay không.
     */
    private boolean hasValidPartResults(DiagnosticAttempt attempt) {
        return attempt.getPartResults() != null && !attempt.getPartResults().isEmpty();
    }
}

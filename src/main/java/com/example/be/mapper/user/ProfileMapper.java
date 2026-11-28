package com.example.be.mapper.user;

import com.example.be.dto.response.user.ProfileResponse;
import com.example.be.dto.response.user.UserInfoResponse;
import com.example.be.dto.response.user.UserSkillStatResponse;
import com.example.be.dto.response.user.UserTargetResponse;
import com.example.be.entity.ability.UserAbility;
import com.example.be.entity.user.TargetProfile;
import com.example.be.entity.user.User;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Mapper chuyển đổi giữa các Entity (User, TargetProfile, UserAbility)
 * và các DTO phản hồi của Hồ sơ cá nhân (ProfileResponse, UserInfoResponse, UserTargetResponse, UserSkillStatResponse).
 */
@Component
public class ProfileMapper {

    /**
     * Chuyển đổi thực thể User sang UserInfoResponse.
     *
     * @param user Thực thể người dùng
     * @return UserInfoResponse Thông tin người dùng cơ bản
     */
    public UserInfoResponse toUserInfoResponse(User user) {
        if (user == null) {
            return null;
        }
        return UserInfoResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .dob(user.getDob())
                .avatar(user.getAvatar())
                .learnerType(user.getLearnerType())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .createdAt(user.getCreatedAt())
                .build();
    }

    /**
     * Chuyển đổi mục tiêu TargetProfile của User sang UserTargetResponse.
     *
     * @param user Thực thể người dùng chứa TargetProfile
     * @return UserTargetResponse Thông tin mốc mục tiêu TOEIC, hoặc null nếu chưa thiết lập
     */
    public UserTargetResponse toUserTargetResponse(User user) {
        if (user == null || user.getTargetProfile() == null) {
            return null;
        }
        TargetProfile target = user.getTargetProfile();
        LocalDateTime assignedAt = user.getUpdatedAt() != null ? user.getUpdatedAt() : user.getCreatedAt();

        return UserTargetResponse.builder()
                .targetProfileId(target.getId())
                .targetTotalScore(target.getTargetTotalScore())
                .description(target.getDescription())
                .assignedAt(assignedAt)
                .build();
    }

    /**
     * Chuyển đổi một bản ghi UserAbility sang UserSkillStatResponse.
     *
     * @param userAbility Bản ghi đo lường năng lực học viên
     * @return UserSkillStatResponse Thống kê chi tiết của kỹ năng
     */
    public UserSkillStatResponse toSkillStatResponse(UserAbility userAbility) {
        if (userAbility == null) {
            return null;
        }
        return UserSkillStatResponse.builder()
                .abilityId(userAbility.getAbility() != null ? userAbility.getAbility().getId() : null)
                .abilityName(userAbility.getAbility() != null ? userAbility.getAbility().getName() : null)
                .partNo(userAbility.getPart() != null ? userAbility.getPart().getId().intValue() : null)
                .accuracyRate(userAbility.getAccuracyRate())
                .evidenceCount(userAbility.getEvidenceCount())
                .status(userAbility.getStatus())
                .build();
    }

    /**
     * Chuyển đổi danh sách UserAbility sang danh sách UserSkillStatResponse.
     *
     * @param userAbilities Danh sách thực thể UserAbility
     * @return List<UserSkillStatResponse> Danh sách DTO thống kê kỹ năng
     */
    public List<UserSkillStatResponse> toSkillStatResponseList(List<UserAbility> userAbilities) {
        if (userAbilities == null) {
            return Collections.emptyList();
        }
        return userAbilities.stream()
                .map(this::toSkillStatResponse)
                .toList();
    }

    /**
     * Tổng hợp toàn bộ dữ liệu người dùng, mục tiêu và ma trận kỹ năng vào ProfileResponse.
     *
     * @param user Thực thể người dùng
     * @param userAbilities Danh sách kỹ năng tích lũy của người dùng
     * @return ProfileResponse DTO tổng hợp hoàn chỉnh
     */
    public ProfileResponse toProfileResponse(User user, List<UserAbility> userAbilities) {
        return toProfileResponseWithSkills(user, toSkillStatResponseList(userAbilities));
    }

    /**
     * Tổng hợp toàn bộ dữ liệu người dùng, mục tiêu và danh sách kỹ năng đã tổng hợp vào ProfileResponse.
     *
     * @param user   Thực thể người dùng
     * @param skills Danh sách DTO thống kê kỹ năng đã tổng hợp từ chẩn đoán & bài thi mô-đun
     * @return ProfileResponse DTO tổng hợp hoàn chỉnh
     */
    public ProfileResponse toProfileResponseWithSkills(User user, List<UserSkillStatResponse> skills) {
        if (user == null) {
            return null;
        }
        return ProfileResponse.builder()
                .user(toUserInfoResponse(user))
                .target(toUserTargetResponse(user))
                .skills(skills != null ? skills : Collections.emptyList())
                .build();
    }
}

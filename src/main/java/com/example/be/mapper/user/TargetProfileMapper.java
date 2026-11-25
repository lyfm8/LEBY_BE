package com.example.be.mapper.user;

import com.example.be.dto.response.user.SelectTargetResponse;
import com.example.be.dto.response.user.TargetProfileResponse;
import com.example.be.dto.response.user.UserTargetProfileResponse;
import com.example.be.entity.user.TargetProfile;
import com.example.be.entity.user.User;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Mapper chuyển đổi dữ liệu giữa Entity {@link TargetProfile} và các DTO phản hồi cho cả
 * luồng Học viên (UC05) lẫn Quản trị viên.
 */
@Component
public class TargetProfileMapper {

    /**
     * QUY TẮC NGHIỆP VỤ: Mốc mục tiêu 650 được đánh dấu gợi ý mặc định trên màn hình chọn AIM.
     */
    private static final int RECOMMENDED_TARGET_SCORE = 650;

    /**
     * Chuyển đổi TargetProfile sang DTO hiển thị cho học viên (UC05).
     *
     * @param profile Thực thể TargetProfile trong DB
     * @return UserTargetProfileResponse hoặc null nếu profile null
     */
    public UserTargetProfileResponse toUserResponse(TargetProfile profile) {
        if (profile == null) {
            return null;
        }
        boolean recommended = profile.getTargetTotalScore() != null
                && profile.getTargetTotalScore() == RECOMMENDED_TARGET_SCORE;

        return UserTargetProfileResponse.builder()
                .id(profile.getId())
                .targetTotalScore(profile.getTargetTotalScore())
                .description(profile.getDescription())
                .status(profile.getStatus())
                .isRecommended(recommended)
                .build();
    }

    /**
     * Chuyển đổi danh sách TargetProfile sang danh sách DTO học viên.
     *
     * @param profiles Danh sách thực thể TargetProfile
     * @return Danh sách UserTargetProfileResponse
     */
    public List<UserTargetProfileResponse> toUserResponseList(List<TargetProfile> profiles) {
        if (profiles == null || profiles.isEmpty()) {
            return Collections.emptyList();
        }
        return profiles.stream()
                .map(this::toUserResponse)
                .toList();
    }

    /**
     * Đóng gói kết quả sau khi chọn/đổi mục tiêu AIM cho Controller trả về Frontend.
     *
     * @param user           Học viên thực hiện thao tác
     * @param targetProfile  Mục tiêu AIM vừa được cập nhật
     * @param redirectAction Chỉ dẫn điều hướng (GO_DIAGNOSTIC | GO_LEARNING_PATH)
     * @param learningPathId ID lộ trình mới nếu đã tái sinh qua BR02, ngược lại là null
     * @return SelectTargetResponse DTO phản hồi hoàn chỉnh
     */
    public SelectTargetResponse toSelectTargetResponse(
            User user,
            TargetProfile targetProfile,
            String redirectAction,
            Long learningPathId
    ) {
        if (user == null || targetProfile == null) {
            return null;
        }
        return SelectTargetResponse.builder()
                .userId(user.getId())
                .targetProfileId(targetProfile.getId())
                .targetTotalScore(targetProfile.getTargetTotalScore())
                .redirectAction(redirectAction)
                .learningPathId(learningPathId)
                .build();
    }

    /**
     * Chuyển đổi TargetProfile sang DTO quản trị viên kèm thống kê số học viên đang chọn.
     *
     * @param profile    Thực thể TargetProfile
     * @param totalUsers Tổng số học viên đang gắn mục tiêu này
     * @return TargetProfileResponse DTO dành cho cổng Admin
     */
    public TargetProfileResponse toAdminResponse(TargetProfile profile, long totalUsers) {
        if (profile == null) {
            return null;
        }
        return TargetProfileResponse.builder()
                .id(profile.getId())
                .aimScore(profile.getTargetTotalScore())
                .description(profile.getDescription())
                .totalUsers(totalUsers)
                .build();
    }
}

package com.example.be.service.user.implement;

import com.example.be.dto.request.user.ChangePasswordRequest;
import com.example.be.dto.request.user.UpdateProfileRequest;
import com.example.be.dto.response.user.ProfileResponse;
import com.example.be.entity.ability.UserAbility;
import com.example.be.entity.user.User;
import com.example.be.exception.BaseException;
import com.example.be.exception.ErrorCode;
import com.example.be.mapper.user.ProfileMapper;
import com.example.be.repository.ability.UserAbilityRepository;
import com.example.be.repository.user.UserRepository;
import com.example.be.service.user.service.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Hiện thực hóa các nghiệp vụ xem hồ sơ, chỉnh sửa thông tin và đổi mật khẩu cho người dùng.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final UserRepository userRepository;
    private final UserAbilityRepository userAbilityRepository;
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

        // Bước 2: Truy vấn ma trận năng lực tích lũy (kèm JOIN FETCH Ability & Part)
        List<UserAbility> userAbilities = userAbilityRepository.findByUserIdWithDetails(userId);

        // Bước 3: Ánh xạ thực thể sang DTO thông qua ProfileMapper
        return profileMapper.toProfileResponse(user, userAbilities);
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

        // Bước 2: Cập nhật các trường dữ liệu được phép thay đổi
        user.setFullName(request.getFullName().trim());
        user.setDob(request.getDob());

        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar().trim().isEmpty() ? null : request.getAvatar().trim());
        }

        if (request.getLearnerType() != null) {
            user.setLearnerType(request.getLearnerType());
        }

        // Bước 3: Lưu thay đổi vào cơ sở dữ liệu
        userRepository.save(user);
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
}

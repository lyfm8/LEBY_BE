package com.example.be.controller.media;

import com.example.be.common.ApiResponse;
import com.example.be.dto.request.media.SaveMediaRequest;
import com.example.be.dto.response.media.MediaUploadSignatureResponse;
import com.example.be.dto.response.media.SavedMediaResponse;
import com.example.be.entity.user.User;
import com.example.be.security.Roles;
import com.example.be.service.media.service.MediaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller cung cấp các API xử lý upload media và lưu trữ metadata cho hệ thống LEBY.
 * Hỗ trợ cả học viên, giáo viên và quản trị viên tải tệp tin an toàn qua Cloudinary Direct Upload.
 */
@Validated
@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;

    /**
     * Cung cấp chữ ký bảo mật để Frontend có thể upload tệp tin trực tiếp lên Cloudinary.
     *
     * @return ResponseEntity bọc trong ApiResponse chứa chữ ký và timestamp
     */
    @GetMapping("/signature")
    @PreAuthorize(Roles.ANY_AUTHENTICATED)
    public ResponseEntity<ApiResponse<MediaUploadSignatureResponse>> getUploadSignature() {
        MediaUploadSignatureResponse signatureResponse = mediaService.getUploadSignature();
        return ResponseEntity.ok(ApiResponse.<MediaUploadSignatureResponse>builder()
                .success(true)
                .message("Lấy thông tin chữ ký upload Cloudinary thành công")
                .data(signatureResponse)
                .build());
    }

    /**
     * Lưu trữ metadata của các tệp tin media sau khi Frontend đã tải lên Cloudinary thành công.
     *
     * @param currentUser Người dùng đang đăng nhập thực hiện tải lên
     * @param requests Danh sách metadata các file đã upload (tối đa 10 file/request)
     * @return ResponseEntity bọc ApiResponse chứa danh sách media đã tạo
     */
    @PostMapping({"", "/save"})
    @PreAuthorize(Roles.ANY_AUTHENTICATED)
    public ResponseEntity<ApiResponse<List<SavedMediaResponse>>> saveMedias(
            @AuthenticationPrincipal User currentUser,
            @Valid
            @NotEmpty(message = "Danh sách media không được để trống")
            @Size(max = 10, message = "Mỗi lần chỉ được lưu tối đa 10 tệp tin media")
            @RequestBody List<@Valid SaveMediaRequest> requests
    ) {
        List<SavedMediaResponse> savedMedias = mediaService.saveMedias(requests, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<List<SavedMediaResponse>>builder()
                        .success(true)
                        .message("Lưu danh sách media thành công")
                        .data(savedMedias)
                        .build());
    }
}

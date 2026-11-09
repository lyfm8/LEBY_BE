package com.example.be.service.media.service;

import com.example.be.dto.request.media.SaveMediaRequest;
import com.example.be.dto.response.media.MediaUploadSignatureResponse;
import com.example.be.dto.response.media.SavedMediaResponse;
import com.example.be.entity.media.Media;
import com.example.be.entity.user.User;
import com.example.be.enums.media.EMediaResourceType;

import java.util.List;

/**
 * Service giao diện cho nghiệp vụ quản lý tài nguyên media tải lên hệ thống LEBY.
 */
public interface MediaService {

    /**
     * Lấy thông tin chữ ký bảo mật để Frontend có thể tải file trực tiếp lên Cloudinary.
     *
     * @return MediaUploadSignatureResponse Chữ ký xác thực kèm thông số cần thiết
     */
    MediaUploadSignatureResponse getUploadSignature();

    /**
     * Lưu trữ danh sách metadata của các media vừa upload vào cơ sở dữ liệu ở trạng thái PENDING.
     *
     * @param requests Danh sách metadata từ client sau khi upload Cloudinary
     * @param user Người dùng thực hiện upload
     * @return List<SavedMediaResponse> Danh sách kết quả media đã lưu trong database
     */
    List<SavedMediaResponse> saveMedias(List<SaveMediaRequest> requests, User user);

    /**
     * Xác nhận kích hoạt chính thức cho một bản ghi Media (chuyển PENDING sang ACTIVE, xóa tag tmp).
     *
     * @param media Bản ghi Media cần kích hoạt
     */
    void confirmMedia(Media media);

    /**
     * Xác nhận kích hoạt chính thức cho Media dựa theo id.
     *
     * @param mediaId ID của Media
     */
    void confirmMediaById(Long mediaId);

    /**
     * Thu hồi kích hoạt của Media khi bị gỡ khỏi thực thể (chuyển về PENDING kèm hạn TTL 12h để scheduler tự dọn dẹp).
     *
     * @param media Đối tượng Entity Media cần thu hồi
     */
    void releaseMedia(Media media);

    /**
     * Xóa tài nguyên vật lý trên Cloudinary.
     *
     * @param publicId Mã định danh tài nguyên trên Cloudinary
     * @param resourceType Phân loại tài nguyên
     */
    void deleteCloudinaryResource(String publicId, EMediaResourceType resourceType);

    /**
     * Tìm kiếm Media theo ID.
     *
     * @param id ID của bản ghi media
     * @return Media Đối tượng entity Media
     */
    Media findById(Long id);

    /**
     * Tìm kiếm Media theo publicId.
     *
     * @param publicId Mã public_id Cloudinary
     * @return Media Đối tượng entity Media
     */
    Media findByPublicId(String publicId);
}

package com.example.be.service.media.util;

import com.example.be.dto.response.media.MediaUploadSignatureResponse;

/**
 * Interface hợp đồng chung cho các nhà cung cấp dịch vụ lưu trữ media đám mây (Cloud Storage).
 * Giúp tầng Service không bị phụ thuộc trực tiếp vào SDK của bất kỳ vendor cụ thể nào.
 */
public interface MediaStorageAdapter {

    /**
     * Tạo chữ ký tải lên ngắn hạn cho client tải trực tiếp lên kho lưu trữ.
     *
     * @return MediaUploadSignatureResponse Chữ ký xác thực kèm thông số cần thiết
     */
    MediaUploadSignatureResponse createUploadSignature();

    /**
     * Xóa tài nguyên media trên dịch vụ đám mây theo public_id.
     *
     * @param publicId Mã định danh tài nguyên trên đám mây
     * @param resourceType Loại tài nguyên (image, video, raw)
     * @return boolean true nếu xóa thành công hoặc tài nguyên đã không còn tồn tại
     */
    boolean deleteResource(String publicId, String resourceType);

    /**
     * Xóa một tag cụ thể khỏi tài nguyên (ví dụ xóa tag 'tmp' khi media được kích hoạt ACTIVE).
     *
     * @param tag Tên tag cần xóa (ví dụ "tmp")
     * @param publicId Mã định danh tài nguyên
     */
    void removeTag(String tag, String publicId);
}

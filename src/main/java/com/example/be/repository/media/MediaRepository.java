package com.example.be.repository.media;

import com.example.be.entity.media.Media;
import com.example.be.enums.media.EMediaStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Kho lưu trữ dữ liệu cho thực thể Media.
 */
@Repository
public interface MediaRepository extends JpaRepository<Media, Long> {

    /**
     * Tìm danh sách các bản ghi media theo tập hợp các public_id.
     *
     * @param publicIds Tập hợp các mã định danh public_id cần kiểm tra
     * @return Danh sách các đối tượng Media đã tồn tại
     */
    List<Media> findAllByPublicIdIn(Collection<String> publicIds);

    /**
     * Tìm kiếm bản ghi media theo public_id duy nhất.
     *
     * @param publicId Mã định danh tài nguyên trên Cloudinary
     * @return Optional chứa đối tượng Media nếu tìm thấy
     */
    Optional<Media> findByPublicId(String publicId);

    /**
     * Tìm kiếm bản ghi media theo đường dẫn an toàn secure_url.
     *
     * @param secureUrl Đường dẫn truy cập tệp tin
     * @return Optional chứa đối tượng Media nếu tìm thấy
     */
    Optional<Media> findBySecureUrl(String secureUrl);

    /**
     * Lấy tối đa 100 bản ghi media ở trạng thái chỉ định đã quá hạn xử lý (phục vụ cronjob dọn rác).
     *
     * @param status Trạng thái cần quét (thường là PENDING)
     * @param now Thời điểm hiện tại làm mốc so sánh hết hạn
     * @return Danh sách các bản ghi Media hết hạn cần dọn dẹp
     */
    List<Media> findTop100ByStatusAndPendingExpiresAtLessThanEqualOrderByPendingExpiresAtAsc(
            EMediaStatus status,
            LocalDateTime now
    );
}

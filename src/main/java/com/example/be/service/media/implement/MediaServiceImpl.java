package com.example.be.service.media.implement;

import com.example.be.dto.request.media.SaveMediaRequest;
import com.example.be.dto.response.media.MediaUploadSignatureResponse;
import com.example.be.dto.response.media.SavedMediaResponse;
import com.example.be.entity.media.Media;
import com.example.be.entity.user.User;
import com.example.be.enums.media.EMediaResourceType;
import com.example.be.enums.media.EMediaStatus;
import com.example.be.exception.BaseException;
import com.example.be.exception.ErrorCode;
import com.example.be.mapper.media.MediaMapper;
import com.example.be.repository.media.MediaRepository;
import com.example.be.service.media.service.MediaService;
import com.example.be.service.media.util.MediaStorageAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Triển khai nghiệp vụ quản lý tài nguyên media tải lên hệ thống LEBY.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MediaServiceImpl implements MediaService {

    private static final long PENDING_MEDIA_TTL_HOURS = 12L;
    private static final String TEMPORARY_TAG = "tmp";

    private final MediaStorageAdapter mediaStorageAdapter;
    private final MediaRepository mediaRepository;
    private final MediaMapper mediaMapper;

    /**
     * Lấy thông tin chữ ký bảo mật phục vụ upload từ client.
     *
     * @return MediaUploadSignatureResponse Thông tin chữ ký và cấu hình
     */
    @Override
    public MediaUploadSignatureResponse getUploadSignature() {
        return mediaStorageAdapter.createUploadSignature();
    }

    /**
     * Lưu trữ danh sách media ở trạng thái tạm thời PENDING.
     *
     * @param requests Danh sách metadata gửi từ client
     * @param user Người dùng thực hiện thao tác
     * @return List<SavedMediaResponse> Danh sách media đã lưu
     */
    @Override
    @Transactional
    public List<SavedMediaResponse> saveMedias(List<SaveMediaRequest> requests, User user) {
        if (requests == null || requests.isEmpty()) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "Danh sách media tải lên không được rỗng");
        }

        // Bước 1: Kiểm tra tính duy nhất của public_id trong nội bộ danh sách gửi lên
        Set<String> publicIds = new HashSet<>();
        for (SaveMediaRequest request : requests) {
            if (!publicIds.add(request.getPublicId())) {
                throw new BaseException(
                        ErrorCode.MEDIA_DUPLICATE,
                        "Danh sách media gửi lên chứa public_id bị trùng lặp: " + request.getPublicId()
                );
            }
        }

        // Bước 2: Kiểm tra xem các public_id này đã tồn tại trong cơ sở dữ liệu chưa (chống ghi đè)
        List<Media> existingMedias = mediaRepository.findAllByPublicIdIn(publicIds);
        if (!existingMedias.isEmpty()) {
            throw new BaseException(
                    ErrorCode.MEDIA_DUPLICATE,
                    "Một hoặc nhiều media đã tồn tại trong hệ thống LEBY"
            );
        }

        // Bước 3: Ánh xạ dữ liệu và gán trạng thái PENDING kèm thời gian hết hạn TTL 12 giờ
        LocalDateTime expireTime = LocalDateTime.now().plusHours(PENDING_MEDIA_TTL_HOURS);
        List<Media> medias = requests.stream()
                .map(request -> buildPendingMedia(request, user, expireTime))
                .toList();

        // Bước 4: Lưu hàng loạt vào database và trả về danh sách phản hồi cho client
        List<Media> savedMedias = mediaRepository.saveAll(medias);
        log.info("Đã lưu {} bản ghi media mới ở trạng thái PENDING bởi userId={}", savedMedias.size(), user.getId());

        return mediaMapper.toSavedMediaResponses(savedMedias);
    }

    /**
     * Kích hoạt chính thức trạng thái ACTIVE cho Media và gỡ bỏ tag 'tmp' trên Cloudinary.
     *
     * @param media Đối tượng Entity Media cần kích hoạt
     */
    @Override
    @Transactional
    public void confirmMedia(Media media) {
        if (media == null) {
            return;
        }

        // Bước 1: Cập nhật trạng thái trong cơ sở dữ liệu
        media.setStatus(EMediaStatus.ACTIVE);
        media.setPendingExpiresAt(null);
        mediaRepository.save(media);

        // Bước 2: Đăng ký gỡ tag 'tmp' trên Cloudinary sau khi database commit thành công
        removeTemporaryTagAfterCommit(media);
    }

    /**
     * Kích hoạt chính thức Media theo ID.
     *
     * @param mediaId ID của bản ghi Media
     */
    @Override
    @Transactional
    public void confirmMediaById(Long mediaId) {
        if (mediaId == null) {
            return;
        }
        Media media = findById(mediaId);
        confirmMedia(media);
    }

    /**
     * Thu hồi kích hoạt Media khi bị gỡ khỏi thực thể nghiệp vụ (chuyển ACTIVE sang PENDING với TTL 12h).
     *
     * @param media Đối tượng Entity Media cần thu hồi
     */
    @Override
    @Transactional
    public void releaseMedia(Media media) {
        if (media == null) {
            return;
        }
        media.setStatus(EMediaStatus.PENDING);
        media.setPendingExpiresAt(LocalDateTime.now().plusHours(PENDING_MEDIA_TTL_HOURS));
        mediaRepository.save(media);
        log.info("Đã chuyển mediaId={} về trạng thái PENDING để thu hồi", media.getId());
    }

    /**
     * Xóa tài nguyên vật lý trên Cloudinary.
     *
     * @param publicId Mã tài nguyên Cloudinary
     * @param resourceType Phân loại tài nguyên
     */
    @Override
    public void deleteCloudinaryResource(String publicId, EMediaResourceType resourceType) {
        String type = resourceType != null ? resourceType.name().toLowerCase(Locale.ROOT) : "image";
        mediaStorageAdapter.deleteResource(publicId, type);
    }

    /**
     * Tìm kiếm Media theo ID.
     *
     * @param id ID của bản ghi media
     * @return Media Đối tượng entity Media
     */
    @Override
    public Media findById(Long id) {
        return mediaRepository.findById(id)
                .orElseThrow(() -> new BaseException(
                        ErrorCode.MEDIA_NOT_FOUND,
                        "Không tìm thấy bản ghi media với ID: " + id
                ));
    }

    /**
     * Tìm kiếm Media theo publicId.
     *
     * @param publicId Mã public_id Cloudinary
     * @return Media Đối tượng entity Media
     */
    @Override
    public Media findByPublicId(String publicId) {
        return mediaRepository.findByPublicId(publicId)
                .orElseThrow(() -> new BaseException(
                        ErrorCode.MEDIA_NOT_FOUND,
                        "Không tìm thấy bản ghi media với publicId: " + publicId
                ));
    }

    private Media buildPendingMedia(SaveMediaRequest request, User user, LocalDateTime expireTime) {
        Media media = mediaMapper.toMedia(request);

        EMediaResourceType resourceType;
        try {
            resourceType = EMediaResourceType.valueOf(request.getResourceType().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            resourceType = EMediaResourceType.AUTO;
        }

        media.setResourceType(resourceType);
        media.setStatus(EMediaStatus.PENDING);
        media.setPendingExpiresAt(expireTime);
        media.setUploadedBy(user);
        return media;
    }

    /**
     * Gỡ bỏ tag 'tmp' trên Cloudinary CHỈ SAU KHI Transaction Database đã commit thành công.
     * Tránh trường hợp rollback DB nhưng trên Cloudinary đã bị gỡ tag.
     */
    private void removeTemporaryTagAfterCommit(Media media) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            mediaStorageAdapter.removeTag(TEMPORARY_TAG, media.getPublicId());
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    mediaStorageAdapter.removeTag(TEMPORARY_TAG, media.getPublicId());
                    log.info("Đã gỡ tag 'tmp' trên Cloudinary cho media publicId={}", media.getPublicId());
                } catch (Exception ex) {
                    log.warn("Không thể gỡ tag 'tmp' cho media publicId={}: {}", media.getPublicId(), ex.getMessage());
                }
            }
        });
    }
}

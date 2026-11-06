package com.example.be.service.media.util;

import com.example.be.entity.media.Media;
import com.example.be.enums.media.EMediaStatus;
import com.example.be.repository.media.MediaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/**
 * Tác vụ ngầm định kỳ quét và xóa các tệp tin media rác (Orphan Media)
 * ở trạng thái PENDING đã quá thời gian chờ (12 giờ) mà không được liên kết vào dữ liệu chính.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PendingMediaCleanupScheduler {

    private final MediaRepository mediaRepository;
    private final MediaStorageAdapter mediaStorageAdapter;

    /**
     * Chạy định kỳ mỗi 1 giờ (sau 5 phút khởi động ứng dụng).
     * Quét tối đa 100 media hết hạn để giải phóng dung lượng Cloudinary và Database.
     */
    @Scheduled(fixedDelay = 3600000L, initialDelay = 300000L)
    @Transactional
    public void cleanExpiredPendingMedias() {
        LocalDateTime now = LocalDateTime.now();
        List<Media> expiredMedias = mediaRepository
                .findTop100ByStatusAndPendingExpiresAtLessThanEqualOrderByPendingExpiresAtAsc(
                        EMediaStatus.PENDING,
                        now
                );

        if (expiredMedias.isEmpty()) {
            return;
        }

        log.info("Bắt đầu dọn dẹp {} tệp tin media PENDING hết hạn...", expiredMedias.size());
        int deletedCount = 0;

        for (Media media : expiredMedias) {
            String resourceType = media.getResourceType() != null
                    ? media.getResourceType().name().toLowerCase(Locale.ROOT)
                    : "image";

            // Xóa file vật lý trên Cloudinary trước
            boolean deletedOnCloud = mediaStorageAdapter.deleteResource(media.getPublicId(), resourceType);

            if (deletedOnCloud) {
                mediaRepository.delete(media);
                deletedCount++;
            } else {
                log.warn("Bỏ qua việc xóa DB cho mediaId={} vì không xóa được trên Cloudinary", media.getId());
            }
        }

        log.info("Hoàn tất dọn dẹp media: đã xóa thành công {}/{} bản ghi", deletedCount, expiredMedias.size());
    }
}

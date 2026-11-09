package com.example.be.service.question.util;

import com.example.be.entity.media.Media;
import com.example.be.enums.media.EMediaKind;
import com.example.be.enums.media.EMediaResourceType;

import java.util.Locale;
import java.util.Set;

/**
 * Tiện ích xác định loại tệp truyền thông (IMAGE, AUDIO, VIDEO) cho câu hỏi TOEIC.
 * Lưu ý đặc thù: Cloudinary nhận diện audio dưới dạng resource_type là 'video' hoặc 'raw',
 * do đó cần kết hợp kiểm tra định dạng tệp (format) để phân biệt chính xác.
 */
public final class MediaKindResolver {

    private static final Set<String> AUDIO_FORMATS = Set.of("mp3", "wav", "m4a", "ogg", "aac", "flac");
    private static final Set<String> VIDEO_FORMATS = Set.of("mp4", "webm", "mov", "avi", "mkv");
    private static final Set<String> IMAGE_FORMATS = Set.of("jpg", "jpeg", "png", "webp", "gif", "svg");

    private MediaKindResolver() {
        // Utility class
    }

    /**
     * Xác định EMediaKind từ đối tượng Media.
     *
     * @param media Bản ghi Media
     * @return EMediaKind (IMAGE / AUDIO / VIDEO)
     */
    public static EMediaKind resolve(Media media) {
        if (media == null) {
            return EMediaKind.IMAGE;
        }
        return resolve(media.getResourceType(), media.getFormat());
    }

    /**
     * Xác định EMediaKind từ resourceType và format.
     *
     * @param resourceType Loại tài nguyên Cloudinary
     * @param format Định dạng đuôi file
     * @return EMediaKind
     */
    public static EMediaKind resolve(EMediaResourceType resourceType, String format) {
        String fmt = format != null ? format.toLowerCase(Locale.ROOT).trim() : "";

        if (AUDIO_FORMATS.contains(fmt)) {
            return EMediaKind.AUDIO;
        }

        if (VIDEO_FORMATS.contains(fmt)) {
            return EMediaKind.VIDEO;
        }

        if (IMAGE_FORMATS.contains(fmt)) {
            return EMediaKind.IMAGE;
        }

        if (resourceType == EMediaResourceType.IMAGE) {
            return EMediaKind.IMAGE;
        }

        if (resourceType == EMediaResourceType.VIDEO) {
            return EMediaKind.VIDEO;
        }

        return EMediaKind.IMAGE;
    }
}


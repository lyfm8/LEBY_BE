package com.example.be.mapper.media;

import com.example.be.dto.request.media.SaveMediaRequest;
import com.example.be.dto.response.media.SavedMediaResponse;
import com.example.be.entity.media.Media;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Mapper chuyển đổi giữa Entity Media và các đối tượng DTO liên quan.
 */
@Component
public class MediaMapper {

    /**
     * Chuyển đổi từ SaveMediaRequest sang đối tượng Entity Media sơ bộ.
     *
     * @param request Dữ liệu request từ client
     * @return Đối tượng Media Entity
     */
    public Media toMedia(SaveMediaRequest request) {
        if (request == null) {
            return null;
        }

        return Media.builder()
                .publicId(request.getPublicId())
                .secureUrl(request.getSecureUrl())
                .format(request.getFormat())
                .bytes(request.getBytes())
                .width(request.getWidth())
                .height(request.getHeight())
                .originalFilename(request.getOriginalFilename())
                .duration(request.getDuration())
                .build();
    }

    /**
     * Chuyển đổi từ Entity Media sang DTO phản hồi SavedMediaResponse.
     *
     * @param media Đối tượng Entity Media
     * @return DTO SavedMediaResponse
     */
    public SavedMediaResponse toSavedMediaResponse(Media media) {
        if (media == null) {
            return null;
        }

        return SavedMediaResponse.builder()
                .id(media.getId())
                .publicId(media.getPublicId())
                .url(media.getSecureUrl())
                .resourceType(media.getResourceType() != null ? media.getResourceType().name() : null)
                .format(media.getFormat())
                .bytes(media.getBytes())
                .width(media.getWidth())
                .height(media.getHeight())
                .originalFilename(media.getOriginalFilename())
                .duration(media.getDuration())
                .status(media.getStatus() != null ? media.getStatus().name() : null)
                .build();
    }

    /**
     * Chuyển đổi danh sách Entity Media sang danh sách DTO phản hồi.
     *
     * @param medias Danh sách Media Entity
     * @return Danh sách SavedMediaResponse
     */
    public List<SavedMediaResponse> toSavedMediaResponses(List<Media> medias) {
        if (medias == null || medias.isEmpty()) {
            return Collections.emptyList();
        }

        return medias.stream()
                .map(this::toSavedMediaResponse)
                .toList();
    }
}

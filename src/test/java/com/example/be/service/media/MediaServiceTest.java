package com.example.be.service.media;

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
import com.example.be.service.media.implement.MediaServiceImpl;
import com.example.be.service.media.util.MediaStorageAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit Test kiểm tra logic nghiệp vụ cho MediaService.
 */
@ExtendWith(MockitoExtension.class)
class MediaServiceTest {

    @Mock
    private MediaStorageAdapter mediaStorageAdapter;

    @Mock
    private MediaRepository mediaRepository;

    @Spy
    private MediaMapper mediaMapper = new MediaMapper();

    @InjectMocks
    private MediaServiceImpl mediaService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(100L);
        mockUser.setEmail("admin@leby.edu.vn");
    }

    @Test
    @DisplayName("Lấy signature upload Cloudinary thành công")
    void testGetUploadSignature_Success() {
        MediaUploadSignatureResponse expected = MediaUploadSignatureResponse.builder()
                .signature("dummy_signature")
                .timestamp(1700000000L)
                .apiKey("test_api_key")
                .cloudName("test_cloud")
                .expiresIn(3600)
                .build();

        when(mediaStorageAdapter.createUploadSignature()).thenReturn(expected);

        MediaUploadSignatureResponse actual = mediaService.getUploadSignature();

        assertNotNull(actual);
        assertEquals("dummy_signature", actual.getSignature());
        assertEquals("test_cloud", actual.getCloudName());
        verify(mediaStorageAdapter).createUploadSignature();
    }

    @Test
    @DisplayName("Lưu danh sách media hợp lệ với trạng thái PENDING thành công")
    void testSaveMedias_Success() {
        SaveMediaRequest request = SaveMediaRequest.builder()
                .publicId("leby_questions/q1_audio")
                .secureUrl("https://res.cloudinary.com/leby/video/upload/q1.mp3")
                .resourceType("video")
                .format("mp3")
                .bytes(102400L)
                .build();

        when(mediaRepository.findAllByPublicIdIn(any())).thenReturn(Collections.emptyList());
        when(mediaRepository.saveAll(any())).thenAnswer(invocation -> {
            List<Media> list = invocation.getArgument(0);
            list.forEach(m -> m.setId(1L));
            return list;
        });

        List<SavedMediaResponse> responses = mediaService.saveMedias(List.of(request), mockUser);

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals("leby_questions/q1_audio", responses.get(0).getPublicId());
        assertEquals("PENDING", responses.get(0).getStatus());
        verify(mediaRepository).saveAll(any());
    }

    @Test
    @DisplayName("Ném ngoại lệ MEDIA_DUPLICATE khi danh sách gửi lên chứa public_id trùng lặp nội bộ")
    void testSaveMedias_DuplicateInRequest() {
        SaveMediaRequest request1 = SaveMediaRequest.builder()
                .publicId("duplicate_id")
                .secureUrl("https://res.cloudinary.com/leby/image/upload/1.png")
                .resourceType("image")
                .format("png")
                .bytes(5000L)
                .build();

        SaveMediaRequest request2 = SaveMediaRequest.builder()
                .publicId("duplicate_id")
                .secureUrl("https://res.cloudinary.com/leby/image/upload/2.png")
                .resourceType("image")
                .format("png")
                .bytes(6000L)
                .build();

        BaseException exception = assertThrows(BaseException.class, () ->
                mediaService.saveMedias(List.of(request1, request2), mockUser)
        );

        assertEquals(ErrorCode.MEDIA_DUPLICATE, exception.getErrorCode());
    }

    @Test
    @DisplayName("Ném ngoại lệ MEDIA_DUPLICATE khi public_id đã tồn tại trong cơ sở dữ liệu")
    void testSaveMedias_DuplicateInDatabase() {
        SaveMediaRequest request = SaveMediaRequest.builder()
                .publicId("existing_in_db")
                .secureUrl("https://res.cloudinary.com/leby/image/upload/1.png")
                .resourceType("image")
                .format("png")
                .bytes(5000L)
                .build();

        Media existing = Media.builder().publicId("existing_in_db").build();
        when(mediaRepository.findAllByPublicIdIn(Set.of("existing_in_db"))).thenReturn(List.of(existing));

        BaseException exception = assertThrows(BaseException.class, () ->
                mediaService.saveMedias(List.of(request), mockUser)
        );

        assertEquals(ErrorCode.MEDIA_DUPLICATE, exception.getErrorCode());
    }

    @Test
    @DisplayName("Xác nhận kích hoạt media thành công (PENDING -> ACTIVE)")
    void testConfirmMedia_Success() {
        Media media = Media.builder()
                .publicId("q1_img")
                .status(EMediaStatus.PENDING)
                .resourceType(EMediaResourceType.IMAGE)
                .build();

        mediaService.confirmMedia(media);

        assertEquals(EMediaStatus.ACTIVE, media.getStatus());
        assertNull(media.getPendingExpiresAt());
        verify(mediaRepository).save(media);
    }

    @Test
    @DisplayName("Tìm kiếm media theo ID thành công")
    void testFindById_Success() {
        Media media = Media.builder()
                .publicId("test_media")
                .secureUrl("https://res.cloudinary.com/img.png")
                .build();
        media.setId(99L);

        when(mediaRepository.findById(99L)).thenReturn(Optional.of(media));

        Media result = mediaService.findById(99L);
        assertNotNull(result);
        assertEquals(99L, result.getId());
    }

    @Test
    @DisplayName("Ném ngoại lệ MEDIA_NOT_FOUND khi không tìm thấy media theo ID")
    void testFindById_NotFound() {
        when(mediaRepository.findById(999L)).thenReturn(Optional.empty());

        BaseException exception = assertThrows(BaseException.class, () ->
                mediaService.findById(999L)
        );

        assertEquals(ErrorCode.MEDIA_NOT_FOUND, exception.getErrorCode());
    }
}

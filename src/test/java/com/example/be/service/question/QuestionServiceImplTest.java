package com.example.be.service.question;

import com.example.be.dto.request.question.CreateQuestionGroupRequest;
import com.example.be.dto.request.question.CreateStandaloneQuestionRequest;
import com.example.be.dto.request.question.QuestionGroupItemRequest;
import com.example.be.dto.request.question.QuestionMediaItemRequest;
import com.example.be.dto.request.question.QuestionOptionRequest;
import com.example.be.dto.response.question.QuestionDetailResponse;
import com.example.be.entity.ability.Ability;
import com.example.be.entity.content.Part;
import com.example.be.entity.media.Media;
import com.example.be.entity.question.Question;
import com.example.be.enums.ability.ESection;
import com.example.be.enums.media.EMediaResourceType;
import com.example.be.enums.media.EMediaStatus;
import com.example.be.exception.BaseException;
import com.example.be.exception.ErrorCode;
import com.example.be.mapper.question.QuestionMapper;
import com.example.be.repository.ability.AbilityRepository;
import com.example.be.repository.content.PartRepository;
import com.example.be.repository.media.MediaRepository;
import com.example.be.repository.question.DiagnosticTestQuestionRepository;
import com.example.be.repository.question.ModuleTestQuestionRepository;
import com.example.be.repository.question.QuestionRepository;
import com.example.be.service.media.service.MediaService;
import com.example.be.service.question.implement.QuestionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit Test kiểm thử logic nghiệp vụ của QuestionServiceImpl (UC A04).
 */
@ExtendWith(MockitoExtension.class)
class QuestionServiceImplTest {

    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private PartRepository partRepository;
    @Mock
    private AbilityRepository abilityRepository;
    @Mock
    private MediaRepository mediaRepository;
    @Mock
    private DiagnosticTestQuestionRepository diagnosticTestQuestionRepository;
    @Mock
    private ModuleTestQuestionRepository moduleTestQuestionRepository;
    @Mock
    private MediaService mediaService;

    @Spy
    private QuestionMapper questionMapper = new QuestionMapper(new ObjectMapper());

    @InjectMocks
    private QuestionServiceImpl questionService;

    private Part listeningPart;
    private Part readingPart;
    private Ability listeningAbility;
    private Media audioMedia;

    @BeforeEach
    void setUp() {
        listeningPart = new Part();
        listeningPart.setId(1L);
        listeningPart.setName("Part 1: Photographs");
        listeningPart.setSections(ESection.LISTENING);

        readingPart = new Part();
        readingPart.setId(5L);
        readingPart.setName("Part 5: Incomplete Sentences");
        readingPart.setSections(ESection.READING);

        listeningAbility = new Ability();
        listeningAbility.setId(10L);
        listeningAbility.setName("Nhận diện hành động người");
        listeningAbility.setPart(listeningPart);

        audioMedia = Media.builder()
                .publicId("audio_01")
                .secureUrl("https://res.cloudinary.com/demo/audio.mp3")
                .format("mp3")
                .resourceType(EMediaResourceType.VIDEO)
                .bytes(1024L)
                .status(EMediaStatus.PENDING)
                .build();
        audioMedia.setId(100L);
    }

    @Test
    @DisplayName("Tạo câu hỏi đơn thành công khi dữ liệu hợp lệ")
    void createStandaloneQuestion_Success() {
        CreateStandaloneQuestionRequest request = CreateStandaloneQuestionRequest.builder()
                .name("Câu hỏi tranh Part 1")
                .partId(1L)
                .abilityIds(List.of(10L))
                .difficulty(2)
                .text("Look at the picture.")
                .options(List.of(
                        new QuestionOptionRequest("A", "Option A"),
                        new QuestionOptionRequest("B", "Option B"),
                        new QuestionOptionRequest("C", "Option C"),
                        new QuestionOptionRequest("D", "Option D")
                ))
                .correctKey("B")
                .medias(List.of(new QuestionMediaItemRequest(100L, 1)))
                .build();

        when(partRepository.findById(1L)).thenReturn(Optional.of(listeningPart));
        when(abilityRepository.findAllById(List.of(10L))).thenReturn(List.of(listeningAbility));
        when(mediaRepository.findAllById(List.of(100L))).thenReturn(List.of(audioMedia));
        when(questionRepository.save(any(Question.class))).thenAnswer(inv -> {
            Question q = inv.getArgument(0);
            q.setId(1L);
            return q;
        });

        QuestionDetailResponse response = questionService.createStandaloneQuestion(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getCorrectKey()).isEqualTo("B");
        verify(mediaService).confirmMedia(audioMedia);
        verify(questionRepository).save(any(Question.class));
    }

    @Test
    @DisplayName("Ném ngoại lệ QUESTION_INVALID khi correctKey không nằm trong danh sách options")
    void createStandaloneQuestion_Throws_WhenInvalidCorrectKey() {
        CreateStandaloneQuestionRequest request = CreateStandaloneQuestionRequest.builder()
                .name("Câu hỏi lỗi")
                .partId(1L)
                .abilityIds(List.of(10L))
                .difficulty(1)
                .options(List.of(
                        new QuestionOptionRequest("A", "Option A"),
                        new QuestionOptionRequest("B", "Option B"),
                        new QuestionOptionRequest("C", "Option C")
                ))
                .correctKey("D")
                .build();

        assertThatThrownBy(() -> questionService.createStandaloneQuestion(request))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.QUESTION_INVALID);
    }

    @Test
    @DisplayName("Ném ngoại lệ QUESTION_INVALID khi kỹ năng Ability không thuộc về Part đã chọn")
    void createStandaloneQuestion_Throws_WhenAbilityDoesNotBelongToPart() {
        Ability readingAbility = new Ability();
        readingAbility.setId(20L);
        readingAbility.setName("Ngữ pháp thì");
        readingAbility.setPart(readingPart);

        CreateStandaloneQuestionRequest request = CreateStandaloneQuestionRequest.builder()
                .name("Câu hỏi lệch Part")
                .partId(1L)
                .abilityIds(List.of(20L))
                .difficulty(2)
                .options(List.of(
                        new QuestionOptionRequest("A", "A"),
                        new QuestionOptionRequest("B", "B"),
                        new QuestionOptionRequest("C", "C")
                ))
                .correctKey("A")
                .build();

        when(partRepository.findById(1L)).thenReturn(Optional.of(listeningPart));
        when(abilityRepository.findAllById(List.of(20L))).thenReturn(List.of(readingAbility));

        assertThatThrownBy(() -> questionService.createStandaloneQuestion(request))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.QUESTION_INVALID);
    }

    @Test
    @DisplayName("Chặn xóa và ném QUESTION_IN_USE khi câu hỏi đang nằm trong đề thi chẩn đoán (Luồng 2b)")
    void deleteQuestion_Throws_WhenQuestionInUse() {
        Question question = new Question();
        question.setId(50L);

        when(questionRepository.findById(50L)).thenReturn(Optional.of(question));
        when(questionRepository.findSubQuestionIdsByParentId(50L)).thenReturn(Collections.emptyList());
        when(diagnosticTestQuestionRepository.existsByQuestionIdIn(List.of(50L))).thenReturn(true);

        assertThatThrownBy(() -> questionService.deleteQuestion(50L))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.QUESTION_IN_USE);
    }

    @Test
    @DisplayName("Xóa câu hỏi thành công và thu hồi media khi câu hỏi không nằm trong đề thi")
    void deleteQuestion_Success() {
        Question question = new Question();
        question.setId(60L);

        when(questionRepository.findById(60L)).thenReturn(Optional.of(question));
        when(questionRepository.findSubQuestionIdsByParentId(60L)).thenReturn(Collections.emptyList());
        when(diagnosticTestQuestionRepository.existsByQuestionIdIn(List.of(60L))).thenReturn(false);
        when(moduleTestQuestionRepository.existsByQuestionIdIn(List.of(60L))).thenReturn(false);

        questionService.deleteQuestion(60L);

        verify(questionRepository).delete(question);
    }
}


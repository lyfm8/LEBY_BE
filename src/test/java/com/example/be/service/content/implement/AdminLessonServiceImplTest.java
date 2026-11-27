package com.example.be.service.content.implement;

import com.example.be.dto.request.content.CreateLessonRequest;
import com.example.be.dto.request.content.PracticeQuestionItemDto;
import com.example.be.dto.request.content.SavePracticeQuestionsRequest;
import com.example.be.dto.response.content.LessonDetailResponse;
import com.example.be.dto.response.content.PracticeQuestionResponse;
import com.example.be.entity.ability.Ability;
import com.example.be.entity.content.Lesson;
import com.example.be.entity.content.Module;
import com.example.be.entity.content.ModuleAbility;
import com.example.be.entity.content.Part;
import com.example.be.entity.question.PracticeQuestion;
import com.example.be.entity.question.Question;
import com.example.be.enums.ability.ESection;
import com.example.be.enums.content.ELessonType;
import com.example.be.enums.content.EModuleStatus;
import com.example.be.enums.question.EQuestionType;
import com.example.be.exception.BaseException;
import com.example.be.exception.ErrorCode;
import com.example.be.mapper.content.LessonMapper;
import com.example.be.repository.ability.AbilityRepository;
import com.example.be.repository.content.LessonRepository;
import com.example.be.repository.content.LessonScriptRepository;
import com.example.be.repository.content.ModuleRepository;
import com.example.be.repository.media.MediaRepository;
import com.example.be.repository.question.PracticeQuestionRepository;
import com.example.be.repository.question.QuestionRepository;
import com.example.be.service.media.service.MediaService;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit Test kiểm chứng nghiệp vụ quản lý câu hỏi luyện tập (Tab 3 - UC A05) trong AdminLessonServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
class AdminLessonServiceImplTest {

    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private ModuleRepository moduleRepository;
    @Mock
    private AbilityRepository abilityRepository;
    @Mock
    private LessonScriptRepository lessonScriptRepository;
    @Mock
    private PracticeQuestionRepository practiceQuestionRepository;
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private MediaRepository mediaRepository;
    @Mock
    private MediaService mediaService;

    private AdminLessonServiceImpl adminLessonService;

    @BeforeEach
    void setUp() {
        LessonMapper lessonMapper = new LessonMapper();
        ObjectMapper objectMapper = new ObjectMapper();
        adminLessonService = new AdminLessonServiceImpl(
                lessonRepository,
                moduleRepository,
                abilityRepository,
                lessonScriptRepository,
                practiceQuestionRepository,
                questionRepository,
                lessonMapper,
                mediaRepository,
                mediaService,
                objectMapper
        );
    }

    @Test
    @DisplayName("getPracticeQuestions trả về danh sách câu hỏi kèm tóm tắt nội dung và thông tin Part")
    void getPracticeQuestions_ShouldReturnMappedList() {
        Lesson lesson = createSampleLesson(3L, ESection.READING);
        Question q1 = createSampleQuestion(101L, "Part 5 Q1", "{\"text\":\"Choose the best answer.\"}", null);

        PracticeQuestion pq = new PracticeQuestion();
        pq.setId(1L);
        pq.setLesson(lesson);
        pq.setQuestion(q1);
        pq.setOrderNo(1);
        pq.setPoints(10);

        when(lessonRepository.findById(3L)).thenReturn(Optional.of(lesson));
        when(practiceQuestionRepository.findAllWithQuestionAndPartByLessonId(3L)).thenReturn(List.of(pq));
        when(questionRepository.findAllWithAbilitiesByIdIn(List.of(101L))).thenReturn(List.of(q1));

        List<PracticeQuestionResponse> result = adminLessonService.getPracticeQuestions(3L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getQuestionId()).isEqualTo(101L);
        assertThat(result.get(0).getQuestionSummary()).isEqualTo("Choose the best answer.");
        assertThat(result.get(0).getSection()).isEqualTo(ESection.READING);
    }

    @Test
    @DisplayName("savePracticeQuestions tự động bóc tách chùm câu hỏi cha thành các câu con hợp lệ")
    @SuppressWarnings("unchecked")
    void savePracticeQuestions_WhenGroupParentProvided_ShouldExpandToSubQuestions() {
        Lesson lesson = createSampleLesson(10L, ESection.READING);
        Question parentGroup = createSampleQuestion(200L, "Part 6 Passage 1", "{\"context\":\"Passage\"}", null);
        Question child1 = createSampleQuestion(201L, "Part 6 Q1", "{\"text\":\"Blank 1\"}", parentGroup);
        Question child2 = createSampleQuestion(202L, "Part 6 Q2", "{\"text\":\"Blank 2\"}", parentGroup);

        SavePracticeQuestionsRequest request = SavePracticeQuestionsRequest.builder()
                .items(List.of(
                        PracticeQuestionItemDto.builder().questionId(200L).orderNo(1).points(15).build()
                ))
                .build();

        when(lessonRepository.findById(10L)).thenReturn(Optional.of(lesson));
        when(practiceQuestionRepository.findByLessonIdOrderByOrderNoAsc(10L)).thenReturn(Collections.emptyList());
        when(questionRepository.findAllById(List.of(200L))).thenReturn(List.of(parentGroup));
        when(questionRepository.findByParentIdInOrderByOrderNoAsc(List.of(200L))).thenReturn(List.of(child1, child2));
        when(lessonScriptRepository.findByLessonIdOrderByOrderNoAsc(10L)).thenReturn(Collections.emptyList());
        when(practiceQuestionRepository.findAllWithQuestionAndPartByLessonId(10L)).thenReturn(Collections.emptyList());

        LessonDetailResponse detail = adminLessonService.savePracticeQuestions(10L, request);

        ArgumentCaptor<List<PracticeQuestion>> captor = ArgumentCaptor.forClass(List.class);
        verify(practiceQuestionRepository).saveAll(captor.capture());

        List<PracticeQuestion> savedItems = captor.getValue();
        assertThat(savedItems).hasSize(2);
        assertThat(savedItems.get(0).getQuestion().getId()).isEqualTo(201L);
        assertThat(savedItems.get(0).getOrderNo()).isEqualTo(1);
        assertThat(savedItems.get(0).getPoints()).isEqualTo(15);
        assertThat(savedItems.get(1).getQuestion().getId()).isEqualTo(202L);
        assertThat(savedItems.get(1).getOrderNo()).isEqualTo(2);
        assertThat(detail.getSection()).isEqualTo(ESection.READING);
    }

    @Test
    @DisplayName("savePracticeQuestions ném ngoại lệ INVALID_INPUT nếu request gửi trùng questionId")
    void savePracticeQuestions_WhenDuplicateQuestionId_ShouldThrowInvalidInput() {
        Lesson lesson = createSampleLesson(5L, ESection.READING);
        SavePracticeQuestionsRequest request = SavePracticeQuestionsRequest.builder()
                .items(List.of(
                        PracticeQuestionItemDto.builder().questionId(11L).orderNo(1).points(10).build(),
                        PracticeQuestionItemDto.builder().questionId(11L).orderNo(2).points(10).build()
                ))
                .build();

        when(lessonRepository.findById(5L)).thenReturn(Optional.of(lesson));
        when(practiceQuestionRepository.findByLessonIdOrderByOrderNoAsc(5L)).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> adminLessonService.savePracticeQuestions(5L, request))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BAD_REQUEST);
    }

    @Test
    @DisplayName("savePracticeQuestions ném ngoại lệ RESOURCE_NOT_FOUND nếu questionId không tồn tại")
    void savePracticeQuestions_WhenQuestionMissing_ShouldThrowNotFound() {
        Lesson lesson = createSampleLesson(5L, ESection.LISTENING);
        SavePracticeQuestionsRequest request = SavePracticeQuestionsRequest.builder()
                .items(List.of(
                        PracticeQuestionItemDto.builder().questionId(999L).orderNo(1).points(10).build()
                ))
                .build();

        when(lessonRepository.findById(5L)).thenReturn(Optional.of(lesson));
        when(practiceQuestionRepository.findByLessonIdOrderByOrderNoAsc(5L)).thenReturn(Collections.emptyList());
        when(questionRepository.findAllById(anyList())).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> adminLessonService.savePracticeQuestions(5L, request))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    @DisplayName("savePracticeQuestions xóa hết bản ghi cũ khi danh sách items rỗng")
    void savePracticeQuestions_WhenEmptyItems_ShouldDeleteAllExisting() {
        Lesson lesson = createSampleLesson(7L, ESection.READING);
        Question q1 = createSampleQuestion(1L, "Q1", "{}", null);
        PracticeQuestion oldItem = new PracticeQuestion();
        oldItem.setId(50L);
        oldItem.setLesson(lesson);
        oldItem.setQuestion(q1);
        oldItem.setOrderNo(1);
        oldItem.setPoints(10);

        SavePracticeQuestionsRequest request = SavePracticeQuestionsRequest.builder()
                .items(Collections.emptyList())
                .build();

        when(lessonRepository.findById(7L)).thenReturn(Optional.of(lesson));
        when(practiceQuestionRepository.findByLessonIdOrderByOrderNoAsc(7L)).thenReturn(List.of(oldItem));
        when(lessonScriptRepository.findByLessonIdOrderByOrderNoAsc(7L)).thenReturn(Collections.emptyList());
        when(practiceQuestionRepository.findAllWithQuestionAndPartByLessonId(7L)).thenReturn(Collections.emptyList());

        adminLessonService.savePracticeQuestions(7L, request);

        verify(practiceQuestionRepository).deleteAll(List.of(oldItem));
        verify(practiceQuestionRepository).flush();
    }

    @Test
    @DisplayName("createLesson ném ngoại lệ BAD_REQUEST nếu Ability không nằm trong danh sách Ability của Module")
    void createLesson_WhenAbilityNotInModule_ShouldThrowBadRequest() {
        Lesson sample = createSampleLesson(1L, ESection.READING);
        Module module = sample.getModule();

        Ability moduleAbility = new Ability();
        moduleAbility.setId(10L);
        moduleAbility.setName("Grammar Foundations");
        moduleAbility.setPart(module.getPart());

        ModuleAbility link = new ModuleAbility();
        link.setModule(module);
        link.setAbility(moduleAbility);
        module.setModuleAbilities(List.of(link));

        Ability otherAbility = new Ability();
        otherAbility.setId(99L);
        otherAbility.setName("Unrelated Ability");
        otherAbility.setPart(module.getPart());

        CreateLessonRequest request = new CreateLessonRequest();
        request.setTitle("Bài học sai Ability");
        request.setAbilityId(99L);

        when(moduleRepository.findById(1L)).thenReturn(Optional.of(module));
        when(lessonRepository.findMaxOrderNoByModuleId(1L)).thenReturn(Optional.of(0));
        when(abilityRepository.findById(99L)).thenReturn(Optional.of(otherAbility));

        assertThatThrownBy(() -> adminLessonService.createLesson(1L, request))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BAD_REQUEST);
    }

    private Lesson createSampleLesson(Long id, ESection section) {
        Part part = new Part();
        part.setId(section == ESection.READING ? 5L : 1L);
        part.setName(section == ESection.READING ? "Part 5: Incomplete Sentences" : "Part 1: Photographs");
        part.setSections(section);

        Module module = new Module();
        module.setId(1L);
        module.setTitle("Module Test");
        module.setPart(part);

        Lesson lesson = new Lesson();
        lesson.setId(id);
        lesson.setTitle("Lesson Practice");
        lesson.setOrderNo(3);
        lesson.setStatus(EModuleStatus.PUBLISHED);
        lesson.setLessonType(ELessonType.PRACTICE);
        lesson.setModule(module);
        return lesson;
    }

    private Question createSampleQuestion(Long id, String name, String questionData, Question parent) {
        Part part = new Part();
        part.setId(5L);
        part.setName("Part 5: Incomplete Sentences");
        part.setSections(ESection.READING);

        Question q = new Question();
        q.setId(id);
        q.setName(name);
        q.setType(EQuestionType.SINGLE_CHOICE);
        q.setQuestionData(questionData);
        q.setDifficulty(2);
        q.setSections(ESection.READING);
        q.setPart(part);
        q.setParent(parent);
        return q;
    }
}

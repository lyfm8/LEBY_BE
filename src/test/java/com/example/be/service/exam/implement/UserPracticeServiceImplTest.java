package com.example.be.service.exam.implement;

import com.example.be.dto.response.exam.PracticeRoomResponse;
import com.example.be.entity.ability.Ability;
import com.example.be.entity.content.LearningPath;
import com.example.be.entity.content.LearningPathItem;
import com.example.be.entity.content.Lesson;
import com.example.be.entity.content.Module;
import com.example.be.entity.content.Part;
import com.example.be.entity.question.PracticeQuestion;
import com.example.be.entity.question.Question;
import com.example.be.enums.ability.ESection;
import com.example.be.enums.content.ELessonStatus;
import com.example.be.enums.content.ELessonType;
import com.example.be.enums.content.EModuleStatus;
import com.example.be.exception.BaseException;
import com.example.be.exception.ErrorCode;
import com.example.be.mapper.exam.PracticeRoomMapper;
import com.example.be.mapper.question.ExamQuestionMapper;
import com.example.be.repository.content.LessonRepository;
import com.example.be.repository.question.PracticeQuestionRepository;
import com.example.be.service.exam.util.QuestionBatchLoader;
import com.example.be.service.exam.util.UserExamAccessHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit test cho {@link UserPracticeServiceImpl} (UC12 - Phòng luyện tập tức thì).
 */
@ExtendWith(MockitoExtension.class)
class UserPracticeServiceImplTest {

    @Mock private LessonRepository lessonRepository;
    @Mock private PracticeQuestionRepository practiceQuestionRepository;
    @Mock private QuestionBatchLoader questionBatchLoader;
    @Mock private UserExamAccessHelper userExamAccessHelper;

    private UserPracticeServiceImpl service;

    @BeforeEach
    void setUp() {
        ExamQuestionMapper examQuestionMapper = new ExamQuestionMapper(new ObjectMapper());
        PracticeRoomMapper practiceRoomMapper = new PracticeRoomMapper(examQuestionMapper);
        service = new UserPracticeServiceImpl(
                lessonRepository,
                practiceQuestionRepository,
                questionBatchLoader,
                userExamAccessHelper,
                practiceRoomMapper
        );
    }

    @Test
    void getPracticeRoom_returnsMappedQuestionsWithAnswerAndExplanation() {
        Lesson lesson = buildPracticeLesson(3L, 1L, ELessonType.PRACTICE, EModuleStatus.PUBLISHED);
        LearningPathItem pathItem = new LearningPathItem();
        pathItem.setLearningPath(new LearningPath());
        pathItem.setStatus(ELessonStatus.IN_PROGRESS);

        Question q1 = new Question();
        q1.setId(10L);
        q1.setPart(lesson.getModule().getPart());
        q1.setSections(ESection.LISTENING);
        q1.setQuestionData("{\"text\":\"Look at the photo\",\"audioUrl\":\"https://cdn/p1.mp3\",\"imageUrl\":\"https://cdn/p1.jpg\",\"options\":[{\"key\":\"A\",\"text\":\"Opt A\"},{\"key\":\"B\",\"text\":\"Opt B\"}]}");
        q1.setCorrectAnswer("{\"key\":\"B\"}");
        q1.setExplanation("{\"clue\":\"Clue text\",\"analysis\":\"Analysis text\"}");

        PracticeQuestion pq = new PracticeQuestion();
        pq.setId(100L);
        pq.setOrderNo(1);
        pq.setPoints(10);
        pq.setLesson(lesson);
        pq.setQuestion(q1);

        when(lessonRepository.findByIdWithModulePartAndAbility(3L)).thenReturn(Optional.of(lesson));
        when(userExamAccessHelper.requireUnlockedPathItem(7L, 1L)).thenReturn(pathItem);
        when(practiceQuestionRepository.findAllWithQuestionAndPartByLessonId(3L)).thenReturn(List.of(pq));
        when(questionBatchLoader.loadDetailedQuestionsMap(List.of(10L))).thenReturn(Map.of(10L, q1));

        PracticeRoomResponse room = service.getPracticeRoom(7L, 3L);

        assertThat(room.getLessonId()).isEqualTo(3L);
        assertThat(room.getModuleId()).isEqualTo(1L);
        assertThat(room.getIsEmpty()).isFalse();
        assertThat(room.getTotalQuestions()).isEqualTo(1);
        assertThat(room.getQuestions().get(0).getCorrectAnswer()).isEqualTo("B");
        assertThat(room.getQuestions().get(0).getAudioUrl()).isEqualTo("https://cdn/p1.mp3");
        assertThat(room.getQuestions().get(0).getExplanation().getClue()).isEqualTo("Clue text");
    }

    @Test
    void getPracticeRoom_emptyQuestions_returnsEmptyRoomFlag() {
        Lesson lesson = buildPracticeLesson(3L, 1L, ELessonType.PRACTICE, EModuleStatus.PUBLISHED);
        when(lessonRepository.findByIdWithModulePartAndAbility(3L)).thenReturn(Optional.of(lesson));
        when(userExamAccessHelper.requireUnlockedPathItem(7L, 1L)).thenReturn(new LearningPathItem());
        when(practiceQuestionRepository.findAllWithQuestionAndPartByLessonId(3L)).thenReturn(Collections.emptyList());

        PracticeRoomResponse room = service.getPracticeRoom(7L, 3L);

        assertThat(room.getIsEmpty()).isTrue();
        assertThat(room.getQuestions()).isEmpty();
    }

    @Test
    void getPracticeRoom_videoLesson_throwsInvalidLessonType() {
        Lesson videoLesson = buildPracticeLesson(1L, 1L, ELessonType.VIDEO, EModuleStatus.PUBLISHED);
        when(lessonRepository.findByIdWithModulePartAndAbility(1L)).thenReturn(Optional.of(videoLesson));

        assertThatThrownBy(() -> service.getPracticeRoom(7L, 1L))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode()).isEqualTo(ErrorCode.INVALID_LESSON_TYPE));
    }

    @Test
    void getPracticeRoom_lockedModule_propagatesContentLocked() {
        Lesson lesson = buildPracticeLesson(3L, 2L, ELessonType.PRACTICE, EModuleStatus.PUBLISHED);
        when(lessonRepository.findByIdWithModulePartAndAbility(3L)).thenReturn(Optional.of(lesson));
        when(userExamAccessHelper.requireUnlockedPathItem(7L, 2L))
                .thenThrow(new BaseException(ErrorCode.CONTENT_LOCKED));

        assertThatThrownBy(() -> service.getPracticeRoom(7L, 3L))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode()).isEqualTo(ErrorCode.CONTENT_LOCKED));
    }

    private Lesson buildPracticeLesson(Long lessonId, Long moduleId, ELessonType type, EModuleStatus status) {
        Part part = new Part();
        part.setId(1L);
        part.setName("Part 1: Photographs");
        part.setSections(ESection.LISTENING);

        Module module = new Module();
        module.setId(moduleId);
        module.setTitle("Module Part 1");
        module.setPart(part);

        Ability ability = new Ability();
        ability.setId(1L);
        ability.setName("Nghe tranh");

        Lesson lesson = new Lesson();
        lesson.setId(lessonId);
        lesson.setTitle("Bài luyện tập 1");
        lesson.setLessonType(type);
        lesson.setStatus(status);
        lesson.setModule(module);
        lesson.setAbility(ability);
        return lesson;
    }
}

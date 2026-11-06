package com.example.be;

import com.example.be.dto.request.content.CreateLessonRequest;
import com.example.be.dto.request.content.CreateModuleRequest;
import com.example.be.dto.request.content.LessonScriptDto;
import com.example.be.dto.request.content.SaveLessonScriptsRequest;
import com.example.be.dto.request.content.UpdateModuleThresholdsRequest;
import com.example.be.dto.response.content.LessonDetailResponse;
import com.example.be.dto.response.content.ModuleDetailResponse;
import com.example.be.entity.content.Lesson;
import com.example.be.entity.content.Module;
import com.example.be.enums.content.EModuleStatus;
import com.example.be.exception.BaseException;
import com.example.be.exception.ErrorCode;
import com.example.be.mapper.content.LessonMapper;
import com.example.be.mapper.content.ModuleMapper;
import com.example.be.repository.ability.AbilityRepository;
import com.example.be.repository.content.LearningPathItemRepository;
import com.example.be.repository.content.LessonRepository;
import com.example.be.repository.content.LessonScriptRepository;
import com.example.be.repository.content.ModuleAbilityRepository;
import com.example.be.repository.content.ModuleRepository;
import com.example.be.repository.content.PartRepository;
import com.example.be.service.content.implement.AdminLessonServiceImpl;
import com.example.be.service.content.implement.AdminModuleServiceImpl;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit Test kiểm tra các logic nghiệp vụ trọng tâm của AdminModuleService và AdminLessonService.
 */
@ExtendWith(MockitoExtension.class)
class AdminModuleLessonServiceTest {

    @Mock
    private ModuleRepository moduleRepository;
    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private PartRepository partRepository;
    @Mock
    private AbilityRepository abilityRepository;
    @Mock
    private ModuleAbilityRepository moduleAbilityRepository;
    @Mock
    private LearningPathItemRepository learningPathItemRepository;
    @Mock
    private LessonScriptRepository lessonScriptRepository;

    @Spy
    private LessonMapper lessonMapper = new LessonMapper();

    @Spy
    private ModuleMapper moduleMapper = new ModuleMapper(lessonMapper);

    @InjectMocks
    private AdminModuleServiceImpl adminModuleService;

    @InjectMocks
    private AdminLessonServiceImpl adminLessonService;

    private Module mockModule;
    private Lesson mockLesson;

    @BeforeEach
    void setUp() {
        mockModule = new Module();
        mockModule.setId(1L);
        mockModule.setTitle("TOEIC Part 1 Overview");
        mockModule.setType("THEORY");
        mockModule.setSequence(1);
        mockModule.setStatus(EModuleStatus.DRAFT);
        mockModule.setDefaultPassScore(65);
        mockModule.setRequiredCompletionRate(80);

        mockLesson = new Lesson();
        mockLesson.setId(10L);
        mockLesson.setTitle("At the Restaurant");
        mockLesson.setOrderNo(1);
        mockLesson.setStatus(EModuleStatus.DRAFT);
        mockLesson.setModule(mockModule);
    }

    @Test
    @DisplayName("Tạo Module mới thành công với các tham số mặc định")
    void testCreateModule_Success() {
        CreateModuleRequest request = CreateModuleRequest.builder()
                .title("TOEIC Part 1 Overview")
                .type("THEORY")
                .sequence(1)
                .status(EModuleStatus.DRAFT)
                .defaultPassScore(65)
                .requiredCompletionRate(80)
                .build();

        when(moduleRepository.save(any(Module.class))).thenReturn(mockModule);
        when(moduleRepository.findById(1L)).thenReturn(Optional.of(mockModule));
        when(lessonRepository.countByModuleId(1L)).thenReturn(0L);

        ModuleDetailResponse response = adminModuleService.createModule(request);

        assertNotNull(response);
        assertEquals("TOEIC Part 1 Overview", response.getTitle());
        assertEquals(65, response.getDefaultPassScore());
        assertEquals(80, response.getRequiredCompletionRate());
        verify(moduleRepository).save(any(Module.class));
    }

    @Test
    @DisplayName("Cập nhật nhanh ngưỡng điểm đỗ và tỷ lệ bài học của Module")
    void testUpdateModuleThresholds_Success() {
        UpdateModuleThresholdsRequest request = UpdateModuleThresholdsRequest.builder()
                .defaultPassScore(75)
                .requiredCompletionRate(85)
                .build();

        when(moduleRepository.findById(1L)).thenReturn(Optional.of(mockModule));
        when(moduleRepository.save(any(Module.class))).thenReturn(mockModule);
        when(lessonRepository.countByModuleId(1L)).thenReturn(0L);

        ModuleDetailResponse response = adminModuleService.updateModuleThresholds(1L, request);

        assertNotNull(response);
        assertEquals(75, mockModule.getDefaultPassScore());
        assertEquals(85, mockModule.getRequiredCompletionRate());
        verify(moduleRepository).save(mockModule);
    }

    @Test
    @DisplayName("Chặn xóa Module khi đang có học viên theo học trên lộ trình (Luồng 2c A05)")
    void testDeleteModule_BlockedWhenActiveLearnersExist() {
        when(moduleRepository.findById(1L)).thenReturn(Optional.of(mockModule));
        when(learningPathItemRepository.existsByModuleId(1L)).thenReturn(true);

        BaseException ex = assertThrows(BaseException.class, () -> adminModuleService.deleteModule(1L));

        assertEquals(ErrorCode.BAD_REQUEST, ex.getErrorCode());
        verify(moduleRepository, never()).delete(any(Module.class));
    }

    @Test
    @DisplayName("Tạo bài học mới trong Module thành công")
    void testCreateLesson_Success() {
        CreateLessonRequest request = CreateLessonRequest.builder()
                .title("At the Restaurant")
                .orderNo(1)
                .status(EModuleStatus.DRAFT)
                .isFree(true)
                .build();

        when(moduleRepository.findById(1L)).thenReturn(Optional.of(mockModule));
        when(lessonRepository.save(any(Lesson.class))).thenReturn(mockLesson);
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(mockLesson));
        when(lessonScriptRepository.findByLessonIdOrderByOrderNoAsc(10L)).thenReturn(Collections.emptyList());

        LessonDetailResponse response = adminLessonService.createLesson(1L, request);

        assertNotNull(response);
        assertEquals("At the Restaurant", response.getTitle());
        assertEquals(1, response.getOrderNo());
        verify(lessonRepository).save(any(Lesson.class));
    }

    @Test
    @DisplayName("Lưu kịch bản phụ đề (Script) cho bài học thành công")
    void testSaveLessonScripts_Success() {
        LessonScriptDto scriptDto = LessonScriptDto.builder()
                .orderNo(1)
                .startTime("00:00")
                .endTime("00:03")
                .speaker("Anna")
                .contentEn("Good morning")
                .contentVi("Chào buổi sáng")
                .build();

        SaveLessonScriptsRequest request = SaveLessonScriptsRequest.builder()
                .scripts(List.of(scriptDto))
                .build();

        when(lessonRepository.findById(10L)).thenReturn(Optional.of(mockLesson));
        when(lessonScriptRepository.findByLessonIdOrderByOrderNoAsc(10L)).thenReturn(Collections.emptyList());

        LessonDetailResponse response = adminLessonService.saveLessonScripts(10L, request);

        assertNotNull(response);
        verify(lessonScriptRepository).deleteByLessonId(10L);
        verify(lessonScriptRepository).saveAll(any());
    }
}

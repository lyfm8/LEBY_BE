package com.example.be.service.content.implement;

import com.example.be.dto.request.content.CreateLessonRequest;
import com.example.be.dto.request.content.LessonScriptDto;
import com.example.be.dto.request.content.ReorderLessonsRequest;
import com.example.be.dto.request.content.SaveLessonScriptsRequest;
import com.example.be.dto.request.content.UpdateLessonRequest;
import com.example.be.dto.response.content.LessonDetailResponse;
import com.example.be.dto.response.content.LessonSummaryResponse;
import com.example.be.entity.ability.Ability;
import com.example.be.entity.content.Lesson;
import com.example.be.entity.content.LessonScript;
import com.example.be.entity.content.Module;
import com.example.be.exception.BaseException;
import com.example.be.exception.ErrorCode;
import com.example.be.mapper.content.LessonMapper;
import com.example.be.repository.ability.AbilityRepository;
import com.example.be.repository.content.LessonRepository;
import com.example.be.repository.content.LessonScriptRepository;
import com.example.be.repository.content.ModuleRepository;
import com.example.be.service.content.service.AdminLessonService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Triển khai nghiệp vụ Quản lý Bài học (Lesson) và Kịch bản phụ đề (Script) dành cho Quản trị viên và Giáo viên.
 */
@Service
@RequiredArgsConstructor
public class AdminLessonServiceImpl implements AdminLessonService {

    private final LessonRepository lessonRepository;
    private final ModuleRepository moduleRepository;
    private final AbilityRepository abilityRepository;
    private final LessonScriptRepository lessonScriptRepository;
    private final LessonMapper lessonMapper;

    @Override
    @Transactional(readOnly = true)
    public List<LessonSummaryResponse> getLessonsByModuleId(Long moduleId) {
        ensureModuleExists(moduleId);
        List<Lesson> lessons = lessonRepository.findByModuleIdOrderByOrderNoAsc(moduleId);
        return lessons.stream().map(lessonMapper::toSummaryResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public LessonDetailResponse getLessonById(Long lessonId) {
        Lesson lesson = findLessonByIdOrThrow(lessonId);
        List<LessonScript> scripts = lessonScriptRepository.findByLessonIdOrderByOrderNoAsc(lesson.getId());
        return lessonMapper.toDetailResponse(lesson, scripts);
    }

    @Override
    @Transactional
    public LessonDetailResponse createLesson(Long moduleId, CreateLessonRequest request) {
        // Bước 1: Kiểm tra Module cha tồn tại
        Module module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy Module với ID: " + moduleId));

        // Bước 2: Tính toán orderNo nếu chưa truyền
        int orderNo = request.getOrderNo() != null
                ? request.getOrderNo()
                : lessonRepository.findMaxOrderNoByModuleId(moduleId).orElse(0) + 1;

        // Bước 3: Tìm Ability nếu có
        Ability ability = null;
        if (request.getAbilityId() != null) {
            ability = abilityRepository.findById(request.getAbilityId())
                    .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy Ability với ID: " + request.getAbilityId()));
        }

        // Bước 4: Chuyển đổi DTO sang Entity qua LessonMapper
        Lesson lesson = lessonMapper.toEntity(request, module, ability, orderNo);

        // Bước 5: Lưu vào CSDL
        Lesson savedLesson = lessonRepository.save(lesson);

        return getLessonById(savedLesson.getId());
    }

    @Override
    @Transactional
    public LessonDetailResponse updateLesson(Long lessonId, UpdateLessonRequest request) {
        // Bước 1: Tìm bài học hiện có
        Lesson lesson = findLessonByIdOrThrow(lessonId);

        // Bước 2: Tìm Ability nếu có
        Ability ability = null;
        if (request.getAbilityId() != null) {
            ability = abilityRepository.findById(request.getAbilityId())
                    .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy Ability với ID: " + request.getAbilityId()));
        }

        // Bước 3: Cập nhật Entity thông qua LessonMapper
        lessonMapper.updateEntity(lesson, request, ability);

        // Bước 4: Lưu thay đổi
        lessonRepository.save(lesson);

        return getLessonById(lessonId);
    }

    @Override
    @Transactional
    public void deleteLesson(Long lessonId) {
        Lesson lesson = findLessonByIdOrThrow(lessonId);
        lessonRepository.delete(lesson);
    }

    @Override
    @Transactional
    public void reorderLessons(Long moduleId, ReorderLessonsRequest request) {
        ensureModuleExists(moduleId);

        List<Long> orderedIds = request.getOrderedLessonIds();
        for (int i = 0; i < orderedIds.size(); i++) {
            Long lessonId = orderedIds.get(i);
            int newOrderNo = i + 1;
            Lesson lesson = lessonRepository.findById(lessonId).orElse(null);
            if (lesson != null && lesson.getModule().getId().equals(moduleId)) {
                lesson.setOrderNo(newOrderNo);
                lessonRepository.save(lesson);
            }
        }
    }

    @Override
    @Transactional
    public LessonDetailResponse saveLessonScripts(Long lessonId, SaveLessonScriptsRequest request) {
        // Bước 1: Kiểm tra bài học tồn tại
        Lesson lesson = findLessonByIdOrThrow(lessonId);

        // Bước 2: Xóa toàn bộ kịch bản cũ
        lessonScriptRepository.deleteByLessonId(lessonId);

        // Bước 3: Tạo mới và lưu toàn bộ kịch bản qua LessonMapper
        if (request.getScripts() != null && !request.getScripts().isEmpty()) {
            List<LessonScript> newScripts = new ArrayList<>();
            for (int i = 0; i < request.getScripts().size(); i++) {
                LessonScriptDto dto = request.getScripts().get(i);
                LessonScript script = lessonMapper.toScriptEntity(dto, lesson, i + 1);
                newScripts.add(script);
            }
            lessonScriptRepository.saveAll(newScripts);
        }

        return getLessonById(lessonId);
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================

    private Lesson findLessonByIdOrThrow(Long id) {
        return lessonRepository.findById(id)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy bài học với ID: " + id));
    }

    private void ensureModuleExists(Long moduleId) {
        if (!moduleRepository.existsById(moduleId)) {
            throw new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy Module với ID: " + moduleId);
        }
    }
}

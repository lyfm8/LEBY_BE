package com.example.be.mapper.content;

import com.example.be.dto.request.content.CreateLessonRequest;
import com.example.be.dto.request.content.LessonScriptDto;
import com.example.be.dto.request.content.UpdateLessonRequest;
import com.example.be.dto.response.content.LessonDetailResponse;
import com.example.be.dto.response.content.LessonSummaryResponse;
import com.example.be.entity.ability.Ability;
import com.example.be.entity.content.Lesson;
import com.example.be.entity.content.LessonScript;
import com.example.be.entity.content.Module;
import com.example.be.enums.content.ELessonType;
import com.example.be.enums.content.EModuleStatus;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper chuyển đổi giữa Entity Lesson / LessonScript và các DTO Request/Response.
 */
@Component
public class LessonMapper {

    /**
     * Chuyển đổi CreateLessonRequest sang JPA Entity Lesson.
     */
    public Lesson toEntity(CreateLessonRequest request, Module module, Ability ability, int orderNo) {
        if (request == null) {
            return null;
        }

        Lesson lesson = new Lesson();
        lesson.setModule(module);
        lesson.setAbility(ability);
        lesson.setTitle(request.getTitle().trim());
        lesson.setOrderNo(orderNo);
        lesson.setStatus(request.getStatus() != null ? request.getStatus() : EModuleStatus.DRAFT);
        lesson.setLessonType(request.getLessonType() != null ? request.getLessonType() : ELessonType.VIDEO);
        lesson.setDurationSeconds(request.getDurationSeconds() != null ? request.getDurationSeconds() : 0);
        lesson.setDescriptions(request.getDescriptions());
        lesson.setAudioUrl(request.getAudioUrl());
        lesson.setVideoUrl(request.getVideoUrl());
        lesson.setImageUrl(request.getImageUrl());
        lesson.setIsFree(request.getIsFree() != null ? request.getIsFree() : true);

        return lesson;
    }

    /**
     * Cập nhật Entity Lesson từ UpdateLessonRequest.
     */
    public void updateEntity(Lesson lesson, UpdateLessonRequest request, Ability ability) {
        if (lesson == null || request == null) {
            return;
        }

        lesson.setTitle(request.getTitle().trim());
        lesson.setOrderNo(request.getOrderNo());
        if (request.getStatus() != null) {
            lesson.setStatus(request.getStatus());
        }
        if (request.getLessonType() != null) {
            lesson.setLessonType(request.getLessonType());
        }
        if (request.getDurationSeconds() != null) {
            lesson.setDurationSeconds(request.getDurationSeconds());
        }
        lesson.setDescriptions(request.getDescriptions());
        lesson.setAudioUrl(request.getAudioUrl());
        lesson.setVideoUrl(request.getVideoUrl());
        lesson.setImageUrl(request.getImageUrl());
        if (request.getIsFree() != null) {
            lesson.setIsFree(request.getIsFree());
        }
        lesson.setAbility(ability);
    }

    /**
     * Chuyển đổi Lesson sang LessonSummaryResponse.
     */
    public LessonSummaryResponse toSummaryResponse(Lesson lesson) {
        if (lesson == null) {
            return null;
        }

        return LessonSummaryResponse.builder()
                .id(lesson.getId())
                .title(lesson.getTitle())
                .orderNo(lesson.getOrderNo())
                .status(lesson.getStatus())
                .lessonType(lesson.getLessonType())
                .durationSeconds(lesson.getDurationSeconds())
                .isFree(lesson.getIsFree())
                .abilityId(lesson.getAbility() != null ? lesson.getAbility().getId() : null)
                .abilityName(lesson.getAbility() != null ? lesson.getAbility().getName() : null)
                .audioUrl(lesson.getAudioUrl())
                .videoUrl(lesson.getVideoUrl())
                .imageUrl(lesson.getImageUrl())
                .build();
    }

    /**
     * Chuyển đổi Lesson sang LessonDetailResponse (kèm kịch bản scripts).
     */
    public LessonDetailResponse toDetailResponse(Lesson lesson, List<LessonScript> scripts) {
        if (lesson == null) {
            return null;
        }

        List<LessonScriptDto> scriptDtos = scripts != null
                ? scripts.stream().map(this::toScriptDto).collect(Collectors.toList())
                : Collections.emptyList();

        return LessonDetailResponse.builder()
                .id(lesson.getId())
                .moduleId(lesson.getModule() != null ? lesson.getModule().getId() : null)
                .moduleTitle(lesson.getModule() != null ? lesson.getModule().getTitle() : null)
                .title(lesson.getTitle())
                .orderNo(lesson.getOrderNo())
                .status(lesson.getStatus())
                .lessonType(lesson.getLessonType())
                .durationSeconds(lesson.getDurationSeconds())
                .isFree(lesson.getIsFree())
                .descriptions(lesson.getDescriptions())
                .abilityId(lesson.getAbility() != null ? lesson.getAbility().getId() : null)
                .abilityName(lesson.getAbility() != null ? lesson.getAbility().getName() : null)
                .audioUrl(lesson.getAudioUrl())
                .videoUrl(lesson.getVideoUrl())
                .imageUrl(lesson.getImageUrl())
                .scripts(scriptDtos)
                .createdAt(lesson.getCreatedAt())
                .updatedAt(lesson.getUpdatedAt())
                .build();
    }

    /**
     * Chuyển đổi LessonScript entity sang LessonScriptDto.
     */
    public LessonScriptDto toScriptDto(LessonScript script) {
        if (script == null) {
            return null;
        }

        return LessonScriptDto.builder()
                .id(script.getId())
                .orderNo(script.getOrderNo())
                .startTime(script.getStartTime())
                .endTime(script.getEndTime())
                .speaker(script.getSpeaker())
                .contentEn(script.getContentEn())
                .contentVi(script.getContentVi())
                .build();
    }

    /**
     * Chuyển đổi LessonScriptDto sang JPA Entity LessonScript.
     */
    public LessonScript toScriptEntity(LessonScriptDto dto, Lesson lesson, int fallbackOrderNo) {
        if (dto == null) {
            return null;
        }

        LessonScript script = new LessonScript();
        script.setLesson(lesson);
        script.setOrderNo(dto.getOrderNo() != null ? dto.getOrderNo() : fallbackOrderNo);
        script.setStartTime(dto.getStartTime().trim());
        script.setEndTime(dto.getEndTime().trim());
        script.setSpeaker(dto.getSpeaker() != null ? dto.getSpeaker().trim() : null);
        script.setContentEn(dto.getContentEn().trim());
        script.setContentVi(dto.getContentVi() != null ? dto.getContentVi().trim() : null);

        return script;
    }
}


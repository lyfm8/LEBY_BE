package com.example.be.mapper.content;

import com.example.be.dto.response.content.PublicCatalogModuleResponse;
import com.example.be.dto.response.content.PublicCatalogPartResponse;
import com.example.be.dto.response.content.PublicCatalogResponse;
import com.example.be.entity.content.Module;
import com.example.be.entity.content.Part;
import com.example.be.enums.ability.ESection;
import com.example.be.enums.content.EModuleTestSelectionMode;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Mapper chuyên biệt chuyển đổi Entity Part/Module sang DTO công khai tối giản cho Landing Page.
 */
@Component
public class PublicCatalogMapper {

    /**
     * Chuyển đổi một Module đã xuất bản cùng số liệu bài học/test sang {@link PublicCatalogModuleResponse}.
     *
     * @param module                  Thực thể Module đã xuất bản
     * @param videoLessonCount        Số bài giảng video đã xuất bản
     * @param practiceLessonCount     Số bài thực hành đã xuất bản
     * @param manualTestQuestionCount Số câu hỏi gán thủ công trong module_test_questions
     * @return DTO mô-đun công khai chỉ gồm tên và số liệu tóm tắt
     */
    public PublicCatalogModuleResponse toModuleResponse(
            Module module,
            int videoLessonCount,
            int practiceLessonCount,
            int manualTestQuestionCount
    ) {
        int testCount = hasConfiguredTest(module, manualTestQuestionCount) ? 1 : 0;

        return PublicCatalogModuleResponse.builder()
                .id(module.getId())
                .sequence(module.getSequence())
                .title(module.getTitle())
                .videoLessonCount(Math.max(0, videoLessonCount))
                .practiceLessonCount(Math.max(0, practiceLessonCount))
                .testCount(testCount)
                .build();
    }

    /**
     * Chuyển đổi một Part và danh sách mô-đun thuộc Part sang {@link PublicCatalogPartResponse}.
     *
     * @param part    Thực thể Part
     * @param modules Danh sách mô-đun đã map thuộc Part (có thể rỗng nếu Part đang cập nhật nội dung)
     * @return DTO nhóm Part công khai
     */
    public PublicCatalogPartResponse toPartResponse(
            Part part,
            List<PublicCatalogModuleResponse> modules
    ) {
        List<PublicCatalogModuleResponse> safeModules = modules != null ? modules : Collections.emptyList();
        ESection section = part.getSections() != null ? part.getSections() : ESection.READING;

        return PublicCatalogPartResponse.builder()
                .partId(part.getId())
                .partName(part.getName())
                .section(section)
                .modules(safeModules)
                .build();
    }

    /**
     * Tổng hợp danh sách các Part công khai thành {@link PublicCatalogResponse}.
     *
     * @param parts Danh sách 7 Part đã map
     * @return DTO phản hồi cho API GET /api/public/catalog
     */
    public PublicCatalogResponse toCatalogResponse(List<PublicCatalogPartResponse> parts) {
        List<PublicCatalogPartResponse> safeParts = parts != null ? parts : Collections.emptyList();

        int totalModules = 0;
        int totalLessons = 0;
        for (PublicCatalogPartResponse part : safeParts) {
            List<PublicCatalogModuleResponse> modules = part.getModules() != null
                    ? part.getModules()
                    : Collections.emptyList();
            totalModules += modules.size();
            for (PublicCatalogModuleResponse mod : modules) {
                totalLessons += (mod.getVideoLessonCount() != null ? mod.getVideoLessonCount() : 0)
                        + (mod.getPracticeLessonCount() != null ? mod.getPracticeLessonCount() : 0);
            }
        }

        return PublicCatalogResponse.builder()
                .totalParts(safeParts.size())
                .totalModules(totalModules)
                .totalLessons(totalLessons)
                .parts(safeParts)
                .build();
    }

    private boolean hasConfiguredTest(Module module, int manualTestQuestionCount) {
        EModuleTestSelectionMode mode = module.getTestSelectionMode() != null
                ? module.getTestSelectionMode()
                : EModuleTestSelectionMode.MANUAL;
        if (mode == EModuleTestSelectionMode.RANDOM_BY_ABILITY) {
            return module.getTestQuestionCount() != null && module.getTestQuestionCount() > 0;
        }
        return manualTestQuestionCount > 0;
    }
}

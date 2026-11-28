package com.example.be.service.content.implement;

import com.example.be.dto.response.content.PublicCatalogModuleResponse;
import com.example.be.dto.response.content.PublicCatalogPartResponse;
import com.example.be.dto.response.content.PublicCatalogResponse;
import com.example.be.entity.content.Module;
import com.example.be.entity.content.Part;
import com.example.be.enums.ability.ESection;
import com.example.be.enums.content.ELessonType;
import com.example.be.enums.content.EModuleStatus;
import com.example.be.enums.content.EModuleTestSelectionMode;
import com.example.be.mapper.content.PublicCatalogMapper;
import com.example.be.repository.content.LessonRepository;
import com.example.be.repository.content.ModuleRepository;
import com.example.be.repository.content.PartRepository;
import com.example.be.repository.question.ModuleTestQuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit Tests cho {@link PublicCatalogServiceImpl} phục vụ Landing Page 7 Part.
 */
@ExtendWith(MockitoExtension.class)
class PublicCatalogServiceImplTest {

    @Mock
    private PartRepository partRepository;

    @Mock
    private ModuleRepository moduleRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private ModuleTestQuestionRepository moduleTestQuestionRepository;

    private PublicCatalogServiceImpl publicCatalogService;

    @BeforeEach
    void setUp() {
        PublicCatalogMapper mapper = new PublicCatalogMapper();
        publicCatalogService = new PublicCatalogServiceImpl(
                partRepository,
                moduleRepository,
                lessonRepository,
                moduleTestQuestionRepository,
                mapper
        );
    }

    @Test
    @DisplayName("Trả về đầy đủ Part kể cả Part chưa có module PUBLISHED và tính đúng video/practice/test")
    void getPublicCatalog_returnsAllPartsIncludingEmptyAndCalculatesCounts() {
        Part part1 = buildPart(1L, "Part 1: Photographs", ESection.LISTENING);
        Part part2 = buildPart(2L, "Part 2: Question - Response", ESection.LISTENING);

        Module modManualWithTest = buildModule(10L, "Tranh tả người", 1, part1, EModuleTestSelectionMode.MANUAL, 15);
        Module modRandomWithTest = buildModule(11L, "Tranh tả vật", 2, part1, EModuleTestSelectionMode.RANDOM_BY_ABILITY, 25);

        when(partRepository.findAllByOrderByIdAsc()).thenReturn(List.of(part1, part2));
        when(moduleRepository.findByStatusWithPartAndAbilitiesOrderBySequenceAsc(EModuleStatus.PUBLISHED))
                .thenReturn(List.of(modManualWithTest, modRandomWithTest));
        when(lessonRepository.countByModuleIdInAndStatusAndLessonType(
                List.of(10L, 11L), EModuleStatus.PUBLISHED, ELessonType.VIDEO))
                .thenReturn(List.of(
                        new Object[]{10L, 2L},
                        new Object[]{11L, 1L}
                ));
        when(lessonRepository.countByModuleIdInAndStatusAndLessonType(
                List.of(10L, 11L), EModuleStatus.PUBLISHED, ELessonType.PRACTICE))
                .thenReturn(List.<Object[]>of(new Object[]{10L, 3L}));
        when(moduleTestQuestionRepository.countQuestionsGroupedByModuleIds(List.of(10L, 11L)))
                .thenReturn(List.<Object[]>of(new Object[]{10L, 10L}));

        PublicCatalogResponse result = publicCatalogService.getPublicCatalog();

        assertThat(result.getTotalParts()).isEqualTo(2);
        assertThat(result.getTotalModules()).isEqualTo(2);
        assertThat(result.getTotalLessons()).isEqualTo(6);

        PublicCatalogPartResponse firstPart = result.getParts().get(0);
        assertThat(firstPart.getPartName()).isEqualTo("Part 1: Photographs");
        assertThat(firstPart.getModules()).hasSize(2);

        PublicCatalogModuleResponse m1 = firstPart.getModules().get(0);
        assertThat(m1.getVideoLessonCount()).isEqualTo(2);
        assertThat(m1.getPracticeLessonCount()).isEqualTo(3);
        assertThat(m1.getTestCount()).isEqualTo(1);

        PublicCatalogModuleResponse m2 = firstPart.getModules().get(1);
        assertThat(m2.getVideoLessonCount()).isEqualTo(1);
        assertThat(m2.getPracticeLessonCount()).isEqualTo(0);
        assertThat(m2.getTestCount()).isEqualTo(1);

        PublicCatalogPartResponse emptyPart = result.getParts().get(1);
        assertThat(emptyPart.getPartName()).isEqualTo("Part 2: Question - Response");
        assertThat(emptyPart.getModules()).isEmpty();
    }

    @Test
    @DisplayName("Không thực hiện query IN khi chưa có module PUBLISHED nào")
    void getPublicCatalog_whenNoPublishedModules_skipsInQueriesSafely() {
        Part part1 = buildPart(1L, "Part 1: Photographs", ESection.LISTENING);
        when(partRepository.findAllByOrderByIdAsc()).thenReturn(List.of(part1));
        when(moduleRepository.findByStatusWithPartAndAbilitiesOrderBySequenceAsc(EModuleStatus.PUBLISHED))
                .thenReturn(Collections.emptyList());

        PublicCatalogResponse result = publicCatalogService.getPublicCatalog();

        assertThat(result.getTotalParts()).isEqualTo(1);
        assertThat(result.getTotalModules()).isZero();
        assertThat(result.getTotalLessons()).isZero();
        verify(lessonRepository, never()).countByModuleIdInAndStatusAndLessonType(
                anyCollection(), eq(EModuleStatus.PUBLISHED), eq(ELessonType.VIDEO));
        verify(moduleTestQuestionRepository, never()).countQuestionsGroupedByModuleIds(anyCollection());
    }

    private Part buildPart(Long id, String name, ESection section) {
        Part part = new Part();
        part.setId(id);
        part.setName(name);
        part.setSections(section);
        return part;
    }

    private Module buildModule(
            Long id,
            String title,
            int sequence,
            Part part,
            EModuleTestSelectionMode selectionMode,
            int testQuestionCount
    ) {
        Module module = new Module();
        module.setId(id);
        module.setTitle(title);
        module.setSequence(sequence);
        module.setPart(part);
        module.setStatus(EModuleStatus.PUBLISHED);
        module.setTestSelectionMode(selectionMode);
        module.setTestQuestionCount(testQuestionCount);
        return module;
    }
}

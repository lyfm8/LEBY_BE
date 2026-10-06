package com.example.be;

import com.example.be.dto.request.ability.AbilityRequest;
import com.example.be.dto.request.content.PartRequest;
import com.example.be.dto.response.ability.AbilityResponse;
import com.example.be.dto.response.content.PartResponse;
import com.example.be.entity.ability.Ability;
import com.example.be.entity.content.Part;
import com.example.be.enums.ability.ESection;
import com.example.be.exception.BaseException;
import com.example.be.mapper.AbilityMapper;
import com.example.be.mapper.PartMapper;
import com.example.be.repository.ability.AbilityRepository;
import com.example.be.repository.content.PartRepository;
import com.example.be.repository.question.QuestionAbilityRepository;
import com.example.be.repository.question.QuestionRepository;
import com.example.be.service.ability.implement.AbilityServiceImpl;
import com.example.be.service.part.implement.PartServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PartAbilityServiceTest {

    @Mock
    private PartRepository partRepository;
    @Mock
    private AbilityRepository abilityRepository;
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private QuestionAbilityRepository questionAbilityRepository;

    @Spy
    private PartMapper partMapper = new PartMapper();
    @Spy
    private AbilityMapper abilityMapper = new AbilityMapper();

    @InjectMocks
    private PartServiceImpl partService;

    @InjectMocks
    private AbilityServiceImpl abilityService;

    private Part samplePart;
    private Ability sampleAbility;

    @BeforeEach
    void setUp() {
        samplePart = new Part();
        samplePart.setId(1L);
        samplePart.setName("Part 1: Photographs");
        samplePart.setDescription("Listening Photographs");
        samplePart.setSections(ESection.LISTENING);

        sampleAbility = new Ability();
        sampleAbility.setId(10L);
        sampleAbility.setName("Identifying Actions");
        sampleAbility.setDescription("Describe actions in pictures");
        sampleAbility.setSections(ESection.LISTENING);
        sampleAbility.setPart(samplePart);
    }

    @Test
    void testCreatePartSuccess() {
        PartRequest request = new PartRequest("Part 1: Photographs", "Listening", ESection.LISTENING);
        when(partRepository.existsByName("Part 1: Photographs")).thenReturn(false);
        when(partRepository.save(any(Part.class))).thenAnswer(invocation -> {
            Part p = invocation.getArgument(0);
            p.setId(1L);
            return p;
        });

        PartResponse response = partService.createPart(request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Part 1: Photographs", response.getName());
    }

    @Test
    void testDeletePartThrowsWhenHasLinkedAbilities() {
        when(partRepository.findById(1L)).thenReturn(Optional.of(samplePart));
        when(abilityRepository.countByPartId(1L)).thenReturn(3L);

        BaseException ex = assertThrows(BaseException.class, () -> partService.deletePart(1L));
        assertTrue(ex.getMessage().contains("Part còn 3 Ability liên kết"));
        verify(partRepository, never()).delete(any());
    }

    @Test
    void testDeleteAbilityThrowsWhenHasLinkedQuestions() {
        when(abilityRepository.findById(10L)).thenReturn(Optional.of(sampleAbility));
        when(questionAbilityRepository.countByAbilityId(10L)).thenReturn(5L);

        BaseException ex = assertThrows(BaseException.class, () -> abilityService.deleteAbility(10L));
        assertTrue(ex.getMessage().contains("Ability còn 5 câu hỏi liên kết"));
        verify(abilityRepository, never()).delete(any());
    }

    @Test
    void testCreateAbilitySuccess() {
        AbilityRequest request = new AbilityRequest("Identifying Actions", "Describe actions");
        when(partRepository.findById(1L)).thenReturn(Optional.of(samplePart));
        when(abilityRepository.existsByPartIdAndName(1L, "Identifying Actions")).thenReturn(false);
        when(abilityRepository.save(any(Ability.class))).thenAnswer(invocation -> {
            Ability a = invocation.getArgument(0);
            a.setId(10L);
            return a;
        });

        AbilityResponse response = abilityService.createAbility(1L, request);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("Identifying Actions", response.getName());
        assertEquals(ESection.LISTENING, response.getSections());
        assertEquals(1L, response.getPartId());
    }
}

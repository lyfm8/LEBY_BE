package com.example.be.service.ability.implement;

import com.example.be.dto.request.ability.AbilityRequest;
import com.example.be.dto.response.ability.AbilityResponse;
import com.example.be.entity.ability.Ability;
import com.example.be.entity.content.Part;
import com.example.be.exception.BaseException;
import com.example.be.exception.ErrorCode;
import com.example.be.mapper.AbilityMapper;
import com.example.be.repository.ability.AbilityRepository;
import com.example.be.repository.content.PartRepository;
import com.example.be.repository.question.QuestionAbilityRepository;
import com.example.be.service.ability.service.AbilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AbilityServiceImpl implements AbilityService {

    private final AbilityRepository abilityRepository;
    private final PartRepository partRepository;
    private final QuestionAbilityRepository questionAbilityRepository;
    private final AbilityMapper abilityMapper;

    @Override
    @Transactional(readOnly = true)
    public List<AbilityResponse> getAbilitiesByPartId(Long partId) {
        if (!partRepository.existsById(partId)) {
            throw new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Part không tồn tại với ID: " + partId);
        }

        return abilityRepository.findByPartIdOrderByIdAsc(partId)
                .stream()
                .map(abilityMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AbilityResponse createAbility(Long partId, AbilityRequest request) {
        Part part = partRepository.findById(partId)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Part không tồn tại với ID: " + partId));

        if (abilityRepository.existsByPartIdAndName(partId, request.getName())) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "Tên Ability đã tồn tại trong Part này: " + request.getName());
        }

        Ability ability = abilityMapper.toEntity(request, part);
        ability = abilityRepository.save(ability);
        return abilityMapper.toResponse(ability);
    }

    @Override
    @Transactional
    public AbilityResponse updateAbility(Long id, AbilityRequest request) {
        Ability ability = abilityRepository.findById(id)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Ability không tồn tại với ID: " + id));

        Long partId = ability.getPart() != null ? ability.getPart().getId() : null;
        if (partId != null && !ability.getName().equalsIgnoreCase(request.getName())
                && abilityRepository.existsByPartIdAndName(partId, request.getName())) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "Tên Ability đã tồn tại trong Part này: " + request.getName());
        }

        ability.setName(request.getName());
        ability.setDescription(request.getDescription());

        ability = abilityRepository.save(ability);
        return abilityMapper.toResponse(ability);
    }

    @Override
    @Transactional
    public void deleteAbility(Long id) {
        Ability ability = abilityRepository.findById(id)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Ability không tồn tại với ID: " + id));

        long linkedQuestions = questionAbilityRepository.countByAbilityId(id);
        if (linkedQuestions > 0) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "Ability còn " + linkedQuestions + " câu hỏi liên kết, không thể xóa.");
        }

        abilityRepository.delete(ability);
    }
}

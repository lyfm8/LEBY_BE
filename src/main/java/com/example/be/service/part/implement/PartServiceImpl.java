package com.example.be.service.part.implement;

import com.example.be.dto.request.content.PartRequest;
import com.example.be.dto.response.content.PartResponse;
import com.example.be.entity.content.Part;
import com.example.be.exception.BaseException;
import com.example.be.exception.ErrorCode;
import com.example.be.mapper.PartMapper;
import com.example.be.repository.ability.AbilityRepository;
import com.example.be.repository.content.PartRepository;
import com.example.be.repository.question.QuestionRepository;
import com.example.be.service.part.service.PartService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PartServiceImpl implements PartService {

    private final PartRepository partRepository;
    private final AbilityRepository abilityRepository;
    private final QuestionRepository questionRepository;
    private final PartMapper partMapper;

    @Override
    @Transactional(readOnly = true)
    public List<PartResponse> getAllParts() {
        return partRepository.findAll(Sort.by("id").ascending())
                .stream()
                .map(partMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PartResponse getPartById(Long id) {
        Part part = partRepository.findById(id)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Part không tồn tại với ID: " + id));
        return partMapper.toResponse(part);
    }

    @Override
    @Transactional
    public PartResponse createPart(PartRequest request) {
        if (partRepository.existsByName(request.getName())) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "Tên Part đã tồn tại: " + request.getName());
        }

        Part part = partMapper.toEntity(request);
        part = partRepository.save(part);
        return partMapper.toResponse(part);
    }

    @Override
    @Transactional
    public PartResponse updatePart(Long id, PartRequest request) {
        Part part = partRepository.findById(id)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Part không tồn tại với ID: " + id));

        if (!part.getName().equalsIgnoreCase(request.getName()) && partRepository.existsByName(request.getName())) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "Tên Part đã tồn tại: " + request.getName());
        }

        part.setName(request.getName());
        part.setDescription(request.getDescription());
        part.setSections(request.getSections());

        part = partRepository.save(part);
        return partMapper.toResponse(part);
    }

    @Override
    @Transactional
    public void deletePart(Long id) {
        Part part = partRepository.findById(id)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Part không tồn tại với ID: " + id));

        long abilityCount = abilityRepository.countByPartId(id);
        if (abilityCount > 0) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "Part còn " + abilityCount + " Ability liên kết, không thể xóa.");
        }

        long questionCount = questionRepository.countByPartId(id);
        if (questionCount > 0) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "Part còn " + questionCount + " câu hỏi liên kết, không thể xóa.");
        }

        partRepository.delete(part);
    }
}

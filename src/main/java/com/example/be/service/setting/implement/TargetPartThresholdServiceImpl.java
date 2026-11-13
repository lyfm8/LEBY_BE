package com.example.be.service.setting.implement;

import com.example.be.dto.request.setting.CreateTargetPartThresholdRequest;
import com.example.be.dto.request.setting.UpdateTargetPartThresholdRequest;
import com.example.be.dto.response.setting.TargetPartThresholdResponse;
import com.example.be.entity.content.Part;
import com.example.be.entity.setting.TargetPartThreshold;
import com.example.be.entity.user.TargetProfile;
import com.example.be.exception.BaseException;
import com.example.be.exception.ErrorCode;
import com.example.be.mapper.setting.TargetPartThresholdMapper;
import com.example.be.repository.content.PartRepository;
import com.example.be.repository.setting.TargetPartThresholdRepository;
import com.example.be.repository.user.TargetProfileRepository;
import com.example.be.service.setting.service.TargetPartThresholdService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TargetPartThresholdServiceImpl implements TargetPartThresholdService {

    private final TargetPartThresholdRepository thresholdRepo;
    private final TargetProfileRepository profileRepo;
    private final PartRepository partRepo;
    private final TargetPartThresholdMapper thresholdMapper;

    @Override
    @Transactional(readOnly = true)
    public List<TargetPartThresholdResponse> getAllThresholds() {
        log.info("Lấy toàn bộ danh sách cấu hình ngưỡng điểm Part");
        List<TargetPartThreshold> thresholds = thresholdRepo.findAllWithDetails();
        return thresholds.stream().map(thresholdMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TargetPartThresholdResponse> getThresholdsByProfile(Long targetProfileId) {
        log.info("Lấy danh sách cấu hình ngưỡng điểm Part theo Target Profile ID: {}", targetProfileId);
        List<TargetPartThreshold> thresholds = thresholdRepo.findByTargetProfileIdWithDetails(targetProfileId);
        return thresholds.stream().map(thresholdMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TargetPartThresholdResponse getThresholdById(Long id) {
        log.info("Lấy chi tiết ngưỡng điểm Part ID: {}", id);
        TargetPartThreshold threshold = thresholdRepo.findById(id)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy cấu hình ngưỡng Part với ID: " + id));
        return thresholdMapper.toResponse(threshold);
    }

    @Override
    @Transactional
    public TargetPartThresholdResponse createThreshold(CreateTargetPartThresholdRequest request) {
        log.info("Tạo mới ngưỡng điểm Part cho Profile {} và Part {}", request.getTargetProfileId(), request.getPartId());

        validateThresholdOrder(request.getPassScore(), request.getConfirmingScore(), request.getFullPartThreshold());

        if (thresholdRepo.existsByTargetProfileIdAndPartId(request.getTargetProfileId(), request.getPartId())) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "Đã tồn tại cấu hình ngưỡng cho Target Profile và Part này");
        }

        TargetProfile profile = profileRepo.findById(request.getTargetProfileId())
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy Target Profile với ID: " + request.getTargetProfileId()));
        Part part = partRepo.findById(request.getPartId())
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy Part với ID: " + request.getPartId()));

        TargetPartThreshold entity = thresholdMapper.toEntity(request, profile, part);
        TargetPartThreshold saved = thresholdRepo.save(entity);
        return thresholdMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public TargetPartThresholdResponse updateThreshold(Long id, UpdateTargetPartThresholdRequest request) {
        log.info("Cập nhật ngưỡng điểm Part ID: {}", id);

        validateThresholdOrder(request.getPassScore(), request.getConfirmingScore(), request.getFullPartThreshold());

        TargetPartThreshold threshold = thresholdRepo.findById(id)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy cấu hình ngưỡng Part với ID: " + id));

        thresholdMapper.updateEntity(threshold, request);
        TargetPartThreshold saved = thresholdRepo.save(threshold);
        return thresholdMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteThreshold(Long id) {
        log.info("Xóa cấu hình ngưỡng điểm Part ID: {}", id);
        if (!thresholdRepo.existsById(id)) {
            throw new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy cấu hình ngưỡng Part với ID: " + id);
        }
        thresholdRepo.deleteById(id);
    }

    private void validateThresholdOrder(Float passScore, Float confirmingScore, Float fullPartThreshold) {
        if (confirmingScore != null && passScore != null && confirmingScore > passScore) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "Điểm cận chuẩn (confirmingScore) không được lớn hơn điểm đạt chuẩn (passScore)");
        }
        if (fullPartThreshold != null && confirmingScore != null && fullPartThreshold > confirmingScore) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "Điểm mất gốc (fullPartThreshold) không được lớn hơn điểm cận chuẩn (confirmingScore)");
        }
        if (fullPartThreshold != null && passScore != null && fullPartThreshold > passScore) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "Điểm mất gốc (fullPartThreshold) không được lớn hơn điểm đạt chuẩn (passScore)");
        }
    }
}

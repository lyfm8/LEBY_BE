package com.example.be.service.part.service;

import com.example.be.dto.request.content.PartRequest;
import com.example.be.dto.response.content.PartResponse;

import java.util.List;

public interface PartService {
    List<PartResponse> getAllParts();
    PartResponse getPartById(Long id);
    PartResponse createPart(PartRequest request);
    PartResponse updatePart(Long id, PartRequest request);
    void deletePart(Long id);
}

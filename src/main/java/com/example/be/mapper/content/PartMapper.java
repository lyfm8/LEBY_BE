package com.example.be.mapper.content;

import com.example.be.dto.request.content.PartRequest;
import com.example.be.dto.response.content.PartResponse;
import com.example.be.entity.content.Part;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi giữa Entity Part và các DTO Request/Response.
 */
@Component
public class PartMapper {

    public PartResponse toResponse(Part part) {
        if (part == null) {
            return null;
        }
        return PartResponse.builder()
                .id(part.getId())
                .name(part.getName())
                .description(part.getDescription())
                .sections(part.getSections())
                .build();
    }

    public Part toEntity(PartRequest request) {
        if (request == null) {
            return null;
        }
        Part part = new Part();
        part.setName(request.getName());
        part.setDescription(request.getDescription());
        part.setSections(request.getSections());
        return part;
    }
}


package com.example.be.dto.response.content;

import com.example.be.enums.content.EMistakeSourceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO phân trang danh sách Sổ tay câu hỏi làm sai kèm bộ đếm tùy chọn bộ lọc (UC15).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MistakeNotebookResponse {

    private Integer totalWrongCount;
    private FilterOptions filterOptions;
    private List<MistakeQuestionItemResponse> items;

    private Integer page;
    private Integer size;
    private Long totalElements;
    private Integer totalPages;
    private Boolean hasNext;
    private Boolean hasPrevious;

    /**
     * Tập tùy chọn lọc theo Part, Kỹ năng và Nguồn bài thi dựa trên dữ liệu thực tế của học viên.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FilterOptions {
        private List<PartCountItem> parts;
        private List<AbilityCountItem> abilities;
        private List<SourceCountItem> sources;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PartCountItem {
        private Long partId;
        private String partName;
        private Integer count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AbilityCountItem {
        private Long abilityId;
        private String abilityName;
        private Long partId;
        private Integer count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SourceCountItem {
        private EMistakeSourceType sourceType;
        private String label;
        private Integer count;
    }
}

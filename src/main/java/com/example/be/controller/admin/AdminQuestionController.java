package com.example.be.controller.admin;

import com.example.be.common.ApiResponse;
import com.example.be.dto.request.question.CreateQuestionGroupRequest;
import com.example.be.dto.request.question.CreateStandaloneQuestionRequest;
import com.example.be.dto.request.question.UpdateQuestionGroupRequest;
import com.example.be.dto.request.question.UpdateStandaloneQuestionRequest;
import com.example.be.dto.response.question.QuestionDetailResponse;
import com.example.be.dto.response.question.QuestionListItemResponse;
import com.example.be.enums.ability.ESection;
import com.example.be.security.Roles;
import com.example.be.service.question.service.QuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST Controller cung cấp các API Quản lý Ngân hàng câu hỏi (UC A04).
 * Hỗ trợ giáo viên và quản trị viên biên soạn, lọc và quản lý kho câu hỏi TOEIC.
 */
@Validated
@RestController
@RequestMapping("/api/admin/questions")
@RequiredArgsConstructor
@PreAuthorize(Roles.TEACHER_OR_ADMIN)
public class AdminQuestionController {

    private final QuestionService questionService;

    /**
     * Lấy danh sách câu hỏi gốc có phân trang và bộ lọc đa tiêu chí.
     *
     * @param page Số trang (bắt đầu từ 0)
     * @param size Kích thước trang
     * @param keyword Từ khóa tìm kiếm theo tên/nội dung
     * @param section Phần thi (LISTENING hoặc READING)
     * @param partId ID của Part
     * @param abilityId ID của Ability
     * @param difficulty Độ khó (1-5)
     * @param structureType Lọc cấu trúc: "ALL", "STANDALONE" hoặc "GROUP"
     * @return Danh sách câu hỏi bọc trong ApiResponse kèm phân trang
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<QuestionListItemResponse>>> getQuestions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) ESection section,
            @RequestParam(required = false) Long partId,
            @RequestParam(required = false) Long abilityId,
            @RequestParam(required = false) Integer difficulty,
            @RequestParam(required = false, defaultValue = "ALL") String structureType
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("updatedAt").descending());
        Page<QuestionListItemResponse> result = questionService.getQuestions(
                keyword,
                section,
                partId,
                abilityId,
                difficulty,
                structureType,
                pageable
        );

        ApiResponse.Pagination pagination = ApiResponse.Pagination.builder()
                .page(result.getNumber())
                .pageSize(result.getSize())
                .totalItems((int) result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();

        return ResponseEntity.ok(ApiResponse.<List<QuestionListItemResponse>>builder()
                .success(true)
                .message("Lấy danh sách câu hỏi thành công")
                .data(result.getContent())
                .pagination(pagination)
                .build());
    }

    /**
     * Lấy thông tin chi tiết một câu hỏi theo ID.
     *
     * @param id ID của câu hỏi
     * @return Chi tiết câu hỏi
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<QuestionDetailResponse>> getQuestionById(@PathVariable Long id) {
        QuestionDetailResponse response = questionService.getQuestionById(id);
        return ResponseEntity.ok(ApiResponse.<QuestionDetailResponse>builder()
                .success(true)
                .message("Lấy chi tiết câu hỏi thành công")
                .data(response)
                .build());
    }

    /**
     * Tạo mới một câu hỏi đơn lập (Part 1, 2, 5).
     *
     * @param request Dữ liệu câu hỏi đơn
     * @return Câu hỏi vừa tạo
     */
    @PostMapping("/standalone")
    public ResponseEntity<ApiResponse<QuestionDetailResponse>> createStandaloneQuestion(
            @Valid @RequestBody CreateStandaloneQuestionRequest request
    ) {
        QuestionDetailResponse response = questionService.createStandaloneQuestion(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<QuestionDetailResponse>builder()
                        .success(true)
                        .message("Thêm câu hỏi thành công")
                        .data(response)
                        .build());
    }

    /**
     * Cập nhật thông tin câu hỏi đơn lập.
     *
     * @param id ID câu hỏi cần sửa
     * @param request Dữ liệu cập nhật
     * @return Câu hỏi sau khi cập nhật
     */
    @PutMapping("/standalone/{id}")
    public ResponseEntity<ApiResponse<QuestionDetailResponse>> updateStandaloneQuestion(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStandaloneQuestionRequest request
    ) {
        QuestionDetailResponse response = questionService.updateStandaloneQuestion(id, request);
        return ResponseEntity.ok(ApiResponse.<QuestionDetailResponse>builder()
                .success(true)
                .message("Cập nhật câu hỏi thành công")
                .data(response)
                .build());
    }

    /**
     * Tạo mới một chùm câu hỏi chung ngữ cảnh (Part 3, 4, 6, 7).
     *
     * @param request Dữ liệu chùm câu hỏi kèm các câu con
     * @return Chùm câu hỏi vừa tạo
     */
    @PostMapping("/groups")
    public ResponseEntity<ApiResponse<QuestionDetailResponse>> createQuestionGroup(
            @Valid @RequestBody CreateQuestionGroupRequest request
    ) {
        QuestionDetailResponse response = questionService.createQuestionGroup(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<QuestionDetailResponse>builder()
                        .success(true)
                        .message("Lưu toàn bộ chùm câu hỏi thành công")
                        .data(response)
                        .build());
    }

    /**
     * Cập nhật chùm câu hỏi và toàn bộ câu con.
     *
     * @param id ID của chùm câu hỏi cha
     * @param request Dữ liệu cập nhật
     * @return Chùm câu hỏi sau cập nhật
     */
    @PutMapping("/groups/{id}")
    public ResponseEntity<ApiResponse<QuestionDetailResponse>> updateQuestionGroup(
            @PathVariable Long id,
            @Valid @RequestBody UpdateQuestionGroupRequest request
    ) {
        QuestionDetailResponse response = questionService.updateQuestionGroup(id, request);
        return ResponseEntity.ok(ApiResponse.<QuestionDetailResponse>builder()
                .success(true)
                .message("Cập nhật chùm câu hỏi thành công")
                .data(response)
                .build());
    }

    /**
     * Xóa một câu hỏi khỏi ngân hàng (câu đơn hoặc chùm câu hỏi).
     *
     * @param id ID câu hỏi cần xóa
     * @return Thông báo thành công
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteQuestion(@PathVariable Long id) {
        questionService.deleteQuestion(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Xóa câu hỏi thành công")
                .build());
    }
}


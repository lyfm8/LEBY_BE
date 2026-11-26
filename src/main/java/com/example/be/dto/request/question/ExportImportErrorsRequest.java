package com.example.be.dto.request.question;

import com.example.be.dto.response.question.ImportRowError;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Yêu cầu xuất tệp Excel báo cáo chi tiết các dòng lỗi khi import câu hỏi.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportImportErrorsRequest {

    @NotEmpty(message = "Danh sách lỗi không được để trống")
    @Size(max = 5000, message = "Số lượng dòng lỗi xuất báo cáo tối đa 5000")
    private List<@Valid ImportRowError> errors;
}

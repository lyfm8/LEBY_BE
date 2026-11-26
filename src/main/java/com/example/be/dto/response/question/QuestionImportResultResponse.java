package com.example.be.dto.response.question;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Phản hồi tổng hợp kết quả import câu hỏi hàng loạt từ file Excel.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionImportResultResponse {

    private boolean imported;
    private int totalRows;
    private int standaloneCount;
    private int groupCount;
    private int groupItemCount;
    private int successCount;
    private int failCount;
    private List<ImportRowError> errors;
}

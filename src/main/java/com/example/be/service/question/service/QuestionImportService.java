package com.example.be.service.question.service;

import com.example.be.dto.response.question.ImportRowError;
import com.example.be.dto.response.question.QuestionImportResultResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Giao diện dịch vụ Import hàng loạt câu hỏi TOEIC từ tệp Excel (UC A04 - Luồng 3a).
 */
public interface QuestionImportService {

    /**
     * Sinh tệp Excel (.xlsx) mẫu chứa cấu trúc câu hỏi đơn, chùm câu hỏi và bảng tra cứu Part/Ability hiện tại.
     *
     * @return Mảng byte của file .xlsx
     */
    byte[] generateImportTemplate();

    /**
     * Đọc, kiểm tra toàn bộ dữ liệu từ file Excel và lưu vào ngân hàng theo nguyên tắc All-or-Nothing.
     *
     * @param file Tệp .xlsx do quản trị viên tải lên
     * @return QuestionImportResultResponse chứa số lượng thành công hoặc danh sách lỗi từng dòng
     */
    QuestionImportResultResponse importFromExcel(MultipartFile file);

    /**
     * Xuất danh sách lỗi import ra file Excel (.xlsx) giúp quản trị viên dễ rà soát và chỉnh sửa.
     *
     * @param errors Danh sách lỗi chi tiết
     * @return Mảng byte của file báo cáo lỗi .xlsx
     */
    byte[] exportErrorReport(List<ImportRowError> errors);
}

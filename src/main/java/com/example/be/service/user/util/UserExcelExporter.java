package com.example.be.service.user.util;

import com.example.be.entity.user.TargetProfile;
import com.example.be.entity.user.User;
import com.example.be.enums.user.ELearnerType;
import com.example.be.enums.user.ERole;
import com.example.be.exception.BaseException;
import com.example.be.exception.ErrorCode;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Tiện ích xuất danh sách tài khoản người dùng sang định dạng Excel (.xlsx) bằng Apache POI Streaming.
 * Dùng SXSSFWorkbook để tiết kiệm bộ nhớ khi xuất toàn bộ danh sách khớp bộ lọc.
 */
@Component
public class UserExcelExporter {

    private static final int STREAM_WINDOW_SIZE = 100;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String[] HEADERS = {
            "Mã người dùng",
            "Họ và tên",
            "Email",
            "Vai trò",
            "Mục tiêu AIM",
            "Đối tượng học tập",
            "Ngày đăng ký",
            "Trạng thái"
    };

    /**
     * Tạo nội dung nhị phân của tệp Excel (.xlsx) từ danh sách người dùng.
     *
     * @param users Danh sách thực thể User đã nạp sẵn TargetProfile
     * @return Mảng byte của file .xlsx sẵn sàng trả về cho client tải xuống
     * @throws BaseException Khi quá trình ghi workbook gặp lỗi I/O
     */
    public byte[] exportUsers(List<User> users) {
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(STREAM_WINDOW_SIZE);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            SXSSFSheet sheet = workbook.createSheet("Danh sách người dùng");
            sheet.trackAllColumnsForAutoSizing();

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);

            writeHeaderRow(sheet, headerStyle);
            writeDataRows(sheet, users, dataStyle);

            for (int col = 0; col < HEADERS.length; col++) {
                sheet.autoSizeColumn(col);
                int currentWidth = sheet.getColumnWidth(col);
                // Cộng thêm khoảng đệm cho dễ đọc và giới hạn tối đa tránh tràn cột
                sheet.setColumnWidth(col, Math.min(currentWidth + 1024, 15000));
            }

            workbook.write(outputStream);
            workbook.dispose();
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "Không thể tạo tệp Excel danh sách người dùng");
        }
    }

    private void writeHeaderRow(Sheet sheet, CellStyle headerStyle) {
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < HEADERS.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(HEADERS[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    private void writeDataRows(Sheet sheet, List<User> users, CellStyle dataStyle) {
        if (users == null) {
            return;
        }
        int rowIndex = 1;
        for (User user : users) {
            Row row = sheet.createRow(rowIndex++);
            TargetProfile target = user.getTargetProfile();
            String aimText = target != null && target.getTargetTotalScore() != null
                    ? "AIM " + target.getTargetTotalScore()
                    : "Chưa thiết lập";
            String createdAtText = user.getCreatedAt() != null
                    ? user.getCreatedAt().format(DATE_FORMATTER)
                    : "";
            String statusText = Boolean.TRUE.equals(user.getIsActive())
                    ? "Đang hoạt động"
                    : "Bị tạm khóa";

            setCell(row, 0, user.getId() != null ? String.valueOf(user.getId()) : "", dataStyle);
            setCell(row, 1, sanitizeCellText(user.getFullName()), dataStyle);
            setCell(row, 2, sanitizeCellText(user.getEmail()), dataStyle);
            setCell(row, 3, formatRole(user.getRole()), dataStyle);
            setCell(row, 4, aimText, dataStyle);
            setCell(row, 5, formatLearnerType(user.getLearnerType()), dataStyle);
            setCell(row, 6, createdAtText, dataStyle);
            setCell(row, 7, statusText, dataStyle);
        }
    }

    private void setCell(Row row, int colIndex, String value, CellStyle style) {
        Cell cell = row.createCell(colIndex);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    /**
     * Chống CSV/Excel Formula Injection: thêm dấu nháy đơn nếu chuỗi người dùng nhập bắt đầu bằng ký tự công thức.
     */
    private String sanitizeCellText(String raw) {
        if (raw == null) {
            return "";
        }
        String trimmed = raw.trim();
        if (!trimmed.isEmpty()) {
            char first = trimmed.charAt(0);
            if (first == '=' || first == '+' || first == '-' || first == '@') {
                return "'" + trimmed;
            }
        }
        return trimmed;
    }

    private String formatRole(ERole role) {
        if (role == null) {
            return "";
        }
        return switch (role) {
            case ADMIN -> "Quản trị viên";
            case TEACHER -> "Giáo viên";
            case STUDENT -> "Học viên";
        };
    }

    private String formatLearnerType(ELearnerType learnerType) {
        if (learnerType == null) {
            return "Chưa cập nhật";
        }
        return switch (learnerType) {
            case PRIMARY_STUDENT -> "Học sinh tiểu học";
            case SECONDARY_STUDENT -> "Học sinh THCS";
            case HIGH_SCHOOL_STUDENT -> "Học sinh THPT";
            case UNIVERSITY_STUDENT -> "Sinh viên đại học";
            case WORKING -> "Người đi làm";
            case OTHER -> "Khác";
        };
    }

    private CellStyle createHeaderStyle(SXSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.LEFT);
        applyBorders(style);
        return style;
    }

    private CellStyle createDataStyle(SXSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        applyBorders(style);
        return style;
    }

    private void applyBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }
}

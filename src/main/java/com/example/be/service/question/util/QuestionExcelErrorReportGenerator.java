package com.example.be.service.question.util;

import com.example.be.dto.response.question.ImportRowError;
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
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Tiện ích xuất danh sách lỗi phát hiện khi import Excel câu hỏi ra tệp .xlsx để quản trị viên tải về sửa chữa.
 */
@Component
public class QuestionExcelErrorReportGenerator {

    private static final int STREAM_WINDOW_SIZE = 100;
    private static final String[] HEADERS = {
            "Sheet",
            "Dòng Excel",
            "Mã chùm",
            "Tên câu hỏi",
            "Chi tiết lỗi"
    };

    /**
     * Tạo nội dung nhị phân file Excel (.xlsx) tổng hợp các lỗi import theo từng dòng.
     *
     * @param errors Danh sách lỗi chi tiết
     * @return Mảng byte của file .xlsx
     */
    public byte[] generateErrorReport(List<ImportRowError> errors) {
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(STREAM_WINDOW_SIZE);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            SXSSFSheet sheet = workbook.createSheet("Báo cáo lỗi Import");
            sheet.trackAllColumnsForAutoSizing();

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);

            writeHeaderRow(sheet, headerStyle);
            writeErrorRows(sheet, errors, dataStyle);

            for (int col = 0; col < HEADERS.length; col++) {
                sheet.autoSizeColumn(col);
                int width = sheet.getColumnWidth(col);
                sheet.setColumnWidth(col, Math.min(Math.max(width + 1024, 4000), 24000));
            }

            workbook.write(out);
            workbook.dispose();
            return out.toByteArray();
        } catch (IOException ex) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "Không thể tạo tệp báo cáo lỗi import câu hỏi");
        }
    }

    private void writeHeaderRow(Sheet sheet, CellStyle headerStyle) {
        Row row = sheet.createRow(0);
        for (int i = 0; i < HEADERS.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(HEADERS[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    private void writeErrorRows(Sheet sheet, List<ImportRowError> errors, CellStyle dataStyle) {
        if (errors == null) {
            return;
        }
        int rowIndex = 1;
        for (ImportRowError err : errors) {
            Row row = sheet.createRow(rowIndex++);
            String joinedMessages = err.getMessages() != null ? String.join("\n• ", err.getMessages()) : "";
            if (!joinedMessages.isEmpty()) {
                joinedMessages = "• " + joinedMessages;
            }

            setCell(row, 0, sanitizeCellText(err.getSheet()), dataStyle);
            setCell(row, 1, err.getRowNumber() > 0 ? String.valueOf(err.getRowNumber()) : "—", dataStyle);
            setCell(row, 2, sanitizeCellText(err.getGroupRowId()), dataStyle);
            setCell(row, 3, sanitizeCellText(err.getQuestionName()), dataStyle);
            setCell(row, 4, sanitizeCellText(joinedMessages), dataStyle);
        }
    }

    private void setCell(Row row, int colIndex, String value, CellStyle style) {
        Cell cell = row.createCell(colIndex);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    /**
     * Chống Excel Formula Injection nếu dữ liệu gốc của file upload bắt đầu bằng ký tự công thức.
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

    private CellStyle createHeaderStyle(SXSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_RED.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.LEFT);
        applyBorders(style);
        return style;
    }

    private CellStyle createDataStyle(SXSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.TOP);
        style.setWrapText(true);
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

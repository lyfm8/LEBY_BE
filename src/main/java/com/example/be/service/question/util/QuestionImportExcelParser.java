package com.example.be.service.question.util;

import com.example.be.dto.request.question.ImportGroupItemRowDto;
import com.example.be.dto.request.question.ImportGroupParentRowDto;
import com.example.be.dto.request.question.ImportStandaloneRowDto;
import com.example.be.dto.response.question.ImportRowError;
import com.example.be.exception.BaseException;
import com.example.be.exception.ErrorCode;
import lombok.Builder;
import lombok.Getter;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Tiện ích phân tích tệp Excel (.xlsx) đầu vào thành danh sách DTO trung gian và lỗi cấu trúc.
 * Áp dụng các biện pháp bảo mật chống Zip Bomb và giới hạn tối đa số dòng đọc vào bộ nhớ.
 */
@Component
public class QuestionImportExcelParser {

    public static final long MAX_FILE_SIZE_BYTES = 20L * 1024L * 1024L;
    private static final int MAX_DATA_ROWS = 2000;
    private static final double MIN_INFLATE_RATIO = 0.01d;

    /**
     * Đọc và kiểm tra cấu trúc cơ bản của file Excel upload.
     *
     * @param file Tệp .xlsx từ Multipart request
     * @return ParsedWorkbookResult chứa danh sách dòng đã đọc và các lỗi cấu trúc/header (nếu có)
     */
    public ParsedWorkbookResult parse(MultipartFile file) {
        validateFileMetadata(file);

        // Bảo vệ chống tấn công giải nén Zip Bomb khi đọc tệp OOXML
        ZipSecureFile.setMinInflateRatio(MIN_INFLATE_RATIO);

        try (InputStream is = file.getInputStream();
             XSSFWorkbook workbook = new XSSFWorkbook(is)) {

            DataFormatter formatter = new DataFormatter(Locale.ROOT);
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();

            Sheet standaloneSheet = findSheet(
                    workbook,
                    QuestionImportTemplateSchema.SHEET_STANDALONE,
                    QuestionImportTemplateSchema.SHEET_STANDALONE_ALIAS
            );
            Sheet groupSheet = findSheet(
                    workbook,
                    QuestionImportTemplateSchema.SHEET_GROUP,
                    QuestionImportTemplateSchema.SHEET_GROUP_ALIAS
            );

            List<ImportRowError> structuralErrors = new ArrayList<>();
            if (standaloneSheet == null && groupSheet == null) {
                structuralErrors.add(ImportRowError.builder()
                        .sheet("Cấu trúc tệp")
                        .rowNumber(1)
                        .messages(List.of("Tệp Excel không có sheet '"
                                + QuestionImportTemplateSchema.SHEET_STANDALONE
                                + "' hoặc '"
                                + QuestionImportTemplateSchema.SHEET_GROUP
                                + "' theo đúng biểu mẫu quy định"))
                        .build());
                return ParsedWorkbookResult.builder()
                        .standaloneRows(List.of())
                        .groupRows(List.of())
                        .structuralErrors(structuralErrors)
                        .build();
            }

            List<ImportStandaloneRowDto> standaloneRows = new ArrayList<>();
            if (standaloneSheet != null) {
                boolean validHeader = validateHeaderRow(
                        standaloneSheet,
                        QuestionImportTemplateSchema.SHEET_STANDALONE,
                        QuestionImportTemplateSchema.STANDALONE_HEADERS,
                        formatter,
                        evaluator,
                        structuralErrors
                );
                if (validHeader) {
                    parseStandaloneSheet(standaloneSheet, formatter, evaluator, standaloneRows, structuralErrors);
                }
            }

            List<ParsedGroupSheetRow> groupRows = new ArrayList<>();
            if (groupSheet != null) {
                boolean validHeader = validateHeaderRow(
                        groupSheet,
                        QuestionImportTemplateSchema.SHEET_GROUP,
                        QuestionImportTemplateSchema.GROUP_HEADERS,
                        formatter,
                        evaluator,
                        structuralErrors
                );
                if (validHeader) {
                    parseGroupSheet(groupSheet, formatter, evaluator, groupRows, structuralErrors);
                }
            }

            return ParsedWorkbookResult.builder()
                    .standaloneRows(standaloneRows)
                    .groupRows(groupRows)
                    .structuralErrors(structuralErrors)
                    .build();

        } catch (NotOfficeXmlFileException ex) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "Tệp tải lên không phải định dạng Excel (.xlsx) hợp lệ");
        } catch (IOException | IllegalArgumentException ex) {
            if (ex.getCause() instanceof InvalidFormatException) {
                throw new BaseException(ErrorCode.BAD_REQUEST, "Tệp Excel (.xlsx) bị lỗi định dạng hoặc không thể đọc");
            }
            throw new BaseException(ErrorCode.BAD_REQUEST, "Không thể đọc nội dung tệp Excel tải lên. Vui lòng kiểm tra lại file mẫu");
        }
    }

    private void validateFileMetadata(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "Vui lòng chọn tệp Excel (.xlsx) để import");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "Dung lượng tệp Excel không được vượt quá 20MB");
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.trim().toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "Hệ thống chỉ hỗ trợ nhập dữ liệu từ tệp định dạng .xlsx");
        }
    }

    private Sheet findSheet(XSSFWorkbook workbook, String primaryName, String aliasName) {
        String normPrimary = normalizeSheetName(primaryName);
        String normAlias = normalizeSheetName(aliasName);
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet sheet = workbook.getSheetAt(i);
            String current = normalizeSheetName(sheet.getSheetName());
            if (current.equals(normPrimary) || current.equals(normAlias)) {
                return sheet;
            }
        }
        return null;
    }

    private boolean validateHeaderRow(
            Sheet sheet,
            String displaySheetName,
            List<String> expectedHeaders,
            DataFormatter formatter,
            FormulaEvaluator evaluator,
            List<ImportRowError> structuralErrors
    ) {
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) {
            structuralErrors.add(ImportRowError.builder()
                    .sheet(displaySheetName)
                    .rowNumber(1)
                    .messages(List.of("Thiếu dòng tiêu đề (dòng 1) trên sheet '" + displaySheetName + "'"))
                    .build());
            return false;
        }

        List<String> headerMismatches = new ArrayList<>();
        for (int i = 0; i < expectedHeaders.size(); i++) {
            String expected = expectedHeaders.get(i);
            String actual = readCellString(headerRow, i, formatter, evaluator);
            if (!normalizeHeader(actual).equals(normalizeHeader(expected))) {
                headerMismatches.add("Cột " + (i + 1) + " kỳ vọng '" + expected + "' nhưng nhận được '"
                        + (actual.isEmpty() ? "(trống)" : actual) + "'");
            }
        }

        if (!headerMismatches.isEmpty()) {
            structuralErrors.add(ImportRowError.builder()
                    .sheet(displaySheetName)
                    .rowNumber(1)
                    .messages(headerMismatches)
                    .build());
            return false;
        }
        return true;
    }

    private void parseStandaloneSheet(
            Sheet sheet,
            DataFormatter formatter,
            FormulaEvaluator evaluator,
            List<ImportStandaloneRowDto> output,
            List<ImportRowError> structuralErrors
    ) {
        int colCount = QuestionImportTemplateSchema.STANDALONE_HEADERS.size();
        int lastRow = sheet.getLastRowNum();
        int dataRowsRead = 0;

        for (int rowIndex = 1; rowIndex <= lastRow; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (isRowCompletelyEmpty(row, colCount, formatter, evaluator)) {
                continue;
            }

            String name = readCellString(row, 0, formatter, evaluator);
            if (isExampleCell(name)) {
                continue;
            }

            dataRowsRead++;
            if (dataRowsRead > MAX_DATA_ROWS) {
                structuralErrors.add(ImportRowError.builder()
                        .sheet(QuestionImportTemplateSchema.SHEET_STANDALONE)
                        .rowNumber(rowIndex + 1)
                        .messages(List.of("Số lượng dòng dữ liệu vượt quá giới hạn tối đa " + MAX_DATA_ROWS + " dòng mỗi lần import"))
                        .build());
                return;
            }

            output.add(ImportStandaloneRowDto.builder()
                    .rowNumber(rowIndex + 1)
                    .name(name)
                    .rawPartId(readCellString(row, 1, formatter, evaluator))
                    .rawAbilityIds(readCellString(row, 2, formatter, evaluator))
                    .rawDifficulty(readCellString(row, 3, formatter, evaluator))
                    .text(readCellString(row, 4, formatter, evaluator))
                    .optionA(readCellString(row, 5, formatter, evaluator))
                    .optionB(readCellString(row, 6, formatter, evaluator))
                    .optionC(readCellString(row, 7, formatter, evaluator))
                    .optionD(readCellString(row, 8, formatter, evaluator))
                    .correctKey(readCellString(row, 9, formatter, evaluator))
                    .rawMediaIds(readCellString(row, 10, formatter, evaluator))
                    .explTranscript(readCellString(row, 11, formatter, evaluator))
                    .explTranslation(readCellString(row, 12, formatter, evaluator))
                    .explClue(readCellString(row, 13, formatter, evaluator))
                    .explAnalysis(readCellString(row, 14, formatter, evaluator))
                    .explVocab(readCellString(row, 15, formatter, evaluator))
                    .descriptions(readCellString(row, 16, formatter, evaluator))
                    .build());
        }
    }

    private void parseGroupSheet(
            Sheet sheet,
            DataFormatter formatter,
            FormulaEvaluator evaluator,
            List<ParsedGroupSheetRow> output,
            List<ImportRowError> structuralErrors
    ) {
        int colCount = QuestionImportTemplateSchema.GROUP_HEADERS.size();
        int lastRow = sheet.getLastRowNum();
        int dataRowsRead = 0;

        for (int rowIndex = 1; rowIndex <= lastRow; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (isRowCompletelyEmpty(row, colCount, formatter, evaluator)) {
                continue;
            }

            String groupRowId = readCellString(row, 0, formatter, evaluator);
            if (isExampleCell(groupRowId)) {
                continue;
            }

            dataRowsRead++;
            if (dataRowsRead > MAX_DATA_ROWS) {
                structuralErrors.add(ImportRowError.builder()
                        .sheet(QuestionImportTemplateSchema.SHEET_GROUP)
                        .rowNumber(rowIndex + 1)
                        .messages(List.of("Số lượng dòng dữ liệu vượt quá giới hạn tối đa " + MAX_DATA_ROWS + " dòng mỗi lần import"))
                        .build());
                return;
            }

            String rawRowType = readCellString(row, 1, formatter, evaluator);
            GroupRowKind kind = resolveGroupRowKind(rawRowType);

            ImportGroupParentRowDto parentDto = null;
            ImportGroupItemRowDto itemDto = null;

            if (kind == GroupRowKind.GROUP) {
                parentDto = ImportGroupParentRowDto.builder()
                        .rowNumber(rowIndex + 1)
                        .groupRowId(groupRowId)
                        .name(readCellString(row, 2, formatter, evaluator))
                        .rawPartId(readCellString(row, 3, formatter, evaluator))
                        .instruction(readCellString(row, 4, formatter, evaluator))
                        .passage(readCellString(row, 5, formatter, evaluator))
                        .transcript(readCellString(row, 6, formatter, evaluator))
                        .translation(readCellString(row, 7, formatter, evaluator))
                        .rawMediaIds(readCellString(row, 8, formatter, evaluator))
                        .descriptions(readCellString(row, 9, formatter, evaluator))
                        .build();
            } else if (kind == GroupRowKind.ITEM) {
                itemDto = ImportGroupItemRowDto.builder()
                        .rowNumber(rowIndex + 1)
                        .groupRowId(groupRowId)
                        .name(readCellString(row, 10, formatter, evaluator))
                        .rawOrderNo(readCellString(row, 11, formatter, evaluator))
                        .text(readCellString(row, 12, formatter, evaluator))
                        .rawAbilityIds(readCellString(row, 13, formatter, evaluator))
                        .rawDifficulty(readCellString(row, 14, formatter, evaluator))
                        .optionA(readCellString(row, 15, formatter, evaluator))
                        .optionB(readCellString(row, 16, formatter, evaluator))
                        .optionC(readCellString(row, 17, formatter, evaluator))
                        .optionD(readCellString(row, 18, formatter, evaluator))
                        .correctKey(readCellString(row, 19, formatter, evaluator))
                        .rawMediaIds(readCellString(row, 20, formatter, evaluator))
                        .explTranscript(readCellString(row, 21, formatter, evaluator))
                        .explTranslation(readCellString(row, 22, formatter, evaluator))
                        .explClue(readCellString(row, 23, formatter, evaluator))
                        .explAnalysis(readCellString(row, 24, formatter, evaluator))
                        .explVocab(readCellString(row, 25, formatter, evaluator))
                        .build();
            }

            output.add(ParsedGroupSheetRow.builder()
                    .rowNumber(rowIndex + 1)
                    .groupRowId(groupRowId)
                    .rawRowType(rawRowType)
                    .kind(kind)
                    .parentRow(parentDto)
                    .itemRow(itemDto)
                    .build());
        }
    }

    private GroupRowKind resolveGroupRowKind(String rawRowType) {
        String normalized = stripAccentsAndUpper(rawRowType);
        if ("CHUM".equals(normalized) || "GROUP".equals(normalized)) {
            return GroupRowKind.GROUP;
        }
        if ("CAU CON".equals(normalized) || "ITEM".equals(normalized)) {
            return GroupRowKind.ITEM;
        }
        return GroupRowKind.UNKNOWN;
    }

    private boolean isRowCompletelyEmpty(
            Row row,
            int colCount,
            DataFormatter formatter,
            FormulaEvaluator evaluator
    ) {
        if (row == null) {
            return true;
        }
        for (int i = 0; i < colCount; i++) {
            String value = readCellString(row, i, formatter, evaluator);
            if (!value.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private String readCellString(Row row, int colIndex, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (row == null) {
            return "";
        }
        Cell cell = row.getCell(colIndex);
        if (cell == null) {
            return "";
        }
        String formatted = formatter.formatCellValue(cell, evaluator);
        return formatted != null ? formatted.trim() : "";
    }

    private boolean isExampleCell(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String upper = value.trim().toUpperCase(Locale.ROOT);
        return upper.startsWith(QuestionImportTemplateSchema.EXAMPLE_PREFIX);
    }

    private String normalizeSheetName(String name) {
        return stripAccentsAndUpper(name);
    }

    private String normalizeHeader(String header) {
        if (header == null) {
            return "";
        }
        return header.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private String stripAccentsAndUpper(String input) {
        if (input == null) {
            return "";
        }
        String decomposed = Normalizer.normalize(input.trim(), Normalizer.Form.NFD);
        String withoutAccents = decomposed.replaceAll("\\p{M}+", "")
                .replace('đ', 'd')
                .replace('Đ', 'D');
        return withoutAccents.replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }

    public enum GroupRowKind {
        GROUP,
        ITEM,
        UNKNOWN
    }

    @Getter
    @Builder
    public static class ParsedGroupSheetRow {
        private final int rowNumber;
        private final String groupRowId;
        private final String rawRowType;
        private final GroupRowKind kind;
        private final ImportGroupParentRowDto parentRow;
        private final ImportGroupItemRowDto itemRow;
    }

    @Getter
    @Builder
    public static class ParsedWorkbookResult {
        private final List<ImportStandaloneRowDto> standaloneRows;
        private final List<ParsedGroupSheetRow> groupRows;
        private final List<ImportRowError> structuralErrors;
    }
}

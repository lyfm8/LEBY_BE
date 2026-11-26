package com.example.be.service.question.util;

import com.example.be.entity.ability.Ability;
import com.example.be.entity.content.Part;
import com.example.be.exception.BaseException;
import com.example.be.exception.ErrorCode;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Tiện ích sinh tệp Excel (.xlsx) mẫu phục vụ tính năng Import câu hỏi hàng loạt (UC A04 - Luồng 3a).
 * Bao gồm 3 sheet: Câu hỏi đơn, Chùm câu hỏi và Hướng dẫn kèm bảng tra cứu Part/Ability động từ DB.
 */
@Component
public class QuestionExcelTemplateGenerator {

    private static final int VALIDATION_MAX_ROW = 1000;

    /**
     * Tạo tệp Excel mẫu (.xlsx) hoàn chỉnh với dữ liệu tra cứu Part và Ability hiện hành.
     *
     * @param parts     Danh sách các Part trong hệ thống
     * @param abilities Danh sách các Ability trong hệ thống
     * @return Mảng byte của file .xlsx
     */
    public byte[] generateTemplate(List<Part> parts, List<Ability> abilities) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            TemplateStyles styles = new TemplateStyles(workbook);

            buildStandaloneSheet(workbook, styles);
            buildGroupSheet(workbook, styles);
            buildGuideSheet(workbook, styles, parts, abilities);

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "Không thể khởi tạo file Excel mẫu import câu hỏi");
        }
    }

    private void buildStandaloneSheet(XSSFWorkbook workbook, TemplateStyles styles) {
        Sheet sheet = workbook.createSheet(QuestionImportTemplateSchema.SHEET_STANDALONE);
        sheet.createFreezePane(0, 1);

        writeHeaders(sheet, QuestionImportTemplateSchema.STANDALONE_HEADERS, styles);

        // Dòng 2: Ví dụ Part 5 (Reading - không bắt buộc audio)
        List<String> part5Example = List.of(
                "[VÍ DỤ] Q-P5-001 Chia thì động từ",
                "5",
                "11",
                "2",
                "Ms. Tanaka _____ the quarterly financial report before the board meeting tomorrow.",
                "will review",
                "reviewing",
                "has reviewed",
                "review",
                "A",
                "",
                "",
                "Bà Tanaka sẽ xem xét báo cáo tài chính quý trước cuộc họp hội đồng quản trị vào ngày mai.",
                "before the board meeting tomorrow",
                "Mệnh đề chỉ thời gian tương lai 'tomorrow' -> chọn thì tương lai đơn 'will review'.",
                "quarterly:adj:hàng quý;financial report:n:báo cáo tài chính",
                "Dòng ví dụ mẫu có tiền tố [VÍ DỤ] sẽ được hệ thống tự động bỏ qua khi import"
        );
        writeRow(sheet, 1, part5Example, styles.exampleRow);

        // Dòng 3: Ví dụ Part 1 (Listening - kèm audio & ảnh minh họa)
        List<String> part1Example = List.of(
                "[VÍ DỤ] Q-P1-002 Mô tả tranh phòng họp",
                "1",
                "1",
                "2",
                "Look at the photograph and choose the statement that best describes what you see.",
                "(A) Engineers are reviewing blueprints.",
                "(B) Workers are climbing a ladder.",
                "(C) Boxes are stacked on the floor.",
                "(D) A truck is parked outside.",
                "A",
                "101|102",
                "(A) Engineers are reviewing blueprints. (B) ...",
                "Các kỹ sư đang xem xét bản vẽ thiết kế.",
                "reviewing blueprints",
                "Tranh mô tả hai kỹ sư đang đứng cạnh bàn xem bản vẽ.",
                "blueprint:n:bản vẽ thiết kế;review:v:xem xét",
                "Thay 101|102 bằng ID ảnh và audio thực tế lấy từ popup Upload media"
        );
        writeRow(sheet, 2, part1Example, styles.exampleRow);

        // Data validations
        addListValidation(sheet, 3, new String[]{"1", "2", "3", "4", "5"});
        addListValidation(sheet, 9, new String[]{"A", "B", "C", "D"});

        setColumnWidths(sheet, QuestionImportTemplateSchema.STANDALONE_HEADERS.size());
    }

    private void buildGroupSheet(XSSFWorkbook workbook, TemplateStyles styles) {
        Sheet sheet = workbook.createSheet(QuestionImportTemplateSchema.SHEET_GROUP);
        sheet.createFreezePane(2, 1);

        writeHeaders(sheet, QuestionImportTemplateSchema.GROUP_HEADERS, styles);

        // Ví dụ chùm Part 7 (1 dòng CHÙM + 2 dòng CÂU CON)
        List<String> groupParent = List.of(
                "[VÍ DỤ] G001",
                QuestionImportTemplateSchema.ROW_TYPE_GROUP,
                "Thông báo bảo trì hệ thống máy chủ",
                "7",
                "Questions 1-2 refer to the following notice.",
                "NOTICE: The main server will undergo scheduled maintenance this Saturday from 10:00 PM to 2:00 AM.",
                "",
                "THÔNG BÁO: Máy chủ chính sẽ được bảo trì định kỳ vào Thứ Bảy tuần này từ 22:00 đến 02:00 sáng.",
                "",
                "Ví dụ chùm Part 7 — có thể gắn thêm ID ảnh bài đọc ở cột Mã Media chung nếu muốn",
                "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", ""
        );
        writeRow(sheet, 1, groupParent, styles.exampleGroupRow);

        List<String> groupItem1 = List.of(
                "[VÍ DỤ] G001",
                QuestionImportTemplateSchema.ROW_TYPE_ITEM,
                "", "", "", "", "", "", "", "",
                "Câu 1: Khi nào máy chủ được bảo trì?",
                "1",
                "When will the server maintenance take place?",
                "15",
                "2",
                "Friday morning",
                "Saturday night",
                "Sunday afternoon",
                "Monday morning",
                "B",
                "",
                "",
                "Việc bảo trì máy chủ diễn ra vào lúc nào?",
                "this Saturday from 10:00 PM",
                "Đoạn thông báo nêu rõ 10:00 PM thứ Bảy tức tối thứ Bảy.",
                "maintenance:n:bảo trì;scheduled:adj:theo lịch trình"
        );
        writeRow(sheet, 2, groupItem1, styles.exampleRow);

        List<String> groupItem2 = List.of(
                "[VÍ DỤ] G001",
                QuestionImportTemplateSchema.ROW_TYPE_ITEM,
                "", "", "", "", "", "", "", "",
                "Câu 2: Quá trình bảo trì kéo dài bao lâu?",
                "2",
                "How long is the maintenance expected to last?",
                "15",
                "3",
                "Two hours",
                "Three hours",
                "Four hours",
                "Five hours",
                "C",
                "",
                "",
                "Quá trình bảo trì dự kiến kéo dài bao lâu?",
                "from 10:00 PM to 2:00 AM",
                "Từ 22:00 đêm đến 02:00 sáng là 4 tiếng (Four hours).",
                "take place:phrase:diễn ra"
        );
        writeRow(sheet, 3, groupItem2, styles.exampleRow);

        // Data validations
        addListValidation(sheet, 1, new String[]{
                QuestionImportTemplateSchema.ROW_TYPE_GROUP,
                QuestionImportTemplateSchema.ROW_TYPE_ITEM
        });
        addListValidation(sheet, 14, new String[]{"1", "2", "3", "4", "5"});
        addListValidation(sheet, 19, new String[]{"A", "B", "C", "D"});

        setColumnWidths(sheet, QuestionImportTemplateSchema.GROUP_HEADERS.size());
    }

    private void buildGuideSheet(
            XSSFWorkbook workbook,
            TemplateStyles styles,
            List<Part> parts,
            List<Ability> abilities
    ) {
        Sheet sheet = workbook.createSheet(QuestionImportTemplateSchema.SHEET_GUIDE);

        int rowIdx = 0;
        Row titleRow = sheet.createRow(rowIdx++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("HƯỚNG DẪN NHẬP CÂU HỎI HÀNG LOẠT VÀ BẢNG TRA CỨU MÃ PART / ABILITY");
        titleCell.setCellStyle(styles.guideTitle);

        rowIdx++;

        String[][] rules = {
                {"1. Chuẩn bị Media trước khi điền Excel",
                        "Tại màn hình Ngân hàng câu hỏi hoặc hộp thoại Import, nhấn nút 'Upload media' để tải ảnh/audio lên Cloudinary. Sao chép Media ID gắn vào cột 'Mã Media đính kèm' (nếu có nhiều file, phân cách bằng dấu |, ví dụ: 101|102)."},
                {"2. Quy định Part & Media",
                        "Part 1, 2 (Listening - Sheet Câu hỏi đơn) và Part 3, 4 (Listening - Sheet Chùm câu hỏi) BẮT BUỘC có 1 tệp Audio. Part 5, 6, 7 (Reading) KHÔNG được gắn Audio."},
                {"3. Cách điền Sheet 'Chùm câu hỏi'",
                        "Mỗi chùm gồm 1 dòng có Loại dòng = 'CHÙM' (chứa ngữ cảnh chung) và từ 2 đến 5 dòng có Loại dòng = 'CÂU CON'. Các dòng thuộc cùng chùm phải điền giống nhau cột 'Mã chùm' (vd: G001)."},
                {"4. Định dạng cột 'Từ vựng'",
                        "Ghi theo cú pháp từ:loại:nghĩa và ngăn cách nhiều từ bằng dấu chấm phẩy (;). Ví dụ: arrange:v:sắp xếp;urgent:adj:khẩn cấp"},
                {"5. Dòng ví dụ mẫu",
                        "Các dòng có tiền tố '[VÍ DỤ]' ở cột đầu tiên sẽ được hệ thống tự động bỏ qua khi import. Bạn có thể giữ lại để tham khảo hoặc ghi đè dữ liệu thật (xóa chữ [VÍ DỤ])."},
                {"6. Xử lý lỗi All-or-Nothing",
                        "Nếu bất kỳ dòng nào sai dữ liệu (sai mã Part, Ability không thuộc Part, thiếu audio Listening...), hệ thống sẽ từ chối lưu toàn bộ file và xuất bảng báo cáo chi tiết từng dòng lỗi để bạn chỉnh sửa."}
        };

        for (String[] rule : rules) {
            Row r = sheet.createRow(rowIdx++);
            Cell c0 = r.createCell(0);
            c0.setCellValue(rule[0]);
            c0.setCellStyle(styles.guideLabel);

            Cell c1 = r.createCell(1);
            c1.setCellValue(rule[1]);
            c1.setCellStyle(styles.guideText);
        }

        rowIdx++;

        // Bảng tra cứu Parts
        Row partTitle = sheet.createRow(rowIdx++);
        Cell ptCell = partTitle.createCell(0);
        ptCell.setCellValue("BẢNG TRA CỨU MÃ PHẦN THI (PART ID)");
        ptCell.setCellStyle(styles.guideTitle);

        writeRow(sheet, rowIdx++, List.of("Part ID", "Tên Phần thi", "Section", "Sheet áp dụng"), styles.requiredHeader);
        for (Part part : parts) {
            String sheetHint = isGroupPart(part.getName()) ? "Chùm câu hỏi" : "Câu hỏi đơn";
            writeRow(
                    sheet,
                    rowIdx++,
                    List.of(
                            String.valueOf(part.getId()),
                            part.getName(),
                            part.getSections() != null ? part.getSections().name() : "",
                            sheetHint
                    ),
                    styles.guideText
            );
        }

        rowIdx++;

        // Bảng tra cứu Abilities
        Row abilityTitle = sheet.createRow(rowIdx++);
        Cell abCell = abilityTitle.createCell(0);
        abCell.setCellValue("BẢNG TRA CỨU MÃ KỸ NĂNG (ABILITY ID)");
        abCell.setCellStyle(styles.guideTitle);

        writeRow(sheet, rowIdx++, List.of("Ability ID", "Tên kỹ năng (Ability)", "Part ID tương ứng", "Tên Part"), styles.requiredHeader);
        for (Ability ability : abilities) {
            String partIdStr = ability.getPart() != null ? String.valueOf(ability.getPart().getId()) : "";
            String partNameStr = ability.getPart() != null ? ability.getPart().getName() : "";
            writeRow(
                    sheet,
                    rowIdx++,
                    List.of(
                            String.valueOf(ability.getId()),
                            ability.getName(),
                            partIdStr,
                            partNameStr
                    ),
                    styles.guideText
            );
        }

        sheet.setColumnWidth(0, 9500);
        sheet.setColumnWidth(1, 22000);
        sheet.setColumnWidth(2, 6000);
        sheet.setColumnWidth(3, 8500);
    }

    private boolean isGroupPart(String partName) {
        if (partName == null) {
            return false;
        }
        return partName.contains("Part 3")
                || partName.contains("Part 4")
                || partName.contains("Part 6")
                || partName.contains("Part 7");
    }

    private void writeHeaders(Sheet sheet, List<String> headers, TemplateStyles styles) {
        Row headerRow = sheet.createRow(0);
        headerRow.setHeightInPoints(34);
        for (int i = 0; i < headers.size(); i++) {
            String header = headers.get(i);
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(header);
            cell.setCellStyle(header.contains("*") ? styles.requiredHeader : styles.optionalHeader);
        }
    }

    private void writeRow(Sheet sheet, int rowIndex, List<String> values, CellStyle style) {
        Row row = sheet.createRow(rowIndex);
        for (int i = 0; i < values.size(); i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(values.get(i) != null ? values.get(i) : "");
            cell.setCellStyle(style);
        }
    }

    private void addListValidation(Sheet sheet, int colIndex, String[] options) {
        DataValidationHelper helper = sheet.getDataValidationHelper();
        DataValidationConstraint constraint = helper.createExplicitListConstraint(options);
        CellRangeAddressList regions = new CellRangeAddressList(1, VALIDATION_MAX_ROW, colIndex, colIndex);
        DataValidation validation = helper.createValidation(constraint, regions);
        validation.setSuppressDropDownArrow(true);
        validation.setShowErrorBox(true);
        sheet.addValidationData(validation);
    }

    private void setColumnWidths(Sheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            int width = (i == 0 || i == 4 || i == 5 || i == 6 || i == 14 || i == 15) ? 8500 : 5800;
            sheet.setColumnWidth(i, width);
        }
    }

    private static final class TemplateStyles {
        private final CellStyle requiredHeader;
        private final CellStyle optionalHeader;
        private final CellStyle exampleRow;
        private final CellStyle exampleGroupRow;
        private final CellStyle guideTitle;
        private final CellStyle guideLabel;
        private final CellStyle guideText;

        private TemplateStyles(XSSFWorkbook wb) {
            this.requiredHeader = createHeaderStyle(wb, IndexedColors.DARK_RED);
            this.optionalHeader = createHeaderStyle(wb, IndexedColors.DARK_BLUE);
            this.exampleRow = createDataStyle(wb, IndexedColors.GREY_25_PERCENT, true);
            this.exampleGroupRow = createDataStyle(wb, IndexedColors.LIGHT_CORNFLOWER_BLUE, true);
            this.guideTitle = createGuideTitleStyle(wb);
            this.guideLabel = createGuideLabelStyle(wb);
            this.guideText = createDataStyle(wb, IndexedColors.WHITE, false);
        }

        private static CellStyle createHeaderStyle(XSSFWorkbook wb, IndexedColors bgColor) {
            CellStyle style = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            font.setColor(IndexedColors.WHITE.getIndex());
            style.setFont(font);
            style.setFillForegroundColor(bgColor.getIndex());
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            style.setAlignment(HorizontalAlignment.CENTER);
            style.setVerticalAlignment(VerticalAlignment.CENTER);
            style.setWrapText(true);
            applyBorders(style);
            return style;
        }

        private static CellStyle createDataStyle(XSSFWorkbook wb, IndexedColors bgColor, boolean italic) {
            CellStyle style = wb.createCellStyle();
            Font font = wb.createFont();
            font.setItalic(italic);
            if (italic) {
                font.setColor(IndexedColors.GREY_80_PERCENT.getIndex());
            }
            style.setFont(font);
            if (bgColor != IndexedColors.WHITE) {
                style.setFillForegroundColor(bgColor.getIndex());
                style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            }
            style.setVerticalAlignment(VerticalAlignment.TOP);
            style.setWrapText(true);
            applyBorders(style);
            return style;
        }

        private static CellStyle createGuideTitleStyle(XSSFWorkbook wb) {
            CellStyle style = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            font.setFontHeightInPoints((short) 12);
            font.setColor(IndexedColors.DARK_BLUE.getIndex());
            style.setFont(font);
            return style;
        }

        private static CellStyle createGuideLabelStyle(XSSFWorkbook wb) {
            CellStyle style = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            style.setFont(font);
            style.setFillForegroundColor(IndexedColors.LEMON_CHIFFON.getIndex());
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            style.setVerticalAlignment(VerticalAlignment.TOP);
            style.setWrapText(true);
            applyBorders(style);
            return style;
        }

        private static void applyBorders(CellStyle style) {
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
        }
    }
}

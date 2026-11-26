package com.example.be.service.question.util;

import java.util.List;

/**
 * Định nghĩa cấu trúc chuẩn dùng chung giữa bộ sinh file Excel mẫu và bộ đọc/đối soát file Excel import.
 * Toàn bộ tiêu đề cột sử dụng nhãn hiển thị thân thiện tương ứng với giao diện quản trị viên.
 */
public final class QuestionImportTemplateSchema {

    public static final String SHEET_STANDALONE = "Câu hỏi đơn";
    public static final String SHEET_STANDALONE_ALIAS = "STANDALONE";

    public static final String SHEET_GROUP = "Chùm câu hỏi";
    public static final String SHEET_GROUP_ALIAS = "GROUP";

    public static final String SHEET_GUIDE = "Hướng dẫn & Tra cứu";

    public static final String EXAMPLE_PREFIX = "[VÍ DỤ]";
    public static final String ROW_TYPE_GROUP = "CHÙM";
    public static final String ROW_TYPE_GROUP_ALIAS = "GROUP";
    public static final String ROW_TYPE_ITEM = "CÂU CON";
    public static final String ROW_TYPE_ITEM_ALIAS = "ITEM";

    public static final List<String> STANDALONE_HEADERS = List.of(
            "Mã gợi nhớ câu hỏi *",
            "Mã Phần thi - Part *",
            "Mã kỹ năng - Ability (cách nhau dấu |) *",
            "Độ khó (1-5) *",
            "Nội dung câu hỏi",
            "Phương án A *",
            "Phương án B *",
            "Phương án C *",
            "Phương án D",
            "Đáp án đúng*",
            "Mã Media đính kèm (cách nhau dấu |)",
            "Lời thoại (Transcript)",
            "Bản dịch tiếng Việt",
            "Manh mối (Clue)",
            "Phân tích đáp án",
            "Từ vựng (từ:loại:nghĩa; ...)",
            "Ghi chú"
    );

    public static final List<String> GROUP_HEADERS = List.of(
            "Mã chùm (vd: G001) *",
            "Loại dòng (CHÙM / CÂU CON) *",
            "Tên chùm câu hỏi *",
            "Mã Phần thi (Part) *",
            "Lời hướng dẫn chung",
            "Đoạn văn đọc hiểu chung (Part 6, 7)",
            "Lời thoại bài nghe chung (Part 3, 4)",
            "Bản dịch tiếng Việt",
            "Mã Media chung (cách nhau dấu |)",
            "Ghi chú",
            "[Câu con] Tên câu con *",
            "[Câu con] Thứ tự câu (1-5)",
            "[Câu con] Nội dung đề bài",
            "[Câu con] Mã kỹ năng - Ability (cách nhau dấu |) *",
            "[Câu con] Độ khó (1-5) *",
            "[Câu con] Phương án A *",
            "[Câu con] Phương án B *",
            "[Câu con] Phương án C *",
            "[Câu con] Phương án D",
            "[Câu con] Đáp án đúng *",
            "[Câu con] Mã Media riêng (cách nhau dấu |)",
            "[Câu con] Transcript",
            "[Câu con] Bản dịch tiếng Việt",
            "[Câu con] Manh mối (Clue)",
            "[Câu con] Phân tích đáp án",
            "[Câu con] Từ vựng (từ:loại:nghĩa; ...)"
    );

    private QuestionImportTemplateSchema() {
        // Constants holder
    }
}

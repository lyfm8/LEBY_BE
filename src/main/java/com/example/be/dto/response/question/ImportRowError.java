package com.example.be.dto.response.question;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Chi tiết lỗi phát hiện trên một dòng Excel trong quá trình import hàng loạt.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportRowError {

    @NotBlank(message = "Tên sheet không được để trống")
    @Size(max = 100, message = "Tên sheet tối đa 100 ký tự")
    private String sheet;

    private int rowNumber;

    @Size(max = 100, message = "Mã chùm tối đa 100 ký tự")
    private String groupRowId;

    @Size(max = 255, message = "Tên câu hỏi tối đa 255 ký tự")
    private String questionName;

    @NotEmpty(message = "Danh sách mô tả lỗi không được rỗng")
    @Size(max = 50, message = "Số lượng thông báo lỗi mỗi dòng tối đa 50")
    private List<String> messages;
}

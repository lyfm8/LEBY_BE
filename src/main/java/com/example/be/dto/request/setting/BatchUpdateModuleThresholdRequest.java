package com.example.be.dto.request.setting;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchUpdateModuleThresholdRequest {

    @NotEmpty(message = "Danh sách cập nhật không được rỗng")
    @Size(max = 500, message = "Số lượng cấu hình cập nhật tối đa là 500 phần tử mỗi lần")
    private List<@Valid ModuleThresholdItemDto> items;
}

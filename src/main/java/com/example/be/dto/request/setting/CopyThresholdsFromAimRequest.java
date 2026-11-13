package com.example.be.dto.request.setting;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO yêu cầu sao chép toàn bộ cấu hình ngưỡng (Part & Ability) từ một AIM nguồn sang AIM đích.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CopyThresholdsFromAimRequest {

    @NotNull(message = "ID mục tiêu AIM đích không được để trống")
    private Long targetProfileId;

    @NotNull(message = "ID mục tiêu AIM nguồn không được để trống")
    private Long sourceProfileId;
}


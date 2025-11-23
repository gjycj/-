package com.house.deed.pavilion.module.housePriceLog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

// 配套的DTO类（HousePriceLogDTO）
@Data
@Schema(description = "房源价格变动记录请求参数")
public class HousePriceLogDTO {
    @Schema(description = "房源ID", example = "1001", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long houseId;

    @Schema(description = "变动前价格（万元）", example = "580.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal oldPrice;

    @Schema(description = "变动后价格（万元）", example = "560.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal newPrice;

    @Schema(description = "价格变动原因", example = "业主降价出售", requiredMode = Schema.RequiredMode.REQUIRED)
    private String changeReason;
}
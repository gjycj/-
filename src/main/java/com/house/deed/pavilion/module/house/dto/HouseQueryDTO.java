package com.house.deed.pavilion.module.house.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "房源查询条件DTO")
public class HouseQueryDTO {

    @Schema(description = "房号（模糊查询）")
    private String houseNo;

    @Schema(description = "房源状态（精确匹配，可选值：ON_SALE/RESERVED/SOLD/OFF_SHELF）")
    private String status;

    @Schema(description = "交易类型（SALE-出售，RENT-出租，BOTH-可售可租）")
    private String transactionType;

    // 可根据业务需求补充其他筛选字段（如面积范围、价格范围等）
}
package com.house.deed.pavilion.module.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "客户查询条件DTO")
public class CustomerQueryDTO {

    @Schema(description = "客户姓名（模糊查询）")
    private String name;

    @Schema(description = "客户状态（如：NORMAL-正常，INVALID-失效）")
    private String status;

    @Schema(description = "客户类型（如：PERSONAL-个人，COMPANY-企业）")
    private String type;

    // 可根据业务需求补充其他筛选字段（如联系方式、意向房源类型等）
}
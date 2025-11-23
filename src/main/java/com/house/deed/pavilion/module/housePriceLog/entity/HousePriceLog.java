package com.house.deed.pavilion.module.housePriceLog.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("house_price_log")
@Schema(description = "房源价格变动记录表（租户级数据）")
public class HousePriceLog {

    @TableId(type = IdType.AUTO)
    @Schema(description = "日志ID", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    @Schema(description = "租户ID（归属租户）", accessMode = Schema.AccessMode.READ_ONLY)
    private Long tenantId;

    @TableField("house_id")
    @Schema(description = "房源ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long houseId;

    @TableField("price_before")
    @Schema(description = "调整前价格", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal priceBefore;

    @TableField("price_after")
    @Schema(description = "调整后价格", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal priceAfter;

    @TableField("change_reason")
    @Schema(description = "调价原因", requiredMode = Schema.RequiredMode.REQUIRED)
    private String changeReason;

    @TableField("operator_id")
    @Schema(description = "操作人ID（经纪人）", accessMode = Schema.AccessMode.READ_ONLY)
    private Long operatorId;

    @TableField("operator_name")
    @Schema(description = "操作人姓名", accessMode = Schema.AccessMode.READ_ONLY)
    private String operatorName;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "调价时间", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createTime;
}
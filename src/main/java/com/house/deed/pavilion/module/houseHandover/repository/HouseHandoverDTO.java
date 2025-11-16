package com.house.deed.pavilion.module.houseHandover.repository;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "房屋交接记录DTO")
public class HouseHandoverDTO {

    @Schema(description = "交接ID（新增时无需传递）", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Schema(description = "合同ID（关联租赁合同时必填）", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long contractId;

    @Schema(description = "房源ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long houseId;

    @Schema(description = "交接类型（CHECK_IN-入住，CHECK_OUT-退租）",
            allowableValues = {"CHECK_IN", "CHECK_OUT"},
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String handoverType;

    @Schema(description = "交接时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime handoverTime;

    @Schema(description = "家具家电清单（JSON格式，如{\"冰箱\":\"海尔\",\"空调\":2台}）")
    private String appliancesList;

    @Schema(description = "水表底数（吨）", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal waterMeter;

    @Schema(description = "电表底数（度）", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal electricityMeter;

    @Schema(description = "燃气表底数（立方米）", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal gasMeter;

    @Schema(description = "房屋损坏记录")
    private String damageRecords;

    @Schema(description = "交接人（房东或代理人）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String handoverPerson;

    @Schema(description = "接收人（租户）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String receiver;

    @Schema(description = "交接确认签字图片URL")
    private String signImageUrl;
}
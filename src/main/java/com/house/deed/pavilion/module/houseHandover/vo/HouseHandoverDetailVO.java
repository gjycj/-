package com.house.deed.pavilion.module.houseHandover.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.house.deed.pavilion.module.maintenanceOrder.entity.MaintenanceOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 房屋交接记录详情VO（含关联维修工单）
 */
@Data
@Schema(description = "房屋交接记录详情（含关联维修工单）")
public class HouseHandoverDetailVO {

    @Schema(description = "交接记录ID")
    private Long id;

    @Schema(description = "租户ID（归属租户）")
    private Long tenantId;

    @Schema(description = "合同ID（关联租赁合同）")
    private Long contractId;

    @Schema(description = "房源ID")
    private Long houseId;

    @Schema(description = "交接类型（CHECK_IN-入住，CHECK_OUT-退租）")
    private String handoverType;

    @Schema(description = "交接时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime handoverTime;

    @Schema(description = "家具家电清单（JSON格式）")
    private String appliancesList;

    @Schema(description = "水表底数（吨）")
    private BigDecimal waterMeter;

    @Schema(description = "电表底数（度）")
    private BigDecimal electricityMeter;

    @Schema(description = "燃气表底数（立方米）")
    private BigDecimal gasMeter;

    @Schema(description = "房屋损坏记录")
    private String damageRecords;

    @Schema(description = "交接人（房东或代理人）")
    private String handoverPerson;

    @Schema(description = "接收人（租户）")
    private String receiver;

    @Schema(description = "交接确认签字图片URL")
    private String signImageUrl;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    // 新增：关联的维修工单列表
    @Schema(description = "关联的维修工单（仅退租交接可能存在）")
    private List<MaintenanceOrder> maintenanceOrders;
}
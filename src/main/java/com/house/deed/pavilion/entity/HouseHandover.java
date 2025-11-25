package com.house.deed.pavilion.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 房屋交接记录表（租户级数据）
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Getter
@Setter
@TableName("house_handover")
@ApiModel(value = "HouseHandover对象", description = "房屋交接记录表（租户级数据）")
public class HouseHandover implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("交接ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("租户ID（归属租户）")
    @TableField("tenant_id")
    private Long tenantId;

    @ApiModelProperty("合同ID（关联contract表，同租户，仅租赁）")
    @TableField("contract_id")
    private Long contractId;

    @ApiModelProperty("房源ID（关联house表，同租户）")
    @TableField("house_id")
    private Long houseId;

    @ApiModelProperty("交接类型（CHECK_IN-入住，CHECK_OUT-退租）")
    @TableField("handover_type")
    private String handoverType;

    @ApiModelProperty("交接时间")
    @TableField("handover_time")
    private LocalDateTime handoverTime;

    @ApiModelProperty("费用结算状态（UNSETTLED-未结算，SETTLED-已结算）")
    @TableField("settlement_status")
    private String settlementStatus;

    @ApiModelProperty("家具家电清单（如{\"冰箱\":\"海尔\",\"空调\":2台}）")
    @TableField("appliances_list")
    private String appliancesList;

    @ApiModelProperty("水表底数（吨）")
    @TableField("water_meter")
    private BigDecimal waterMeter;

    @ApiModelProperty("电表底数（度）")
    @TableField("electricity_meter")
    private BigDecimal electricityMeter;

    @ApiModelProperty("燃气表底数（立方米）")
    @TableField("gas_meter")
    private BigDecimal gasMeter;

    @ApiModelProperty("房屋损坏记录（如墙面划痕）")
    @TableField("damage_records")
    private String damageRecords;

    @ApiModelProperty("交接人（房东或其代理人）")
    @TableField("handover_person")
    private String handoverPerson;

    @ApiModelProperty("接收人（租户）")
    @TableField("receiver")
    private String receiver;

    @ApiModelProperty("交接确认签字图片URL")
    @TableField("sign_image_url")
    private String signImageUrl;

    @ApiModelProperty("交接记录状态（DRAFT-草稿，CONFIRMED-已确认）")
    @TableField("status")
    private String status;

    @TableField("create_time")
    private LocalDateTime createTime;

    @ApiModelProperty("维修结果备注（同步自维修工单）")
    @TableField("maintenance_remark")
    private String maintenanceRemark;

    @ApiModelProperty("维修费用（元）")
    @TableField("maintenance_cost")
    private BigDecimal maintenanceCost;

    @ApiModelProperty("维修费用承担方（LANDLORD/TENANT/SHARED）")
    @TableField("maintenance_bearer")
    private String maintenanceBearer;

    @ApiModelProperty("关联的最后一次维修工单ID")
    @TableField("last_maintenance_id")
    private Long lastMaintenanceId;
}

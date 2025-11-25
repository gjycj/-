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
 * 房源维修工单表（租户级数据）
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Getter
@Setter
@TableName("maintenance_order")
@ApiModel(value = "MaintenanceOrder对象", description = "房源维修工单表（租户级数据）")
public class MaintenanceOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("工单ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("租户ID（归属租户）")
    @TableField("tenant_id")
    private Long tenantId;

    @ApiModelProperty("房源ID（关联house表，同租户）")
    @TableField("house_id")
    private Long houseId;

    @ApiModelProperty("关联合同ID（仅限租赁场景，关联contract表，同租户）")
    @TableField("contract_id")
    private Long contractId;

    @ApiModelProperty("关联的房屋交接记录ID（退租维修时非空，同租户）")
    @TableField("house_handover_id")
    private Long houseHandoverId;

    @ApiModelProperty("工单编号（租户内唯一）")
    @TableField("order_no")
    private String orderNo;

    @ApiModelProperty("报修人类型（TENANT-租户等）")
    @TableField("reporter_type")
    private String reporterType;

    @ApiModelProperty("报修人ID（关联对应表，同租户）")
    @TableField("reporter_id")
    private Long reporterId;

    @ApiModelProperty("报修人电话")
    @TableField("reporter_phone")
    private String reporterPhone;

    @ApiModelProperty("维修类型（WATER-水电，APPLIANCE-家电等）")
    @TableField("maintenance_type")
    private String maintenanceType;

    @ApiModelProperty("故障描述（如空调不制冷）")
    @TableField("description")
    private String description;

    @ApiModelProperty("紧急程度（1-低，2-中，3-高）")
    @TableField("urgency_level")
    private Byte urgencyLevel;

    @ApiModelProperty("维修师傅ID（可关联外部表）")
    @TableField("repairman_id")
    private Long repairmanId;

    @ApiModelProperty("状态（SUBMITTED-已提交等）")
    @TableField("status")
    private String status;

    @ApiModelProperty("预约维修时间")
    @TableField("appointment_time")
    private LocalDateTime appointmentTime;

    @ApiModelProperty("完成时间")
    @TableField("complete_time")
    private LocalDateTime completeTime;

    @ApiModelProperty("维修费用（元）")
    @TableField("cost_amount")
    private BigDecimal costAmount;

    @ApiModelProperty("费用承担方（LANDLORD-房东等）")
    @TableField("cost_bearer")
    private String costBearer;

    @ApiModelProperty("维修结果备注")
    @TableField("remark")
    private String remark;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}

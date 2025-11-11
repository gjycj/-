package com.house.deed.pavilion.module.maintenanceOrder.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 房源维修工单表（租户级数据）
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
@Getter
@Setter
@TableName("maintenance_order")
public class MaintenanceOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 工单ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID（归属租户）
     */
    @TableField("tenant_id")
    private Long tenantId;

    /**
     * 房源ID（关联house表，同租户）
     */
    @TableField("house_id")
    private Long houseId;

    /**
     * 工单编号（租户内唯一）
     */
    @TableField("order_no")
    private String orderNo;

    /**
     * 报修人类型（TENANT-租户等）
     */
    @TableField("reporter_type")
    private String reporterType;

    /**
     * 报修人ID（关联对应表，同租户）
     */
    @TableField("reporter_id")
    private Long reporterId;

    /**
     * 报修人电话
     */
    @TableField("reporter_phone")
    private String reporterPhone;

    /**
     * 维修类型（WATER-水电，APPLIANCE-家电等）
     */
    @TableField("maintenance_type")
    private String maintenanceType;

    /**
     * 故障描述（如空调不制冷）
     */
    @TableField("description")
    private String description;

    /**
     * 紧急程度（1-低，2-中，3-高）
     */
    @TableField("urgency_level")
    private Byte urgencyLevel;

    /**
     * 维修师傅ID（可关联外部表）
     */
    @TableField("repairman_id")
    private Long repairmanId;

    /**
     * 状态（SUBMITTED-已提交等）
     */
    @TableField("status")
    private String status;

    /**
     * 预约维修时间
     */
    @TableField("appointment_time")
    private LocalDateTime appointmentTime;

    /**
     * 完成时间
     */
    @TableField("complete_time")
    private LocalDateTime completeTime;

    /**
     * 维修费用（元）
     */
    @TableField("cost_amount")
    private BigDecimal costAmount;

    /**
     * 费用承担方（LANDLORD-房东等）
     */
    @TableField("cost_bearer")
    private String costBearer;

    /**
     * 维修结果备注
     */
    @TableField("remark")
    private String remark;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}

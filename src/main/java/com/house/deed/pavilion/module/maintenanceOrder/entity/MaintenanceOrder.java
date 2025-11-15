package com.house.deed.pavilion.module.maintenanceOrder.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * <p>
 * 房源维修工单表（租户级数据）
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
@Schema(
        description = "房源维修工单实体类，存储租户级别的维修工单信息，包含报修人、维修类型、状态等核心字段，关联房源表",
        requiredMode = Schema.RequiredMode.REQUIRED
)
@Getter
@Setter
@TableName("maintenance_order")
public class MaintenanceOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 工单ID
     */
    @Schema(
            description = "工单唯一标识（自增主键）",
            example = "10001",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID（归属租户）
     */
    @Schema(
            description = "租户ID（数据隔离字段），由系统自动填充当前登录租户ID，无需手动设置",
            example = "101",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    @TableField("tenant_id")
    private Long tenantId;

    /**
     * 房源ID（关联house表，同租户）
     */
    @Schema(
            description = "关联的房源ID，必须属于当前租户（关联house表id）",
            example = "20001",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @TableField("house_id")
    private Long houseId;

    /**
     * 工单编号（租户内唯一）
     */
    @Schema(
            description = "工单编号，租户内唯一，由系统自动生成（格式：TENANT{租户ID}_MAINT{yyyyMMdd}{3位序号}）",
            example = "TENANT101_MAINT20251107001",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    @TableField("order_no")
    private String orderNo;

    /**
     * 报修人类型（TENANT-租户等）
     */
    @Schema(
            description = "报修人类型",
            example = "TENANT",
            allowableValues = {"TENANT", "LANDLORD", "AGENT"},
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @TableField("reporter_type")
    private String reporterType;

    /**
     * 报修人ID（关联对应表，同租户）
     */
    @Schema(
            description = "报修人ID，关联对应类型的表（如租户表、房东表），需与reporterType匹配",
            example = "30001",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @TableField("reporter_id")
    private Long reporterId;

    /**
     * 报修人电话
     */
    @Schema(
            description = "报修人联系电话，用于维修沟通",
            example = "13800138000",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @TableField("reporter_phone")
    private String reporterPhone;

    /**
     * 维修类型（WATER-水电，APPLIANCE-家电等）
     */
    @Schema(
            description = "维修类型，标识故障所属类别",
            example = "APPLIANCE",
            allowableValues = {"WATER", "ELECTRIC", "APPLIANCE", "STRUCTURE", "OTHER"},
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @TableField("maintenance_type")
    private String maintenanceType;

    /**
     * 故障描述（如空调不制冷）
     */
    @Schema(
            description = "故障详细描述，需说明具体问题现象",
            example = "客厅空调运行时不制冷，外机有异响",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @TableField("description")
    private String description;

    /**
     * 紧急程度（1-低，2-中，3-高）
     */
    @Schema(
            description = "紧急程度等级",
            example = "2",
            allowableValues = {"1", "2", "3"},
            defaultValue = "2",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @TableField("urgency_level")
    private Byte urgencyLevel;

    /**
     * 维修师傅ID（可关联外部表）
     */
    @Schema(
            description = "负责维修的师傅ID，工单分配后填充",
            example = "40001"
    )
    @TableField("repairman_id")
    private Long repairmanId;

    /**
     * 状态（SUBMITTED-已提交等）
     */
    @Schema(
            description = "工单状态",
            example = "SUBMITTED",
            allowableValues = {"SUBMITTED", "ASSIGNED", "IN_PROGRESS", "COMPLETED", "CANCELLED"},
            defaultValue = "SUBMITTED",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @TableField("status")
    private String status;

    /**
     * 预约维修时间
     */
    @Schema(
            description = "预约的维修时间，格式：yyyy-MM-ddTHH:mm:ss",
            example = "2025-11-10T09:30:00"
    )
    @TableField("appointment_time")
    private LocalDateTime appointmentTime;

    /**
     * 完成时间
     */
    @Schema(
            description = "维修完成时间，状态变更为COMPLETED时自动填充，格式：yyyy-MM-ddTHH:mm:ss",
            example = "2025-11-10T11:20:00",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    @TableField("complete_time")
    private LocalDateTime completeTime;

    /**
     * 维修费用（元）
     */
    @Schema(
            description = "维修产生的费用（单位：元），保留两位小数",
            example = "150.50"
    )
    @TableField("cost_amount")
    private BigDecimal costAmount;

    /**
     * 费用承担方（LANDLORD-房东等）
     */
    @Schema(
            description = "维修费用的承担方",
            example = "LANDLORD",
            allowableValues = {"LANDLORD", "TENANT", "SHARED"}
    )
    @TableField("cost_bearer")
    private String costBearer;

    /**
     * 维修结果备注
     */
    @Schema(
            description = "维修完成后的结果说明，如更换零件、故障原因等",
            example = "更换空调压缩机，故障排除"
    )
    @TableField("remark")
    private String remark;

    @Schema(
            description = "创建时间（系统自动填充），格式：yyyy-MM-ddTHH:mm:ss",
            example = "2025-11-07T10:00:00",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    @TableField("create_time")
    private LocalDateTime createTime;

    @Schema(
            description = "更新时间（系统自动填充），格式：yyyy-MM-ddTHH:mm:ss",
            example = "2025-11-07T14:30:00",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    @TableField("update_time")
    private LocalDateTime updateTime;
}
package com.house.deed.pavilion.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 房源维修工单表（租户级数据隔离）
 * <p>
 * 核心业务说明：
 * 1. 维修流程核心：记录租户内房源的维修需求全流程（报修→派单→维修→验收→结算），覆盖租赁/退租/业主报修等场景；
 * 2. 租户隔离：所有字段绑定tenant_id，仅当前租户可查询/操作自身的维修工单，保护维修业务数据隔离性；
 * 3. 多场景关联：
 *    - 基础关联：house_id（关联房源）、contract_id（租赁场景必选）、house_handover_id（退租维修必选）；
 *    - 人员关联：reporter_id（报修人）、repairman_id（维修师傅），支持多角色参与；
 * 4. 工单核心要素：
 *    - 报修信息：reporter_type（报修人类型）、故障描述、紧急程度（1-低/2-中/3-高）；
 *    - 维修类型：区分水电、家电、墙面、管道等细分场景，支撑精准派单；
 *    - 状态流转：SUBMITTED-已提交 → ASSIGNED-已派单 → REPAIRING-维修中 → COMPLETED-已完成 → CANCELED-已取消；
 *    - 费用结算：cost_amount（维修费用）、cost_bearer（费用承担方：房东/租户/共同承担），支撑费用分摊；
 * 5. 数据规范：工单编号租户内唯一，故障描述清晰，状态与时间节点联动（如完成状态需填完成时间），确保维修流程可追溯。
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Getter
@Setter
@TableName(value = "maintenance_order", autoResultMap = true) // 开启自动结果映射
@Schema(
        name = "MaintenanceOrder",
        description = "房源维修工单实体（租户级数据），记录维修需求全流程（报修→派单→维修→结算），支撑维修业务闭环",
        example = "{\"tenantId\": 1001, \"houseId\": 101, \"contractId\": 5001, \"orderNo\": \"MO20251128001\", \"reporterType\": \"TENANT\", \"reporterId\": 4001, \"reporterPhone\": \"13800138000\", \"maintenanceType\": \"APPLIANCE\", \"description\": \"客厅空调不制冷，开机后显示错误代码E1\", \"urgencyLevel\": 2, \"status\": \"SUBMITTED\", \"appointmentTime\": \"2025-11-30 15:00:00\"}"
)
public class MaintenanceOrder implements Serializable {

    /**
     * 序列化版本号：保证对象序列化/反序列化一致性，避免类结构变更导致反序列化异常
     */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 工单主键ID
     * 自增策略，唯一标识单条维修工单，无业务含义，仅作为数据库主键
     */
    @Schema(
            description = "工单主键ID（自增）",
            example = "601",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY // 只读：新增时数据库自增，无需传值
    )
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID
     * 关联租户表主键，标识工单归属的租户，核心隔离字段，非空
     */
    @Schema(
            description = "租户ID（归属租户，关联租户表主键）",
            example = "1001",
            nullable = false
    )
    @NotNull(message = "租户ID不能为空")
    @TableField(value = "tenant_id", exist = true)
    private Long tenantId;

    /**
     * 房源ID
     * 关联house表主键（同租户下的房源），标识需要维修的房源，非空（核心关联字段）
     */
    @Schema(
            description = "房源ID（关联house表，仅同租户下的房源有效）",
            example = "101",
            nullable = false
    )
    @NotNull(message = "房源ID不能为空")
    @TableField(value = "house_id")
    private Long houseId;

    /**
     * 关联合同ID
     * 关联contract表主键（同租户下的租赁合同），租赁场景必填，退租/业主报修可空
     */
    @Schema(
            description = "关联合同ID（租赁场景必填，退租/业主报修可空；仅同租户下的租赁合同有效）",
            example = "5001",
            nullable = true
    )
    @TableField(value = "contract_id")
    private Long contractId;

    /**
     * 关联房屋交接记录ID
     * 关联house_handover表主键（同租户下），退租维修场景必填（如退租时发现墙面损坏），其他场景可空
     */
    @Schema(
            description = "关联房屋交接记录ID（退租维修场景必填，其他场景可空）",
            example = "801",
            nullable = true
    )
    @TableField(value = "house_handover_id")
    private Long houseHandoverId;

    /**
     * 工单编号
     * 租户内唯一标识（格式建议：MO+年月日+3位序号，如MO20251128001），非空，长度≤20字符
     */
    @Schema(
            description = "工单编号（租户内唯一，格式：MO+年月日+3位序号）",
            example = "MO20251128001",
            nullable = false,
            maxLength = 20
    )
    @NotBlank(message = "工单编号不能为空")
    @Size(max = 20, message = "工单编号长度不能超过20字符")
    @TableField(value = "order_no")
    private String orderNo;

    /**
     * 报修人类型
     * 枚举值：TENANT-租户，LANDLORD-房东，AGENT-经纪人，OTHER-其他，非空
     */
    @Schema(
            description = "报修人类型（TENANT=租户，LANDLORD=房东，AGENT=经纪人，OTHER=其他）",
            example = "TENANT",
            nullable = false,
            allowableValues = {"TENANT", "LANDLORD", "AGENT", "OTHER"}
    )
    @NotBlank(message = "报修人类型不能为空")
    @TableField(value = "reporter_type")
    private String reporterType;

    /**
     * 报修人ID
     * 关联对应表主键（租户→tenant表，房东→landlord表，经纪人→agent表），同租户下有效，非空
     */
    @Schema(
            description = "报修人ID（关联对应角色表：租户→tenant表，房东→landlord表，经纪人→agent表）",
            example = "4001",
            nullable = false
    )
    @NotNull(message = "报修人ID不能为空")
    @TableField(value = "reporter_id")
    private Long reporterId;

    /**
     * 报修人电话
     * 报修人常用联系电话（支持11位手机号，含+86前缀），非空，用于维修沟通
     */
    @Schema(
            description = "报修人联系电话（支持11位手机号，含+86前缀）",
            example = "13800138000",
            nullable = false,
            maxLength = 20,
            pattern = "^(\\+86)?1[3-9]\\d{9}$"
    )
    @NotBlank(message = "报修人电话不能为空")
    @Size(max = 20, message = "报修人电话长度不能超过20字符")
    @Pattern(regexp = "^(\\+86)?1[3-9]\\d{9}$", message = "报修人电话格式错误（支持11位手机号，可带+86前缀）")
    @TableField(value = "reporter_phone")
    private String reporterPhone;

    /**
     * 维修类型
     * 枚举值：WATER-水电维修，APPLIANCE-家电维修，WALL-墙面维修，PIPELINE-管道维修，DOOR_WINDOW-门窗维修，OTHER-其他维修
     */
    @Schema(
            description = "维修类型（WATER=水电维修，APPLIANCE=家电维修，WALL=墙面维修，PIPELINE=管道维修，DOOR_WINDOW=门窗维修，OTHER=其他）",
            example = "APPLIANCE",
            nullable = false,
            allowableValues = {"WATER", "APPLIANCE", "WALL", "PIPELINE", "DOOR_WINDOW", "OTHER"}
    )
    @NotBlank(message = "维修类型不能为空")
    @TableField(value = "maintenance_type")
    private String maintenanceType;

    /**
     * 故障描述
     * 详细描述故障现象（如空调不制冷、水管漏水、墙面开裂），非空，长度≤500字符
     */
    @Schema(
            description = "故障详细描述（如空调不制冷、水管漏水、墙面开裂）",
            example = "客厅空调不制冷，开机后显示错误代码E1，已尝试重启无效",
            nullable = false,
            maxLength = 500
    )
    @NotBlank(message = "故障描述不能为空")
    @Size(max = 500, message = "故障描述长度不能超过500字符")
    @TableField(value = "description")
    private String description;

    /**
     * 紧急程度
     * 枚举值：1-低（非紧急，3个工作日内处理），2-中（一般紧急，1个工作日内处理），3-高（紧急，24小时内处理），非空
     */
    @Schema(
            description = "紧急程度（1=低，2=中，3=高）",
            example = "2",
            nullable = false,
            allowableValues = {"1", "2", "3"}
    )
    @NotNull(message = "紧急程度不能为空")
    @Min(value = 1, message = "紧急程度仅支持1（低）、2（中）、3（高）")
    @Max(value = 3, message = "紧急程度仅支持1（低）、2（中）、3（高）")
    @TableField(value = "urgency_level")
    private Byte urgencyLevel;

    /**
     * 维修师傅ID
     * 关联维修师傅表（可外部关联），派单后非空，未派单可空
     */
    @Schema(
            description = "维修师傅ID（派单后必填，未派单可空）",
            example = "7001",
            nullable = true
    )
    @TableField(value = "repairman_id")
    private Long repairmanId;

    /**
     * 工单状态
     * 枚举值：SUBMITTED-已提交（待派单），ASSIGNED-已派单（待维修），REPAIRING-维修中，COMPLETED-已完成，CANCELED-已取消，非空
     */
    @Schema(
            description = "工单状态（SUBMITTED=已提交，ASSIGNED=已派单，REPAIRING=维修中，COMPLETED=已完成，CANCELED=已取消）",
            example = "SUBMITTED",
            nullable = false,
            allowableValues = {"SUBMITTED", "ASSIGNED", "REPAIRING", "COMPLETED", "CANCELED"}
    )
    @NotBlank(message = "工单状态不能为空")
    @TableField(value = "status")
    private String status;

    /**
     * 预约维修时间
     * 与报修人约定的维修时间，派单后非空，未派单可空
     */
    @Schema(
            description = "预约维修时间（派单后必填，格式：yyyy-MM-dd HH:mm:ss）",
            example = "2025-11-30 15:00:00",
            nullable = true,
            format = "date-time"
    )
    @TableField(value = "appointment_time")
    private LocalDateTime appointmentTime;

    /**
     * 完成时间
     * 维修完成并验收的时间，状态为COMPLETED时非空，其他状态可空
     */
    @Schema(
            description = "维修完成时间（仅状态为已完成时必填，格式：yyyy-MM-dd HH:mm:ss）",
            example = "2025-11-30 16:30:00",
            nullable = true,
            format = "date-time"
    )
    @TableField(value = "complete_time")
    private LocalDateTime completeTime;

    /**
     * 维修费用
     * 维修产生的总费用（单位：元），非负，保留2位小数，状态为COMPLETED时非空，其他状态可空
     */
    @Schema(
            description = "维修费用（单位：元），保留2位小数，仅已完成状态必填",
            example = "300.00",
            nullable = true,
            minimum = "0.00",
            format = "decimal",
            multipleOf = 0.01
    )
    @DecimalMin(value = "0.00", inclusive = true, message = "维修费用不能为负数")
    @TableField(value = "cost_amount")
    private BigDecimal costAmount;

    /**
     * 费用承担方
     * 枚举值：LANDLORD-房东承担，TENANT-租户承担，SHARED-共同承担，状态为COMPLETED时非空，其他状态可空
     */
    @Schema(
            description = "费用承担方（LANDLORD=房东，TENANT=租户，SHARED=共同承担），仅已完成状态必填",
            example = "LANDLORD",
            nullable = true,
            allowableValues = {"LANDLORD", "TENANT", "SHARED"}
    )
    @TableField(value = "cost_bearer")
    private String costBearer;

    /**
     * 维修结果备注
     * 记录维修过程及结果（如“更换空调压缩机，已恢复制冷”），状态为COMPLETED时非空，其他状态可空，长度≤300字符
     */
    @Schema(
            description = "维修结果备注（记录维修过程及结果），仅已完成状态必填",
            example = "更换空调压缩机，开机测试正常，制冷效果达标",
            nullable = true,
            maxLength = 300
    )
    @Size(max = 300, message = "维修结果备注长度不能超过300字符")
    @TableField(value = "remark")
    private String remark;

    /**
     * 创建时间
     * 工单提交时间，数据库自动填充，无需手动传值，只读
     */
    @Schema(
            description = "工单创建时间（数据库自动填充）",
            example = "2025-11-28 09:00:00",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY,
            format = "date-time"
    )
    @TableField(value = "create_time", fill = FieldFill.INSERT) // 声明新增时自动填充
    private LocalDateTime createTime;

    /**
     * 更新时间
     * 工单信息更新时间（如状态变更、派单、费用录入），数据库自动填充，无需手动传值，只读
     */
    @Schema(
            description = "工单更新时间（数据库自动填充）",
            example = "2025-11-28 10:00:00",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY,
            format = "date-time"
    )
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE) // 声明新增/修改时自动填充
    private LocalDateTime updateTime;
}
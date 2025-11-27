package com.house.deed.pavilion.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 房源维护计划表（租户级数据隔离）
 * <p>
 * 核心业务说明：
 * 1. 维护管控：记录租户内房源的周期性/一次性维护计划（如保洁、设备检修、家具维修），支撑维护任务执行与跟踪；
 * 2. 租户隔离：所有字段绑定tenant_id，仅当前租户可查询/操作自身房源的维护计划；
 * 3. 核心关联：
 *    - house_id 关联房源表（同租户），标识需要维护的房源；
 *    - executor_id 关联经纪人/第三方服务商表（同租户），标识维护任务执行人；
 * 4. 计划核心要素：
 *    - 维护类型：maintain_type 区分维护场景（保洁、维修、安全巡检等）；
 *    - 执行周期：cycle 定义维护频率（一次性、每周、每月等），决定任务触发规则；
 *    - 时间范围：start_date（开始日期）必填，end_date（结束日期）对周期性计划必填、一次性计划可空；
 *    - 状态管控：status 标识计划状态（生效中、暂停、取消），管控任务执行；
 * 5. 业务约束：start_date 不得晚于 end_date（周期性计划），维护要求（remark）需明确执行标准（如“每周六上午保洁，清洁范围含客厅+卧室”）。
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Getter
@Setter
@TableName(value = "house_maintain_plan", autoResultMap = true) // 开启自动结果映射
@Schema(
        name = "HouseMaintainPlan",
        description = "房源维护计划实体（租户级数据），记录周期性/一次性维护任务（保洁/检修等）的执行规则与状态",
        example = "{\"tenantId\": 1001, \"houseId\": 101, \"maintainType\": \"CLEAN\", \"cycle\": \"WEEKLY\", \"startDate\": \"2025-12-01\", \"endDate\": \"2026-12-01\", \"executorId\": 3001, \"status\": \"ACTIVE\", \"remark\": \"每周六上午9点保洁，清洁范围：客厅、卧室、厨房、卫生间\"}"
)
public class HouseMaintainPlan implements Serializable {

    /**
     * 序列化版本号：保证对象序列化/反序列化一致性，避免类结构变更导致反序列化异常
     */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 维护计划主键ID
     * 自增策略，唯一标识单条维护计划，无业务含义，仅作为数据库主键
     */
    @Schema(
            description = "维护计划主键ID（自增）",
            example = "1",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY // 只读：新增时数据库自增，无需传值
    )
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID
     * 关联租户表主键，标识维护计划归属的租户，核心隔离字段，非空
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
     * 关联house表主键（同租户下的房源），标识需要维护的房源，非空（核心关联字段）
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
     * 维护类型
     * 枚举值：CLEAN-保洁，REPAIR-设备维修，INSPECTION-安全巡检，FURNITURE_MAINT-家具保养，OTHER-其他维护
     */
    @Schema(
            description = "维护类型（CLEAN=保洁，REPAIR=设备维修，INSPECTION=安全巡检，FURNITURE_MAINT=家具保养，OTHER=其他）",
            example = "CLEAN",
            nullable = false,
            allowableValues = {"CLEAN", "REPAIR", "INSPECTION", "FURNITURE_MAINT", "OTHER"}
    )
    @NotBlank(message = "维护类型不能为空")
    @TableField(value = "maintain_type")
    private String maintainType;

    /**
     * 执行周期
     * 枚举值：ONCE-一次性，WEEKLY-每周，MONTHLY-每月，QUARTERLY-每季度，YEARLY-每年
     */
    @Schema(
            description = "执行周期（ONCE=一次性，WEEKLY=每周，MONTHLY=每月，QUARTERLY=每季度，YEARLY=每年）",
            example = "WEEKLY",
            nullable = false,
            allowableValues = {"ONCE", "WEEKLY", "MONTHLY", "QUARTERLY", "YEARLY"}
    )
    @NotBlank(message = "执行周期不能为空")
    @TableField(value = "cycle")
    private String cycle;

    /**
     * 开始日期
     * 维护计划生效的开始日期（格式：yyyy-MM-dd），非空，且不能早于当前日期
     */
    @Schema(
            description = "计划开始日期（格式：yyyy-MM-dd，不能早于当前日期）",
            example = "2025-12-01",
            nullable = false,
            format = "date"
    )
    @NotNull(message = "开始日期不能为空")
    @FutureOrPresent(message = "开始日期不能早于当前日期")
    @TableField(value = "start_date")
    private LocalDate startDate;

    /**
     * 结束日期
     * 维护计划生效的结束日期（格式：yyyy-MM-dd）；周期为ONCE时可空，其他周期必填，且不能早于开始日期
     */
    @Schema(
            description = "计划结束日期（格式：yyyy-MM-dd）；一次性计划可空，其他周期必填且不能早于开始日期",
            example = "2026-12-01",
            nullable = true,
            format = "date"
    )
    @TableField(value = "end_date")
    private LocalDate endDate;

    /**
     * 执行人ID
     * 关联经纪人/第三方服务商表主键（同租户下），标识负责执行维护任务的人员/机构，非空
     */
    @Schema(
            description = "执行人ID（关联经纪人/第三方服务商表，仅同租户下有效）",
            example = "3001",
            nullable = false
    )
    @NotNull(message = "执行人ID不能为空")
    @TableField(value = "executor_id")
    private Long executorId;

    /**
     * 计划状态
     * 枚举值：ACTIVE-生效中（正常执行），PAUSED-暂停（临时停止），CANCELED-已取消（永久终止）
     */
    @Schema(
            description = "计划状态（ACTIVE=生效中，PAUSED=暂停，CANCELED=已取消）",
            example = "ACTIVE",
            nullable = false,
            allowableValues = {"ACTIVE", "PAUSED", "CANCELED"}
    )
    @NotBlank(message = "计划状态不能为空")
    @TableField(value = "status")
    private String status;

    /**
     * 维护要求说明
     * 详细描述维护标准、执行时间、注意事项等（如“每周六上午9点保洁，清洁范围含客厅+卧室+厨房”），可空，长度≤500字符
     */
    @Schema(
            description = "维护要求（如执行时间、清洁范围、维修标准等）",
            example = "每周六上午9点保洁，清洁范围：客厅、卧室、厨房、卫生间，需使用环保清洁剂",
            nullable = true,
            maxLength = 500
    )
    @Size(max = 500, message = "维护要求长度不能超过500字符")
    @TableField(value = "remark")
    private String remark;

    /**
     * 创建时间
     * 维护计划创建时间，数据库自动填充（新增时），无需手动传值，只读
     */
    @Schema(
            description = "计划创建时间（数据库自动填充）",
            example = "2025-11-26 14:00:00",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY,
            format = "date-time"
    )
    @TableField(value = "create_time", fill = FieldFill.INSERT) // 声明新增时自动填充
    private LocalDateTime createTime;
}
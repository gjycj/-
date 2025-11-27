package com.house.deed.pavilion.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 客户跟进记录表（租户级数据隔离）
 * <p>
 * 核心业务说明：
 * 1. 跟进管控：记录租户内客户跟进全流程信息，支撑经纪人跟进效率监控、客户需求变化追溯；
 * 2. 租户隔离：所有字段绑定tenant_id，仅当前租户可查询/操作自身客户的跟进记录；
 * 3. 多维度关联：
 *    - 客户维度：customer_id 关联客户表，明确跟进对象；
 *    - 执行维度：agent_id 关联经纪人表，标识跟进责任人；
 *    - 业务维度：house_id 关联房源表（带看场景必填），contract_id 关联合同表（成交后补充）；
 * 4. 核心字段：
 *    - follow_time：跟进实际发生时间（必填，区分于记录创建时间）；
 *    - content：跟进核心内容（客户反馈、沟通要点）；
 *    - demand_change：客户需求调整记录（如户型、价格区间变更）；
 *    - next_follow_plan：下次跟进计划（时间+内容，支撑跟进闭环）；
 * 5. 数据规范：文本字段长度限制，确保跟进信息简洁有效，便于追溯。
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Getter
@Setter
@TableName(value = "customer_follow_up", autoResultMap = true)
@Schema(
        name = "CustomerFollowUp",
        description = "客户跟进记录实体（租户级数据），记录客户跟进过程、需求变化及下次跟进计划",
        example = "{\"tenantId\": 1001, \"customerId\": 1, \"agentId\": 3001, \"houseId\": 101, \"followTime\": \"2025-11-26 15:30:00\", \"content\": \"客户反馈房源采光满意，价格可谈\", \"demandChange\": \"价格上限调整为160万\", \"nextFollowPlan\": \"2025-11-28 10:00 带看同小区另一套房源\", \"contractId\": null}"
)
public class CustomerFollowUp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 跟进记录主键ID
     * 自增策略，唯一标识单条跟进记录，无业务含义，仅作为数据库主键
     */
    @Schema(
            description = "跟进记录主键ID（自增）",
            example = "1",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY
    )
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID
     * 关联租户表主键，标识跟进记录归属的租户，核心隔离字段，非空
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
     * 客户ID
     * 关联customer表主键（同租户下的客户），标识被跟进的客户，非空
     */
    @Schema(
            description = "客户ID（关联customer表，仅同租户下的客户有效）",
            example = "1",
            nullable = false
    )
    @NotNull(message = "客户ID不能为空")
    @TableField(value = "customer_id")
    private Long customerId;

    /**
     * 跟进经纪人ID
     * 关联agent表主键（同租户下的经纪人），标识执行跟进的经纪人，非空
     */
    @Schema(
            description = "跟进经纪人ID（关联agent表，仅同租户下的经纪人有效）",
            example = "3001",
            nullable = false
    )
    @NotNull(message = "跟进经纪人ID不能为空")
    @TableField(value = "agent_id")
    private Long agentId;

    /**
     * 带看房源ID
     * 关联house表主键（同租户下的房源），仅带看场景填写，可空（如电话跟进无带看）
     */
    @Schema(
            description = "带看房源ID（关联house表，仅带看场景填写，无带看可填null）",
            example = "101",
            nullable = true
    )
    @TableField(value = "house_id")
    private Long houseId;

    /**
     * 跟进时间
     * 跟进实际发生的时间（如电话沟通、带看时间），非空，格式：yyyy-MM-dd HH:mm:ss
     */
    @Schema(
            description = "跟进实际发生时间（格式：yyyy-MM-dd HH:mm:ss）",
            example = "2025-11-26 15:30:00",
            nullable = false,
            format = "date-time"
    )
    @NotNull(message = "跟进时间不能为空")
    @TableField(value = "follow_time")
    private LocalDateTime followTime;

    /**
     * 跟进内容
     * 详细记录跟进过程（客户反馈、沟通要点、房源评价等），非空，长度≤500字符
     */
    @Schema(
            description = "跟进内容（客户反馈、沟通要点、房源评价等）",
            example = "客户反馈房源采光满意，价格可谈，希望了解过户流程",
            nullable = false,
            maxLength = 500
    )
    @NotBlank(message = "跟进内容不能为空")
    @Size(max = 500, message = "跟进内容长度不能超过500字符")
    @TableField(value = "content")
    private String content;

    /**
     * 需求调整记录
     * 客户需求变更内容（如户型从两居改三居、价格上限调整），可空，长度≤300字符
     */
    @Schema(
            description = "客户需求调整记录（如户型、价格、区域变更）",
            example = "价格上限调整为160万，新增要求近地铁",
            nullable = true,
            maxLength = 300
    )
    @Size(max = 300, message = "需求调整记录长度不能超过300字符")
    @TableField(value = "demand_change")
    private String demandChange;

    /**
     * 下次跟进计划
     * 记录下次跟进的时间和内容（如“2025-11-28 带看XX房源”），非空，支撑跟进闭环
     */
    @Schema(
            description = "下次跟进计划（时间+内容，如“2025-11-28 10:00 带看同小区另一套房源”）",
            example = "2025-11-28 10:00 带看同小区另一套房源，同步过户流程细节",
            nullable = false,
            maxLength = 300
    )
    @NotBlank(message = "下次跟进计划不能为空")
    @Size(max = 300, message = "下次跟进计划长度不能超过300字符")
    @TableField(value = "next_follow_plan")
    private String nextFollowPlan;

    /**
     * 记录创建时间
     * 跟进记录录入系统的时间，数据库自动填充，无需手动传值，只读
     */
    @Schema(
            description = "记录创建时间（数据库自动填充）",
            example = "2025-11-26 15:35:00",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY,
            format = "date-time"
    )
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 关联合同ID
     * 关联contract表主键（同租户下的合同），客户成交后补充，可空（未成交时为null）
     */
    @Schema(
            description = "关联合同ID（关联contract表，客户成交后补充，未成交可填null）",
            example = "5001",
            nullable = true
    )
    @TableField(value = "contract_id")
    private Long contractId;
}
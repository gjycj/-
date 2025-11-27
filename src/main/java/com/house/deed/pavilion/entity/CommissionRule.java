package com.house.deed.pavilion.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 佣金计算规则表（租户级数据隔离）
 * <p>
 * 核心业务说明：
 * 1. 规则用途：定义租户内经纪人佣金结算的核心规则，支撑佣金自动计算（如按房源类型/经纪人等级差异化提成）；
 * 2. 租户隔离：所有字段绑定tenant_id，仅当前租户可配置/使用自身佣金规则；
 * 3. 规则要素：
 *    - applicable_type：适用场景（如独家房源/普通房源/租赁房源）；
 *    - condition：规则生效条件（EL表达式，如"agent_level = 'STAR' and deal_amount > 100"）；
 *    - commission_rate：佣金比例（0~1之间，如0.15代表15%）；
 * 4. 状态管控：status标识规则生效/失效，仅生效规则参与佣金计算；
 * 5. 数据规范：佣金比例保留4位小数，规则条件支持自定义EL表达式，长度≤200字符。
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Getter
@Setter
@TableName(value = "commission_rule", autoResultMap = true) // 开启自动结果映射（兼容复杂类型）
@Schema(
        name = "CommissionRule",
        description = "佣金计算规则实体（租户级数据），定义租户内经纪人佣金结算规则及生效条件",
        example = "{\"tenantId\": 1001, \"ruleName\": \"独家房源明星经纪人提成规则\", \"applicableType\": \"EXCLUSIVE_HOUSE\", \"condition\": \"agent_level = 'STAR'\", \"commissionRate\": 0.15, \"status\": 1}"
)
public class CommissionRule implements Serializable {

    /**
     * 序列化版本号：保证对象序列化/反序列化一致性，避免类结构变更导致反序列化异常
     */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 规则主键ID
     * 自增策略，唯一标识单条佣金规则，无业务含义，仅作为数据库主键
     */
    @Schema(
            description = "规则主键ID（自增）",
            example = "1",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY // 只读：新增时数据库自增，无需传值
    )
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID
     * 关联租户表主键，标识规则归属的租户，核心隔离字段，非空
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
     * 规则名称
     * 便于租户识别规则用途（如“独家房源明星经纪人提成规则”），非空，长度≤50字符
     */
    @Schema(
            description = "规则名称（便于识别规则用途，如“独家房源明星经纪人提成规则”）",
            example = "独家房源明星经纪人提成规则",
            nullable = false,
            maxLength = 50
    )
    @NotBlank(message = "规则名称不能为空")
    @Size(max = 50, message = "规则名称长度不能超过50字符")
    @TableField(value = "rule_name")
    private String ruleName;

    /**
     * 适用类型
     * 规则生效的业务场景，枚举值：EXCLUSIVE_HOUSE-独家房源，ORDINARY_HOUSE-普通房源，LEASE_HOUSE-租赁房源
     */
    @Schema(
            description = "适用类型（EXCLUSIVE_HOUSE=独家房源，ORDINARY_HOUSE=普通房源，LEASE_HOUSE=租赁房源）",
            example = "EXCLUSIVE_HOUSE",
            nullable = false,
            allowableValues = {"EXCLUSIVE_HOUSE", "ORDINARY_HOUSE", "LEASE_HOUSE"} // 业务枚举值
    )
    @NotBlank(message = "适用类型不能为空")
    @TableField(value = "applicable_type")
    private String applicableType;

    /**
     * 规则条件
     * 规则生效的自定义条件（EL表达式，如"agent_level = 'STAR' and deal_amount > 100"），非空，长度≤200字符
     */
    @Schema(
            description = "规则生效条件（EL表达式，如'agent_level = \"STAR\" and deal_amount > 100'）",
            example = "agent_level = 'STAR'",
            nullable = false,
            maxLength = 200
    )
    @NotBlank(message = "规则条件不能为空")
    @Size(max = 200, message = "规则条件长度不能超过200字符")
    @TableField(value = "condition")
    private String condition;

    /**
     * 佣金比例
     * 0~1之间的小数（如0.15代表15%），保留4位小数，非空（佣金比例为0时代表无提成）
     */
    @Schema(
            description = "佣金比例（0~1之间，如0.15代表15%），保留4位小数",
            example = "0.15",
            nullable = false,
            minimum = "0.0",
            maximum = "1.0",
            format = "decimal",
            multipleOf = 0.0001 // 确保保留4位小数
    )
    @NotNull(message = "佣金比例不能为空")
    @DecimalMin(value = "0.0", inclusive = true, message = "佣金比例不能小于0")
    @DecimalMax(value = "1.0", inclusive = true, message = "佣金比例不能大于1")
    @TableField(value = "commission_rate")
    private BigDecimal commissionRate;

    /**
     * 规则状态
     * 1=生效（参与佣金计算），0=失效（不参与佣金计算），非空
     */
    @Schema(
            description = "规则状态（1=生效，0=失效）",
            example = "1",
            nullable = false,
            allowableValues = {"0", "1"}
    )
    @NotNull(message = "规则状态不能为空")
    @TableField(value = "status")
    private Byte status;

    /**
     * 创建时间
     * 规则创建时间，数据库自动填充（新增时），无需手动传值，只读
     */
    @Schema(
            description = "规则创建时间（数据库自动填充）",
            example = "2025-11-26 10:00:00",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY,
            format = "date-time"
    )
    @TableField(value = "create_time", fill = FieldFill.INSERT) // 声明新增时自动填充
    private LocalDateTime createTime;

    /**
     * 更新时间
     * 规则更新时间，数据库自动填充（新增/修改时），无需手动传值，只读
     */
    @Schema(
            description = "规则更新时间（数据库自动填充）",
            example = "2025-11-26 11:00:00",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY,
            format = "date-time"
    )
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE) // 声明新增/修改时自动填充
    private LocalDateTime updateTime;
}
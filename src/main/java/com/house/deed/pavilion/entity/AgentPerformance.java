package com.house.deed.pavilion.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 经纪人业绩记录表（租户级数据隔离）
 * <p>
 * 核心业务说明：
 * 1. 业绩追溯：记录经纪人每月成交业绩，关联成交合同（contract表），支撑佣金结算、业绩统计；
 * 2. 租户隔离：所有字段绑定tenant_id，仅当前租户可查询/操作自身经纪人的业绩数据；
 * 3. 金额规范：deal_amount（成交金额）单位为万元，commission_amount（佣金金额）单位为元，均非负；
 * 4. 状态管控：performance_status标识业绩结算状态（未结算/已结算/已取消），settle_time记录实际结算时间；
 * 5. 时间维度：performance_month按「yyyyMM」格式存储（如202310），用于月度业绩汇总。
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Getter
@Setter
@TableName(value = "agent_performance", autoResultMap = true) // 开启自动结果映射（兼容复杂类型）
@Schema(
        name = "AgentPerformance",
        description = "经纪人业绩记录实体（租户级数据），记录经纪人成交业绩、佣金及结算状态",
        example = "{\"tenantId\": 1001, \"agentId\": 3001, \"contractId\": 5001, \"performanceMonth\": \"202511\", \"dealAmount\": 120.5, \"commissionAmount\": 36000.00, \"performanceStatus\": \"UNSETTLED\"}"
)
public class AgentPerformance implements Serializable {

    /**
     * 序列化版本号：保证对象序列化/反序列化一致性，避免类结构变更导致反序列化异常
     */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 业绩记录主键ID
     * 自增策略，唯一标识单条业绩记录，无业务含义，仅作为数据库主键
     */
    @Schema(
            description = "业绩记录主键ID（自增）",
            example = "1",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY // 只读：新增时数据库自增，无需传值
    )
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID
     * 关联租户表主键，标识业绩数据归属的租户，核心隔离字段，非空
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
     * 经纪人ID
     * 关联agent表主键（同租户下的经纪人），标识业绩归属的经纪人，非空
     */
    @Schema(
            description = "经纪人ID（关联agent表，仅同租户下的经纪人有效）",
            example = "3001",
            nullable = false
    )
    @NotNull(message = "经纪人ID不能为空")
    @TableField(value = "agent_id")
    private Long agentId;

    /**
     * 成交合同ID
     * 关联contract表主键（同租户下的合同），标识业绩对应的成交合同，非空（核心关联字段）
     */
    @Schema(
            description = "成交合同ID（关联contract表，仅同租户下的合同有效）",
            example = "5001",
            nullable = false
    )
    @NotNull(message = "成交合同ID不能为空")
    @TableField(value = "contract_id")
    private Long contractId;

    /**
     * 业绩月份
     * 格式：yyyyMM（如202511代表2025年11月），用于月度业绩汇总，非空
     */
    @Schema(
            description = "业绩月份（格式：yyyyMM，如202511代表2025年11月）",
            example = "202511",
            nullable = false,
            pattern = "^\\d{6}$", // 约束6位数字格式
            maxLength = 6
    )
    @NotBlank(message = "业绩月份不能为空")
    @Pattern(regexp = "^\\d{6}$", message = "业绩月份格式错误（需为yyyyMM，如202511）")
    @TableField(value = "performance_month")
    private String performanceMonth;

    /**
     * 成交金额
     * 单位：万元，非负（支持0，如0元成交），保留2位小数
     */
    @Schema(
            description = "成交金额（单位：万元），非负，保留2位小数",
            example = "120.50",
            nullable = false,
            minimum = "0",
            format = "decimal", // 标识小数格式
            multipleOf = 0.01 // 确保保留2位小数
    )
    @NotNull(message = "成交金额不能为空")
    @DecimalMin(value = "0.00", inclusive = true, message = "成交金额不能为负数")
    @TableField(value = "deal_amount")
    private BigDecimal dealAmount;

    /**
     * 佣金金额
     * 单位：元，非负，保留2位小数（佣金可0，如无佣金成交）
     */
    @Schema(
            description = "佣金金额（单位：元），非负，保留2位小数",
            example = "36000.00",
            nullable = false,
            minimum = "0",
            format = "decimal",
            multipleOf = 0.01
    )
    @NotNull(message = "佣金金额不能为空")
    @DecimalMin(value = "0.00", inclusive = true, message = "佣金金额不能为负数")
    @TableField(value = "commission_amount")
    private BigDecimal commissionAmount;

    /**
     * 业绩状态
     * 枚举值：UNSETTLED-未结算，SETTLED-已结算，CANCELED-已取消，非空
     */
    @Schema(
            description = "业绩状态（UNSETTLED=未结算，SETTLED=已结算，CANCELED=已取消）",
            example = "UNSETTLED",
            nullable = false,
            allowableValues = {"UNSETTLED", "SETTLED", "CANCELED"} // 声明合法枚举值
    )
    @NotBlank(message = "业绩状态不能为空")
    @Pattern(regexp = "^(UNSETTLED|SETTLED|CANCELED)$", message = "业绩状态错误（仅支持UNSETTLED/SETTLED/CANCELED）")
    @TableField(value = "performance_status")
    private String performanceStatus;

    /**
     * 结算时间
     * 业绩状态为SETTLED时必填，未结算时为null；格式：yyyy-MM-dd HH:mm:ss
     */
    @Schema(
            description = "业绩结算时间（状态为SETTLED时必填，格式：yyyy-MM-dd HH:mm:ss）",
            example = "2025-11-30 18:00:00",
            nullable = true,
            accessMode = Schema.AccessMode.READ_WRITE,
            format = "date-time"
    )
    @TableField(value = "settle_time")
    private LocalDateTime settleTime;

    /**
     * 创建时间
     * 数据库自动填充（新增时），无需手动传值，只读
     */
    @Schema(
            description = "业绩记录创建时间（数据库自动填充）",
            example = "2025-11-26 10:00:00",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY,
            format = "date-time"
    )
    @TableField(value = "create_time", fill = FieldFill.INSERT) // 声明新增时自动填充
    private LocalDateTime createTime;
}
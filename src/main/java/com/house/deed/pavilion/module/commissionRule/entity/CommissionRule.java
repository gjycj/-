package com.house.deed.pavilion.module.commissionRule.entity;

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
 * 佣金计算规则表（租户级数据）
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
@Getter
@Setter
@TableName("commission_rule")
public class CommissionRule implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 规则ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID（归属租户）
     */
    @TableField("tenant_id")
    private Long tenantId;

    /**
     * 规则名称（如“独家房源提成规则”）
     */
    @TableField("rule_name")
    private String ruleName;

    /**
     * 适用类型（EXCLUSIVE_HOUSE-独家房源等）
     */
    @TableField("applicable_type")
    private String applicableType;

    /**
     * 规则条件（如“agent_level = 'STAR'”）
     */
    @TableField("condition")
    private String condition;

    /**
     * 佣金比例（如0.15代表15%）
     */
    @TableField("commission_rate")
    private BigDecimal commissionRate;

    /**
     * 状态（1-生效，0-失效）
     */
    @TableField("status")
    private Byte status;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}

package com.house.deed.pavilion.module.agentPerformance.entity;

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
 * 经纪人业绩记录表（租户级数据）
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
@Getter
@Setter
@TableName("agent_performance")
public class AgentPerformance implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 业绩ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID（归属租户）
     */
    @TableField("tenant_id")
    private Long tenantId;

    /**
     * 经纪人ID（关联agent表，同租户）
     */
    @TableField("agent_id")
    private Long agentId;

    /**
     * 成交合同ID（关联contract表，同租户）
     */
    @TableField("contract_id")
    private Long contractId;

    /**
     * 业绩月份（如202310）
     */
    @TableField("performance_month")
    private String performanceMonth;

    /**
     * 成交金额（万元）
     */
    @TableField("deal_amount")
    private BigDecimal dealAmount;

    /**
     * 佣金金额（元）
     */
    @TableField("commission_amount")
    private BigDecimal commissionAmount;

    /**
     * 业绩状态（UNSETTLED-未结算等）
     */
    @TableField("performance_status")
    private String performanceStatus;

    /**
     * 结算时间
     */
    @TableField("settle_time")
    private LocalDateTime settleTime;

    @TableField("create_time")
    private LocalDateTime createTime;
}

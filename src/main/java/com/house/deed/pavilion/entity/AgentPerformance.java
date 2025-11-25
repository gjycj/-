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
 * 经纪人业绩记录表（租户级数据）
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Getter
@Setter
@TableName("agent_performance")
@ApiModel(value = "AgentPerformance对象", description = "经纪人业绩记录表（租户级数据）")
public class AgentPerformance implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("业绩ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("租户ID（归属租户）")
    @TableField("tenant_id")
    private Long tenantId;

    @ApiModelProperty("经纪人ID（关联agent表，同租户）")
    @TableField("agent_id")
    private Long agentId;

    @ApiModelProperty("成交合同ID（关联contract表，同租户）")
    @TableField("contract_id")
    private Long contractId;

    @ApiModelProperty("业绩月份（如202310）")
    @TableField("performance_month")
    private String performanceMonth;

    @ApiModelProperty("成交金额（万元）")
    @TableField("deal_amount")
    private BigDecimal dealAmount;

    @ApiModelProperty("佣金金额（元）")
    @TableField("commission_amount")
    private BigDecimal commissionAmount;

    @ApiModelProperty("业绩状态（UNSETTLED-未结算等）")
    @TableField("performance_status")
    private String performanceStatus;

    @ApiModelProperty("结算时间")
    @TableField("settle_time")
    private LocalDateTime settleTime;

    @TableField("create_time")
    private LocalDateTime createTime;
}

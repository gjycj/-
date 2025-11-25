package com.house.deed.pavilion.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 客户跟进记录表（租户级数据）
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Getter
@Setter
@TableName("customer_follow_up")
@ApiModel(value = "CustomerFollowUp对象", description = "客户跟进记录表（租户级数据）")
public class CustomerFollowUp implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("日志ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("租户ID（归属租户）")
    @TableField("tenant_id")
    private Long tenantId;

    @ApiModelProperty("客户ID（关联customer表，同租户）")
    @TableField("customer_id")
    private Long customerId;

    @ApiModelProperty("跟进经纪人ID（关联agent表，同租户）")
    @TableField("agent_id")
    private Long agentId;

    @ApiModelProperty("带看房源ID（关联house表，同租户）")
    @TableField("house_id")
    private Long houseId;

    @ApiModelProperty("跟进时间")
    @TableField("follow_time")
    private LocalDateTime followTime;

    @ApiModelProperty("跟进内容（如需求变化、看过的房源反馈等）")
    @TableField("content")
    private String content;

    @ApiModelProperty("需求调整记录（如从两居改三居）")
    @TableField("demand_change")
    private String demandChange;

    @ApiModelProperty("下次跟进计划")
    @TableField("next_follow_plan")
    private String nextFollowPlan;

    @TableField("create_time")
    private LocalDateTime createTime;

    @ApiModelProperty("关联合同ID（关联contract表，同租户）")
    @TableField("contract_id")
    private Long contractId;
}

package com.house.deed.pavilion.module.customerFollowUp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 客户跟进记录表（租户级数据）
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
@Getter
@Setter
@TableName("customer_follow_up")
public class CustomerFollowUp implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 日志ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID（归属租户）
     */
    @TableField("tenant_id")
    private Long tenantId;

    /**
     * 客户ID（关联customer表，同租户）
     */
    @TableField("customer_id")
    private Long customerId;

    /**
     * 关联带看记录ID（若跟进与某次带看相关，关联visit_record表，同租户）
     */
    @TableField("visit_record_id")
    private Long visitRecordId;

    @TableField("contract_id")
    private Long contractId; // 关联合同ID（可为null，未签约时为空）

    @TableField("house_id")
    private Long houseId;

    /**
     * 跟进经纪人ID（关联agent表，同租户）
     */
    @TableField("agent_id")
    private Long agentId;

    /**
     * 跟进时间
     */
    @TableField("follow_time")
    private LocalDateTime followTime;

    /**
     * 跟进内容（如需求变化、看过的房源反馈等）
     */
    @TableField("content")
    private String content;

    /**
     * 需求调整记录（如从两居改三居）
     */
    @TableField("demand_change")
    private String demandChange;

    /**
     * 下次跟进计划
     */
    @TableField("next_follow_plan")
    private String nextFollowPlan;

    @TableField("create_time")
    private LocalDateTime createTime;
}

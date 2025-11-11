package com.house.deed.pavilion.module.complaintDispute.entity;

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
 * 投诉与纠纷记录表（租户级数据）
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
@Getter
@Setter
@TableName("complaint_dispute")
public class ComplaintDispute implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 纠纷ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID（归属租户）
     */
    @TableField("tenant_id")
    private Long tenantId;

    /**
     * 纠纷编号（租户内唯一）
     */
    @TableField("dispute_no")
    private String disputeNo;

    /**
     * 关联合同ID（可空，同租户）
     */
    @TableField("related_contract_id")
    private Long relatedContractId;

    /**
     * 投诉人类型（CUSTOMER-客户等）
     */
    @TableField("complainant_type")
    private String complainantType;

    /**
     * 投诉人ID（关联对应表，同租户）
     */
    @TableField("complainant_id")
    private Long complainantId;

    /**
     * 投诉人电话
     */
    @TableField("complainant_phone")
    private String complainantPhone;

    /**
     * 纠纷类型（SERVICE-服务投诉等）
     */
    @TableField("dispute_type")
    private String disputeType;

    /**
     * 纠纷描述
     */
    @TableField("description")
    private String description;

    /**
     * 状态（ACCEPTED-已受理等）
     */
    @TableField("status")
    private String status;

    /**
     * 处理人ID（管理员/店长，同租户）
     */
    @TableField("handler_id")
    private Long handlerId;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}

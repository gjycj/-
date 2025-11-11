package com.house.deed.pavilion.module.contract.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 交易合同表（租户核心业务数据）
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
@Getter
@Setter
@TableName("contract")
public class Contract implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 合同ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID（归属租户）
     */
    @TableField("tenant_id")
    private Long tenantId;

    /**
     * 合同编号（租户内唯一）
     */
    @TableField("contract_no")
    private String contractNo;

    /**
     * 房源ID（关联house表，同租户）
     */
    @TableField("house_id")
    private Long houseId;

    /**
     * 客户ID（关联customer表，同租户）
     */
    @TableField("customer_id")
    private Long customerId;

    /**
     * 房东ID（关联landlord表，同租户）
     */
    @TableField("landlord_id")
    private Long landlordId;

    /**
     * 签约经纪人ID（关联agent表，同租户）
     */
    @TableField("agent_id")
    private Long agentId;

    /**
     * 合同类型（SALE-买卖合同，RENT-租赁合同）
     */
    @TableField("contract_type")
    private String contractType;

    /**
     * 交易总金额（万元/租金总额）
     */
    @TableField("amount")
    private BigDecimal amount;

    /**
     * 定金/押金（万元）
     */
    @TableField("deposit")
    private BigDecimal deposit;

    /**
     * 付款方式（全款/按揭/月付等）
     */
    @TableField("payment_method")
    private String paymentMethod;

    /**
     * 签约时间
     */
    @TableField("sign_time")
    private LocalDateTime signTime;

    /**
     * 生效日期（租赁适用）
     */
    @TableField("start_date")
    private LocalDate startDate;

    /**
     * 到期日期（租赁适用）
     */
    @TableField("end_date")
    private LocalDate endDate;

    /**
     * 状态（SIGNED-已签约，EXECUTING-执行中，COMPLETED-已完成，TERMINATED-已终止）
     */
    @TableField("status")
    private String status;

    /**
     * 其他约定
     */
    @TableField("remark")
    private String remark;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}

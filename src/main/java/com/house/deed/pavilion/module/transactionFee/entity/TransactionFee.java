package com.house.deed.pavilion.module.transactionFee.entity;

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
 * 交易费用明细表（租户级数据）
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
@Getter
@Setter
@TableName("transaction_fee")
public class TransactionFee implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 费用ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID（归属租户）
     */
    @TableField("tenant_id")
    private Long tenantId;

    /**
     * 合同ID（关联contract表，同租户）
     */
    @TableField("contract_id")
    private Long contractId;

    /**
     * 费用类型（AGENCY_FEE-中介费，TAX-税费等）
     */
    @TableField("fee_type")
    private String feeType;

    /**
     * 费用金额（元）
     */
    @TableField("amount")
    private BigDecimal amount;

    /**
     * 支付方（CUSTOMER-客户，LANDLORD-房东）
     */
    @TableField("payer")
    private String payer;

    /**
     * 支付状态（UNPAID-未付，PAID-已付）
     */
    @TableField("payment_status")
    private String paymentStatus;

    /**
     * 支付时间
     */
    @TableField("payment_time")
    private LocalDateTime paymentTime;

    /**
     * 备注（如发票号）
     */
    @TableField("remark")
    private String remark;

    @TableField("create_time")
    private LocalDateTime createTime;
}

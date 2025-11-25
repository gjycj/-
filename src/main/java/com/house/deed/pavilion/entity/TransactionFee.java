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
 * 交易费用明细表（租户级数据）
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Getter
@Setter
@TableName("transaction_fee")
@ApiModel(value = "TransactionFee对象", description = "交易费用明细表（租户级数据）")
public class TransactionFee implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("费用ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("租户ID（归属租户）")
    @TableField("tenant_id")
    private Long tenantId;

    @ApiModelProperty("合同ID（关联contract表，同租户）")
    @TableField("contract_id")
    private Long contractId;

    @ApiModelProperty("费用类型（AGENCY_FEE-中介费，TAX-税费等）")
    @TableField("fee_type")
    private String feeType;

    @ApiModelProperty("费用金额（元）")
    @TableField("amount")
    private BigDecimal amount;

    @ApiModelProperty("支付方（CUSTOMER-客户，LANDLORD-房东）")
    @TableField("payer")
    private String payer;

    @ApiModelProperty("支付状态（UNPAID-未付，PAID-已付）")
    @TableField("payment_status")
    private String paymentStatus;

    @ApiModelProperty("支付时间")
    @TableField("payment_time")
    private LocalDateTime paymentTime;

    @ApiModelProperty("备注（如发票号）")
    @TableField("remark")
    private String remark;

    @TableField("create_time")
    private LocalDateTime createTime;
}

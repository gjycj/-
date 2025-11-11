package com.house.deed.pavilion.module.loanInfo.entity;

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
 * 贷款信息表（租户级数据）
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
@Getter
@Setter
@TableName("loan_info")
public class LoanInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 贷款ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID（归属租户）
     */
    @TableField("tenant_id")
    private Long tenantId;

    /**
     * 合同ID（关联contract表，同租户，仅买卖）
     */
    @TableField("contract_id")
    private Long contractId;

    /**
     * 贷款银行
     */
    @TableField("bank_name")
    private String bankName;

    /**
     * 贷款金额（万元）
     */
    @TableField("loan_amount")
    private BigDecimal loanAmount;

    /**
     * 贷款期限（月）
     */
    @TableField("loan_term")
    private Integer loanTerm;

    /**
     * 贷款利率（如0.049表示4.9%）
     */
    @TableField("interest_rate")
    private BigDecimal interestRate;

    /**
     * 贷款类型（COMMERCIAL-商业贷款，FUND-公积金贷款）
     */
    @TableField("loan_type")
    private String loanType;

    /**
     * 申请时间
     */
    @TableField("apply_time")
    private LocalDateTime applyTime;

    /**
     * 审批通过时间
     */
    @TableField("approve_time")
    private LocalDateTime approveTime;

    /**
     * 贷款状态（APPLYING-申请中，APPROVED-已审批，REJECTED-被拒）
     */
    @TableField("loan_status")
    private String loanStatus;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}

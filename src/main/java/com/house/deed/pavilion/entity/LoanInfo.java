package com.house.deed.pavilion.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 贷款信息表（租户级数据隔离）
 * <p>
 * 核心业务说明：
 * 1. 交易专属：仅关联买卖类型合同（contract_type=SALE），存储房产交易中客户的贷款申请、审批及状态信息，是买卖交易流程的重要组成部分；
 * 2. 租户隔离：所有字段绑定tenant_id，仅当前租户可查询/操作自身的贷款数据，保护金融信息安全；
 * 3. 核心关联：
 *    - contract_id 关联合同表（同租户下的买卖合同），标识贷款对应的交易订单；
 * 4. 贷款核心要素：
 *    - 基础信息：bank_name（贷款银行）、loan_amount（贷款金额）、loan_term（贷款期限，月）、interest_rate（贷款利率）；
 *    - 类型区分：loan_type（商业贷款/公积金贷款），支撑不同贷款政策的适配；
 *    - 流程状态：loan_status（申请中/已审批/被拒），管控贷款流程节点；
 *    - 时间节点：apply_time（申请时间）、approve_time（审批通过时间，仅审批通过时非空）；
 * 5. 合规规范：贷款金额非负，期限符合常见贷款周期（1-30年），利率格式统一（如0.049=4.9%），状态与时间节点联动（审批通过需填写审批时间）。
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Getter
@Setter
@TableName(value = "loan_info", autoResultMap = true) // 开启自动结果映射
@Schema(
        name = "LoanInfo",
        description = "贷款信息实体（租户级数据），存储买卖交易中客户的贷款申请、审批及状态信息，支撑交易流程闭环",
        example = "{\"tenantId\": 1001, \"contractId\": 5001, \"bankName\": \"中国工商银行杭州分行\", \"loanAmount\": 120.00, \"loanTerm\": 240, \"interestRate\": 0.049, \"loanType\": \"COMMERCIAL\", \"applyTime\": \"2025-11-27 09:00:00\", \"loanStatus\": \"APPLYING\"}"
)
public class LoanInfo implements Serializable {

    /**
     * 序列化版本号：保证对象序列化/反序列化一致性，避免类结构变更导致反序列化异常
     */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 贷款记录主键ID
     * 自增策略，唯一标识单条贷款信息记录，无业务含义，仅作为数据库主键
     */
    @Schema(
            description = "贷款记录主键ID（自增）",
            example = "401",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY // 只读：新增时数据库自增，无需传值
    )
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID
     * 关联租户表主键，标识贷款记录归属的租户，核心隔离字段，非空
     */
    @Schema(
            description = "租户ID（归属租户，关联租户表主键）",
            example = "1001",
            nullable = false
    )
    @NotNull(message = "租户ID不能为空")
    @TableField(value = "tenant_id", exist = true)
    private Long tenantId;

    /**
     * 合同ID
     * 关联contract表主键（同租户下的买卖合同，contract_type=SALE），非空（核心关联字段）
     */
    @Schema(
            description = "合同ID（关联买卖类型合同，仅同租户下的买卖合同有效）",
            example = "5001",
            nullable = false
    )
    @NotNull(message = "合同ID不能为空")
    @TableField(value = "contract_id")
    private Long contractId;

    /**
     * 贷款银行
     * 发放贷款的银行名称（如中国工商银行、招商银行），非空，长度≤50字符
     */
    @Schema(
            description = "贷款银行名称（如中国工商银行、招商银行）",
            example = "中国工商银行杭州分行",
            nullable = false,
            maxLength = 50
    )
    @NotBlank(message = "贷款银行不能为空")
    @Size(max = 50, message = "贷款银行名称长度不能超过50字符")
    @TableField(value = "bank_name")
    private String bankName;

    /**
     * 贷款金额
     * 申请/审批通过的贷款金额（单位：万元），非负，保留2位小数，非空
     */
    @Schema(
            description = "贷款金额（单位：万元），保留2位小数",
            example = "120.00",
            nullable = false,
            minimum = "0.00",
            format = "decimal",
            multipleOf = 0.01
    )
    @NotNull(message = "贷款金额不能为空")
    @DecimalMin(value = "0.00", inclusive = true, message = "贷款金额不能为负数")
    @TableField(value = "loan_amount")
    private BigDecimal loanAmount;

    /**
     * 贷款期限
     * 贷款还款周期（单位：月），正整数，范围12-360个月（1-30年），非空
     */
    @Schema(
            description = "贷款期限（单位：月），范围12-360个月（1-30年）",
            example = "240",
            nullable = false,
            minimum = "12",
            maximum = "360"
    )
    @NotNull(message = "贷款期限不能为空")
    @Min(value = 12, message = "贷款期限不能少于12个月（1年）")
    @Max(value = 360, message = "贷款期限不能超过360个月（30年）")
    @TableField(value = "loan_term")
    private Integer loanTerm;

    /**
     * 贷款利率
     * 贷款年利率（如0.049表示4.9%），非负，保留4位小数，非空（符合金融利率精度要求）
     */
    @Schema(
            description = "贷款年利率（如0.049=4.9%），保留4位小数",
            example = "0.0490",
            nullable = false,
            minimum = "0.0000",
            format = "decimal",
            multipleOf = 0.0001
    )
    @NotNull(message = "贷款利率不能为空")
    @DecimalMin(value = "0.0000", inclusive = true, message = "贷款利率不能为负数")
    @TableField(value = "interest_rate")
    private BigDecimal interestRate;

    /**
     * 贷款类型
     * 枚举值：COMMERCIAL-商业贷款，FUND-公积金贷款，COMBINED-组合贷款（商业+公积金），非空
     */
    @Schema(
            description = "贷款类型（COMMERCIAL=商业贷款，FUND=公积金贷款，COMBINED=组合贷款）",
            example = "COMMERCIAL",
            nullable = false,
            allowableValues = {"COMMERCIAL", "FUND", "COMBINED"}
    )
    @NotBlank(message = "贷款类型不能为空")
    @TableField(value = "loan_type")
    private String loanType;

    /**
     * 申请时间
     * 客户提交贷款申请的时间（精确到时分秒），非空（核心时间节点）
     */
    @Schema(
            description = "贷款申请提交时间（格式：yyyy-MM-dd HH:mm:ss）",
            example = "2025-11-27 09:00:00",
            nullable = false,
            format = "date-time"
    )
    @NotNull(message = "申请时间不能为空")
    @TableField(value = "apply_time")
    private LocalDateTime applyTime;

    /**
     * 审批通过时间
     * 银行审批通过贷款的时间（精确到时分秒），可空（仅loan_status=APPROVED时非空）
     */
    @Schema(
            description = "贷款审批通过时间（仅审批通过状态有效，格式：yyyy-MM-dd HH:mm:ss）",
            example = "2025-12-05 14:30:00",
            nullable = true,
            format = "date-time"
    )
    @TableField(value = "approve_time")
    private LocalDateTime approveTime;

    /**
     * 贷款状态
     * 枚举值：APPLYING-申请中（已提交申请未审批），APPROVED-已审批（贷款通过），REJECTED-被拒（贷款申请驳回），非空
     */
    @Schema(
            description = "贷款状态（APPLYING=申请中，APPROVED=已审批，REJECTED=被拒）",
            example = "APPLYING",
            nullable = false,
            allowableValues = {"APPLYING", "APPROVED", "REJECTED"}
    )
    @NotBlank(message = "贷款状态不能为空")
    @TableField(value = "loan_status")
    private String loanStatus;

    /**
     * 创建时间
     * 贷款记录录入系统的时间，数据库自动填充，无需手动传值，只读
     */
    @Schema(
            description = "贷款记录创建时间（数据库自动填充）",
            example = "2025-11-27 09:30:00",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY,
            format = "date-time"
    )
    @TableField(value = "create_time", fill = FieldFill.INSERT) // 声明新增时自动填充
    private LocalDateTime createTime;

    /**
     * 更新时间
     * 贷款信息更新时间（如状态变更、审批时间补充），数据库自动填充，无需手动传值，只读
     */
    @Schema(
            description = "贷款记录更新时间（数据库自动填充）",
            example = "2025-12-05 14:30:00",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY,
            format = "date-time"
    )
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE) // 声明新增/修改时自动填充
    private LocalDateTime updateTime;
}
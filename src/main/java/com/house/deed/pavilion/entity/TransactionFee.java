package com.house.deed.pavilion.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
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
 * 交易费用明细表（租户级数据隔离）
 * <p>
 * 核心业务说明：
 * 1. 财务对账核心：记录租户内房产交易（租赁/买卖）的各项费用明细（中介费、税费、押金等），支撑财务对账、发票开具及合规审计；
 * 2. 租户隔离：所有字段绑定tenant_id，仅当前租户可查询/操作自身费用数据，保护财务数据安全；
 * 3. 核心关联：
 *    - contract_id 关联合同表（同租户下的交易合同），标识费用对应的交易订单，非空（费用必须绑定合同）；
 * 4. 费用核心要素：
 *    - 费用类型：区分交易中的各类费用（中介费、税费、押金、服务费等），适配不同财务核算规则；
 *    - 支付信息：payer（支付方）、payment_status（支付状态）、payment_time（支付时间），形成支付全链路记录；
 *    - 金额精度：费用金额保留2位小数（单位：元），非负，符合财务数据规范；
 * 5. 状态流转：UNPAID-未付 → PARTIALLY_PAID-部分支付 → PAID-已付 → REFUNDED-已退款（按需启用部分支付/退款场景）；
 * 6. 合规规范：费用明细需与合同金额、支付凭证一致，备注字段可存储发票号、支付方式等关键信息，便于追溯。
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Getter
@Setter
@TableName(value = "transaction_fee", autoResultMap = true) // 开启自动结果映射
@Schema(
        name = "TransactionFee",
        description = "交易费用明细实体（租户级数据），记录交易中的中介费、税费等明细，支撑财务对账和合规审计",
        example = "{\"tenantId\": 1001, \"contractId\": 5001, \"feeType\": \"AGENCY_FEE\", \"amount\": 3000.00, \"payer\": \"CUSTOMER\", \"paymentStatus\": \"PAID\", \"paymentTime\": \"2025-11-30 15:00:00\", \"remark\": \"发票号：20251130001，微信支付\"}"
)
public class TransactionFee implements Serializable {

    /**
     * 序列化版本号：保证对象序列化/反序列化一致性，避免类结构变更导致反序列化异常
     */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 费用明细主键ID
     * 自增策略，唯一标识单条费用记录，无业务含义，仅作为数据库主键
     */
    @Schema(
            description = "费用明细主键ID（自增）",
            example = "901",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY // 只读：新增时数据库自增，无需传值
    )
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID
     * 关联租户表主键，标识费用归属的租户，核心隔离字段，非空
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
     * 关联contract表主键（同租户下的交易合同），标识费用对应的交易订单，非空（核心关联字段）
     */
    @Schema(
            description = "合同ID（关联交易合同，仅同租户下的合同有效）",
            example = "5001",
            nullable = false
    )
    @NotNull(message = "合同ID不能为空")
    @TableField(value = "contract_id")
    private Long contractId;

    /**
     * 费用类型
     * 枚举值：AGENCY_FEE-中介费，TAX-税费（如契税、个税），DEPOSIT-押金（可退），SERVICE_FEE-服务费，OTHER-其他费用，非空
     */
    @Schema(
            description = "费用类型（AGENCY_FEE=中介费，TAX=税费，DEPOSIT=押金，SERVICE_FEE=服务费，OTHER=其他费用）",
            example = "AGENCY_FEE",
            nullable = false,
            allowableValues = {"AGENCY_FEE", "TAX", "DEPOSIT", "SERVICE_FEE", "OTHER"}
    )
    @NotBlank(message = "费用类型不能为空")
    @TableField(value = "fee_type")
    private String feeType;

    /**
     * 费用金额
     * 费用具体金额（单位：元），非负，保留2位小数，非空（财务数据精度要求）
     */
    @Schema(
            description = "费用金额（单位：元），保留2位小数",
            example = "3000.00",
            nullable = false,
            minimum = "0.00",
            format = "decimal",
            multipleOf = 0.01
    )
    @NotNull(message = "费用金额不能为空")
    @DecimalMin(value = "0.00", inclusive = true, message = "费用金额不能为负数")
    @TableField(value = "amount")
    private BigDecimal amount;

    /**
     * 支付方
     * 枚举值：CUSTOMER-客户支付，LANDLORD-房东支付，SHARED-共同承担，非空
     */
    @Schema(
            description = "支付方（CUSTOMER=客户，LANDLORD=房东，SHARED=共同承担）",
            example = "CUSTOMER",
            nullable = false,
            allowableValues = {"CUSTOMER", "LANDLORD", "SHARED"}
    )
    @NotBlank(message = "支付方不能为空")
    @TableField(value = "payer")
    private String payer;

    /**
     * 支付状态
     * 枚举值：UNPAID-未付，PARTIALLY_PAID-部分支付，PAID-已付，REFUNDED-已退款，非空
     */
    @Schema(
            description = "支付状态（UNPAID=未付，PARTIALLY_PAID=部分支付，PAID=已付，REFUNDED=已退款）",
            example = "PAID",
            nullable = false,
            allowableValues = {"UNPAID", "PARTIALLY_PAID", "PAID", "REFUNDED"}
    )
    @NotBlank(message = "支付状态不能为空")
    @TableField(value = "payment_status")
    private String paymentStatus;

    /**
     * 支付时间
     * 费用全额支付/退款完成的时间（精确到时分秒），可空（仅PAID/REFUNDED状态时非空）
     */
    @Schema(
            description = "支付/退款完成时间（仅已付/已退款状态有效，格式：yyyy-MM-dd HH:mm:ss）",
            example = "2025-11-30 15:00:00",
            nullable = true,
            format = "date-time"
    )
    @TableField(value = "payment_time")
    private LocalDateTime paymentTime;

    /**
     * 备注信息
     * 补充说明（如发票号、支付方式、退款原因等），可空，长度≤300字符
     */
    @Schema(
            description = "备注（如发票号、支付方式、退款原因等）",
            example = "发票号：20251130001，微信支付",
            nullable = true,
            maxLength = 300
    )
    @Size(max = 300, message = "备注长度不能超过300字符")
    @TableField(value = "remark")
    private String remark;

    /**
     * 创建时间
     * 费用记录创建时间（如合同签订时生成费用明细），数据库自动填充，无需手动传值，只读
     */
    @Schema(
            description = "费用记录创建时间（数据库自动填充）",
            example = "2025-11-29 10:00:00",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY,
            format = "date-time"
    )
    @TableField(value = "create_time", fill = FieldFill.INSERT) // 声明新增时自动填充
    private LocalDateTime createTime;
}
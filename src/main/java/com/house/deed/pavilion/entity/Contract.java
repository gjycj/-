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
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 交易合同表（租户核心业务数据，租户级数据隔离）
 * <p>
 * 核心业务说明：
 * 1. 业务核心：记录租户内房产交易（买卖/租赁）的核心法律文件信息，是业绩统计、佣金结算、纠纷追溯的基础；
 * 2. 租户隔离：所有字段绑定tenant_id，仅当前租户可查询/操作自身合同数据，严禁跨租户访问；
 * 3. 多维度关联：
 *    - 房源维度：house_id 关联房源表（同租户），明确合同对应的具体房源；
 *    - 角色维度：customer_id（客户）、landlord_id（房东）、agent_id（签约经纪人）分别关联对应角色表；
 * 4. 合同分类：
 *    - SALE（买卖合同）：核心字段为amount（成交总额）、deposit（定金）、payment_method（付款方式）；
 *    - RENT（租赁合同）：额外需填写start_date（生效日期）、end_date（到期日期）、amount（租金总额）；
 * 5. 状态流转：SIGNED（已签约）→ EXECUTING（执行中）→ COMPLETED（已完成）/ TERMINATED（已终止），支撑合同全生命周期管理；
 * 6. 财务规范：金额单位统一为“万元”，保留2位小数，定金≤成交总额（买卖合同）。
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Getter
@Setter
@TableName(value = "contract", autoResultMap = true) // 开启自动结果映射（兼容复杂类型）
@Schema(
        name = "Contract",
        description = "交易合同实体（租户核心业务数据），记录房产买卖/租赁交易的核心信息及全生命周期状态",
        example = "{\"tenantId\": 1001, \"contractNo\": \"CON20251126001\", \"houseId\": 101, \"customerId\": 4001, \"landlordId\": 5001, \"agentId\": 3001, \"contractType\": \"SALE\", \"amount\": 150.80, \"deposit\": 15.00, \"paymentMethod\": \"全款\", \"signTime\": \"2025-11-26 10:00:00\", \"status\": \"SIGNED\", \"remark\": \"无特殊约定\", \"createAgentId\": 3001}"
)
public class Contract implements Serializable {

    /**
     * 序列化版本号：保证对象序列化/反序列化一致性，避免类结构变更导致反序列化异常
     */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 合同主键ID
     * 自增策略，唯一标识单份合同，无业务含义，仅作为数据库主键
     */
    @Schema(
            description = "合同主键ID（自增）",
            example = "1",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY // 只读：新增时数据库自增，无需传值
    )
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID
     * 关联租户表主键，标识合同归属的租户，核心隔离字段，非空
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
     * 合同编号
     * 租户内唯一，格式：CON+年月日+3位流水号（如CON20251126001），非空，长度≤20字符
     */
    @Schema(
            description = "合同编号（租户内唯一，格式：CON+年月日+3位流水号，如CON20251126001）",
            example = "CON20251126001",
            nullable = false,
            maxLength = 20,
            pattern = "^CON\\d{8}\\d{3}$" // 编码格式约束
    )
    @NotBlank(message = "合同编号不能为空")
    @Size(max = 20, message = "合同编号长度不能超过20字符")
    @Pattern(regexp = "^CON\\d{8}\\d{3}$", message = "合同编号格式错误（需为CON+年月日+3位流水号，如CON20251126001）")
    @TableField(value = "contract_no")
    private String contractNo;

    /**
     * 房源ID
     * 关联house表主键（同租户下的房源），明确合同对应的房源，非空（核心关联字段）
     */
    @Schema(
            description = "房源ID（关联house表，仅同租户下的房源有效）",
            example = "101",
            nullable = false
    )
    @NotNull(message = "房源ID不能为空")
    @TableField(value = "house_id")
    private Long houseId;

    /**
     * 客户ID
     * 关联customer表主键（同租户下的客户），合同的购买方/承租方，非空
     */
    @Schema(
            description = "客户ID（关联customer表，仅同租户下的客户有效）",
            example = "4001",
            nullable = false
    )
    @NotNull(message = "客户ID不能为空")
    @TableField(value = "customer_id")
    private Long customerId;

    /**
     * 房东ID
     * 关联landlord表主键（同租户下的房东），合同的出售方/出租方，非空
     */
    @Schema(
            description = "房东ID（关联landlord表，仅同租户下的房东有效）",
            example = "5001",
            nullable = false
    )
    @NotNull(message = "房东ID不能为空")
    @TableField(value = "landlord_id")
    private Long landlordId;

    /**
     * 签约经纪人ID
     * 关联agent表主键（同租户下的经纪人），负责本次交易的签约经纪人，非空
     */
    @Schema(
            description = "签约经纪人ID（关联agent表，仅同租户下的经纪人有效）",
            example = "3001",
            nullable = false
    )
    @NotNull(message = "签约经纪人ID不能为空")
    @TableField(value = "agent_id")
    private Long agentId;

    /**
     * 合同类型
     * 枚举值：SALE-买卖合同，RENT-租赁合同，非空（决定合同核心字段的有效性）
     */
    @Schema(
            description = "合同类型（SALE=买卖合同，RENT=租赁合同）",
            example = "SALE",
            nullable = false,
            allowableValues = {"SALE", "RENT"}
    )
    @NotBlank(message = "合同类型不能为空")
    @TableField(value = "contract_type")
    private String contractType;

    /**
     * 交易总金额
     * 单位：万元，非负，保留2位小数；买卖合同为成交总额，租赁合同为租金总额（如年租/月租总和）
     */
    @Schema(
            description = "交易总金额（单位：万元），保留2位小数；买卖合同=成交总额，租赁合同=租金总额",
            example = "150.80",
            nullable = false,
            minimum = "0.00",
            format = "decimal",
            multipleOf = 0.01 // 确保2位小数精度
    )
    @NotNull(message = "交易总金额不能为空")
    @DecimalMin(value = "0.00", inclusive = true, message = "交易总金额不能为负数")
    @TableField(value = "amount")
    private BigDecimal amount;

    /**
     * 定金/押金
     * 单位：万元，非负，保留2位小数；买卖合同=定金（≤成交总额），租赁合同=押金（通常为1-3个月租金）
     */
    @Schema(
            description = "定金/押金（单位：万元），保留2位小数；买卖合同=定金，租赁合同=押金",
            example = "15.00",
            nullable = false,
            minimum = "0.00",
            format = "decimal",
            multipleOf = 0.01
    )
    @NotNull(message = "定金/押金不能为空")
    @DecimalMin(value = "0.00", inclusive = true, message = "定金/押金不能为负数")
    @TableField(value = "deposit")
    private BigDecimal deposit;

    /**
     * 付款方式
     * 行业通用值：全款、按揭、公积金贷款（买卖合同）；月付、季付、年付（租赁合同），非空
     */
    @Schema(
            description = "付款方式（买卖合同：全款/按揭/公积金贷款；租赁合同：月付/季付/年付）",
            example = "全款",
            nullable = false,
            maxLength = 30
    )
    @NotBlank(message = "付款方式不能为空")
    @Size(max = 30, message = "付款方式长度不能超过30字符")
    @TableField(value = "payment_method")
    private String paymentMethod;

    /**
     * 签约时间
     * 合同正式签订的时间，格式：yyyy-MM-dd HH:mm:ss，非空（合同生效的核心时间节点）
     */
    @Schema(
            description = "合同签约时间（格式：yyyy-MM-dd HH:mm:ss）",
            example = "2025-11-26 10:00:00",
            nullable = false,
            format = "date-time"
    )
    @NotNull(message = "签约时间不能为空")
    @TableField(value = "sign_time")
    private LocalDateTime signTime;

    /**
     * 生效日期（租赁适用）
     * 租赁合同开始执行的日期（格式：yyyy-MM-dd），租赁合同必填，买卖合同可空
     */
    @Schema(
            description = "合同生效日期（格式：yyyy-MM-dd），仅租赁合同必填，买卖合同可空",
            example = "2025-12-01",
            nullable = true,
            format = "date"
    )
    @TableField(value = "start_date")
    private LocalDate startDate;

    /**
     * 到期日期（租赁适用）
     * 租赁合同终止的日期（格式：yyyy-MM-dd），需晚于生效日期，租赁合同必填，买卖合同可空
     */
    @Schema(
            description = "合同到期日期（格式：yyyy-MM-dd），仅租赁合同必填，需晚于生效日期，买卖合同可空",
            example = "2026-12-01",
            nullable = true,
            format = "date"
    )
    @TableField(value = "end_date")
    private LocalDate endDate;

    /**
     * 合同状态
     * 枚举值：SIGNED-已签约（未执行），EXECUTING-执行中，COMPLETED-已完成（交易结束），TERMINATED-已终止（提前解除）
     */
    @Schema(
            description = "合同状态（SIGNED=已签约，EXECUTING=执行中，COMPLETED=已完成，TERMINATED=已终止）",
            example = "SIGNED",
            nullable = false,
            allowableValues = {"SIGNED", "EXECUTING", "COMPLETED", "TERMINATED"}
    )
    @NotBlank(message = "合同状态不能为空")
    @TableField(value = "status")
    private String status;

    /**
     * 其他约定
     * 合同补充条款、特殊约定等，可空，长度≤500字符
     */
    @Schema(
            description = "合同其他约定/补充条款",
            example = "无特殊约定",
            nullable = true,
            maxLength = 500
    )
    @Size(max = 500, message = "其他约定长度不能超过500字符")
    @TableField(value = "remark")
    private String remark;

    /**
     * 创建时间
     * 合同记录创建时间，数据库自动填充（新增时），无需手动传值，只读
     */
    @Schema(
            description = "合同记录创建时间（数据库自动填充）",
            example = "2025-11-26 10:30:00",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY,
            format = "date-time"
    )
    @TableField(value = "create_time", fill = FieldFill.INSERT) // 声明新增时自动填充
    private LocalDateTime createTime;

    /**
     * 更新时间
     * 合同信息更新时间（如状态变更、条款补充），数据库自动填充，无需手动传值，只读
     */
    @Schema(
            description = "合同记录更新时间（数据库自动填充）",
            example = "2025-11-26 11:00:00",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY,
            format = "date-time"
    )
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE) // 声明新增/修改时自动填充
    private LocalDateTime updateTime;

    /**
     * 创建人ID（经纪人）
     * 关联agent表主键（同租户下的经纪人），标识合同记录的创建者，非空
     */
    @Schema(
            description = "创建人ID（关联agent表，仅同租户下的经纪人有效）",
            example = "3001",
            nullable = false
    )
    @NotNull(message = "创建人ID不能为空")
    @TableField(value = "create_agent_id")
    private Long createAgentId;
}
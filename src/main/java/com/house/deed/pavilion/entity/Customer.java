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
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 客户信息表（租户级数据隔离）
 * <p>
 * 核心业务说明：
 * 1. 客户画像：存储租户内购房/租房客户的基础信息、意向需求和跟进状态，支撑房源精准匹配和客户生命周期管理；
 * 2. 租户隔离：所有字段绑定tenant_id，仅当前租户可查询/操作自身客户数据，保护客户隐私；
 * 3. 核心维度：
 *    - 身份标识：姓名、电话、身份证号（唯一标识客户，需严格格式校验）；
 *    - 需求画像：意向区域、价格区间、户型，用于房源智能匹配；
 *    - 客户分层：customer_type（客户类型）、potential_level（潜力等级），用于跟进优先级排序；
 *    - 状态管理：status（活跃/已成交/休眠），支撑客户跟进流程管控；
 * 4. 关联关系：create_agent_id 关联经纪人表，标识客户归属的跟进经纪人；intended_region_id 关联区域表，明确意向区域。
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Getter
@Setter
@TableName(value = "customer", autoResultMap = true) // 开启自动结果映射（兼容复杂类型）
@Schema(
        name = "Customer",
        description = "客户信息实体（租户级数据），存储客户基础信息、意向需求和跟进状态，支撑房源匹配",
        example = "{\"tenantId\": 1001, \"name\": \"李四\", \"phone\": \"13900139000\", \"idCard\": \"330106199501011234\", \"source\": \"线上\", \"intendedRegionId\": 201, \"intendedPriceMin\": 100.00, \"intendedPriceMax\": 150.00, \"intendedHouseType\": \"两居室\", \"customerType\": \"ORDINARY\", \"potentialLevel\": 2, \"status\": \"ACTIVE\", \"createAgentId\": 3001}"
)
public class Customer implements Serializable {

    /**
     * 序列化版本号：保证对象序列化/反序列化一致性，避免类结构变更导致反序列化异常
     */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 客户主键ID
     * 自增策略，唯一标识单个客户，无业务含义，仅作为数据库主键
     */
    @Schema(
            description = "客户主键ID（自增）",
            example = "1",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY // 只读：新增时数据库自增，无需传值
    )
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID
     * 关联租户表主键，标识客户归属的租户，核心隔离字段，非空
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
     * 客户姓名
     * 真实姓名，非空，长度≤20字符
     */
    @Schema(
            description = "客户真实姓名",
            example = "李四",
            nullable = false,
            maxLength = 20
    )
    @NotBlank(message = "客户姓名不能为空")
    @Size(max = 20, message = "客户姓名长度不能超过20字符")
    @TableField(value = "name")
    private String name;

    /**
     * 联系电话
     * 11位有效手机号，客户核心联系方式，非空，唯一（建议数据库加唯一索引）
     */
    @Schema(
            description = "客户联系电话（11位有效手机号）",
            example = "13900139000",
            nullable = false,
            pattern = "^1[3-9]\\d{9}$"
    )
    @NotBlank(message = "联系电话不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式错误（需为11位有效手机号）")
    @TableField(value = "phone")
    private String phone;

    /**
     * 身份证号
     * 18位有效身份证号（含最后一位X），客户身份唯一标识，非空
     */
    @Schema(
            description = "客户身份证号（18位，支持最后一位X）",
            example = "330106199501011234",
            nullable = false,
            pattern = "^[1-9]\\d{5}(18|19|20)\\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]$"
    )
    @NotBlank(message = "身份证号不能为空")
    @Pattern(regexp = "^[1-9]\\d{5}(18|19|20)\\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]$",
            message = "身份证号格式错误（需为18位有效身份证号）")
    @TableField(value = "id_card")
    private String idCard;

    /**
     * 客户来源
     * 客户获取渠道，如：门店、线上（官网/小程序）、转介绍、老客户复购，非空，长度≤30字符
     */
    @Schema(
            description = "客户来源（如：门店、线上、转介绍、老客户复购）",
            example = "线上",
            nullable = false,
            maxLength = 30
    )
    @NotBlank(message = "客户来源不能为空")
    @Size(max = 30, message = "客户来源长度不能超过30字符")
    @TableField(value = "source")
    private String source;

    /**
     * 意向区域ID
     * 关联region表主键（同租户下的区域），客户意向购房/租房的区域，可空（无明确意向时为null）
     */
    @Schema(
            description = "意向区域ID（关联region表，仅同租户下的区域有效，无明确意向可填null）",
            example = "201",
            nullable = true
    )
    @TableField(value = "intended_region_id")
    private Long intendedRegionId;

    /**
     * 意向价格下限
     * 客户可接受的最低价格（单位：万元），非负，可空（无下限限制时为null），需≤价格上限
     */
    @Schema(
            description = "意向价格下限（单位：万元），非负，可空（无下限限制）",
            example = "100.00",
            nullable = true,
            minimum = "0.00",
            format = "decimal",
            multipleOf = 0.01
    )
    @DecimalMin(value = "0.00", inclusive = true, message = "意向价格下限不能为负数")
    @TableField(value = "intended_price_min")
    private BigDecimal intendedPriceMin;

    /**
     * 意向价格上限
     * 客户可接受的最高价格（单位：万元），非负，可空（无上限限制时为null），需≥价格下限
     */
    @Schema(
            description = "意向价格上限（单位：万元），非负，可空（无上限限制）",
            example = "150.00",
            nullable = true,
            minimum = "0.00",
            format = "decimal",
            multipleOf = 0.01
    )
    @DecimalMin(value = "0.00", inclusive = true, message = "意向价格上限不能为负数")
    @TableField(value = "intended_price_max")
    private BigDecimal intendedPriceMax;

    /**
     * 意向户型
     * 客户意向的房屋户型，如：一居室、两居室、三居室、别墅，可空，长度≤20字符
     */
    @Schema(
            description = "意向户型（如：一居室、两居室、三居室、别墅）",
            example = "两居室",
            nullable = true,
            maxLength = 20
    )
    @Size(max = 20, message = "意向户型长度不能超过20字符")
    @TableField(value = "intended_house_type")
    private String intendedHouseType;

    /**
     * 客户类型
     * 枚举值：ORDINARY-普通客户，VIP-会员客户（享优先服务），INVEST-投资客（侧重投资属性房源）
     */
    @Schema(
            description = "客户类型（ORDINARY=普通客户，VIP=会员客户，INVEST=投资客）",
            example = "ORDINARY",
            nullable = false,
            allowableValues = {"ORDINARY", "VIP", "INVEST"}
    )
    @NotBlank(message = "客户类型不能为空")
    @TableField(value = "customer_type")
    private String customerType;

    /**
     * 潜力等级
     * 客户成交潜力：1=低（短期无成交计划），2=中（3-6个月计划），3=高（1-3个月计划），非空
     */
    @Schema(
            description = "客户成交潜力等级（1=低，2=中，3=高）",
            example = "2",
            nullable = false,
            minimum = "1",
            maximum = "3"
    )
    @NotNull(message = "潜力等级不能为空")
    @Min(value = 1, message = "潜力等级不能小于1")
    @Max(value = 3, message = "潜力等级不能大于3")
    @TableField(value = "potential_level")
    private Byte potentialLevel;

    /**
     * 客户状态
     * 枚举值：ACTIVE-活跃（持续跟进），DEALED-已成交（已签订合同），DORMANT-休眠（3个月无跟进）
     */
    @Schema(
            description = "客户状态（ACTIVE=活跃，DEALED=已成交，DORMANT=休眠）",
            example = "ACTIVE",
            nullable = false,
            allowableValues = {"ACTIVE", "DEALED", "DORMANT"}
    )
    @NotBlank(message = "客户状态不能为空")
    @TableField(value = "status")
    private String status;

    /**
     * 创建人ID（经纪人）
     * 关联agent表主键（同租户下的经纪人），标识客户归属的跟进经纪人，非空
     */
    @Schema(
            description = "创建人ID（关联agent表，仅同租户下的经纪人有效）",
            example = "3001",
            nullable = false
    )
    @NotNull(message = "创建人ID不能为空")
    @TableField(value = "create_agent_id")
    private Long createAgentId;

    /**
     * 创建时间
     * 客户记录创建时间，数据库自动填充（新增时），无需手动传值，只读
     */
    @Schema(
            description = "客户记录创建时间（数据库自动填充）",
            example = "2025-11-26 09:00:00",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY,
            format = "date-time"
    )
    @TableField(value = "create_time", fill = FieldFill.INSERT) // 声明新增时自动填充
    private LocalDateTime createTime;

    /**
     * 更新时间
     * 客户信息更新时间（如需求变更、状态更新），数据库自动填充，无需手动传值，只读
     */
    @Schema(
            description = "客户记录更新时间（数据库自动填充）",
            example = "2025-11-26 10:00:00",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY,
            format = "date-time"
    )
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE) // 声明新增/修改时自动填充
    private LocalDateTime updateTime;
}
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
 * 客户删除备份表（租户级数据存档）
 * <p>
 * 核心业务说明：
 * 1. 存档逻辑：客户数据从主表（customer）删除时，自动同步全量数据到本表，用于客户信息追溯、恢复或合规审计；
 * 2. 租户隔离：所有字段绑定tenant_id，仅当前租户可查询/操作自身的客户备份数据，保护客户隐私；
 * 3. 关键追溯字段：保留 original_id（原客户ID）、delete_time（删除时间）、delete_operator（删除人），支撑操作审计；
 * 4. 数据完整性：同步主表所有核心字段（基础信息、意向需求、客户分层等），确保备份数据可完整还原原客户信息；
 * 5. 审计规范：删除人、删除时间为必填审计字段，不可为空，便于追溯删除操作责任人。
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Getter
@Setter
@TableName(value = "customer_backup", autoResultMap = true) // 开启自动结果映射（兼容复杂类型）
@Schema(
        name = "CustomerBackup",
        description = "客户删除备份实体（租户级存档），存储删除的客户全量数据，用于追溯/恢复/审计",
        example = "{\"originalId\": 1, \"tenantId\": 1001, \"name\": \"李四\", \"phone\": \"13900139000\", \"idCard\": \"330106199501011234\", \"source\": \"线上\", \"intendedRegionId\": 201, \"intendedPriceMin\": 100.00, \"intendedPriceMax\": 150.00, \"intendedHouseType\": \"两居室\", \"customerType\": \"ORDINARY\", \"potentialLevel\": 2, \"status\": \"ACTIVE\", \"createAgentId\": 3001, \"deleteOperator\": \"系统管理员\"}"
)
public class CustomerBackup implements Serializable {

    /**
     * 序列化版本号：保证对象序列化/反序列化一致性，避免类结构变更导致反序列化异常
     */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 备份记录主键ID
     * 自增策略，唯一标识单条客户备份记录，无业务含义，仅作为数据库主键
     */
    @Schema(
            description = "备份记录主键ID（自增）",
            example = "1",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY // 只读：新增时数据库自增，无需传值
    )
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 原客户ID
     * 关联customer表主键，标识本条备份数据对应的原客户，非空（核心追溯字段）
     */
    @Schema(
            description = "原客户ID（关联customer表主键）",
            example = "1",
            nullable = false
    )
    @NotNull(message = "原客户ID不能为空")
    @TableField(value = "original_id", exist = true)
    private Long originalId;

    /**
     * 租户ID
     * 关联租户表主键，标识备份数据归属的租户，核心隔离字段，非空
     */
    @Schema(
            description = "租户ID（归属租户，关联租户表主键）",
            example = "1001",
            nullable = false
    )
    @NotNull(message = "租户ID不能为空")
    @TableField(value = "tenant_id")
    private Long tenantId;

    /**
     * 原客户姓名
     * 备份原客户的真实姓名，非空
     */
    @Schema(
            description = "原客户真实姓名",
            example = "李四",
            nullable = false,
            maxLength = 20
    )
    @NotBlank(message = "原客户姓名不能为空")
    @Size(max = 20, message = "原客户姓名长度不能超过20字符")
    @TableField(value = "name")
    private String name;

    /**
     * 原联系电话
     * 备份原客户的11位有效手机号，非空
     */
    @Schema(
            description = "原客户联系电话（11位有效手机号）",
            example = "13900139000",
            nullable = false,
            pattern = "^1[3-9]\\d{9}$"
    )
    @NotBlank(message = "原联系电话不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式错误（需为11位有效手机号）")
    @TableField(value = "phone")
    private String phone;

    /**
     * 原身份证号
     * 备份原客户的18位有效身份证号，非空
     */
    @Schema(
            description = "原客户身份证号（18位，支持最后一位X）",
            example = "330106199501011234",
            nullable = false,
            pattern = "^[1-9]\\d{5}(18|19|20)\\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]$"
    )
    @NotBlank(message = "原身份证号不能为空")
    @Pattern(regexp = "^[1-9]\\d{5}(18|19|20)\\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]$",
            message = "身份证号格式错误（需为18位有效身份证号）")
    @TableField(value = "id_card")
    private String idCard;

    /**
     * 原客户来源
     * 备份原客户的获取渠道（如门店/线上/转介绍），非空
     */
    @Schema(
            description = "原客户来源（如：门店、线上、转介绍）",
            example = "线上",
            nullable = false,
            maxLength = 30
    )
    @NotBlank(message = "原客户来源不能为空")
    @Size(max = 30, message = "原客户来源长度不能超过30字符")
    @TableField(value = "source")
    private String source;

    /**
     * 原意向区域ID
     * 备份原客户的意向区域ID（关联region表），可空（原客户无明确意向时为null）
     */
    @Schema(
            description = "原客户意向区域ID（关联region表，无明确意向可填null）",
            example = "201",
            nullable = true
    )
    @TableField(value = "intended_region_id")
    private Long intendedRegionId;

    /**
     * 原意向价格下限
     * 备份原客户的最低可接受价格（单位：万元），非负，可空
     */
    @Schema(
            description = "原客户意向价格下限（单位：万元），非负，可空",
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
     * 原意向价格上限
     * 备份原客户的最高可接受价格（单位：万元），非负，可空
     */
    @Schema(
            description = "原客户意向价格上限（单位：万元），非负，可空",
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
     * 原意向户型
     * 备份原客户的意向户型（如一居室/两居室），可空
     */
    @Schema(
            description = "原客户意向户型（如：一居室、两居室）",
            example = "两居室",
            nullable = true,
            maxLength = 20
    )
    @Size(max = 20, message = "意向户型长度不能超过20字符")
    @TableField(value = "intended_house_type")
    private String intendedHouseType;

    /**
     * 原客户类型
     * 备份原客户的类型（ORDINARY-普通/VIP-会员/INVEST-投资客），非空
     */
    @Schema(
            description = "原客户类型（ORDINARY=普通客户，VIP=会员客户，INVEST=投资客）",
            example = "ORDINARY",
            nullable = false,
            allowableValues = {"ORDINARY", "VIP", "INVEST"}
    )
    @NotBlank(message = "原客户类型不能为空")
    @TableField(value = "customer_type")
    private String customerType;

    /**
     * 原潜力等级
     * 备份原客户的成交潜力等级（1-低/2-中/3-高），非空
     */
    @Schema(
            description = "原客户成交潜力等级（1=低，2=中，3=高）",
            example = "2",
            nullable = false,
            minimum = "1",
            maximum = "3"
    )
    @NotNull(message = "原潜力等级不能为空")
    @Min(value = 1, message = "潜力等级不能小于1")
    @Max(value = 3, message = "潜力等级不能大于3")
    @TableField(value = "potential_level")
    private Byte potentialLevel;

    /**
     * 原客户状态
     * 备份原客户删除时的状态（ACTIVE-活跃/DEALED-已成交/DORMANT-休眠），非空
     */
    @Schema(
            description = "原客户删除时的状态（ACTIVE=活跃，DEALED=已成交，DORMANT=休眠）",
            example = "ACTIVE",
            nullable = false,
            allowableValues = {"ACTIVE", "DEALED", "DORMANT"}
    )
    @NotBlank(message = "原客户状态不能为空")
    @TableField(value = "status")
    private String status;

    /**
     * 原创建人ID（经纪人）
     * 备份原客户的跟进经纪人ID（关联agent表），非空
     */
    @Schema(
            description = "原客户创建人ID（关联agent表，仅同租户下的经纪人有效）",
            example = "3001",
            nullable = false
    )
    @NotNull(message = "原创建人ID不能为空")
    @TableField(value = "create_agent_id")
    private Long createAgentId;

    /**
     * 删除时间
     * 客户数据被删除的时间，数据库自动填充（同步备份时），无需手动传值，只读
     */
    @Schema(
            description = "客户数据删除时间（数据库自动填充）",
            example = "2025-11-26 16:00:00",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY,
            format = "date-time"
    )
    @TableField(value = "delete_time", fill = FieldFill.INSERT) // 声明新增时自动填充
    private LocalDateTime deleteTime;

    /**
     * 删除人
     * 执行客户删除操作的人员（如经纪人姓名/系统管理员），非空（审计关键字段）
     */
    @Schema(
            description = "删除操作人（如经纪人姓名/系统管理员）",
            example = "系统管理员",
            nullable = false,
            maxLength = 50
    )
    @NotBlank(message = "删除人不能为空")
    @Size(max = 50, message = "删除人长度不能超过50字符")
    @TableField(value = "delete_operator")
    private String deleteOperator;
}
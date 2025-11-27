package com.house.deed.pavilion.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 房源与房东关联表（租户级数据隔离）
 * <p>
 * 核心业务说明：
 * 1. 多对多关联：解决“一个房源多个房东”或“一个房东多套房源”的关联场景，存储两者的归属关系及所有权占比；
 * 2. 租户隔离：所有字段绑定tenant_id，仅当前租户可查询/操作自身的房源-房东关联数据，保护业务数据隔离性；
 * 3. 核心关联：
 *    - house_id 关联房源表（同租户），标识关联的房源；
 *    - landlord_id 关联房东表（同租户），标识关联的房东；
 * 4. 所有权管理：ownership 字段记录房东对该房源的所有权占比（如100%、50%），支持共有产权场景，格式为“数字+%”；
 * 5. 唯一性约束：同一租户下，{house_id, landlord_id} 组合唯一（避免重复关联），所有权占比总和建议为100%（业务层校验）。
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Getter
@Setter
@TableName(value = "house_landlord", autoResultMap = true) // 开启自动结果映射
@Schema(
        name = "HouseLandlord",
        description = "房源与房东关联实体（租户级数据），解决多对多关联场景，记录归属关系及所有权占比",
        example = "{\"tenantId\": 1001, \"houseId\": 101, \"landlordId\": 201, \"ownership\": \"100%\"}"
)
public class HouseLandlord implements Serializable {

    /**
     * 序列化版本号：保证对象序列化/反序列化一致性，避免类结构变更导致反序列化异常
     */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 关联记录主键ID
     * 自增策略，唯一标识单条房源-房东关联记录，无业务含义，仅作为数据库主键
     */
    @Schema(
            description = "关联记录主键ID（自增）",
            example = "1",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY // 只读：新增时数据库自增，无需传值
    )
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID
     * 关联租户表主键，标识关联记录归属的租户，核心隔离字段，非空
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
     * 房源ID
     * 关联house表主键（同租户下的房源），标识关联的房源，非空（核心关联字段）
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
     * 房东ID
     * 关联landlord表主键（同租户下的房东），标识关联的房东，非空（核心关联字段）
     */
    @Schema(
            description = "房东ID（关联landlord表，仅同租户下的房东有效）",
            example = "201",
            nullable = false
    )
    @NotNull(message = "房东ID不能为空")
    @TableField(value = "landlord_id")
    private Long landlordId;

    /**
     * 所有权占比
     * 房东对该房源的所有权比例（格式：数字+%，如100%、50%、33.33%），非空，长度≤10字符
     */
    @Schema(
            description = "所有权占比（格式：数字+%，如100%、50%、33.33%）",
            example = "100%",
            nullable = false,
            maxLength = 10,
            pattern = "^\\d+(\\.\\d+)?%$" // 校验“数字+%”格式（支持整数/小数）
    )
    @NotBlank(message = "所有权占比不能为空")
    @Size(max = 10, message = "所有权占比长度不能超过10字符")
    @Pattern(regexp = "^\\d+(\\.\\d+)?%$", message = "所有权占比格式错误（需为数字+%，如100%、33.33%）")
    @TableField(value = "ownership")
    private String ownership;

    /**
     * 创建时间
     * 关联记录创建时间，数据库自动填充（新增时），无需手动传值，只读
     */
    @Schema(
            description = "关联记录创建时间（数据库自动填充）",
            example = "2025-11-26 13:00:00",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY,
            format = "date-time"
    )
    @TableField(value = "create_time", fill = FieldFill.INSERT) // 声明新增时自动填充
    private LocalDateTime createTime;
}
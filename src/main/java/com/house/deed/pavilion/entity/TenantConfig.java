package com.house.deed.pavilion.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
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
import java.time.LocalDateTime;

/**
 * 租户个性化配置表（分条存储 + 租户隔离 + 系统默认兜底）
 * <p>
 * 核心业务说明：
 * 1. 灵活配置模式：分条存储租户个性化配置项（替代 Tenant 表的 config_json 字段），支持单个配置项独立修改，适配多场景配置需求；
 * 2. 租户隔离规则：
 *    - tenant_id=0：系统默认配置（内置基础配置项，不可删除，租户可覆盖）；
 *    - tenant_id>0：租户自定义配置（优先级高于系统默认，仅当前租户生效）；
 * 3. 配置核心约束：
 *    - 唯一性：同一租户下 config_key 唯一（tenant_id + config_key 组合唯一），避免配置项重复；
 *    - 系统内置：is_system=1 为系统内置配置（不可删除，仅可修改值），is_system=0 为租户自定义（可增删改）；
 *    - 配置类型：config_key 为配置项唯一标识（如佣金比例、最大房源数），config_value 支持字符串/数值/布尔等格式（需业务层解析）；
 * 4. 业务价值：支持不同租户个性化配置（如A中介佣金比例2%，B中介3%），系统默认配置兜底，降低配置维护复杂度。
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Getter
@Setter
@TableName(value = "tenant_config", autoResultMap = true) // 开启自动结果映射
@Schema(
        name = "TenantConfig",
        description = "租户个性化配置实体（分条存储），支持租户自定义配置与系统默认配置兜底，适配多场景灵活配置",
        example = "{\"tenantId\": 1001, \"configKey\": \"COMMISSION_RATE\", \"configValue\": \"0.02\", \"configDesc\": \"房源成交佣金比例（小数形式，如0.02=2%）\", \"isSystem\": 0}"
)
public class TenantConfig implements Serializable {

    /**
     * 序列化版本号：保证对象序列化/反序列化一致性，避免类结构变更导致反序列化异常
     */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 配置项主键ID
     * 自增策略，唯一标识单个配置项，无业务含义，仅作为数据库主键
     */
    @Schema(
            description = "配置项主键ID（自增）",
            example = "801",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY // 只读：新增时数据库自增，无需传值
    )
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID
     * 关联租户表主键，标识配置归属；tenant_id=0为系统默认配置，tenant_id>0为租户自定义配置
     */
    @Schema(
            description = "租户ID（0=系统默认配置，>0=租户自定义配置，关联tenant表主键）",
            example = "1001",
            nullable = false
    )
    @NotNull(message = "租户ID不能为空")
    @TableField(value = "tenant_id", exist = true)
    private Long tenantId;

    /**
     * 配置项键
     * 配置项唯一标识（大写字母+下划线命名，如COMMISSION_RATE=佣金比例、MAX_HOUSE_NUM=最大房源数），非空，长度≤50字符
     */
    @Schema(
            description = "配置项键（大写字母+下划线命名，如COMMISSION_RATE=佣金比例）",
            example = "COMMISSION_RATE",
            nullable = false,
            maxLength = 50,
            pattern = "^[A-Z_]+$"
    )
    @NotBlank(message = "配置项键不能为空")
    @Size(max = 50, message = "配置项键长度不能超过50字符")
    @Pattern(regexp = "^[A-Z_]+$", message = "配置项键仅支持大写字母和下划线（如COMMISSION_RATE）")
    @TableField(value = "config_key")
    private String configKey;

    /**
     * 配置项值
     * 配置项具体值（支持字符串、数值、布尔等格式，如0.02=2%、100=最大房源数、true=启用审核），非空，长度≤200字符
     */
    @Schema(
            description = "配置项值（支持字符串/数值/布尔格式，如0.02=2%、true=启用）",
            example = "0.02",
            nullable = false,
            maxLength = 200
    )
    @NotBlank(message = "配置项值不能为空")
    @Size(max = 200, message = "配置项值长度不能超过200字符")
    @TableField(value = "config_value")
    private String configValue;

    /**
     * 配置项说明
     * 配置项用途描述（如“房源成交佣金比例（小数形式）”），可空，长度≤300字符
     */
    @Schema(
            description = "配置项用途说明（如“房源成交佣金比例（小数形式）”）",
            example = "房源成交佣金比例（小数形式，如0.02=2%）",
            nullable = true,
            maxLength = 300
    )
    @Size(max = 300, message = "配置项说明长度不能超过300字符")
    @TableField(value = "config_desc")
    private String configDesc;

    /**
     * 是否系统内置
     * 枚举值：1=是（系统内置配置，不可删除，仅可修改值），0=否（租户自定义配置，可增删改），非空
     */
    @Schema(
            description = "是否系统内置配置（1=是，不可删除；0=否，租户自定义）",
            example = "0",
            nullable = false,
            allowableValues = {"0", "1"}
    )
    @NotNull(message = "是否系统内置不能为空")
    @Min(value = 0, message = "是否系统内置仅支持0（否）或1（是）")
    @Max(value = 1, message = "是否系统内置仅支持0（否）或1（是）")
    @TableField(value = "is_system")
    private Byte isSystem;

    /**
     * 创建时间
     * 配置项创建时间（系统内置为初始化时间，租户自定义为创建时间），数据库自动填充，只读
     */
    @Schema(
            description = "配置项创建时间（数据库自动填充）",
            example = "2025-11-28 14:00:00",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY,
            format = "date-time"
    )
    @TableField(value = "create_time", fill = FieldFill.INSERT) // 声明新增时自动填充
    private LocalDateTime createTime;

    /**
     * 更新时间
     * 配置项更新时间（如修改配置值），数据库自动填充，只读
     */
    @Schema(
            description = "配置项更新时间（数据库自动填充）",
            example = "2025-11-28 14:30:00",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY,
            format = "date-time"
    )
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE) // 声明新增/修改时自动填充
    private LocalDateTime updateTime;
}
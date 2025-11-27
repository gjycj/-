package com.house.deed.pavilion.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Flyway 数据库版本控制历史表（自动生成与维护，禁止手动操作）
 * <p>
 * 核心特性说明：
 * 1. 自动维护：本表由 Flyway 数据库版本迁移工具自动创建、插入和更新，记录所有数据库迁移脚本（SQL/JAVA）的执行轨迹；
 * 2. 核心用途：支撑数据库版本回溯、迁移脚本完整性校验、多环境数据库结构一致性保障；
 * 3. 数据来源：所有字段值由 Flyway 迁移时自动填充，无需人工干预，也禁止手动修改（否则会导致 Flyway 版本控制逻辑异常）；
 * 4. 字段含义：严格对应 Flyway 迁移机制，例如 `version` 为数据库版本号，`checksum` 为脚本防篡改校验和，`success` 为迁移执行结果。
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Getter
@Setter
@TableName(value = "flyway_schema_history") // 保持表名与 Flyway 默认生成一致
@Schema(
        name = "FlywaySchemaHistory",
        description = "Flyway 数据库版本迁移历史表（自动维护，禁止手动CRUD），记录迁移脚本执行详情、版本变更及执行状态",
        example = "{\"installedRank\": 3, \"version\": \"1.2.0\", \"description\": \"add_tenant_isolation\", \"type\": \"SQL\", \"script\": \"V1.2.0__add_tenant_isolation.sql\", \"checksum\": 987654, \"installedBy\": \"house_admin\", \"installedOn\": \"2025-11-26 10:15:30\", \"executionTime\": 800, \"success\": true}"
)
public class FlywaySchemaHistory implements Serializable {

    /**
     * 序列化版本号：保证对象序列化/反序列化一致性，避免类结构变更导致解析异常
     */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 迁移执行顺序排名（主键）
     * Flyway 按迁移脚本顺序自动分配的自增编号，唯一标识单次迁移执行记录，决定版本回溯顺序
     */
    @Schema(
            description = "迁移脚本执行顺序排名（自增主键），决定版本回溯优先级",
            example = "3",
            nullable = false,
            accessMode = Schema.AccessMode.READ_ONLY // 只读：Flyway 自动生成，禁止手动修改
    )
    @TableId(value = "installed_rank", type = IdType.AUTO)
    private Integer installedRank;

    /**
     * 数据库版本号
     * 迁移脚本对应的数据库版本（遵循语义化版本，如 1.0、1.2.0），空值表示基线迁移（初始化数据库结构）
     */
    @Schema(
            description = "数据库版本号（语义化版本，如 1.2.0），空值=基线迁移",
            example = "1.2.0",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY
    )
    @TableField("version")
    private String version;

    /**
     * 迁移脚本描述
     * 由迁移脚本文件名定义的简短描述（如 add_tenant_isolation=新增租户隔离字段），用于快速识别迁移目的
     */
    @Schema(
            description = "迁移脚本描述（由脚本文件名定义，如 add_tenant_isolation=新增租户隔离）",
            example = "add_tenant_isolation",
            nullable = false,
            accessMode = Schema.AccessMode.READ_ONLY
    )
    @TableField("description")
    private String description;

    /**
     * 迁移脚本类型
     * Flyway 支持的迁移脚本类型，常用值：SQL=SQL脚本，JAVA=Java迁移类，UNDO=回滚脚本
     */
    @Schema(
            description = "迁移脚本类型（SQL=SQL脚本，JAVA=Java迁移类，UNDO=回滚脚本）",
            example = "SQL",
            nullable = false,
            accessMode = Schema.AccessMode.READ_ONLY
    )
    @TableField("type")
    private String type;

    /**
     * 迁移脚本路径/文件名
     * 迁移脚本的相对路径（默认在 classpath:db/migration/ 目录下），如 V1.2.0__add_tenant_isolation.sql
     */
    @Schema(
            description = "迁移脚本相对路径/文件名（默认存储在 classpath:db/migration/ 目录）",
            example = "V1.2.0__add_tenant_isolation.sql",
            nullable = false,
            accessMode = Schema.AccessMode.READ_ONLY
    )
    @TableField("script")
    private String script;

    /**
     * 脚本校验和
     * Flyway 计算的迁移脚本内容校验和（整数），用于校验脚本是否被篡改，空值表示未启用校验和
     */
    @Schema(
            description = "迁移脚本内容校验和（防篡改校验），空值=未启用校验和",
            example = "987654",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY
    )
    @TableField("checksum")
    private Integer checksum;

    /**
     * 执行迁移的数据库用户
     * 执行当前迁移脚本的数据库登录用户（如 house_admin、root），用于权限审计
     */
    @Schema(
            description = "执行迁移脚本的数据库登录用户（权限审计用）",
            example = "house_admin",
            nullable = false,
            accessMode = Schema.AccessMode.READ_ONLY
    )
    @TableField("installed_by")
    private String installedBy;

    /**
     * 迁移执行完成时间
     * 脚本执行成功/失败的时间戳（精确到时分秒），用于迁移时间线追溯
     */
    @Schema(
            description = "迁移脚本执行完成时间（精确到时分秒）",
            example = "2025-11-26 10:15:30",
            nullable = false,
            accessMode = Schema.AccessMode.READ_ONLY,
            format = "date-time"
    )
    @TableField("installed_on")
    private LocalDateTime installedOn;

    /**
     * 迁移执行耗时（毫秒）
     * 脚本执行的总耗时（单位：ms），用于评估迁移性能（如大表迁移耗时统计）
     */
    @Schema(
            description = "迁移脚本执行耗时（单位：毫秒），用于性能评估",
            example = "800",
            nullable = false,
            accessMode = Schema.AccessMode.READ_ONLY
    )
    @TableField("execution_time")
    private Integer executionTime;

    /**
     * 迁移执行结果
     * 执行状态：true=迁移成功，false=迁移失败（失败时需手动排查脚本问题后重新执行）
     */
    @Schema(
            description = "迁移执行结果（true=成功，false=失败；失败需手动排查脚本问题）",
            example = "true",
            nullable = false,
            accessMode = Schema.AccessMode.READ_ONLY
    )
    @TableField("success")
    private Boolean success;
}
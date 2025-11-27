package com.house.deed.pavilion.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 经纪人删除备份表（租户级数据存档）
 * <p>
 * 核心业务说明：
 * 1. 存档逻辑：经纪人数据从主表（agent）删除时，自动同步全量数据到本表，用于数据追溯、恢复或审计；
 * 2. 租户级隔离：所有字段绑定 tenant_id，仅当前租户可查询/操作自身的经纪人备份数据；
 * 3. 关键追溯字段：保留 original_id（原经纪人ID）、delete_time（删除时间）、delete_operator（删除人），支撑操作审计；
 * 4. 数据完整性：同步主表所有核心字段，确保备份数据可完整还原原经纪人信息。
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Getter
@Setter
@TableName(value = "agent_backup", autoResultMap = true) // 开启自动结果映射（兼容复杂类型）
@Schema(
        name = "AgentBackup",
        description = "经纪人删除备份实体（租户级存档），存储删除的经纪人全量数据，用于追溯/恢复",
        example = "{\"originalId\": 1, \"tenantId\": 1001, \"storeId\": 201, \"agentCode\": \"BJ001\", \"name\": \"张三\", \"phone\": \"13800138000\", \"idCard\": \"330106199001011234\", \"position\": \"经纪人\", \"level\": \"SENIOR\", \"entryTime\": \"2020-01-01\", \"status\": 1, \"deleteOperator\": \"系统管理员\"}"
)
public class AgentBackup implements Serializable {

    /**
     * 序列化版本号：保证对象序列化/反序列化一致性，避免类结构变更导致反序列化异常
     */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 备份记录主键ID
     * 自增策略，唯一标识单条备份记录，无业务含义，仅作为数据库主键
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
     * 原经纪人ID
     * 关联agent表主键，标识本条备份数据对应的原经纪人，非空（核心追溯字段）
     */
    @Schema(
            description = "原经纪人ID（关联agent表主键）",
            example = "1",
            nullable = false
    )
    @NotNull(message = "原经纪人ID不能为空")
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
     * 原所属门店ID
     * 关联store表主键（同租户下的门店），备份原经纪人的门店归属信息，非空
     */
    @Schema(
            description = "原经纪人所属门店ID（关联store表）",
            example = "201",
            nullable = false
    )
    @NotNull(message = "原所属门店ID不能为空")
    @TableField(value = "store_id")
    private Long storeId;

    /**
     * 原经纪人工号
     * 备份原经纪人的工号（租户内唯一），非空
     */
    @Schema(
            description = "原经纪人工号（租户内唯一）",
            example = "BJ001",
            nullable = false,
            maxLength = 10
    )
    @NotBlank(message = "原经纪人工号不能为空")
    @TableField(value = "agent_code")
    private String agentCode;

    /**
     * 原经纪人姓名
     * 备份原经纪人的真实姓名，非空
     */
    @Schema(
            description = "原经纪人真实姓名",
            example = "张三",
            nullable = false,
            maxLength = 20
    )
    @NotBlank(message = "原经纪人姓名不能为空")
    @TableField(value = "name")
    private String name;

    /**
     * 原联系电话
     * 备份原经纪人的11位手机号，非空
     */
    @Schema(
            description = "原经纪人联系电话（11位手机号）",
            example = "13800138000",
            nullable = false,
            pattern = "^1[3-9]\\d{9}$"
    )
    @NotBlank(message = "原联系电话不能为空")
    @TableField(value = "phone")
    private String phone;

    /**
     * 原身份证号
     * 备份原经纪人的18位身份证号，非空
     */
    @Schema(
            description = "原经纪人身份证号（18位，支持最后一位X）",
            example = "330106199001011234",
            nullable = false,
            pattern = "^[1-9]\\d{5}(18|19|20)\\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]$"
    )
    @NotBlank(message = "原身份证号不能为空")
    @TableField(value = "id_card")
    private String idCard;

    /**
     * 原职位
     * 备份原经纪人的职位（如经纪人、店长），非空
     */
    @Schema(
            description = "原经纪人职位（如：经纪人、店长）",
            example = "经纪人",
            nullable = false,
            maxLength = 30
    )
    @NotBlank(message = "原职位不能为空")
    @TableField(value = "position")
    private String position;

    /**
     * 原经纪人等级
     * 备份原经纪人的等级（JUNIOR-初级，SENIOR-高级，STAR-明星），非空
     */
    @Schema(
            description = "原经纪人等级（JUNIOR=初级，SENIOR=高级，STAR=明星）",
            example = "SENIOR",
            nullable = false,
            allowableValues = {"JUNIOR", "SENIOR", "STAR"}
    )
    @NotBlank(message = "原经纪人等级不能为空")
    @TableField(value = "level")
    private String level;

    /**
     * 原入职时间
     * 备份原经纪人的入职时间（格式：yyyy-MM-dd），非空
     */
    @Schema(
            description = "原经纪人入职时间（格式：yyyy-MM-dd）",
            example = "2020-01-01",
            nullable = false,
            format = "date"
    )
    @NotNull(message = "原入职时间不能为空")
    @TableField(value = "entry_time")
    private LocalDate entryTime;

    /**
     * 原状态
     * 备份原经纪人的状态（1=在职，0=离职），非空
     */
    @Schema(
            description = "原经纪人状态（1=在职，0=离职）",
            example = "1",
            nullable = false,
            allowableValues = {"0", "1"}
    )
    @NotNull(message = "原经纪人状态不能为空")
    @TableField(value = "status")
    private Byte status;

    /**
     * 删除时间
     * 经纪人数据被删除的时间，数据库自动填充（同步备份时），无需手动传值
     */
    @Schema(
            description = "经纪人数据删除时间（数据库自动填充）",
            example = "2025-11-26 15:30:00",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY,
            format = "date-time"
    )
    @TableField(value = "delete_time", fill = FieldFill.INSERT) // 声明新增时自动填充
    private LocalDateTime deleteTime;

    /**
     * 删除人
     * 执行经纪人删除操作的人员（如经纪人姓名/系统管理员），非空（审计关键字段）
     */
    @Schema(
            description = "删除操作人（如经纪人姓名/系统管理员）",
            example = "系统管理员",
            nullable = false,
            maxLength = 50
    )
    @NotBlank(message = "删除人不能为空")
    @TableField(value = "delete_operator")
    private String deleteOperator;
}
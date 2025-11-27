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
 * 系统操作日志表（租户级审计）
 * <p>
 * 核心业务说明：
 * 1. 审计追溯：记录系统所有关键操作轨迹（如新增房源、修改客户信息、删除合同），支撑合规审计、安全追责及问题排查；
 * 2. 租户隔离：默认按tenant_id隔离（仅当前租户可查询自身操作日志），tenant_id=0时表示系统级操作（如管理员配置修改）；
 * 3. 审计三要素：
 *    - 操作人：operator_id（操作人ID，租户内为经纪人/管理员，系统操作为null）、operator_name（操作人姓名，冗余存储）；
 *    - 操作信息：module（操作模块）、operation_type（操作类型）、operation_content（操作详情，需明确变更前后差异）；
 *    - 环境信息：ip_address（操作客户端IP地址，支撑安全审计）；
 * 4. 数据规范：操作内容需详细可追溯（如“修改房源ID=101的价格：从180万→175万”），操作类型/模块枚举统一，确保日志可读性。
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Getter
@Setter
@TableName(value = "operation_log", autoResultMap = true) // 开启自动结果映射
@Schema(
        name = "OperationLog",
        description = "系统操作日志实体（租户级审计），记录系统关键操作轨迹，支撑合规审计和安全追责",
        example = "{\"tenantId\": 1001, \"module\": \"HOUSE_MANAGE\", \"operationType\": \"UPDATE\", \"operationContent\": \"修改房源ID=101的价格：从180.00万元→175.00万元\", \"operatorId\": 3001, \"operatorName\": \"张三（经纪人）\", \"ipAddress\": \"192.168.1.100\"}"
)
public class OperationLog implements Serializable {

    /**
     * 序列化版本号：保证对象序列化/反序列化一致性，避免类结构变更导致反序列化异常
     */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 日志主键ID
     * 自增策略，唯一标识单条操作日志，无业务含义，仅作为数据库主键
     */
    @Schema(
            description = "日志主键ID（自增）",
            example = "10001",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY // 只读：新增时数据库自增，无需传值
    )
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID
     * 关联租户表主键，标识日志归属的租户；tenant_id=0表示系统级操作（如管理员配置），可空
     */
    @Schema(
            description = "租户ID（归属租户，关联租户表主键；tenant_id=0表示系统级操作）",
            example = "1001",
            nullable = true
    )
    @TableField(value = "tenant_id", exist = true)
    private Long tenantId;

    /**
     * 操作模块
     * 枚举值：HOUSE_MANAGE-房源管理，CUSTOMER_MANAGE-客户管理，CONTRACT_MANAGE-合同管理，LANDLORD_MANAGE-房东管理，USER_MANAGE-用户管理，SYSTEM_CONFIG-系统配置，OTHER-其他模块，非空
     */
    @Schema(
            description = "操作模块（HOUSE_MANAGE=房源管理，CUSTOMER_MANAGE=客户管理，CONTRACT_MANAGE=合同管理，LANDLORD_MANAGE=房东管理，USER_MANAGE=用户管理，SYSTEM_CONFIG=系统配置，OTHER=其他）",
            example = "HOUSE_MANAGE",
            nullable = false,
            allowableValues = {"HOUSE_MANAGE", "CUSTOMER_MANAGE", "CONTRACT_MANAGE", "LANDLORD_MANAGE", "USER_MANAGE", "SYSTEM_CONFIG", "OTHER"}
    )
    @NotBlank(message = "操作模块不能为空")
    @TableField(value = "module")
    private String module;

    /**
     * 操作类型
     * 枚举值：ADD-新增，UPDATE-修改，DELETE-删除，QUERY-查询，IMPORT-导入，EXPORT-导出，CONFIG-配置修改，OTHER-其他操作，非空
     */
    @Schema(
            description = "操作类型（ADD=新增，UPDATE=修改，DELETE=删除，QUERY=查询，IMPORT=导入，EXPORT=导出，CONFIG=配置修改，OTHER=其他）",
            example = "UPDATE",
            nullable = false,
            allowableValues = {"ADD", "UPDATE", "DELETE", "QUERY", "IMPORT", "EXPORT", "CONFIG", "OTHER"}
    )
    @NotBlank(message = "操作类型不能为空")
    @TableField(value = "operation_type")
    private String operationType;

    /**
     * 操作内容
     * 详细描述操作行为（如“新增房源ID=101：房号=1单元301，价格=180万”“修改客户ID=401的手机号：13800138000→13900139000”），非空，长度≤500字符
     */
    @Schema(
            description = "操作详细内容（需明确操作对象、变更前后差异，如“修改房源ID=101的价格：180.00万→175.00万”）",
            example = "修改房源ID=101的价格：从180.00万元→175.00万元",
            nullable = false,
            maxLength = 500
    )
    @NotBlank(message = "操作内容不能为空")
    @Size(max = 500, message = "操作内容长度不能超过500字符")
    @TableField(value = "operation_content")
    private String operationContent;

    /**
     * 操作人ID
     * 关联对应角色表（租户内为经纪人/管理员ID，同租户；系统操作可为null），可空
     */
    @Schema(
            description = "操作人ID（租户内为经纪人/管理员ID，系统操作可为null）",
            example = "3001",
            nullable = true
    )
    @TableField(value = "operator_id")
    private Long operatorId;

    /**
     * 操作人姓名
     * 操作人真实姓名（冗余存储，如“张三（经纪人）”“系统管理员”），非空，长度≤50字符
     */
    @Schema(
            description = "操作人姓名（冗余存储，如“张三（经纪人）”“系统管理员”）",
            example = "张三（经纪人）",
            nullable = false,
            maxLength = 50
    )
    @NotBlank(message = "操作人姓名不能为空")
    @Size(max = 50, message = "操作人姓名长度不能超过50字符")
    @TableField(value = "operator_name")
    private String operatorName;

    /**
     * 操作IP地址
     * 操作客户端的IP地址（支持IPv4/IPv6），非空，用于安全审计和位置追溯
     */
    @Schema(
            description = "操作客户端IP地址（支持IPv4/IPv6）",
            example = "192.168.1.100",
            nullable = false,
            maxLength = 50,
            pattern = "^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$|^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$"
    )
    @NotBlank(message = "操作IP地址不能为空")
    @Size(max = 50, message = "IP地址长度不能超过50字符")
    @Pattern(regexp = "^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$|^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$", message = "IP地址格式错误（支持IPv4/IPv6）")
    @TableField(value = "ip_address")
    private String ipAddress;

    /**
     * 操作时间
     * 操作执行的时间（精确到时分秒），数据库自动填充，无需手动传值，只读
     */
    @Schema(
            description = "操作执行时间（数据库自动填充）",
            example = "2025-11-28 14:00:00",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY,
            format = "date-time"
    )
    @TableField(value = "create_time", fill = FieldFill.INSERT) // 声明新增时自动填充
    private LocalDateTime createTime;
}
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
 * 多租户核心信息表（租户隔离根表）
 * <p>
 * 核心业务说明：
 * 1. 租户隔离根基：所有业务表（房源/合同/门店/标签等）均通过tenant_id关联本表，是多租户数据隔离的核心锚点；
 * 2. 路由核心：tenant_code 作为分库/分表路由键（全局唯一），决定租户数据的存储节点，不可修改；
 * 3. 租户生命周期管控：
 *    - 状态流转：NORMAL(1)-正常 → DISABLED(0)-禁用（暂停租户所有操作） → EXPIRED(2)-过期（超期自动禁用）；
 *    - 过期规则：expire_time为空表示永久有效，非空时超期后状态自动切换为2（过期）；
 * 4. 个性化配置：config_json 存储租户个性化配置（如LOGO地址、业务流程开关、字段显示规则），JSON格式存储；
 * 5. 全局唯一约束：tenant_code 全局唯一（跨所有租户），tenant_name 建议租户内/全局唯一（提升辨识度）。
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Getter
@Setter
@TableName(value = "tenant", autoResultMap = true) // 开启自动结果映射
@Schema(
        name = "Tenant",
        description = "多租户核心信息实体（隔离根表），管控租户生命周期、路由规则及个性化配置",
        example = "{\"tenantCode\": \"T20251128001\", \"tenantName\": \"杭州链家房地产经纪有限公司\", \"contactPerson\": \"王经理\", \"contactPhone\": \"13800138000\", \"domain\": \"hz.lianjia.com\", \"configJson\": \"{\\\"logoUrl\\\":\\\"https://oss.example.com/logo/lianjia.png\\\",\\\"houseAuditSwitch\\\":true}\", \"status\": 1}"
)
public class Tenant implements Serializable {

    /**
     * 序列化版本号：保证对象序列化/反序列化一致性，避免类结构变更导致反序列化异常
     */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 租户主键ID
     * 自增策略，唯一标识单个租户，作为所有业务表tenant_id的关联键，无路由含义
     */
    @Schema(
            description = "租户主键ID（自增，业务表关联键）",
            example = "1001",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY // 只读：新增时数据库自增，无需传值
    )
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 租户编码
     * 分库/分表路由键（全局唯一，建议格式：T+年月日+3位序号，如T20251128001），非空，长度≤20字符，不可修改
     */
    @Schema(
            description = "租户编码（全局唯一，分库/分表路由键，格式：T+年月日+3位序号）",
            example = "T20251128001",
            nullable = false,
            maxLength = 20
    )
    @NotBlank(message = "租户编码不能为空")
    @Size(max = 20, message = "租户编码长度不能超过20字符")
    @TableField(value = "tenant_code")
    private String tenantCode;

    /**
     * 租户名称
     * 租户对外/对内展示名称（如中介公司全称），非空，长度≤100字符
     */
    @Schema(
            description = "租户名称（如中介公司全称）",
            example = "杭州链家房地产经纪有限公司",
            nullable = false,
            maxLength = 100
    )
    @NotBlank(message = "租户名称不能为空")
    @Size(max = 100, message = "租户名称长度不能超过100字符")
    @TableField(value = "tenant_name")
    private String tenantName;

    /**
     * 租户联系人
     * 租户对接负责人姓名（如管理员/商务联系人），非空，长度≤50字符
     */
    @Schema(
            description = "租户对接联系人姓名",
            example = "王经理",
            nullable = false,
            maxLength = 50
    )
    @NotBlank(message = "租户联系人不能为空")
    @Size(max = 50, message = "租户联系人长度不能超过50字符")
    @TableField(value = "contact_person")
    private String contactPerson;

    /**
     * 联系人电话
     * 租户联系人常用手机号（支持11位手机号，含+86前缀），非空，用于租户运维沟通
     */
    @Schema(
            description = "租户联系人电话（11位手机号，可带+86前缀）",
            example = "13800138000",
            nullable = false,
            maxLength = 20,
            pattern = "^(\\+86)?1[3-9]\\d{9}$"
    )
    @NotBlank(message = "联系人电话不能为空")
    @Size(max = 20, message = "联系人电话长度不能超过20字符")
    @Pattern(regexp = "^(\\+86)?1[3-9]\\d{9}$", message = "联系人电话格式错误（仅支持11位手机号）")
    @TableField(value = "contact_phone")
    private String contactPhone;

    /**
     * 租户独立域名
     * 租户自定义访问域名（如hz.lianjia.com），可空，长度≤100字符，需符合域名格式
     */
    @Schema(
            description = "租户独立访问域名（如hz.lianjia.com），可空",
            example = "hz.lianjia.com",
            nullable = true,
            maxLength = 100,
            pattern = "^[a-zA-Z0-9][a-zA-Z0-9-]{0,61}[a-zA-Z0-9]\\.[a-zA-Z]{2,}$"
    )
    @Size(max = 100, message = "租户域名长度不能超过100字符")
    @Pattern(regexp = "^[a-zA-Z0-9][a-zA-Z0-9-]{0,61}[a-zA-Z0-9]\\.[a-zA-Z]{2,}$", message = "域名格式错误（如hz.lianjia.com）")
    @TableField(value = "domain")
    private String domain;

    /**
     * 租户个性化配置
     * JSON格式存储租户个性化配置（如LOGO地址、业务流程开关、字段显示规则），可空，长度≤2000字符
     */
    @Schema(
            description = "租户个性化配置（JSON格式，如{\\\"logoUrl\\\":\\\"xxx\\\",\\\"houseAuditSwitch\\\":true}）",
            example = "{\"logoUrl\":\"https://oss.example.com/logo/lianjia.png\",\"houseAuditSwitch\":true}",
            nullable = true,
            maxLength = 2000
    )
    @Size(max = 2000, message = "个性化配置长度不能超过2000字符")
    @Pattern(regexp = "^\\{.*\\}$", message = "配置内容需为JSON格式（以{}包裹）", flags = Pattern.Flag.DOTALL)
    @TableField(value = "config_json")
    private String configJson;

    /**
     * 租户状态
     * 枚举值：1=正常（可正常操作所有业务），0=禁用（暂停所有操作），2=过期（超期自动禁用），非空
     */
    @Schema(
            description = "租户状态（1=正常，0=禁用，2=过期）",
            example = "1",
            nullable = false,
            allowableValues = {"0", "1", "2"}
    )
    @NotNull(message = "租户状态不能为空")
    @Min(value = 0, message = "租户状态仅支持0（禁用）、1（正常）、2（过期）")
    @Max(value = 2, message = "租户状态仅支持0（禁用）、1（正常）、2（过期）")
    @TableField(value = "status")
    private Byte status;

    /**
     * 租户过期时间
     * 为空表示永久有效，非空时超期后状态自动切换为2（过期），格式：yyyy-MM-dd HH:mm:ss
     */
    @Schema(
            description = "租户过期时间（为空表示永久有效，超期自动切换为过期状态）",
            example = "2026-11-28 00:00:00",
            nullable = true,
            format = "date-time"
    )
    @TableField(value = "expire_time")
    private LocalDateTime expireTime;

    /**
     * 创建时间
     * 租户注册/录入系统时间，数据库自动填充，无需手动传值，只读
     */
    @Schema(
            description = "租户创建时间（数据库自动填充）",
            example = "2025-11-28 09:00:00",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY,
            format = "date-time"
    )
    @TableField(value = "create_time", fill = FieldFill.INSERT) // 声明新增时自动填充
    private LocalDateTime createTime;

    /**
     * 更新时间
     * 租户信息更新时间（如状态变更、联系人修改、配置更新），数据库自动填充，无需手动传值，只读
     */
    @Schema(
            description = "租户信息更新时间（数据库自动填充）",
            example = "2025-11-28 10:00:00",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY,
            format = "date-time"
    )
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE) // 声明新增/修改时自动填充
    private LocalDateTime updateTime;
}
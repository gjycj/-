package com.house.deed.pavilion.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 标签表（租户级数据隔离）
 * <p>
 * 核心业务说明：
 * 1. 多场景分类：存储租户内通用标签（房源标签/客户标签），用于对房源、客户进行分类标记，支撑精准筛选（如“学区房”“刚需客户”）；
 * 2. 租户隔离：所有字段绑定tenant_id，仅当前租户可查询/操作自身标签数据，保护标签分类的独立性；
 * 3. 核心关联：
 *    - 房源关联：通过house_tag关联表实现“一个标签多个房源”的多对多关系；
 *    - 客户关联：可通过customer_tag关联表（扩展）实现“一个标签多个客户”的多对多关系；
 * 4. 关键约束：
 *    - 标签名称：tenant_id + tag_name 组合唯一（租户内标签名称不重复，避免分类混乱）；
 *    - 标签类型：区分标签适用场景（房源/客户），确保标签与关联对象类型匹配；
 * 5. 业务价值：标签化管理降低数据分类复杂度，提升筛选效率（如筛选“近地铁+学区房”的房源、“刚需+首套房”的客户）。
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Getter
@Setter
@TableName(value = "tag", autoResultMap = true) // 开启自动结果映射
@Schema(
        name = "Tag",
        description = "标签实体（租户级数据），存储房源/客户分类标签，支撑精准筛选和分类管理",
        example = "{\"tenantId\": 1001, \"tagName\": \"学区房\", \"tagType\": \"HOUSE\"}"
)
public class Tag implements Serializable {

    /**
     * 序列化版本号：保证对象序列化/反序列化一致性，避免类结构变更导致反序列化异常
     */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 标签主键ID
     * 自增策略，唯一标识单个标签，无业务含义，仅作为数据库主键
     */
    @Schema(
            description = "标签主键ID（自增）",
            example = "501",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY // 只读：新增时数据库自增，无需传值
    )
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID
     * 关联租户表主键，标识标签归属的租户，核心隔离字段，非空
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
     * 标签名称
     * 标签展示名称（如学区房、近地铁、刚需客户、首套房），非空，长度≤30字符，租户内唯一
     */
    @Schema(
            description = "标签名称（如学区房、近地铁、刚需客户），租户内唯一",
            example = "学区房",
            nullable = false,
            maxLength = 30
    )
    @NotBlank(message = "标签名称不能为空")
    @Size(max = 30, message = "标签名称长度不能超过30字符")
    @TableField(value = "tag_name")
    private String tagName;

    /**
     * 标签类型
     * 枚举值：HOUSE-房源标签（用于标记房源特性），CUSTOMER-客户标签（用于标记客户需求），非空
     */
    @Schema(
            description = "标签类型（HOUSE=房源标签，CUSTOMER=客户标签）",
            example = "HOUSE",
            nullable = false,
            allowableValues = {"HOUSE", "CUSTOMER"}
    )
    @NotBlank(message = "标签类型不能为空")
    @TableField(value = "tag_type")
    private String tagType;

    /**
     * 创建时间
     * 标签创建时间，数据库自动填充，无需手动传值，只读
     */
    @Schema(
            description = "标签创建时间（数据库自动填充）",
            example = "2025-11-26 10:00:00",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY,
            format = "date-time"
    )
    @TableField(value = "create_time", fill = FieldFill.INSERT) // 声明新增时自动填充
    private LocalDateTime createTime;
}
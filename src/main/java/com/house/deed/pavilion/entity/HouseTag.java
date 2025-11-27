package com.house.deed.pavilion.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 房源与标签关联表（租户级数据隔离）
 * <p>
 * 核心业务说明：
 * 1. 多对多关联：解决“一个房源多个标签”或“一个标签多个房源”的关联场景，用于给房源打标签（如近地铁、学区房、拎包入住）；
 * 2. 租户隔离：所有字段绑定tenant_id，仅当前租户可查询/操作自身的房源-标签关联数据，保护业务数据隔离性；
 * 3. 核心关联：
 *    - house_id 关联房源表（同租户），标识被打标签的房源；
 *    - tag_id 关联标签表（tag，同租户），标识关联的标签；
 * 4. 唯一性约束：同一租户下，{house_id, tag_id} 组合唯一（避免同一房源重复添加同一标签）；
 * 5. 核心价值：支撑房源标签化筛选（如筛选“近地铁+学区房”的房源）、分类展示等功能，提升房源查找效率。
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Getter
@Setter
@TableName(value = "house_tag", autoResultMap = true) // 开启自动结果映射
@Schema(
        name = "HouseTag",
        description = "房源与标签关联实体（租户级数据），实现房源与标签的多对多关联，支撑标签化筛选",
        example = "{\"tenantId\": 1001, \"houseId\": 101, \"tagId\": 501}"
)
public class HouseTag implements Serializable {

    /**
     * 序列化版本号：保证对象序列化/反序列化一致性，避免类结构变更导致反序列化异常
     */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 关联记录主键ID
     * 自增策略，唯一标识单条房源-标签关联记录，无业务含义，仅作为数据库主键
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
     * 关联house表主键（同租户下的房源），标识被打标签的房源，非空（核心关联字段）
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
     * 标签ID
     * 关联tag表主键（同租户下的标签），标识关联的标签，非空（核心关联字段）
     */
    @Schema(
            description = "标签ID（关联tag表，仅同租户下的标签有效）",
            example = "501",
            nullable = false
    )
    @NotNull(message = "标签ID不能为空")
    @TableField(value = "tag_id")
    private Long tagId;

    /**
     * 创建时间
     * 关联记录创建时间（房源添加标签的时间），数据库自动填充，无需手动传值，只读
     */
    @Schema(
            description = "标签关联时间（数据库自动填充）",
            example = "2025-11-26 17:00:00",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY,
            format = "date-time"
    )
    @TableField(value = "create_time", fill = FieldFill.INSERT) // 声明新增时自动填充
    private LocalDateTime createTime;
}
package com.house.deed.pavilion.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
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
 * 房源图片表（租户级数据隔离）
 * <p>
 * 核心业务说明：
 * 1. 房源展示：存储租户内房源的各类图片（封面、户型图、室内场景图等），支撑房源详情页图文展示；
 * 2. 租户隔离：所有字段绑定tenant_id，仅当前租户可查询/操作自身房源的图片数据；
 * 3. 核心关联：house_id 关联房源表（同租户），确保图片与房源一一对应；
 * 4. 图片管控：
 *    - 分类管理：image_type 区分图片类型（封面/客厅/卧室等），支撑图片分类展示；
 *    - 排序控制：sort 字段控制图片展示顺序（数字越小越靠前，封面图建议设为0）；
 *    - 存储规范：image_url 存储图片访问路径（HTTP/HTTPS/OSS），确保图片可直接加载；
 * 5. 数据约束：单房源可关联多张图片，同一房源下的图片类型可重复（如多张卧室图），排序唯一。
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Getter
@Setter
@TableName(value = "house_image", autoResultMap = true) // 开启自动结果映射
@Schema(
        name = "HouseImage",
        description = "房源图片实体（租户级数据），存储房源各类图片（封面/客厅等），支撑房源图文展示",
        example = "{\"tenantId\": 1001, \"houseId\": 101, \"imageUrl\": \"https://oss.example.com/house/101/cover.jpg\", \"imageType\": \"COVER\", \"sort\": 0}"
)
public class HouseImage implements Serializable {

    /**
     * 序列化版本号：保证对象序列化/反序列化一致性，避免类结构变更导致反序列化异常
     */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 图片主键ID
     * 自增策略，唯一标识单张房源图片，无业务含义，仅作为数据库主键
     */
    @Schema(
            description = "图片主键ID（自增）",
            example = "1",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY // 只读：新增时数据库自增，无需传值
    )
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID
     * 关联租户表主键，标识图片归属的租户，核心隔离字段，非空
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
     * 关联house表主键（同租户下的房源），标识图片所属的房源，非空（核心关联字段）
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
     * 图片URL
     * 图片访问路径（支持HTTP/HTTPS/OSS），非空，长度≤500字符，确保图片可直接加载
     */
    @Schema(
            description = "图片访问URL（支持HTTP/HTTPS/OSS）",
            example = "https://oss.example.com/house/101/cover.jpg",
            nullable = false,
            maxLength = 500,
            pattern = "^(https?://|oss://).*$"
    )
    @NotBlank(message = "图片URL不能为空")
    @Size(max = 500, message = "图片URL长度不能超过500字符")
    @Pattern(regexp = "^(https?://|oss://).*$", message = "图片URL格式错误（支持HTTP/HTTPS/OSS）")
    @TableField(value = "image_url")
    private String imageUrl;

    /**
     * 图片类型
     * 枚举值：COVER-封面图（房源主图），LAYOUT-户型图，LIVING_ROOM-客厅，BEDROOM-卧室，KITCHEN-厨房，BATHROOM-卫生间，BALCONY-阳台，OTHER-其他场景
     */
    @Schema(
            description = "图片类型（COVER=封面图，LAYOUT=户型图，LIVING_ROOM=客厅，BEDROOM=卧室，KITCHEN=厨房，BATHROOM=卫生间，BALCONY=阳台，OTHER=其他）",
            example = "COVER",
            nullable = false,
            allowableValues = {"COVER", "LAYOUT", "LIVING_ROOM", "BEDROOM", "KITCHEN", "BATHROOM", "BALCONY", "OTHER"}
    )
    @NotBlank(message = "图片类型不能为空")
    @TableField(value = "image_type")
    private String imageType;

    /**
     * 排序序号
     * 图片展示顺序（数字越小越靠前），非负整数，默认0（封面图建议设为0），可空（默认0）
     */
    @Schema(
            description = "图片展示排序（数字越小越靠前，默认0）",
            example = "0",
            nullable = true,
            minimum = "0"
    )
    @Min(value = 0, message = "排序序号不能为负数")
    @TableField(value = "sort")
    private Integer sort;

    /**
     * 创建时间
     * 图片上传到系统的时间，数据库自动填充（新增时），无需手动传值，只读
     */
    @Schema(
            description = "图片上传时间（数据库自动填充）",
            example = "2025-11-26 11:00:00",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY,
            format = "date-time"
    )
    @TableField(value = "create_time", fill = FieldFill.INSERT) // 声明新增时自动填充
    private LocalDateTime createTime;
}
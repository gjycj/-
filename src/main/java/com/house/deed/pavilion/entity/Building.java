package com.house.deed.pavilion.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 楼栋信息表（租户级数据隔离）
 * <p>
 * 核心业务说明：
 * 1. 层级关联：隶属于楼盘（property表），是房源信息的上一级维度（房源需关联具体楼栋）；
 * 2. 租户隔离：所有字段绑定tenant_id，仅当前租户可查询/操作自身楼栋数据；
 * 3. 核心属性：记录楼栋号、单元数、总层数、建筑类型等物理属性，支撑房源的精准定位；
 * 4. 数据规范：单元数/总层数为正整数（至少1），建筑类型限定为行业通用值（板楼/塔楼等）。
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Getter
@Setter
@TableName(value = "building", autoResultMap = true) // 开启自动结果映射（兼容复杂类型）
@Schema(
        name = "Building",
        description = "楼栋信息实体（租户级数据），隶属于楼盘，记录楼栋物理属性及归属信息",
        example = "{\"tenantId\": 1001, \"propertyId\": 1, \"buildingNo\": \"1号楼\", \"unitCount\": 2, \"totalFloor\": 18, \"buildingType\": \"板楼\", \"createAgentId\": 3001}"
)
public class Building implements Serializable {

    /**
     * 序列化版本号：保证对象序列化/反序列化一致性，避免类结构变更导致反序列化异常
     */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 楼栋主键ID
     * 自增策略，唯一标识单栋楼栋，无业务含义，仅作为数据库主键
     */
    @Schema(
            description = "楼栋主键ID（自增）",
            example = "1",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY // 只读：新增时数据库自增，无需传值
    )
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID
     * 关联租户表主键，标识楼栋归属的租户，核心隔离字段，非空
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
     * 所属楼盘ID
     * 关联property表主键（同租户下的楼盘），标识楼栋归属的楼盘，非空（核心关联字段）
     */
    @Schema(
            description = "所属楼盘ID（关联property表，仅同租户下的楼盘有效）",
            example = "1",
            nullable = false
    )
    @NotNull(message = "所属楼盘ID不能为空")
    @TableField(value = "property_id")
    private Long propertyId;

    /**
     * 楼栋号
     * 如：1号楼、2栋、A座等，行业通用命名规则，非空，长度≤20字符
     */
    @Schema(
            description = "楼栋号（如：1号楼、2栋、A座）",
            example = "1号楼",
            nullable = false,
            maxLength = 20
    )
    @NotBlank(message = "楼栋号不能为空")
    @TableField(value = "building_no")
    private String buildingNo;

    /**
     * 单元数
     * 楼栋包含的单元数量（如2单元、3单元），正整数（至少1），非空
     */
    @Schema(
            description = "楼栋单元数（正整数，至少1）",
            example = "2",
            nullable = false,
            minimum = "1" // 数值最小约束
    )
    @NotNull(message = "单元数不能为空")
    @Min(value = 1, message = "单元数不能小于1")
    @TableField(value = "unit_count")
    private Integer unitCount;

    /**
     * 总层数
     * 楼栋的总楼层数（含地下层则需注明，如“18（含2层地下）”），正整数（至少1），非空
     */
    @Schema(
            description = "楼栋总层数（正整数，至少1；含地下层需注明，如18（含2层地下））",
            example = "18",
            nullable = false,
            minimum = "1"
    )
    @NotNull(message = "总层数不能为空")
    @Min(value = 1, message = "总层数不能小于1")
    @TableField(value = "total_floor")
    private Integer totalFloor;

    /**
     * 建筑类型
     * 行业通用分类：板楼、塔楼、板塔结合，非空，仅支持指定值
     */
    @Schema(
            description = "楼栋建筑类型（板楼=南北通透/塔楼=点式楼/板塔结合=混合结构）",
            example = "板楼",
            nullable = false,
            allowableValues = {"板楼", "塔楼", "板塔结合"} // 行业通用枚举值
    )
    @NotBlank(message = "建筑类型不能为空")
    @TableField(value = "building_type")
    private String buildingType;

    /**
     * 创建时间
     * 楼栋信息创建时间，数据库自动填充（新增时），无需手动传值，只读
     */
    @Schema(
            description = "楼栋信息创建时间（数据库自动填充）",
            example = "2025-11-26 10:00:00",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY,
            format = "date-time"
    )
    @TableField(value = "create_time", fill = FieldFill.INSERT) // 声明新增时自动填充
    private LocalDateTime createTime;

    /**
     * 创建人ID（经纪人）
     * 关联agent表主键（同租户下的经纪人），标识楼栋信息的创建者，非空
     */
    @Schema(
            description = "创建人ID（关联agent表，仅同租户下的经纪人有效）",
            example = "3001",
            nullable = false
    )
    @NotNull(message = "创建人ID不能为空")
    @TableField(value = "create_agent_id")
    private Long createAgentId;
}
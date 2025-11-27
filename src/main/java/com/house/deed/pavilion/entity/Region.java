package com.house.deed.pavilion.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
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
 * 区域管理表（租户级数据隔离 + 树形层级结构）
 * <p>
 * 核心业务说明：
 * 1. 树形层级设计：存储省/市/区/街道四级行政区域，支持树形结构查询（父级关联子级），支撑房源地址筛选、客户地址关联等功能；
 * 2. 租户隔离规则：
 *    - tenant_id=0：系统默认区域（全国标准省市区街道，不可修改）；
 *    - tenant_id>0：租户自定义扩展区域（如商圈、小区细分，仅当前租户可用）；
 * 3. 核心字段约束：
 *    - 层级控制：region_level（1=省，2=市，3=区，4=街道），子级层级必须比父级高1级；
 *    - 父子关联：parent_id=0为顶级区域（仅省级），其他层级需关联上级区域ID；
 *    - 行政编码：region_code 对应身份证前6位行政编码（系统默认区域必填，租户自定义区域可空）；
 * 4. 排序功能：sort 字段控制区域展示顺序（升序排列，数字越小越靠前），适配前端下拉选择或列表展示需求。
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Getter
@Setter
@TableName(value = "region", autoResultMap = true) // 开启自动结果映射
@Schema(
        name = "Region",
        description = "区域管理实体（租户级数据+树形层级），存储省/市/区/街道四级行政区域，支持租户自定义扩展",
        example = "{\"tenantId\": 0, \"parentId\": 0, \"regionName\": \"浙江省\", \"regionLevel\": 1, \"regionCode\": \"330000\", \"sort\": 10}"
)
public class Region implements Serializable {

    /**
     * 序列化版本号：保证对象序列化/反序列化一致性，避免类结构变更导致反序列化异常
     */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 区域主键ID
     * 自增策略，唯一标识单个区域，无业务含义，仅作为数据库主键
     */
    @Schema(
            description = "区域主键ID（自增）",
            example = "1",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY // 只读：新增时数据库自增，无需传值
    )
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID
     * 关联租户表主键，标识区域归属；tenant_id=0为系统默认区域（不可修改），tenant_id>0为租户自定义区域
     */
    @Schema(
            description = "租户ID（0=系统默认区域，>0=租户自定义区域）",
            example = "0",
            nullable = true
    )
    @TableField(value = "tenant_id", exist = true)
    private Long tenantId;

    /**
     * 父级区域ID
     * 关联当前表主键，标识上级区域；parent_id=0为顶级区域（仅省级），子级需关联对应父级ID
     */
    @Schema(
            description = "父级区域ID（0=顶级区域，仅省级可设为0）",
            example = "0",
            nullable = true
    )
    @DecimalMin(value = "0", inclusive = true, message = "父级区域ID不能为负数")
    @TableField(value = "parent_id")
    private Long parentId;

    /**
     * 区域名称
     * 行政区域名称（如浙江省、杭州市、滨江区、西兴街道），非空，长度≤50字符
     */
    @Schema(
            description = "区域名称（如浙江省、杭州市、滨江区）",
            example = "浙江省",
            nullable = false,
            maxLength = 50
    )
    @NotBlank(message = "区域名称不能为空")
    @Size(max = 50, message = "区域名称长度不能超过50字符")
    @TableField(value = "region_name")
    private String regionName;

    /**
     * 区域层级
     * 枚举值：1=省，2=市，3=区，4=街道，非空（子级层级必须比父级高1级）
     */
    @Schema(
            description = "区域层级（1=省，2=市，3=区，4=街道）",
            example = "1",
            nullable = false,
            allowableValues = {"1", "2", "3", "4"}
    )
    @NotNull(message = "区域层级不能为空")
    @Min(value = 1, message = "区域层级仅支持1（省）、2（市）、3（区）、4（街道）")
    @Max(value = 4, message = "区域层级仅支持1（省）、2（市）、3（区）、4（街道）")
    @TableField(value = "region_level")
    private Byte regionLevel;

    /**
     * 行政编码
     * 系统默认区域必填（对应身份证前6位行政编码，如330000=浙江省）；租户自定义区域可空，长度=6位
     */
    @Schema(
            description = "行政编码（系统默认区域必填，6位数字；租户自定义区域可空）",
            example = "330000",
            nullable = true,
            minLength = 6,
            maxLength = 6,
            pattern = "^\\d{6}$"
    )
    @Pattern(regexp = "^\\d{6}$", message = "行政编码必须为6位数字")
    @TableField(value = "region_code")
    private String regionCode;

    /**
     * 排序序号
     * 区域展示顺序（升序排列，数字越小越靠前），非负整数，可空（默认0）
     */
    @Schema(
            description = "区域排序序号（升序排列，数字越小越靠前）",
            example = "10",
            nullable = true,
            minimum = "0"
    )
    @Min(value = 0, message = "排序序号不能为负数")
    @TableField(value = "sort")
    private Integer sort;

    /**
     * 创建时间
     * 区域记录创建时间（系统默认区域为初始化时间，租户自定义区域为创建时间），数据库自动填充，只读
     */
    @Schema(
            description = "区域创建时间（数据库自动填充）",
            example = "2025-11-26 00:00:00",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY,
            format = "date-time"
    )
    @TableField(value = "create_time", fill = FieldFill.INSERT) // 声明新增时自动填充
    private LocalDateTime createTime;
}
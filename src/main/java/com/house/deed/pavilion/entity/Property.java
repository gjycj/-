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
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 楼盘信息表（租户级数据隔离）
 * <p>
 * 核心业务说明：
 * 1. 存储楼盘基础属性（名称、地址、开发商、绿化率等），作为房源关联的核心维度；
 * 2. 租户级隔离：所有字段均绑定 tenant_id，仅当前租户可查询/操作自身楼盘数据；
 * 3. 关联关系：region_id 关联 region 表（同租户下的区域字典），create_agent_id 关联 agent 表（创建该楼盘的经纪人）。
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Getter
@Setter
@TableName(value = "property", autoResultMap = true)
@Schema(
        name = "Property",
        description = "楼盘信息实体（租户级数据隔离）",
        example = "{\"tenantId\": 1001, \"propertyName\": \"滨江花园\", \"regionId\": 201, \"address\": \"杭州市滨江区江南大道123号\", \"developer\": \"滨江集团\", \"greenRate\": 35.5, \"completionYear\": 2018, \"propertyManagement\": \"滨江物业\", \"createAgentId\": 3001}"
)
public class Property implements Serializable {

    /**
     * 序列化版本号：保证对象序列化/反序列化的一致性，避免类结构变更导致的反序列化异常
     */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 楼盘主键ID
     * 自增策略，唯一标识单个楼盘，无业务含义，仅作为数据库主键
     */
    @Schema(
            description = "楼盘主键ID（自增）",
            example = "1",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY
    )
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID
     * 关联租户表主键，标识楼盘归属的租户，核心隔离字段，非空
     */
    @Schema(
            description = "租户ID（归属租户，关联租户表主键）",
            example = "1001",
            nullable = false // 替代 required = true
    )
    @NotNull(message = "租户ID不能为空") // JSR-380 校验，Swagger 自动识别为必填
    @TableField(value = "tenant_id", exist = true)
    private Long tenantId;

    /**
     * 楼盘名称
     * 如：滨江花园、万科城，非空，长度建议≤50字符
     */
    @Schema(
            description = "楼盘名称（如：滨江花园、万科城）",
            example = "滨江花园",
            nullable = false,
            maxLength = 50
    )
    @NotBlank(message = "楼盘名称不能为空") // 字符串非空且非空白
    @Size(max = 50, message = "楼盘名称长度不能超过50字符")
    @TableField(value = "property_name")
    private String propertyName;

    /**
     * 所属区域ID
     * 关联region表主键（同租户下的区域字典），如：滨江区、西湖区
     */
    @Schema(
            description = "所属区域ID（关联region表，仅同租户下的区域有效）",
            example = "201",
            nullable = false
    )
    @NotNull(message = "所属区域ID不能为空")
    @TableField(value = "region_id")
    private Long regionId;

    /**
     * 楼盘详细地址
     * 精确到门牌号，长度建议≤200字符
     */
    @Schema(
            description = "楼盘详细地址（精确到门牌号）",
            example = "杭州市滨江区江南大道123号",
            nullable = false,
            maxLength = 200
    )
    @NotBlank(message = "楼盘详细地址不能为空")
    @Size(max = 200, message = "楼盘详细地址长度不能超过200字符")
    @TableField(value = "address")
    private String address;

    /**
     * 开发商名称
     * 如：滨江集团、万科地产，长度建议≤50字符
     */
    @Schema(
            description = "楼盘开发商名称",
            example = "滨江集团",
            nullable = false,
            maxLength = 50
    )
    @NotBlank(message = "开发商名称不能为空")
    @Size(max = 50, message = "开发商名称长度不能超过50字符")
    @TableField(value = "developer")
    private String developer;

    /**
     * 绿化率（百分比）
     * 取值范围：0~100，保留1位小数，如35.5代表35.5%
     */
    @Schema(
            description = "绿化率（%），取值范围0~100，保留1位小数",
            example = "35.5",
            nullable = false,
            minimum = "0",
            maximum = "100",
            format = "decimal"
    )
    @NotNull(message = "绿化率不能为空")
    @Min(value = 0, message = "绿化率不能小于0")
    @Max(value = 100, message = "绿化率不能大于100")
    @TableField(value = "green_rate")
    private BigDecimal greenRate;

    /**
     * 建成年份
     * 取值范围：1900~当前年份，如2018代表2018年建成
     */
    @Schema(
            description = "楼盘建成年份（取值范围：1900~当前年份）",
            example = "2018",
            nullable = false,
            minimum = "1900",
            maximum = "2025"
    )
    @NotNull(message = "建成年份不能为空")
    @Min(value = 1900, message = "建成年份不能小于1900")
    @Max(value = 2025, message = "建成年份不能大于2025")
    @TableField(value = "completion_year")
    private Integer completionYear;

    /**
     * 物业公司名称
     * 如：滨江物业、绿城服务，长度建议≤50字符
     */
    @Schema(
            description = "物业服务公司名称",
            example = "滨江物业",
            nullable = false,
            maxLength = 50
    )
    @NotBlank(message = "物业公司名称不能为空")
    @Size(max = 50, message = "物业公司名称长度不能超过50字符")
    @TableField(value = "property_management")
    private String propertyManagement;

    /**
     * 创建时间
     * 数据库自动填充（新增时），无需手动传值
     */
    @Schema(
            description = "楼盘信息创建时间（数据库自动填充）",
            example = "2025-11-26 10:00:00",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY
    )
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     * 数据库自动填充（新增/修改时），无需手动传值
     */
    @Schema(
            description = "楼盘信息更新时间（数据库自动填充）",
            example = "2025-11-26 11:00:00",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY
    )
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 创建人ID（经纪人）
     * 关联agent表主键（同租户下的经纪人），标识该楼盘的创建者
     */
    @Schema(
            description = "创建人ID（关联经纪人表，仅同租户下的经纪人有效）",
            example = "3001",
            nullable = false
    )
    @NotNull(message = "创建人ID不能为空")
    @TableField(value = "create_agent_id")
    private Long createAgentId;
}
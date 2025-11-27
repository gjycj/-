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
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 房源信息表（租户核心业务数据，租户级数据隔离）
 * <p>
 * 核心业务说明：
 * 1. 房源核心：存储租户内房产的物理属性、交易属性、产权信息，是交易合同、客户匹配的基础数据；
 * 2. 租户隔离：所有字段绑定tenant_id，仅当前租户可查询/操作自身房源数据，严禁跨租户访问；
 * 3. 多维度关联：
 *    - 楼栋维度：building_id 关联楼栋表（同租户），明确房源所属物理位置；
 *    - 经纪人维度：create_agent_id 关联经纪人表（同租户），标识房源录入责任人；
 * 4. 核心属性分类：
 *    - 物理属性：户型、面积、楼层、朝向、装修情况，支撑客户精准筛选；
 *    - 交易属性：交易类型（出售/出租/可售可租）、挂牌价、房源状态（在售/已售等），支撑交易流程管控；
 *    - 产权属性：产权性质、产权证号、产权年限、抵押状态，支撑合规交易审核；
 * 5. 数据规范：面积/价格非负，楼层≤总楼层，产权年限≤70年（商品房常规上限），抵押详情在已抵押时必填。
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Getter
@Setter
@TableName(value = "house", autoResultMap = true) // 开启自动结果映射（兼容复杂类型）
@Schema(
        name = "House",
        description = "房源信息实体（租户核心数据），存储房产物理属性、交易属性及产权信息，支撑交易与客户匹配",
        example = "{\"tenantId\": 1001, \"buildingId\": 1, \"houseNo\": \"1单元301\", \"houseType\": \"3室2厅\", \"area\": 120.50, \"insideArea\": 105.30, \"floor\": 3, \"totalFloor\": 18, \"orientation\": \"南北通透\", \"decoration\": \"精装\", \"propertyRight\": \"商品房\", \"propertyRightCertNo\": \"浙房地权证杭字第123456号\", \"propertyRightYears\": 70, \"mortgageStatus\": \"NONE\", \"price\": 180.00, \"transactionType\": \"SALE\", \"status\": \"ON_SALE\", \"description\": \"中间楼层，南北通透，配套成熟\", \"createAgentId\": 3001}"
)
public class House implements Serializable {

    /**
     * 序列化版本号：保证对象序列化/反序列化一致性，避免类结构变更导致反序列化异常
     */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 房源主键ID
     * 自增策略，唯一标识单个房源，无业务含义，仅作为数据库主键
     */
    @Schema(
            description = "房源主键ID（自增）",
            example = "1",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY // 只读：新增时数据库自增，无需传值
    )
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID
     * 关联租户表主键，标识房源归属的租户，核心隔离字段，非空
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
     * 所属楼栋ID
     * 关联building表主键（同租户下的楼栋），明确房源所属物理楼栋，非空（核心关联字段）
     */
    @Schema(
            description = "所属楼栋ID（关联building表，仅同租户下的楼栋有效）",
            example = "1",
            nullable = false
    )
    @NotNull(message = "所属楼栋ID不能为空")
    @TableField(value = "building_id")
    private Long buildingId;

    /**
     * 房号
     * 楼栋内唯一标识（如1单元301、2栋502），非空，长度≤20字符
     */
    @Schema(
            description = "房号（楼栋内唯一，如1单元301、2栋502）",
            example = "1单元301",
            nullable = false,
            maxLength = 20
    )
    @NotBlank(message = "房号不能为空")
    @Size(max = 20, message = "房号长度不能超过20字符")
    @TableField(value = "house_no")
    private String houseNo;

    /**
     * 户型
     * 房产户型描述（如1室1厅、3室2厅、别墅），非空，长度≤20字符
     */
    @Schema(
            description = "房产户型（如1室1厅、3室2厅、别墅）",
            example = "3室2厅",
            nullable = false,
            maxLength = 20
    )
    @NotBlank(message = "户型不能为空")
    @Size(max = 20, message = "户型长度不能超过20字符")
    @TableField(value = "house_type")
    private String houseType;

    /**
     * 建筑面积
     * 房产建筑面积（单位：㎡），非负，保留2位小数，非空
     */
    @Schema(
            description = "建筑面积（单位：㎡），保留2位小数",
            example = "120.50",
            nullable = false,
            minimum = "0.00",
            format = "decimal",
            multipleOf = 0.01 // 确保2位小数精度
    )
    @NotNull(message = "建筑面积不能为空")
    @DecimalMin(value = "0.00", inclusive = true, message = "建筑面积不能为负数")
    @TableField(value = "area")
    private BigDecimal area;

    /**
     * 套内面积
     * 房产套内实际使用面积（单位：㎡），非负，保留2位小数，可空（未测量时为null）
     */
    @Schema(
            description = "套内面积（单位：㎡），保留2位小数，未测量可填null",
            example = "105.30",
            nullable = true,
            minimum = "0.00",
            format = "decimal",
            multipleOf = 0.01
    )
    @DecimalMin(value = "0.00", inclusive = true, message = "套内面积不能为负数")
    @TableField(value = "inside_area")
    private BigDecimal insideArea;

    /**
     * 所在楼层
     * 房源所在楼层（如3、15），正整数，非空，需≤总楼层
     */
    @Schema(
            description = "所在楼层（正整数，需≤总楼层）",
            example = "3",
            nullable = false,
            minimum = "1"
    )
    @NotNull(message = "所在楼层不能为空")
    @Min(value = 1, message = "所在楼层不能小于1")
    @TableField(value = "floor")
    private Integer floor;

    /**
     * 总楼层
     * 房源所属楼栋的总楼层数，正整数，非空
     */
    @Schema(
            description = "楼栋总楼层数（正整数）",
            example = "18",
            nullable = false,
            minimum = "1"
    )
    @NotNull(message = "总楼层不能为空")
    @Min(value = 1, message = "总楼层不能小于1")
    @TableField(value = "total_floor")
    private Integer totalFloor;

    /**
     * 朝向
     * 房产朝向（如南北通透、朝南、东南向），非空，长度≤20字符
     */
    @Schema(
            description = "房产朝向（如南北通透、朝南、东南向）",
            example = "南北通透",
            nullable = false,
            maxLength = 20
    )
    @NotBlank(message = "朝向不能为空")
    @Size(max = 20, message = "朝向长度不能超过20字符")
    @TableField(value = "orientation")
    private String orientation;

    /**
     * 装修情况
     * 枚举值：UNFINISHED-毛坯，SIMPLE-简装，DELUXE-精装，非空
     */
    @Schema(
            description = "装修情况（UNFINISHED=毛坯，SIMPLE=简装，DELUXE=精装）",
            example = "精装",
            nullable = false,
            allowableValues = {"UNFINISHED", "SIMPLE", "DELUXE"}
    )
    @NotBlank(message = "装修情况不能为空")
    @TableField(value = "decoration")
    private String decoration;

    /**
     * 产权性质
     * 房产产权类型（如商品房、经济适用房、回迁房、商住两用），非空，长度≤30字符
     */
    @Schema(
            description = "产权性质（如商品房、经济适用房、回迁房、商住两用）",
            example = "商品房",
            nullable = false,
            maxLength = 30
    )
    @NotBlank(message = "产权性质不能为空")
    @Size(max = 30, message = "产权性质长度不能超过30字符")
    @TableField(value = "property_right")
    private String propertyRight;

    /**
     * 产权证号
     * 房产产权证编号（如浙房地权证杭字第123456号），非空，长度≤50字符
     */
    @Schema(
            description = "产权证号（如浙房地权证杭字第123456号）",
            example = "浙房地权证杭字第123456号",
            nullable = false,
            maxLength = 50
    )
    @NotBlank(message = "产权证号不能为空")
    @Size(max = 50, message = "产权证号长度不能超过50字符")
    @TableField(value = "property_right_cert_no")
    private String propertyRightCertNo;

    /**
     * 产权年限
     * 房产产权使用年限（如70年、50年、40年），正整数，非空，≤70（常规上限）
     */
    @Schema(
            description = "产权年限（正整数，常规上限70年）",
            example = "70",
            nullable = false,
            minimum = "1",
            maximum = "70"
    )
    @NotNull(message = "产权年限不能为空")
    @Min(value = 1, message = "产权年限不能小于1")
    @Max(value = 70, message = "产权年限不能超过70年")
    @TableField(value = "property_right_years")
    private Integer propertyRightYears;

    /**
     * 抵押状态
     * 枚举值：NONE-无抵押，MORTGAGED-已抵押，非空（影响交易流程，已抵押需先解押）
     */
    @Schema(
            description = "抵押状态（NONE=无抵押，MORTGAGED=已抵押；已抵押需先解押才能交易）",
            example = "NONE",
            nullable = false,
            allowableValues = {"NONE", "MORTGAGED"}
    )
    @NotBlank(message = "抵押状态不能为空")
    @TableField(value = "mortgage_status")
    private String mortgageStatus;

    /**
     * 抵押详情
     * 抵押状态为MORTGAGED时必填（如“中国工商银行杭州分行，抵押金额50万元”），可空，长度≤200字符
     */
    @Schema(
            description = "抵押详情（抵押状态为已抵押时必填，如“抵押银行+抵押金额”）",
            example = "中国工商银行杭州分行，抵押金额50万元",
            nullable = true,
            maxLength = 200
    )
    @Size(max = 200, message = "抵押详情长度不能超过200字符")
    @TableField(value = "mortgage_details")
    private String mortgageDetails;

    /**
     * 挂牌价
     * 房源挂牌价格（单位：万元），非负，保留2位小数，非空（出售/出租均需定价）
     */
    @Schema(
            description = "挂牌价（单位：万元），保留2位小数；出售=总价，出租=月租金总额",
            example = "180.00",
            nullable = false,
            minimum = "0.00",
            format = "decimal",
            multipleOf = 0.01
    )
    @NotNull(message = "挂牌价不能为空")
    @DecimalMin(value = "0.00", inclusive = true, message = "挂牌价不能为负数")
    @TableField(value = "price")
    private BigDecimal price;

    /**
     * 交易类型
     * 枚举值：SALE-出售，RENT-出租，BOTH-可售可租，非空（决定房源交易方式）
     */
    @Schema(
            description = "交易类型（SALE=出售，RENT=出租，BOTH=可售可租）",
            example = "SALE",
            nullable = false,
            allowableValues = {"SALE", "RENT", "BOTH"}
    )
    @NotBlank(message = "交易类型不能为空")
    @TableField(value = "transaction_type")
    private String transactionType;

    /**
     * 房源状态
     * 枚举值：ON_SALE-在售/在租，RESERVED-已预订，SOLD-已售/已租，OFF_SHELF-下架，非空（管控房源展示与交易）
     */
    @Schema(
            description = "房源状态（ON_SALE=在售/在租，RESERVED=已预订，SOLD=已售/已租，OFF_SHELF=下架）",
            example = "ON_SALE",
            nullable = false,
            allowableValues = {"ON_SALE", "RESERVED", "SOLD", "OFF_SHELF"}
    )
    @NotBlank(message = "房源状态不能为空")
    @TableField(value = "status")
    private String status;

    /**
     * 房源描述
     * 补充房源优势、配套设施、周边环境等信息，可空，长度≤500字符
     */
    @Schema(
            description = "房源描述（补充优势、配套、周边环境等信息）",
            example = "中间楼层，南北通透，采光充足，周边300米内有地铁、超市、学校",
            nullable = true,
            maxLength = 500
    )
    @Size(max = 500, message = "房源描述长度不能超过500字符")
    @TableField(value = "description")
    private String description;

    /**
     * 录入经纪人ID
     * 关联agent表主键（同租户下的经纪人），标识房源录入责任人，非空
     */
    @Schema(
            description = "录入经纪人ID（关联agent表，仅同租户下的经纪人有效）",
            example = "3001",
            nullable = false
    )
    @NotNull(message = "录入经纪人ID不能为空")
    @TableField(value = "create_agent_id")
    private Long createAgentId;

    /**
     * 创建时间
     * 房源信息录入时间，数据库自动填充（新增时），无需手动传值，只读
     */
    @Schema(
            description = "房源录入时间（数据库自动填充）",
            example = "2025-11-26 09:30:00",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY,
            format = "date-time"
    )
    @TableField(value = "create_time", fill = FieldFill.INSERT) // 声明新增时自动填充
    private LocalDateTime createTime;

    /**
     * 更新时间
     * 房源信息更新时间（如价格调整、状态变更），数据库自动填充，无需手动传值，只读
     */
    @Schema(
            description = "房源信息更新时间（数据库自动填充）",
            example = "2025-11-26 10:30:00",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY,
            format = "date-time"
    )
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE) // 声明新增/修改时自动填充
    private LocalDateTime updateTime;
}
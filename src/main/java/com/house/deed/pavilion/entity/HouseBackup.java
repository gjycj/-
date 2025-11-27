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
 * 房源删除备份表（租户级数据存档）
 * <p>
 * 核心业务说明：
 * 1. 存档逻辑：房源数据从主表（house）删除时，自动同步全量数据到本表，用于房源信息追溯、恢复或合规审计；
 * 2. 租户隔离：所有字段绑定tenant_id，仅当前租户可查询/操作自身的房源备份数据，保护业务数据安全性；
 * 3. 关键追溯字段：保留 original_id（原房源ID）、delete_time（删除时间）、delete_operator（删除人），支撑操作审计；
 * 4. 数据完整性：同步主表所有核心字段（物理属性、交易属性、产权信息等），确保备份数据可完整还原原房源信息；
 * 5. 审计规范：删除人、删除时间为必填审计字段，不可为空，便于追溯删除操作责任人及时间节点。
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Getter
@Setter
@TableName(value = "house_backup", autoResultMap = true) // 开启自动结果映射（兼容复杂类型）
@Schema(
        name = "HouseBackup",
        description = "房源删除备份实体（租户级存档），存储删除的房源全量数据，用于追溯/恢复/审计",
        example = "{\"originalId\": 1, \"tenantId\": 1001, \"buildingId\": 1, \"houseNo\": \"1单元301\", \"houseType\": \"3室2厅\", \"area\": 120.50, \"insideArea\": 105.30, \"floor\": 3, \"totalFloor\": 18, \"orientation\": \"南北通透\", \"decoration\": \"精装\", \"propertyRight\": \"商品房\", \"propertyRightCertNo\": \"浙房地权证杭字第123456号\", \"propertyRightYears\": 70, \"mortgageStatus\": \"NONE\", \"mortgageDetails\": null, \"price\": 180.00, \"transactionType\": \"SALE\", \"status\": \"ON_SALE\", \"description\": \"中间楼层，南北通透\", \"createAgentId\": 3001, \"deleteOperator\": \"张三（经纪人）\"}"
)
public class HouseBackup implements Serializable {

    /**
     * 序列化版本号：保证对象序列化/反序列化一致性，避免类结构变更导致反序列化异常
     */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 备份记录主键ID
     * 自增策略，唯一标识单条房源备份记录，无业务含义，仅作为数据库主键
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
     * 原房源ID
     * 关联house表主键，标识本条备份数据对应的原房源，非空（核心追溯字段）
     */
    @Schema(
            description = "原房源ID（关联house表主键）",
            example = "1",
            nullable = false,
            accessMode = Schema.AccessMode.READ_ONLY
    )
    @NotNull(message = "原房源ID不能为空")
    @TableField(value = "original_id", exist = true)
    private Long originalId;

    /**
     * 租户ID
     * 关联租户表主键，标识备份数据归属的租户，核心隔离字段，非空
     */
    @Schema(
            description = "租户ID（归属租户，关联租户表主键）",
            example = "1001",
            nullable = false,
            accessMode = Schema.AccessMode.READ_ONLY
    )
    @NotNull(message = "租户ID不能为空")
    @TableField(value = "tenant_id")
    private Long tenantId;

    /**
     * 原所属楼栋ID
     * 备份原房源的所属楼栋ID（关联building表），非空
     */
    @Schema(
            description = "原房源所属楼栋ID（关联building表，仅同租户下的楼栋有效）",
            example = "1",
            nullable = false,
            accessMode = Schema.AccessMode.READ_ONLY
    )
    @NotNull(message = "原所属楼栋ID不能为空")
    @TableField(value = "building_id")
    private Long buildingId;

    /**
     * 原房号
     * 备份原房源的楼栋内唯一标识（如1单元301），非空
     */
    @Schema(
            description = "原房源房号（楼栋内唯一，如1单元301）",
            example = "1单元301",
            nullable = false,
            maxLength = 20,
            accessMode = Schema.AccessMode.READ_ONLY
    )
    @NotBlank(message = "原房号不能为空")
    @Size(max = 20, message = "原房号长度不能超过20字符")
    @TableField(value = "house_no")
    private String houseNo;

    /**
     * 原户型
     * 备份原房源的户型描述（如3室2厅），非空
     */
    @Schema(
            description = "原房源户型（如3室2厅、别墅）",
            example = "3室2厅",
            nullable = false,
            maxLength = 20,
            accessMode = Schema.AccessMode.READ_ONLY
    )
    @NotBlank(message = "原户型不能为空")
    @Size(max = 20, message = "原户型长度不能超过20字符")
    @TableField(value = "house_type")
    private String houseType;

    /**
     * 原建筑面积
     * 备份原房源的建筑面积（单位：㎡），非负，保留2位小数，非空
     */
    @Schema(
            description = "原房源建筑面积（单位：㎡），保留2位小数",
            example = "120.50",
            nullable = false,
            minimum = "0.00",
            format = "decimal",
            multipleOf = 0.01,
            accessMode = Schema.AccessMode.READ_ONLY
    )
    @NotNull(message = "原建筑面积不能为空")
    @DecimalMin(value = "0.00", inclusive = true, message = "原建筑面积不能为负数")
    @TableField(value = "area")
    private BigDecimal area;

    /**
     * 原套内面积
     * 备份原房源的套内面积（单位：㎡），非负，保留2位小数，可空
     */
    @Schema(
            description = "原房源套内面积（单位：㎡），保留2位小数，未测量可填null",
            example = "105.30",
            nullable = true,
            minimum = "0.00",
            format = "decimal",
            multipleOf = 0.01,
            accessMode = Schema.AccessMode.READ_ONLY
    )
    @DecimalMin(value = "0.00", inclusive = true, message = "原套内面积不能为负数")
    @TableField(value = "inside_area")
    private BigDecimal insideArea;

    /**
     * 原所在楼层
     * 备份原房源的所在楼层（正整数），非空，需≤原总楼层
     */
    @Schema(
            description = "原房源所在楼层（正整数，需≤原总楼层）",
            example = "3",
            nullable = false,
            minimum = "1",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    @NotNull(message = "原所在楼层不能为空")
    @Min(value = 1, message = "原所在楼层不能小于1")
    @TableField(value = "floor")
    private Integer floor;

    /**
     * 原总楼层
     * 备份原房源所属楼栋的总楼层数，正整数，非空
     */
    @Schema(
            description = "原房源所属楼栋的总楼层数（正整数）",
            example = "18",
            nullable = false,
            minimum = "1",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    @NotNull(message = "原总楼层不能为空")
    @Min(value = 1, message = "原总楼层不能小于1")
    @TableField(value = "total_floor")
    private Integer totalFloor;

    /**
     * 原朝向
     * 备份原房源的朝向（如南北通透），非空
     */
    @Schema(
            description = "原房源朝向（如南北通透、朝南）",
            example = "南北通透",
            nullable = false,
            maxLength = 20,
            accessMode = Schema.AccessMode.READ_ONLY
    )
    @NotBlank(message = "原朝向不能为空")
    @Size(max = 20, message = "原朝向长度不能超过20字符")
    @TableField(value = "orientation")
    private String orientation;

    /**
     * 原装修情况
     * 备份原房源的装修类型（UNFINISHED=毛坯，SIMPLE=简装，DELUXE=精装），非空
     */
    @Schema(
            description = "原房源装修情况（UNFINISHED=毛坯，SIMPLE=简装，DELUXE=精装）",
            example = "精装",
            nullable = false,
            allowableValues = {"UNFINISHED", "SIMPLE", "DELUXE"},
            accessMode = Schema.AccessMode.READ_ONLY
    )
    @NotBlank(message = "原装修情况不能为空")
    @TableField(value = "decoration")
    private String decoration;

    /**
     * 原产权性质
     * 备份原房源的产权类型（如商品房、经济适用房），非空
     */
    @Schema(
            description = "原房源产权性质（如商品房、经济适用房、回迁房）",
            example = "商品房",
            nullable = false,
            maxLength = 30,
            accessMode = Schema.AccessMode.READ_ONLY
    )
    @NotBlank(message = "原产权性质不能为空")
    @Size(max = 30, message = "原产权性质长度不能超过30字符")
    @TableField(value = "property_right")
    private String propertyRight;

    /**
     * 原产权证号
     * 备份原房源的产权证编号，非空
     */
    @Schema(
            description = "原房源产权证号（如浙房地权证杭字第123456号）",
            example = "浙房地权证杭字第123456号",
            nullable = false,
            maxLength = 50,
            accessMode = Schema.AccessMode.READ_ONLY
    )
    @NotBlank(message = "原产权证号不能为空")
    @Size(max = 50, message = "原产权证号长度不能超过50字符")
    @TableField(value = "property_right_cert_no")
    private String propertyRightCertNo;

    /**
     * 原产权年限
     * 备份原房源的产权使用年限（≤70年），非空
     */
    @Schema(
            description = "原房源产权年限（正整数，常规上限70年）",
            example = "70",
            nullable = false,
            minimum = "1",
            maximum = "70",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    @NotNull(message = "原产权年限不能为空")
    @Min(value = 1, message = "原产权年限不能小于1")
    @Max(value = 70, message = "原产权年限不能超过70年")
    @TableField(value = "property_right_years")
    private Integer propertyRightYears;

    /**
     * 原抵押状态
     * 备份原房源的抵押状态（NONE=无抵押，MORTGAGED=已抵押），非空
     */
    @Schema(
            description = "原房源抵押状态（NONE=无抵押，MORTGAGED=已抵押）",
            example = "NONE",
            nullable = false,
            allowableValues = {"NONE", "MORTGAGED"},
            accessMode = Schema.AccessMode.READ_ONLY
    )
    @NotBlank(message = "原抵押状态不能为空")
    @TableField(value = "mortgage_status")
    private String mortgageStatus;

    /**
     * 原抵押详情
     * 备份原房源的抵押信息（抵押状态为已抵押时必填），可空
     */
    @Schema(
            description = "原房源抵押详情（抵押状态为已抵押时必填）",
            example = "中国工商银行杭州分行，抵押金额50万元",
            nullable = true,
            maxLength = 200,
            accessMode = Schema.AccessMode.READ_ONLY
    )
    @Size(max = 200, message = "原抵押详情长度不能超过200字符")
    @TableField(value = "mortgage_details")
    private String mortgageDetails;

    /**
     * 原挂牌价
     * 备份原房源的挂牌价格（单位：万元），非负，保留2位小数，非空
     */
    @Schema(
            description = "原房源挂牌价（单位：万元），保留2位小数",
            example = "180.00",
            nullable = false,
            minimum = "0.00",
            format = "decimal",
            multipleOf = 0.01,
            accessMode = Schema.AccessMode.READ_ONLY
    )
    @NotNull(message = "原挂牌价不能为空")
    @DecimalMin(value = "0.00", inclusive = true, message = "原挂牌价不能为负数")
    @TableField(value = "price")
    private BigDecimal price;

    /**
     * 原交易类型
     * 备份原房源的交易类型（SALE=出售，RENT=出租，BOTH=可售可租），非空
     */
    @Schema(
            description = "原房源交易类型（SALE=出售，RENT=出租，BOTH=可售可租）",
            example = "SALE",
            nullable = false,
            allowableValues = {"SALE", "RENT", "BOTH"},
            accessMode = Schema.AccessMode.READ_ONLY
    )
    @NotBlank(message = "原交易类型不能为空")
    @TableField(value = "transaction_type")
    private String transactionType;

    /**
     * 原房源状态
     * 备份原房源删除时的状态（ON_SALE=在售，RESERVED=已预订，SOLD=已售，OFF_SHELF=下架），非空
     */
    @Schema(
            description = "原房源删除时的状态（ON_SALE=在售，RESERVED=已预订，SOLD=已售，OFF_SHELF=下架）",
            example = "ON_SALE",
            nullable = false,
            allowableValues = {"ON_SALE", "RESERVED", "SOLD", "OFF_SHELF"},
            accessMode = Schema.AccessMode.READ_ONLY
    )
    @NotBlank(message = "原房源状态不能为空")
    @TableField(value = "status")
    private String status;

    /**
     * 原房源描述
     * 备份原房源的补充描述信息，可空
     */
    @Schema(
            description = "原房源描述（补充优势、配套等信息）",
            example = "中间楼层，南北通透，采光充足",
            nullable = true,
            maxLength = 500,
            accessMode = Schema.AccessMode.READ_ONLY
    )
    @Size(max = 500, message = "原房源描述长度不能超过500字符")
    @TableField(value = "description")
    private String description;

    /**
     * 原录入经纪人ID
     * 备份原房源的录入经纪人ID（关联agent表），非空
     */
    @Schema(
            description = "原房源录入经纪人ID（关联agent表，仅同租户下的经纪人有效）",
            example = "3001",
            nullable = false,
            accessMode = Schema.AccessMode.READ_ONLY
    )
    @NotNull(message = "原录入经纪人ID不能为空")
    @TableField(value = "create_agent_id")
    private Long createAgentId;

    /**
     * 删除时间
     * 房源数据被删除的时间，数据库自动填充（同步备份时），无需手动传值，只读
     */
    @Schema(
            description = "房源删除时间（数据库自动填充）",
            example = "2025-11-26 17:00:00",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY,
            format = "date-time"
    )
    @TableField(value = "delete_time", fill = FieldFill.INSERT) // 声明新增时自动填充
    private LocalDateTime deleteTime;

    /**
     * 删除人
     * 执行房源删除操作的人员（如经纪人姓名/系统管理员），非空（审计关键字段）
     */
    @Schema(
            description = "删除操作人（如经纪人姓名/系统管理员）",
            example = "张三（经纪人）",
            nullable = false,
            maxLength = 50,
            accessMode = Schema.AccessMode.READ_ONLY
    )
    @NotBlank(message = "删除人不能为空")
    @Size(max = 50, message = "删除人长度不能超过50字符")
    @TableField(value = "delete_operator")
    private String deleteOperator;
}
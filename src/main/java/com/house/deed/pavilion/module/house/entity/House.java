package com.house.deed.pavilion.module.house.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.house.deed.pavilion.module.house.repository.HouseStatus;
import com.house.deed.pavilion.module.house.repository.HouseStatusTypeHandler;
import com.house.deed.pavilion.module.house.repository.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(
        description = "房源信息核心表，存储租户下的房源基础信息，支持房屋出售、出租及租售结合业务场景。关联楼栋（building）、房东（house_landlord关联表）、标签（house_tag关联表）等实体",
        requiredMode = Schema.RequiredMode.REQUIRED
)
@Getter
@Setter
@TableName("house")
public class House implements Serializable {

    @Serial
    @Schema(hidden = true)
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(
            description = "房源唯一标识（自增主键）",
            example = "1001",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    private Long id;

    @TableField(value = "tenant_id", fill = FieldFill.INSERT) // 插入时自动填充
    @Schema(
            description = "租户ID（数据隔离字段），由系统自动填充当前登录租户ID，无需手动设置",
            example = "101",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    private Long tenantId;

    @TableField("building_id")
    @Schema(
            description = "所属楼栋ID，必须关联当前租户下已存在的楼栋（关联building表id）",
            example = "2001",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Long buildingId;

    @TableField("house_no")
    @Schema(
            description = "房号（物理标识），格式建议为[单元号]单元[房间号]（如1单元301、2栋502），同一楼栋内唯一",
            example = "1单元301",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String houseNo;

    @TableField("house_type")
    @Schema(
            description = "户型描述（如3室2厅1卫）",
            example = "3室2厅1卫",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String houseType;

    @TableField("area")
    @Schema(
            description = "建筑面积（单位：㎡）",
            example = "120.50",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private BigDecimal area;

    @TableField("inside_area")
    @Schema(
            description = "套内面积（单位：㎡）",
            example = "105.30"
    )
    private BigDecimal insideArea;

    @TableField("floor")
    @Schema(
            description = "所在楼层",
            example = "15",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Integer floor;

    @TableField("total_floor")
    @Schema(
            description = "楼栋总层数",
            example = "33",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Integer totalFloor;

    @TableField("orientation")
    @Schema(
            description = "房屋朝向（如南北通透、朝南等）",
            example = "南北通透",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String orientation;

    @TableField("decoration")
    @Schema(
            description = "装修情况",
            example = "精装",
            allowableValues = {"毛坯", "简装", "精装"},
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String decoration;

    @TableField("property_right")
    @Schema(
            description = "产权性质",
            example = "商品房",
            allowableValues = {"商品房", "经济适用房", "廉租房", "公租房"}
    )
    private String propertyRight;

    @TableField("property_right_cert_no")
    @Schema(
            description = "产权证号",
            example = "京房权证海字第123456号"
    )
    private String propertyRightCertNo;

    @TableField("property_right_years")
    @Schema(
            description = "产权年限（单位：年）",
            example = "70"
    )
    private Integer propertyRightYears;

    @TableField("mortgage_status")
    @Schema(
            description = "抵押状态",
            example = "NONE",
            allowableValues = {"NONE", "MORTGAGED"},
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String mortgageStatus;

    @TableField("mortgage_details")
    @Schema(
            description = "抵押详情（如抵押银行、抵押金额等，抵押状态为MORTGAGED时必填）",
            example = "招商银行，抵押金额50万元"
    )
    private String mortgageDetails;

    @TableField("price")
    @Schema(
            description = "挂牌价（单位：万元）",
            example = "580.00",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private BigDecimal price;

    @TableField(value = "transaction_type", typeHandler = JacksonTypeHandler.class)
    @Schema(
            description = "交易类型",
            example = "SALE",
            allowableValues = {"SALE", "RENT", "BOTH"},
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private TransactionType transactionType;

    @TableField(value = "status", typeHandler = HouseStatusTypeHandler.class)
    @Schema(
            description = "房源状态",
            example = "ON_SALE",
            allowableValues = {"ON_SALE", "RESERVED", "SOLD", "OFF_SHELF"},
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private HouseStatus status;

    @TableField("description")
    @Schema(
            description = "房源详细描述（如配套设施、交通情况等）",
            example = "临近地铁10号线，小区绿化率30%，周边有三甲医院"
    )
    private String description;

    @TableField("create_agent_id")
    @Schema(
            description = "录入经纪人ID（关联当前租户下的agent表）",
            example = "3001",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Long createAgentId;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(
            description = "创建时间（系统自动填充）",
            example = "2025-11-07T10:30:00",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    @Schema(
            description = "更新时间（系统自动填充）",
            example = "2025-11-08T15:20:00",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    private LocalDateTime updateTime;
}
package com.house.deed.pavilion.module.house.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 房源信息表（租户核心数据）
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
@Getter
@Setter
@TableName("house")
public class House implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 房源ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    @ApiModelProperty(value = "房源ID（自增）")
    private Long id;

    /**
     * 租户ID（归属租户）
     */
    @ApiModelProperty(value = "租户ID（多租户隔离）")
    @TableField(value = "tenant_id", fill = FieldFill.INSERT) // 插入时自动填充
    private Long tenantId;

    /**
     * 所属楼栋ID（关联building表，同租户）
     */
    @TableField("building_id")
    @ApiModelProperty(value = "楼栋ID")
    private Long buildingId;

    /**
     * 房号（如1单元301）
     */
    @TableField("house_no")
    @ApiModelProperty(value = "房号")
    private String houseNo;

    /**
     * 户型（如3室2厅）
     */
    @TableField("house_type")
    @ApiModelProperty(value = "户型")
    private String houseType;

    /**
     * 建筑面积（㎡）
     */
    @TableField("area")
    @ApiModelProperty(value = "建筑面积（㎡）")
    private BigDecimal area;

    /**
     * 套内面积（㎡）
     */
    @TableField("inside_area")
    @ApiModelProperty(value = "套内面积（㎡）")
    private BigDecimal insideArea;

    /**
     * 所在楼层
     */
    @TableField("floor")
    @ApiModelProperty(value = "所在楼层")
    private Integer floor;

    /**
     * 总楼层
     */
    @TableField("total_floor")
    @ApiModelProperty(value = "总楼层")
    private Integer totalFloor;

    /**
     * 朝向（南北通透等）
     */
    @TableField("orientation")
    @ApiModelProperty(value = "朝向")
    private String orientation;

    /**
     * 装修情况（毛坯/简装/精装）
     */
    @TableField("decoration")
    @ApiModelProperty(value = "装修情况")
    private String decoration;

    /**
     * 产权性质（商品房/经济适用房等）
     */
    @TableField("property_right")
    @ApiModelProperty(value = "产权性质")
    private String propertyRight;

    /**
     * 产权证号
     */
    @TableField("property_right_cert_no")
    @ApiModelProperty(value = "产权证号")
    private String propertyRightCertNo;

    /**
     * 产权年限
     */
    @TableField("property_right_years")
    @ApiModelProperty(value = "产权年限（年）")
    private Integer propertyRightYears;

    /**
     * 抵押状态（NONE-无抵押，MORTGAGED-已抵押）
     */
    @TableField("mortgage_status")
    @ApiModelProperty(value = "抵押状态")
    private String mortgageStatus;

    /**
     * 抵押详情（如抵押银行、金额）
     */
    @TableField("mortgage_details")
    @ApiModelProperty(value = "抵押详情")
    private String mortgageDetails;

    /**
     * 挂牌价（万元）
     */
    @TableField("price")
    @ApiModelProperty(value = "挂牌价（万元）")
    private BigDecimal price;

    /**
     * 交易类型（SALE-出售，RENT-出租，BOTH-可售可租）
     */
    @TableField("transaction_type")
    @ApiModelProperty(value = "交易类型")
    private String transactionType;

    /**
     * 状态（ON_SALE-在售，RESERVED-已预订，SOLD-已售，OFF_SHELF-下架）
     */
    @TableField("status")
    @ApiModelProperty(value = "房源状态")
    private String status;

    /**
     * 房源描述
     */
    @TableField("description")
    @ApiModelProperty(value = "房源描述")
    private String description;

    /**
     * 录入经纪人ID（同租户）
     */
    @TableField("create_agent_id")
    @ApiModelProperty(value = "录入经纪人ID")
    private Long createAgentId;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updateTime;
}

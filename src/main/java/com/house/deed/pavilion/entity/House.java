package com.house.deed.pavilion.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 房源信息表（租户核心数据）
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Getter
@Setter
@TableName("house")
@ApiModel(value = "House对象", description = "房源信息表（租户核心数据）")
public class House implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("房源ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("租户ID（归属租户）")
    @TableField("tenant_id")
    private Long tenantId;

    @ApiModelProperty("所属楼栋ID（关联building表，同租户）")
    @TableField("building_id")
    private Long buildingId;

    @ApiModelProperty("房号（如1单元301）")
    @TableField("house_no")
    private String houseNo;

    @ApiModelProperty("户型（如3室2厅）")
    @TableField("house_type")
    private String houseType;

    @ApiModelProperty("建筑面积（㎡）")
    @TableField("area")
    private BigDecimal area;

    @ApiModelProperty("套内面积（㎡）")
    @TableField("inside_area")
    private BigDecimal insideArea;

    @ApiModelProperty("所在楼层")
    @TableField("floor")
    private Integer floor;

    @ApiModelProperty("总楼层")
    @TableField("total_floor")
    private Integer totalFloor;

    @ApiModelProperty("朝向（南北通透等）")
    @TableField("orientation")
    private String orientation;

    @ApiModelProperty("装修情况（毛坯/简装/精装）")
    @TableField("decoration")
    private String decoration;

    @ApiModelProperty("产权性质（商品房/经济适用房等）")
    @TableField("property_right")
    private String propertyRight;

    @ApiModelProperty("产权证号")
    @TableField("property_right_cert_no")
    private String propertyRightCertNo;

    @ApiModelProperty("产权年限")
    @TableField("property_right_years")
    private Integer propertyRightYears;

    @ApiModelProperty("抵押状态（NONE-无抵押，MORTGAGED-已抵押）")
    @TableField("mortgage_status")
    private String mortgageStatus;

    @ApiModelProperty("抵押详情（如抵押银行、金额）")
    @TableField("mortgage_details")
    private String mortgageDetails;

    @ApiModelProperty("挂牌价（万元）")
    @TableField("price")
    private BigDecimal price;

    @ApiModelProperty("交易类型（SALE-出售，RENT-出租，BOTH-可售可租）")
    @TableField("transaction_type")
    private String transactionType;

    @ApiModelProperty("状态（ON_SALE-在售，RESERVED-已预订，SOLD-已售，OFF_SHELF-下架）")
    @TableField("status")
    private String status;

    @ApiModelProperty("房源描述")
    @TableField("description")
    private String description;

    @ApiModelProperty("录入经纪人ID（同租户）")
    @TableField("create_agent_id")
    private Long createAgentId;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}

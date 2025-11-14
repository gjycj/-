package com.house.deed.pavilion.module.house.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 房源录入请求DTO
 */
@Data
@ApiModel(value = "HouseAddDTO", description = "房源录入请求参数")
public class HouseAddDTO {

    @ApiModelProperty(value = "楼盘ID", required = true)
    private Long propertyId;

    @ApiModelProperty(value = "楼栋ID", required = true)
    private Long buildingId;

    @ApiModelProperty(value = "房号", required = true, example = "1-301")
    private String houseNo;

    @ApiModelProperty(value = "户型", required = true, example = "3室2厅")
    private String houseType;

    @ApiModelProperty(value = "建筑面积（㎡）", required = true, example = "120.50")
    private BigDecimal area;

    @ApiModelProperty(value = "套内面积（㎡）", example = "96.40")
    private BigDecimal insideArea;

    @ApiModelProperty(value = "所在楼层", required = true, example = "3")
    private Integer floor;

    @ApiModelProperty(value = "总楼层", required = true, example = "30")
    private Integer totalFloor;

    @ApiModelProperty(value = "朝向", example = "南北通透")
    private String orientation;

    @ApiModelProperty(value = "装修情况", example = "精装")
    private String decoration;

    @ApiModelProperty(value = "产权性质", required = true, example = "商品房")
    private String propertyRight;

    @ApiModelProperty(value = "产权证号", example = "123456789012345")
    private String propertyRightCertNo;

    @ApiModelProperty(value = "产权年限（年）", example = "70")
    private Integer propertyRightYears;

    @ApiModelProperty(value = "抵押状态", example = "NONE")
    private String mortgageStatus;

    @ApiModelProperty(value = "抵押详情", example = "无抵押")
    private String mortgageDetails;

    @ApiModelProperty(value = "挂牌价（万元）", required = true, example = "150.00")
    private BigDecimal price;

    @ApiModelProperty(value = "交易类型", required = true, example = "SALE", notes = "SALE-出售，RENT-出租，BOTH-可售可租")
    private String transactionType;

    @ApiModelProperty(value = "房源描述", example = "采光好，近地铁")
    private String description;

    @ApiModelProperty(value = "关联房东ID（可选）")
    private Long landlordId;

    @ApiModelProperty(value = "关联标签ID列表（可选）")
    private List<Long> tagIds;

    @ApiModelProperty(value = "房源图片URL列表（可选）")
    private List<HouseImageDTO> imageList;
}
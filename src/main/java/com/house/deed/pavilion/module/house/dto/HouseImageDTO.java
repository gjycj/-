package com.house.deed.pavilion.module.house.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/**
 * 房源图片子DTO
 */
@Data
@ApiModel(value = "HouseImageDTO", description = "房源图片参数")
public class HouseImageDTO {

    @ApiModelProperty(value = "图片URL", required = true)
    private String imageUrl;

    @ApiModelProperty(value = "图片类型", example = "COVER")
    private String imageType;

    @ApiModelProperty(value = "排序（数字越小越靠前）", example = "0")
    private Integer sort;
}
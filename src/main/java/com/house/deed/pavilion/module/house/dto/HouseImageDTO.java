package com.house.deed.pavilion.module.house.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 房源图片子DTO，用于接收房源图片的URL、类型及排序信息
 */
@Data
@Schema(description = "房源图片参数，包含图片URL、类型及排序信息")
public class HouseImageDTO {

    @Schema(
            description = "图片URL地址（需为有效的图片资源路径，同租户下可访问）",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String imageUrl;

    @Schema(
            description = "图片类型（区分图片展示场景）",
            example = "COVER",
            allowableValues = {"COVER", "LIVING_ROOM", "BEDROOM", "KITCHEN", "BATHROOM", "BALCONY", "OTHER"},
            defaultValue = "OTHER"
    )
    private String imageType;

    @Schema(
            description = "图片排序序号（数字越小越靠前，默认按0排序）",
            example = "0",
            defaultValue = "0"
    )
    private Integer sort;
}
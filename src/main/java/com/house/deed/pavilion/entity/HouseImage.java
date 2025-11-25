package com.house.deed.pavilion.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 房源图片表（租户级数据）
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Getter
@Setter
@TableName("house_image")
@ApiModel(value = "HouseImage对象", description = "房源图片表（租户级数据）")
public class HouseImage implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("图片ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("租户ID（归属租户）")
    @TableField("tenant_id")
    private Long tenantId;

    @ApiModelProperty("房源ID（关联house表，同租户）")
    @TableField("house_id")
    private Long houseId;

    @ApiModelProperty("图片URL")
    @TableField("image_url")
    private String imageUrl;

    @ApiModelProperty("图片类型（COVER-封面，LIVING_ROOM-客厅等）")
    @TableField("image_type")
    private String imageType;

    @ApiModelProperty("排序（数字越小越靠前）")
    @TableField("sort")
    private Integer sort;

    @TableField("create_time")
    private LocalDateTime createTime;
}

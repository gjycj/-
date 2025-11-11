package com.house.deed.pavilion.module.houseImage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 房源图片表（租户级数据）
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
@Getter
@Setter
@TableName("house_image")
public class HouseImage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 图片ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID（归属租户）
     */
    @TableField("tenant_id")
    private Long tenantId;

    /**
     * 房源ID（关联house表，同租户）
     */
    @TableField("house_id")
    private Long houseId;

    /**
     * 图片URL
     */
    @TableField("image_url")
    private String imageUrl;

    /**
     * 图片类型（COVER-封面，LIVING_ROOM-客厅等）
     */
    @TableField("image_type")
    private String imageType;

    /**
     * 排序（数字越小越靠前）
     */
    @TableField("sort")
    private Integer sort;

    @TableField("create_time")
    private LocalDateTime createTime;
}

package com.house.deed.pavilion.module.building.entity;

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
 * 楼栋信息表（租户级数据）
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
@Getter
@Setter
@TableName("building")
public class Building implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 楼栋ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID（归属租户）
     */
    @TableField("tenant_id")
    private Long tenantId;

    /**
     * 所属楼盘ID（关联property表，同租户）
     */
    @TableField("property_id")
    private Long propertyId;

    /**
     * 楼栋号（如1号楼）
     */
    @TableField("building_no")
    private String buildingNo;

    /**
     * 单元数
     */
    @TableField("unit_count")
    private Integer unitCount;

    /**
     * 总层数
     */
    @TableField("total_floor")
    private Integer totalFloor;

    /**
     * 建筑类型（板楼/塔楼等）
     */
    @TableField("building_type")
    private String buildingType;

    @TableField("create_time")
    private LocalDateTime createTime;
}

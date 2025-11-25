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
 * 区域管理表（租户级数据隔离）
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Getter
@Setter
@TableName("region")
@ApiModel(value = "Region对象", description = "区域管理表（租户级数据隔离）")
public class Region implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("区域ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("租户ID（0为系统默认区域，租户可扩展）")
    @TableField("tenant_id")
    private Long tenantId;

    @ApiModelProperty("父级区域ID（0为顶级）")
    @TableField("parent_id")
    private Long parentId;

    @ApiModelProperty("区域名称（省/市/区/街道）")
    @TableField("region_name")
    private String regionName;

    @ApiModelProperty("层级（1-省，2-市，3-区，4-街道）")
    @TableField("region_level")
    private Byte regionLevel;

    @ApiModelProperty("行政编码（如身份证前6位）")
    @TableField("region_code")
    private String regionCode;

    @ApiModelProperty("排序（升序）")
    @TableField("sort")
    private Integer sort;

    @TableField("create_time")
    private LocalDateTime createTime;
}

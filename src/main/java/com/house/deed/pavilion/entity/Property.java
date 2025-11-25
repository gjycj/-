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
 * 楼盘信息表（租户级数据隔离）
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Getter
@Setter
@TableName("property")
@ApiModel(value = "Property对象", description = "楼盘信息表（租户级数据隔离）")
public class Property implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("楼盘ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("租户ID（归属租户）")
    @TableField("tenant_id")
    private Long tenantId;

    @ApiModelProperty("楼盘名称")
    @TableField("property_name")
    private String propertyName;

    @ApiModelProperty("所属区域ID（关联region表，同租户）")
    @TableField("region_id")
    private Long regionId;

    @ApiModelProperty("详细地址")
    @TableField("address")
    private String address;

    @ApiModelProperty("开发商")
    @TableField("developer")
    private String developer;

    @ApiModelProperty("绿化率（%）")
    @TableField("green_rate")
    private BigDecimal greenRate;

    @ApiModelProperty("建成年份")
    @TableField("completion_year")
    private Integer completionYear;

    @ApiModelProperty("物业公司")
    @TableField("property_management")
    private String propertyManagement;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @ApiModelProperty("创建人ID（经纪人，同租户）")
    @TableField("create_agent_id")
    private Long createAgentId;
}

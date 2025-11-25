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
 * 门店信息表（租户级数据）
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Getter
@Setter
@TableName("store")
@ApiModel(value = "Store对象", description = "门店信息表（租户级数据）")
public class Store implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("门店ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("租户ID（归属租户）")
    @TableField("tenant_id")
    private Long tenantId;

    @ApiModelProperty("门店编码（租户内唯一）")
    @TableField("store_code")
    private String storeCode;

    @ApiModelProperty("门店名称")
    @TableField("store_name")
    private String storeName;

    @ApiModelProperty("所属区域ID（关联region表，同租户）")
    @TableField("region_id")
    private Long regionId;

    @ApiModelProperty("详细地址")
    @TableField("address")
    private String address;

    @ApiModelProperty("店长ID（关联agent表，同租户）")
    @TableField("manager_id")
    private Long managerId;

    @ApiModelProperty("门店电话")
    @TableField("phone")
    private String phone;

    @ApiModelProperty("状态（1-营业，0-停业）")
    @TableField("status")
    private Byte status;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}

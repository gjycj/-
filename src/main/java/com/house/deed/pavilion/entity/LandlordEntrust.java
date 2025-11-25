package com.house.deed.pavilion.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 房东委托信息表（租户级数据）
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Getter
@Setter
@TableName("landlord_entrust")
@ApiModel(value = "LandlordEntrust对象", description = "房东委托信息表（租户级数据）")
public class LandlordEntrust implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("委托ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("租户ID（归属租户）")
    @TableField("tenant_id")
    private Long tenantId;

    @ApiModelProperty("房东ID（关联landlord表，同租户）")
    @TableField("landlord_id")
    private Long landlordId;

    @ApiModelProperty("房源ID（关联house表，同租户）")
    @TableField("house_id")
    private Long houseId;

    @ApiModelProperty("委托类型（EXCLUSIVE-独家，NON_EXCLUSIVE-非独家）")
    @TableField("entrust_type")
    private String entrustType;

    @ApiModelProperty("委托开始时间")
    @TableField("entrust_start_time")
    private LocalDate entrustStartTime;

    @ApiModelProperty("委托结束时间")
    @TableField("entrust_end_time")
    private LocalDate entrustEndTime;

    @ApiModelProperty("是否到期提醒（1-是，0-否）")
    @TableField("renew_remind")
    private Byte renewRemind;

    @ApiModelProperty("委托备注（如特殊要求）")
    @TableField("remark")
    private String remark;

    @ApiModelProperty("状态（1-有效，0-过期/取消）")
    @TableField("status")
    private Byte status;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}

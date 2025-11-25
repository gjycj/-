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
 * 房源价格变动记录表（租户级数据）
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Getter
@Setter
@TableName("house_price_log")
@ApiModel(value = "HousePriceLog对象", description = "房源价格变动记录表（租户级数据）")
public class HousePriceLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("日志ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("租户ID（归属租户）")
    @TableField("tenant_id")
    private Long tenantId;

    @ApiModelProperty("房源ID（关联house表，同租户）")
    @TableField("house_id")
    private Long houseId;

    @ApiModelProperty("调整前价格")
    @TableField("price_before")
    private BigDecimal priceBefore;

    @ApiModelProperty("调整后价格")
    @TableField("price_after")
    private BigDecimal priceAfter;

    @ApiModelProperty("调价原因（房东要求/市场行情/促销等）")
    @TableField("change_reason")
    private String changeReason;

    @ApiModelProperty("操作人ID（经纪人，同租户）")
    @TableField("operator_id")
    private Long operatorId;

    @ApiModelProperty("操作人姓名")
    @TableField("operator_name")
    private String operatorName;

    @ApiModelProperty("调价时间")
    @TableField("create_time")
    private LocalDateTime createTime;
}

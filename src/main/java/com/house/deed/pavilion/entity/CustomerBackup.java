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
 * 客户删除备份表（租户级存档）
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Getter
@Setter
@TableName("customer_backup")
@ApiModel(value = "CustomerBackup对象", description = "客户删除备份表（租户级存档）")
public class CustomerBackup implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("备份ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("原客户ID")
    @TableField("original_id")
    private Long originalId;

    @ApiModelProperty("租户ID（归属租户）")
    @TableField("tenant_id")
    private Long tenantId;

    @TableField("name")
    private String name;

    @TableField("phone")
    private String phone;

    @TableField("id_card")
    private String idCard;

    @TableField("source")
    private String source;

    @TableField("intended_region_id")
    private Long intendedRegionId;

    @TableField("intended_price_min")
    private BigDecimal intendedPriceMin;

    @TableField("intended_price_max")
    private BigDecimal intendedPriceMax;

    @TableField("intended_house_type")
    private String intendedHouseType;

    @TableField("customer_type")
    private String customerType;

    @TableField("potential_level")
    private Byte potentialLevel;

    @TableField("status")
    private String status;

    @TableField("create_agent_id")
    private Long createAgentId;

    @TableField("delete_time")
    private LocalDateTime deleteTime;

    @ApiModelProperty("删除人")
    @TableField("delete_operator")
    private String deleteOperator;
}

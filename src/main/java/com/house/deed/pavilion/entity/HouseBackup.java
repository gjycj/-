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
 * 房源删除备份表（租户级存档）
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Getter
@Setter
@TableName("house_backup")
@ApiModel(value = "HouseBackup对象", description = "房源删除备份表（租户级存档）")
public class HouseBackup implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("备份ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("原房源ID")
    @TableField("original_id")
    private Long originalId;

    @ApiModelProperty("租户ID（归属租户）")
    @TableField("tenant_id")
    private Long tenantId;

    @TableField("building_id")
    private Long buildingId;

    @TableField("house_no")
    private String houseNo;

    @TableField("house_type")
    private String houseType;

    @TableField("area")
    private BigDecimal area;

    @TableField("inside_area")
    private BigDecimal insideArea;

    @TableField("floor")
    private Integer floor;

    @TableField("total_floor")
    private Integer totalFloor;

    @TableField("orientation")
    private String orientation;

    @TableField("decoration")
    private String decoration;

    @TableField("property_right")
    private String propertyRight;

    @TableField("property_right_cert_no")
    private String propertyRightCertNo;

    @TableField("property_right_years")
    private Integer propertyRightYears;

    @TableField("mortgage_status")
    private String mortgageStatus;

    @TableField("mortgage_details")
    private String mortgageDetails;

    @TableField("price")
    private BigDecimal price;

    @TableField("transaction_type")
    private String transactionType;

    @TableField("status")
    private String status;

    @ApiModelProperty("房源描述")
    @TableField("description")
    private String description;

    @TableField("create_agent_id")
    private Long createAgentId;

    @TableField("delete_time")
    private LocalDateTime deleteTime;

    @ApiModelProperty("删除人")
    @TableField("delete_operator")
    private String deleteOperator;
}

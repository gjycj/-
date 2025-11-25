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
 * 经纪人删除备份表（租户级存档）
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Getter
@Setter
@TableName("agent_backup")
@ApiModel(value = "AgentBackup对象", description = "经纪人删除备份表（租户级存档）")
public class AgentBackup implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("备份ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("原经纪人ID")
    @TableField("original_id")
    private Long originalId;

    @ApiModelProperty("租户ID（归属租户）")
    @TableField("tenant_id")
    private Long tenantId;

    @TableField("store_id")
    private Long storeId;

    @TableField("agent_code")
    private String agentCode;

    @TableField("name")
    private String name;

    @TableField("phone")
    private String phone;

    @TableField("id_card")
    private String idCard;

    @TableField("position")
    private String position;

    @TableField("level")
    private String level;

    @TableField("entry_time")
    private LocalDate entryTime;

    @TableField("status")
    private Byte status;

    @TableField("delete_time")
    private LocalDateTime deleteTime;

    @ApiModelProperty("删除人")
    @TableField("delete_operator")
    private String deleteOperator;
}

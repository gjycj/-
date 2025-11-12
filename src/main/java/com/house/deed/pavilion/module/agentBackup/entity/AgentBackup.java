package com.house.deed.pavilion.module.agentBackup.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 经纪人删除备份表（租户级存档）
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
@Getter
@Setter
@TableName("agent_backup")
public class AgentBackup implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 备份ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 原经纪人ID
     */
    @TableField("original_id")
    private Long originalId;

    /**
     * 租户ID（归属租户）
     */
    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
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

    /**
     * 删除人
     */
    @TableField("delete_operator")
    private String deleteOperator;
}

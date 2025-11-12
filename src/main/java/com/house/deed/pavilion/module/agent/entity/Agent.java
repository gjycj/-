package com.house.deed.pavilion.module.agent.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 经纪人信息表（租户级数据）
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
@Getter
@Setter
@TableName("agent")
public class Agent implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 经纪人ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID（归属租户）
     */
    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private Long tenantId;

    /**
     * 所属门店ID（关联store表，同租户）
     */
    @TableField("store_id")
    private Long storeId;

    /**
     * 工号（租户内唯一）
     */
    @TableField("agent_code")
    private String agentCode;

    /**
     * 姓名
     */
    @TableField("name")
    private String name;

    /**
     * 联系电话
     */
    @TableField("phone")
    private String phone;

    /**
     * 身份证号
     */
    @TableField("id_card")
    private String idCard;

    /**
     * 职位（经纪人/店长等）
     */
    @TableField("position")
    private String position;

    /**
     * 级别（JUNIOR-初级，SENIOR-高级，STAR-明星）
     */
    @TableField("level")
    private String level;

    /**
     * 入职时间
     */
    @TableField("entry_time")
    private LocalDate entryTime;

    /**
     * 状态（1-在职，0-离职）
     */
    @TableField("status")
    private Byte status;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;
}

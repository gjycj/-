package com.house.deed.pavilion.module.landlord.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 房东信息表（租户级数据隔离）
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
@Getter
@Setter
@TableName("landlord")
public class Landlord implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 房东ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID（归属租户）
     */
    @TableField("tenant_id")
    private Long tenantId;

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
     * 联系地址
     */
    @TableField("address")
    private String address;

    /**
     * 创建人（经纪人ID，同租户）
     */
    @TableField("create_agent_id")
    private Long createAgentId;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}

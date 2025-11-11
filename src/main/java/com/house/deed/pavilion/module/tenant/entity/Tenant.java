package com.house.deed.pavilion.module.tenant.entity;

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
 * 多租户核心信息表（租户隔离根表）
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
@Getter
@Setter
@TableName("tenant")
public class Tenant implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 租户ID（唯一标识）
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 租户编码（分库/分表路由键，全局唯一）
     */
    @TableField("tenant_code")
    private String tenantCode;

    /**
     * 租户名称（如中介公司全称）
     */
    @TableField("tenant_name")
    private String tenantName;

    /**
     * 租户联系人
     */
    @TableField("contact_person")
    private String contactPerson;

    /**
     * 联系人电话
     */
    @TableField("contact_phone")
    private String contactPhone;

    /**
     * 租户独立域名（如租户自定义访问地址）
     */
    @TableField("domain")
    private String domain;

    /**
     * 租户个性化配置（如logo、流程开关等）
     */
    @TableField("config_json")
    private String configJson;

    /**
     * 状态（1-正常，0-禁用，2-过期）
     */
    @TableField("status")
    private Byte status;

    /**
     * 租户过期时间（为空表示永久）
     */
    @TableField("expire_time")
    private LocalDateTime expireTime;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}

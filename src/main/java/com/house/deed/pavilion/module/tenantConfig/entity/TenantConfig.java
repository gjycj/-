package com.house.deed.pavilion.module.tenantConfig.entity;

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
 * 租户个性化配置表
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
@Getter
@Setter
@TableName("tenant_config")
public class TenantConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 配置ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID（关联tenant表，0为系统默认配置）
     */
    @TableField("tenant_id")
    private Long tenantId;

    /**
     * 配置项键（如COMMISSION_RATE、MAX_HOUSE_NUM）
     */
    @TableField("config_key")
    private String configKey;

    /**
     * 配置项值
     */
    @TableField("config_value")
    private String configValue;

    /**
     * 配置说明
     */
    @TableField("config_desc")
    private String configDesc;

    /**
     * 是否系统内置（1-是，0-否）
     */
    @TableField("is_system")
    private Byte isSystem;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}

package com.house.deed.pavilion.module.store.entity;

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
 * 门店信息表（租户级数据）
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
@Getter
@Setter
@TableName("store")
public class Store implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 门店ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID（归属租户）
     */
    @TableField("tenant_id")
    private Long tenantId;

    /**
     * 门店编码（租户内唯一）
     */
    @TableField("store_code")
    private String storeCode;

    /**
     * 门店名称
     */
    @TableField("store_name")
    private String storeName;

    /**
     * 所属区域ID（关联region表，同租户）
     */
    @TableField("region_id")
    private Long regionId;

    /**
     * 详细地址
     */
    @TableField("address")
    private String address;

    /**
     * 店长ID（关联agent表，同租户）
     */
    @TableField("manager_id")
    private Long managerId;

    /**
     * 门店电话
     */
    @TableField("phone")
    private String phone;

    /**
     * 状态（1-营业，0-停业）
     */
    @TableField("status")
    private Byte status;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}

package com.house.deed.pavilion.module.customer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 客户信息表（租户级数据隔离）
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
@Getter
@Setter
@TableName("customer")
public class Customer implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 客户ID
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
     * 客户来源（门店/线上/转介绍等）
     */
    @TableField("source")
    private String source;

    /**
     * 意向区域ID（关联region表，同租户）
     */
    @TableField("intended_region_id")
    private Long intendedRegionId;

    /**
     * 意向价格下限（万元）
     */
    @TableField("intended_price_min")
    private BigDecimal intendedPriceMin;

    /**
     * 意向价格上限（万元）
     */
    @TableField("intended_price_max")
    private BigDecimal intendedPriceMax;

    /**
     * 意向户型（如两居室）
     */
    @TableField("intended_house_type")
    private String intendedHouseType;

    /**
     * 客户类型（ORDINARY-普通，VIP-会员，INVEST-投资客）
     */
    @TableField("customer_type")
    private String customerType;

    /**
     * 潜力等级（1-低，2-中，3-高）
     */
    @TableField("potential_level")
    private Byte potentialLevel;

    /**
     * 状态（ACTIVE-活跃，DEALED-已成交，DORMANT-休眠）
     */
    @TableField("status")
    private String status;

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

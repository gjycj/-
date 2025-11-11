package com.house.deed.pavilion.module.customerHistoryDeal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 客户历史成交记录表（租户级数据）
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
@Getter
@Setter
@TableName("customer_history_deal")
public class CustomerHistoryDeal implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 关联ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID（归属租户）
     */
    @TableField("tenant_id")
    private Long tenantId;

    /**
     * 客户ID（关联customer表，同租户）
     */
    @TableField("customer_id")
    private Long customerId;

    /**
     * 历史成交合同ID（关联contract表，同租户）
     */
    @TableField("contract_id")
    private Long contractId;

    /**
     * 成交日期
     */
    @TableField("deal_time")
    private LocalDate dealTime;

    /**
     * 成交房源简要信息（如XX小区3室2厅）
     */
    @TableField("house_info")
    private String houseInfo;

    /**
     * 成交类型（SALE-买卖，RENT-租赁）
     */
    @TableField("deal_type")
    private String dealType;

    @TableField("create_time")
    private LocalDateTime createTime;
}

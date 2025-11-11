package com.house.deed.pavilion.module.houseStatusLog.entity;

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
 * 房源状态变更日志表（租户级数据）
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
@Getter
@Setter
@TableName("house_status_log")
public class HouseStatusLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 日志ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID（归属租户）
     */
    @TableField("tenant_id")
    private Long tenantId;

    /**
     * 房源ID（关联house表，同租户）
     */
    @TableField("house_id")
    private Long houseId;

    /**
     * 变更前状态
     */
    @TableField("status_before")
    private String statusBefore;

    /**
     * 变更后状态
     */
    @TableField("status_after")
    private String statusAfter;

    /**
     * 变更原因（如客户支付诚意金）
     */
    @TableField("change_reason")
    private String changeReason;

    /**
     * 操作人ID（经纪人ID，同租户）
     */
    @TableField("operator_id")
    private Long operatorId;

    /**
     * 操作人姓名
     */
    @TableField("operator_name")
    private String operatorName;

    @TableField("create_time")
    private LocalDateTime createTime;
}

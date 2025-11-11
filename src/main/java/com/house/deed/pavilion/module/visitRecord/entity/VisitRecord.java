package com.house.deed.pavilion.module.visitRecord.entity;

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
 * 带看记录表（租户级数据）
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
@Getter
@Setter
@TableName("visit_record")
public class VisitRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 带看ID
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
     * 客户ID（关联customer表，同租户）
     */
    @TableField("customer_id")
    private Long customerId;

    /**
     * 带看经纪人ID（关联agent表，同租户）
     */
    @TableField("agent_id")
    private Long agentId;

    /**
     * 带看时间
     */
    @TableField("visit_time")
    private LocalDateTime visitTime;

    /**
     * 带看方式（线下/VR）
     */
    @TableField("visit_type")
    private String visitType;

    /**
     * 客户反馈（如价格太高、户型满意）
     */
    @TableField("customer_feedback")
    private String customerFeedback;

    /**
     * 意向程度（1-低，2-中，3-高）
     */
    @TableField("intention_level")
    private Byte intentionLevel;

    @TableField("create_time")
    private LocalDateTime createTime;
}

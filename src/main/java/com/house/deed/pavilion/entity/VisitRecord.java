package com.house.deed.pavilion.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 带看记录表（租户级数据）
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Getter
@Setter
@TableName("visit_record")
@ApiModel(value = "VisitRecord对象", description = "带看记录表（租户级数据）")
public class VisitRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("带看ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("租户ID（归属租户）")
    @TableField("tenant_id")
    private Long tenantId;

    @ApiModelProperty("房源ID（关联house表，同租户）")
    @TableField("house_id")
    private Long houseId;

    @ApiModelProperty("客户ID（关联customer表，同租户）")
    @TableField("customer_id")
    private Long customerId;

    @ApiModelProperty("带看经纪人ID（关联agent表，同租户）")
    @TableField("agent_id")
    private Long agentId;

    @ApiModelProperty("带看时间")
    @TableField("visit_time")
    private LocalDateTime visitTime;

    @ApiModelProperty("带看方式（线下/VR）")
    @TableField("visit_type")
    private String visitType;

    @ApiModelProperty("客户反馈（如价格太高、户型满意）")
    @TableField("customer_feedback")
    private String customerFeedback;

    @ApiModelProperty("意向程度（1-低，2-中，3-高）")
    @TableField("intention_level")
    private Byte intentionLevel;

    @TableField("create_time")
    private LocalDateTime createTime;

    @ApiModelProperty("关联合同ID（关联contract表，同租户）")
    @TableField("contract_id")
    private Long contractId;
}

package com.house.deed.pavilion.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 房源维护计划表（租户级数据）
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Getter
@Setter
@TableName("house_maintain_plan")
@ApiModel(value = "HouseMaintainPlan对象", description = "房源维护计划表（租户级数据）")
public class HouseMaintainPlan implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("计划ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("租户ID（归属租户）")
    @TableField("tenant_id")
    private Long tenantId;

    @ApiModelProperty("房源ID（关联house表，同租户）")
    @TableField("house_id")
    private Long houseId;

    @ApiModelProperty("维护类型（CLEAN-保洁等）")
    @TableField("maintain_type")
    private String maintainType;

    @ApiModelProperty("周期（ONCE-一次性，WEEKLY-每周等）")
    @TableField("cycle")
    private String cycle;

    @ApiModelProperty("开始日期")
    @TableField("start_date")
    private LocalDate startDate;

    @ApiModelProperty("结束日期（一次性维护可为空）")
    @TableField("end_date")
    private LocalDate endDate;

    @ApiModelProperty("执行人ID（经纪人/第三方，同租户）")
    @TableField("executor_id")
    private Long executorId;

    @ApiModelProperty("状态（ACTIVE-生效中等）")
    @TableField("status")
    private String status;

    @ApiModelProperty("维护要求（如每周六保洁）")
    @TableField("remark")
    private String remark;

    @TableField("create_time")
    private LocalDateTime createTime;
}

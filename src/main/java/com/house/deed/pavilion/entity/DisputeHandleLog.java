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
 * 纠纷处理日志表（租户级数据）
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Getter
@Setter
@TableName("dispute_handle_log")
@ApiModel(value = "DisputeHandleLog对象", description = "纠纷处理日志表（租户级数据）")
public class DisputeHandleLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("日志ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("租户ID（归属租户）")
    @TableField("tenant_id")
    private Long tenantId;

    @ApiModelProperty("纠纷ID（关联complaint_dispute表，同租户）")
    @TableField("dispute_id")
    private Long disputeId;

    @ApiModelProperty("处理时间")
    @TableField("handle_time")
    private LocalDateTime handleTime;

    @ApiModelProperty("处理人ID（同租户）")
    @TableField("handler_id")
    private Long handlerId;

    @ApiModelProperty("处理人姓名")
    @TableField("handler_name")
    private String handlerName;

    @ApiModelProperty("处理内容（如沟通记录、解决方案）")
    @TableField("handle_content")
    private String handleContent;

    @ApiModelProperty("处理前状态")
    @TableField("status_before")
    private String statusBefore;

    @ApiModelProperty("处理后状态")
    @TableField("status_after")
    private String statusAfter;

    @TableField("create_time")
    private LocalDateTime createTime;
}

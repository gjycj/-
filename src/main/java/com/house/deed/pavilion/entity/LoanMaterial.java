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
 * 贷款材料提交记录表（租户级数据）
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Getter
@Setter
@TableName("loan_material")
@ApiModel(value = "LoanMaterial对象", description = "贷款材料提交记录表（租户级数据）")
public class LoanMaterial implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("材料ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("租户ID（归属租户）")
    @TableField("tenant_id")
    private Long tenantId;

    @ApiModelProperty("贷款ID（关联loan_info表，同租户）")
    @TableField("loan_id")
    private Long loanId;

    @ApiModelProperty("材料类型（INCOME_PROOF-收入证明等）")
    @TableField("material_type")
    private String materialType;

    @ApiModelProperty("状态（UNSUBMITTED-未提交等）")
    @TableField("status")
    private String status;

    @ApiModelProperty("提交时间")
    @TableField("submit_time")
    private LocalDateTime submitTime;

    @ApiModelProperty("不合格原因")
    @TableField("reject_reason")
    private String rejectReason;

    @ApiModelProperty("材料扫描件URL")
    @TableField("material_url")
    private String materialUrl;

    @TableField("create_time")
    private LocalDateTime createTime;
}

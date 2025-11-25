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
 * 租赁合同附加条款表（租户级数据）
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Getter
@Setter
@TableName("contract_lease_terms")
@ApiModel(value = "ContractLeaseTerms对象", description = "租赁合同附加条款表（租户级数据）")
public class ContractLeaseTerms implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("条款ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("租户ID（归属租户）")
    @TableField("tenant_id")
    private Long tenantId;

    @ApiModelProperty("合同ID（关联contract表，同租户，仅租赁）")
    @TableField("contract_id")
    private Long contractId;

    @ApiModelProperty("是否允许养宠物（1-是，0-否）")
    @TableField("allow_pet")
    private Byte allowPet;

    @ApiModelProperty("是否允许转租（1-是，0-否）")
    @TableField("allow_sublet")
    private Byte allowSublet;

    @ApiModelProperty("费用承担（如物业费房东承担，水电费租户承担）")
    @TableField("fee_bear")
    private String feeBear;

    @ApiModelProperty("家具维修约定（如自然损坏房东负责）")
    @TableField("furniture_maintenance")
    private String furnitureMaintenance;

    @TableField("create_time")
    private LocalDateTime createTime;
}

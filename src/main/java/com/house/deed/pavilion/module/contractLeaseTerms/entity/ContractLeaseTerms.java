package com.house.deed.pavilion.module.contractLeaseTerms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 租赁合同附加条款表（租户级数据）
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
@Getter
@Setter
@TableName("contract_lease_terms")
public class ContractLeaseTerms implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 条款ID（新增时无需传入，更新时必传）
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID（归属租户，由上下文自动填充，无需前端传入）
     */
    @TableField("tenant_id")
    private Long tenantId;

    /**
     * 合同ID（关联contract表，同租户，仅租赁，必传）
     */
    @TableField("contract_id")
    @NotNull(message = "合同ID不能为空")
    private Long contractId;

    /**
     * 是否允许养宠物（1-是，0-否，必传且只能为0或1）
     */
    @TableField("allow_pet")
    @NotNull(message = "是否允许养宠物不能为空")
    @Min(value = 0, message = "是否允许养宠物只能为0或1")
    @Max(value = 1, message = "是否允许养宠物只能为0或1")
    private Byte allowPet;

    /**
     * 是否允许转租（1-是，0-否，必传且只能为0或1）
     */
    @TableField("allow_sublet")
    @NotNull(message = "是否允许转租不能为空")
    @Min(value = 0, message = "是否允许转租只能为0或1")
    @Max(value = 1, message = "是否允许转租只能为0或1")
    private Byte allowSublet;

    /**
     * 费用承担（如物业费房东承担，水电费租户承担，非必传但长度限制）
     */
    @TableField("fee_bear")
    private String feeBear;

    /**
     * 家具维修约定（如自然损坏房东负责，非必传）
     */
    @TableField("furniture_maintenance")
    private String furnitureMaintenance;

    @TableField("create_time")
    private LocalDateTime createTime;
}
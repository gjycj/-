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
 * 电子签约信息表（租户级数据）
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Getter
@Setter
@TableName("electronic_sign")
@ApiModel(value = "ElectronicSign对象", description = "电子签约信息表（租户级数据）")
public class ElectronicSign implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("电子签ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("租户ID（归属租户）")
    @TableField("tenant_id")
    private Long tenantId;

    @ApiModelProperty("合同ID（关联contract表，同租户）")
    @TableField("contract_id")
    private Long contractId;

    @ApiModelProperty("电子签平台（如e签宝/法大大）")
    @TableField("sign_platform")
    private String signPlatform;

    @ApiModelProperty("签约链接")
    @TableField("sign_url")
    private String signUrl;

    @ApiModelProperty("签约状态（PENDING-待签，SIGNED-已签，REJECTED-拒签，EXPIRED-过期）")
    @TableField("sign_status")
    private String signStatus;

    @ApiModelProperty("客户签约时间")
    @TableField("customer_sign_time")
    private LocalDateTime customerSignTime;

    @ApiModelProperty("房东签约时间")
    @TableField("landlord_sign_time")
    private LocalDateTime landlordSignTime;

    @ApiModelProperty("电子签名哈希值（防篡改）")
    @TableField("sign_hash")
    private String signHash;

    @ApiModelProperty("电子合同PDF地址")
    @TableField("contract_pdf_url")
    private String contractPdfUrl;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}

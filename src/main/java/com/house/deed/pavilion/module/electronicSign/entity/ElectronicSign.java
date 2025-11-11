package com.house.deed.pavilion.module.electronicSign.entity;

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
 * 电子签约信息表（租户级数据）
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
@Getter
@Setter
@TableName("electronic_sign")
public class ElectronicSign implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 电子签ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID（归属租户）
     */
    @TableField("tenant_id")
    private Long tenantId;

    /**
     * 合同ID（关联contract表，同租户）
     */
    @TableField("contract_id")
    private Long contractId;

    /**
     * 电子签平台（如e签宝/法大大）
     */
    @TableField("sign_platform")
    private String signPlatform;

    /**
     * 签约链接
     */
    @TableField("sign_url")
    private String signUrl;

    /**
     * 签约状态（PENDING-待签，SIGNED-已签，REJECTED-拒签，EXPIRED-过期）
     */
    @TableField("sign_status")
    private String signStatus;

    /**
     * 客户签约时间
     */
    @TableField("customer_sign_time")
    private LocalDateTime customerSignTime;

    /**
     * 房东签约时间
     */
    @TableField("landlord_sign_time")
    private LocalDateTime landlordSignTime;

    /**
     * 电子签名哈希值（防篡改）
     */
    @TableField("sign_hash")
    private String signHash;

    /**
     * 电子合同PDF地址
     */
    @TableField("contract_pdf_url")
    private String contractPdfUrl;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}

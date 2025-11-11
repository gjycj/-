package com.house.deed.pavilion.module.contractAttachment.entity;

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
 * 合同附件表（租户级数据）
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
@Getter
@Setter
@TableName("contract_attachment")
public class ContractAttachment implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 附件ID
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
     * 附件类型（ID_CARD-身份证，PROPERTY_CERT-房产证等）
     */
    @TableField("attachment_type")
    private String attachmentType;

    /**
     * 附件URL
     */
    @TableField("attachment_url")
    private String attachmentUrl;

    /**
     * 文件名称
     */
    @TableField("file_name")
    private String fileName;

    /**
     * 上传时间
     */
    @TableField("upload_time")
    private LocalDateTime uploadTime;

    /**
     * 上传人ID（经纪人，同租户）
     */
    @TableField("uploader_id")
    private Long uploaderId;
}

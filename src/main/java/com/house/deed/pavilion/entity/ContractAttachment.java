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
 * 合同附件表（租户级数据）
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Getter
@Setter
@TableName("contract_attachment")
@ApiModel(value = "ContractAttachment对象", description = "合同附件表（租户级数据）")
public class ContractAttachment implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("附件ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("租户ID（归属租户）")
    @TableField("tenant_id")
    private Long tenantId;

    @ApiModelProperty("合同ID（关联contract表，同租户）")
    @TableField("contract_id")
    private Long contractId;

    @ApiModelProperty("附件类型（ID_CARD-身份证，PROPERTY_CERT-房产证等）")
    @TableField("attachment_type")
    private String attachmentType;

    @ApiModelProperty("附件URL")
    @TableField("attachment_url")
    private String attachmentUrl;

    @ApiModelProperty("文件名称")
    @TableField("file_name")
    private String fileName;

    @ApiModelProperty("上传时间")
    @TableField("upload_time")
    private LocalDateTime uploadTime;

    @ApiModelProperty("上传人ID（经纪人，同租户）")
    @TableField("uploader_id")
    private Long uploaderId;
}

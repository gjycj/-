package com.house.deed.pavilion.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 合同附件表（租户级数据隔离）
 * <p>
 * 核心业务说明：
 * 1. 附件存储：关联交易合同（contract表），存储合同相关的电子附件（身份证、房产证、签字扫描件等）；
 * 2. 租户隔离：所有字段绑定tenant_id，仅当前租户可查询/操作自身合同的附件数据；
 * 3. 附件分类：通过attachment_type区分附件类型（身份证/房产证等），支撑附件快速检索；
 * 4. 存储规范：attachment_url存储文件的访问路径（如OSS URL/服务器本地路径），fileName保留原始文件名；
 * 5. 追溯信息：记录上传人（经纪人）和上传时间，便于附件操作审计。
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Getter
@Setter
@TableName(value = "contract_attachment", autoResultMap = true) // 开启自动结果映射（兼容复杂类型）
@Schema(
        name = "ContractAttachment",
        description = "合同附件实体（租户级数据），存储交易合同相关的电子附件（身份证、房产证等）及访问信息",
        example = "{\"tenantId\": 1001, \"contractId\": 1, \"attachmentType\": \"ID_CARD\", \"attachmentUrl\": \"https://oss.example.com/contract/1/ID_CARD_张三_20251126.pdf\", \"fileName\": \"张三身份证.pdf\", \"uploaderId\": 3001}"
)
public class ContractAttachment implements Serializable {

    /**
     * 序列化版本号：保证对象序列化/反序列化一致性，避免类结构变更导致反序列化异常
     */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 附件主键ID
     * 自增策略，唯一标识单个合同附件，无业务含义，仅作为数据库主键
     */
    @Schema(
            description = "附件主键ID（自增）",
            example = "1",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY // 只读：新增时数据库自增，无需传值
    )
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID
     * 关联租户表主键，标识附件归属的租户，核心隔离字段，非空
     */
    @Schema(
            description = "租户ID（归属租户，关联租户表主键）",
            example = "1001",
            nullable = false
    )
    @NotNull(message = "租户ID不能为空")
    @TableField(value = "tenant_id", exist = true)
    private Long tenantId;

    /**
     * 合同ID
     * 关联contract表主键（同租户下的合同），标识附件所属的合同，非空（核心关联字段）
     */
    @Schema(
            description = "合同ID（关联contract表，仅同租户下的合同有效）",
            example = "1",
            nullable = false
    )
    @NotNull(message = "合同ID不能为空")
    @TableField(value = "contract_id")
    private Long contractId;

    /**
     * 附件类型
     * 枚举值：ID_CARD-身份证（客户/房东），PROPERTY_CERT-房产证，LAND_CERT-土地证，CONTRACT_SCAN-合同扫描件，OTHER-其他附件
     */
    @Schema(
            description = "附件类型（ID_CARD=身份证，PROPERTY_CERT=房产证，LAND_CERT=土地证，CONTRACT_SCAN=合同扫描件，OTHER=其他附件）",
            example = "ID_CARD",
            nullable = false,
            allowableValues = {"ID_CARD", "PROPERTY_CERT", "LAND_CERT", "CONTRACT_SCAN", "OTHER"}
    )
    @NotBlank(message = "附件类型不能为空")
    @TableField(value = "attachment_type")
    private String attachmentType;

    /**
     * 附件URL
     * 文件访问路径（如OSS URL、服务器本地绝对路径），非空，长度≤500字符
     */
    @Schema(
            description = "附件访问URL（如OSS URL/服务器本地路径）",
            example = "https://oss.example.com/contract/1/ID_CARD_张三_20251126.pdf",
            nullable = false,
            maxLength = 500,
            pattern = "^(https?://|oss://|/).*$" // 支持HTTP/HTTPS/OSS/本地路径
    )
    @NotBlank(message = "附件URL不能为空")
    @Size(max = 500, message = "附件URL长度不能超过500字符")
    @Pattern(regexp = "^(https?://|oss://|/).*$", message = "附件URL格式错误（支持HTTP/HTTPS/OSS/本地路径）")
    @TableField(value = "attachment_url")
    private String attachmentUrl;

    /**
     * 文件名称
     * 附件原始文件名（含后缀，如“张三身份证.pdf”），非空，长度≤100字符
     */
    @Schema(
            description = "附件原始文件名（含后缀，如“张三身份证.pdf”）",
            example = "张三身份证.pdf",
            nullable = false,
            maxLength = 100
    )
    @NotBlank(message = "文件名称不能为空")
    @Size(max = 100, message = "文件名称长度不能超过100字符")
    @TableField(value = "file_name")
    private String fileName;

    /**
     * 上传时间
     * 附件上传到系统的时间，数据库自动填充（上传时），无需手动传值，只读
     */
    @Schema(
            description = "附件上传时间（数据库自动填充）",
            example = "2025-11-26 14:00:00",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY,
            format = "date-time"
    )
    @TableField(value = "upload_time", fill = FieldFill.INSERT) // 声明新增时自动填充
    private LocalDateTime uploadTime;

    /**
     * 上传人ID（经纪人）
     * 关联agent表主键（同租户下的经纪人），标识附件的上传者，非空
     */
    @Schema(
            description = "上传人ID（关联agent表，仅同租户下的经纪人有效）",
            example = "3001",
            nullable = false
    )
    @NotNull(message = "上传人ID不能为空")
    @TableField(value = "uploader_id")
    private Long uploaderId;
}
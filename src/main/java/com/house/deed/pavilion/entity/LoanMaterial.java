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
 * 贷款材料提交记录表（租户级数据隔离）
 * <p>
 * 核心业务说明：
 * 1. 贷款材料管控：记录租户内贷款申请所需材料的提交状态、审核结果及扫描件信息，是贷款审核流程的核心支撑数据；
 * 2. 租户隔离：所有字段绑定tenant_id，仅当前租户可查询/操作自身的贷款材料记录，保护敏感材料信息安全；
 * 3. 核心关联：
 *    - loan_id 关联贷款信息表（loan_info，同租户），标识材料对应的贷款申请；
 * 4. 材料流转核心：
 *    - 材料类型：区分贷款所需的各类材料（如收入证明、身份证、房产证等）；
 *    - 状态流转：UNSUBMITTED-未提交 → SUBMITTED-已提交 → APPROVED-审核通过 → REJECTED-审核驳回；
 *    - 关键字段：
 *      - material_url：材料扫描件/电子版URL（提交后必填）；
 *      - submit_time：材料提交时间（已提交状态后非空）；
 *      - reject_reason：审核驳回原因（仅REJECTED状态时必填）；
 * 5. 合规规范：材料类型与贷款类型联动（如房贷需房产证、经营贷需营业执照），审核结果需明确记录，支撑贷款合规审计。
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Getter
@Setter
@TableName(value = "loan_material", autoResultMap = true) // 开启自动结果映射
@Schema(
        name = "LoanMaterial",
        description = "贷款材料提交记录实体（租户级数据），记录贷款申请所需材料的提交状态、审核结果及扫描件信息",
        example = "{\"tenantId\": 1001, \"loanId\": 401, \"materialType\": \"INCOME_PROOF\", \"status\": \"SUBMITTED\", \"submitTime\": \"2025-11-28 10:00:00\", \"materialUrl\": \"https://oss.example.com/loan/material/401/income_proof.pdf\"}"
)
public class LoanMaterial implements Serializable {

    /**
     * 序列化版本号：保证对象序列化/反序列化一致性，避免类结构变更导致反序列化异常
     */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 材料记录主键ID
     * 自增策略，唯一标识单条贷款材料记录，无业务含义，仅作为数据库主键
     */
    @Schema(
            description = "材料记录主键ID（自增）",
            example = "501",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY // 只读：新增时数据库自增，无需传值
    )
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID
     * 关联租户表主键，标识材料记录归属的租户，核心隔离字段，非空
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
     * 贷款ID
     * 关联loan_info表主键（同租户下的贷款申请），标识材料对应的贷款订单，非空（核心关联字段）
     */
    @Schema(
            description = "贷款ID（关联loan_info表，仅同租户下的贷款申请有效）",
            example = "401",
            nullable = false
    )
    @NotNull(message = "贷款ID不能为空")
    @TableField(value = "loan_id")
    private Long loanId;

    /**
     * 材料类型
     * 枚举值：INCOME_PROOF-收入证明，ID_CARD-身份证正反面，HOUSE_PROPERTY-房产证，MARRIAGE_CERT-结婚证，BANK_FLOW-银行流水，OTHER-其他材料
     */
    @Schema(
            description = "贷款所需材料类型（INCOME_PROOF=收入证明，ID_CARD=身份证正反面，HOUSE_PROPERTY=房产证，MARRIAGE_CERT=结婚证，BANK_FLOW=银行流水，OTHER=其他材料）",
            example = "INCOME_PROOF",
            nullable = false,
            allowableValues = {"INCOME_PROOF", "ID_CARD", "HOUSE_PROPERTY", "MARRIAGE_CERT", "BANK_FLOW", "OTHER"}
    )
    @NotBlank(message = "材料类型不能为空")
    @TableField(value = "material_type")
    private String materialType;

    /**
     * 材料状态
     * 枚举值：UNSUBMITTED-未提交，SUBMITTED-已提交（待审核），APPROVED-审核通过，REJECTED-审核驳回
     */
    @Schema(
            description = "材料提交及审核状态（UNSUBMITTED=未提交，SUBMITTED=已提交，APPROVED=审核通过，REJECTED=审核驳回）",
            example = "SUBMITTED",
            nullable = false,
            allowableValues = {"UNSUBMITTED", "SUBMITTED", "APPROVED", "REJECTED"}
    )
    @NotBlank(message = "材料状态不能为空")
    @TableField(value = "status")
    private String status;

    /**
     * 提交时间
     * 材料提交至系统的时间（精确到时分秒），可空（仅状态为SUBMITTED/APPROVED/REJECTED时非空）
     */
    @Schema(
            description = "材料提交时间（仅已提交及后续状态有效，格式：yyyy-MM-dd HH:mm:ss）",
            example = "2025-11-28 10:00:00",
            nullable = true,
            format = "date-time"
    )
    @TableField(value = "submit_time")
    private LocalDateTime submitTime;

    /**
     * 不合格原因
     * 材料审核驳回时的具体原因（如“收入证明未盖公章”“身份证复印件不清晰”），可空（仅状态为REJECTED时必填）
     */
    @Schema(
            description = "审核驳回原因（仅状态为REJECTED时必填）",
            example = "收入证明未盖单位公章，需补充提交",
            nullable = true,
            maxLength = 300
    )
    @Size(max = 300, message = "不合格原因长度不能超过300字符")
    @TableField(value = "reject_reason")
    private String rejectReason;

    /**
     * 材料扫描件URL
     * 材料电子版/扫描件的访问URL（支持HTTP/HTTPS/OSS），可空（仅状态为SUBMITTED/APPROVED/REJECTED时必填）
     */
    @Schema(
            description = "材料扫描件/电子版URL（支持HTTP/HTTPS/OSS，仅已提交及后续状态有效）",
            example = "https://oss.example.com/loan/material/401/income_proof.pdf",
            nullable = true,
            maxLength = 500,
            pattern = "^(https?://|oss://).*$"
    )
    @Size(max = 500, message = "材料URL长度不能超过500字符")
    @Pattern(regexp = "^(https?://|oss://).*$", message = "材料URL格式错误（支持HTTP/HTTPS/OSS）")
    @TableField(value = "material_url")
    private String materialUrl;

    /**
     * 创建时间
     * 材料记录初始化时间（系统自动生成），无需手动传值，只读
     */
    @Schema(
            description = "材料记录创建时间（数据库自动填充）",
            example = "2025-11-27 16:00:00",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY,
            format = "date-time"
    )
    @TableField(value = "create_time", fill = FieldFill.INSERT) // 声明新增时自动填充
    private LocalDateTime createTime;
}
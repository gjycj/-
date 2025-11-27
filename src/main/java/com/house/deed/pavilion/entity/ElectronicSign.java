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
 * 电子签约信息表（租户级数据隔离）
 * <p>
 * 核心业务说明：
 * 1. 合规签约：关联交易合同（contract表），存储电子签约全流程信息，支撑合同电子签章、防篡改、合规追溯；
 * 2. 租户隔离：所有字段绑定tenant_id，仅当前租户可查询/操作自身合同的电子签约数据；
 * 3. 核心要素：
 *    - 平台关联：sign_platform 记录电子签服务提供商（如e签宝/法大大），适配多平台对接；
 *    - 签约链路：sign_url 为签约跳转链接（时效性链接），contract_pdf_url 为最终生成的电子合同PDF地址；
 *    - 状态管控：sign_status 标识签约进度（待签/已签/拒签/过期），支撑签约流程跟踪；
 *    - 合规保障：sign_hash 存储电子合同哈希值（如SHA-256），用于防篡改校验；
 *    - 时间节点：customer_sign_time/landlord_sign_time 分别记录客户/房东的签约完成时间（仅已签状态有效）；
 * 4. 关联约束：contract_id 严格关联同租户下的交易合同，确保电子签与实体合同一一对应。
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Getter
@Setter
@TableName(value = "electronic_sign", autoResultMap = true) // 开启自动结果映射（兼容复杂类型）
@Schema(
        name = "ElectronicSign",
        description = "电子签约信息实体（租户级数据），存储合同电子签约流程、状态及合规校验信息",
        example = "{\"tenantId\": 1001, \"contractId\": 5001, \"signPlatform\": \"e签宝\", \"signUrl\": \"https://esign.example.com/link/xxx123\", \"signStatus\": \"PENDING\", \"signHash\": \"a1b2c3d4e5f6...\", \"contractPdfUrl\": null, \"customerSignTime\": null, \"landlordSignTime\": null}"
)
public class ElectronicSign implements Serializable {

    /**
     * 序列化版本号：保证对象序列化/反序列化一致性，避免类结构变更导致反序列化异常
     */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 电子签记录主键ID
     * 自增策略，唯一标识单条电子签约记录，无业务含义，仅作为数据库主键
     */
    @Schema(
            description = "电子签记录主键ID（自增）",
            example = "1",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY // 只读：新增时数据库自增，无需传值
    )
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID
     * 关联租户表主键，标识电子签记录归属的租户，核心隔离字段，非空
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
     * 关联contract表主键（同租户下的交易合同），标识电子签对应的合同，非空（核心关联字段）
     */
    @Schema(
            description = "合同ID（关联contract表，仅同租户下的合同有效）",
            example = "5001",
            nullable = false
    )
    @NotNull(message = "合同ID不能为空")
    @TableField(value = "contract_id")
    private Long contractId;

    /**
     * 电子签平台
     * 电子签约服务提供商名称（如e签宝、法大大、上上签），非空，长度≤30字符
     */
    @Schema(
            description = "电子签平台名称（如e签宝、法大大、上上签）",
            example = "e签宝",
            nullable = false,
            maxLength = 30
    )
    @NotBlank(message = "电子签平台不能为空")
    @Size(max = 30, message = "电子签平台名称长度不能超过30字符")
    @TableField(value = "sign_platform")
    private String signPlatform;

    /**
     * 签约链接
     * 电子签平台生成的时效性签约跳转链接（如HTTP/HTTPS/OSS链接），非空，长度≤500字符
     */
    @Schema(
            description = "电子签约跳转链接（时效性链接，支持HTTP/HTTPS/OSS格式）",
            example = "https://esign.example.com/link/xxx123",
            nullable = false,
            maxLength = 500,
            pattern = "^(https?://|oss://).*$" // 支持HTTP/HTTPS/OSS链接格式
    )
    @NotBlank(message = "签约链接不能为空")
    @Size(max = 500, message = "签约链接长度不能超过500字符")
    @Pattern(regexp = "^(https?://|oss://).*$", message = "签约链接格式错误（支持HTTP/HTTPS/OSS链接）")
    @TableField(value = "sign_url")
    private String signUrl;

    /**
     * 签约状态
     * 枚举值：PENDING-待签（链接已生成，未完成签约），SIGNED-已签（客户+房东均完成签约），REJECTED-拒签（任意一方拒绝），EXPIRED-过期（链接失效未签约）
     */
    @Schema(
            description = "签约状态（PENDING=待签，SIGNED=已签，REJECTED=拒签，EXPIRED=过期）",
            example = "PENDING",
            nullable = false,
            allowableValues = {"PENDING", "SIGNED", "REJECTED", "EXPIRED"}
    )
    @NotBlank(message = "签约状态不能为空")
    @TableField(value = "sign_status")
    private String signStatus;

    /**
     * 客户签约时间
     * 客户完成电子签约的时间，格式：yyyy-MM-dd HH:mm:ss；仅sign_status=SIGNED时非空
     */
    @Schema(
            description = "客户签约完成时间（仅已签状态有效，格式：yyyy-MM-dd HH:mm:ss）",
            example = "2025-11-27 10:00:00",
            nullable = true,
            format = "date-time"
    )
    @TableField(value = "customer_sign_time")
    private LocalDateTime customerSignTime;

    /**
     * 房东签约时间
     * 房东完成电子签约的时间，格式：yyyy-MM-dd HH:mm:ss；仅sign_status=SIGNED时非空
     */
    @Schema(
            description = "房东签约完成时间（仅已签状态有效，格式：yyyy-MM-dd HH:mm:ss）",
            example = "2025-11-27 10:30:00",
            nullable = true,
            format = "date-time"
    )
    @TableField(value = "landlord_sign_time")
    private LocalDateTime landlordSignTime;

    /**
     * 电子签名哈希值
     * 电子合同文件的哈希值（如SHA-256），用于防篡改校验，非空，长度≤64字符（SHA-256标准长度）
     */
    @Schema(
            description = "电子合同哈希值（SHA-256，防篡改校验）",
            example = "a1b2c3d4e5f67890abcdef1234567890abcdef1234567890abcdef1234567890",
            nullable = false,
            maxLength = 64
    )
    @NotBlank(message = "电子签名哈希值不能为空")
    @Size(max = 64, message = "哈希值长度不能超过64字符（SHA-256标准）")
    @TableField(value = "sign_hash")
    private String signHash;

    /**
     * 电子合同PDF地址
     * 签约完成后生成的电子合同PDF永久访问地址，可空（未签约时为null），长度≤500字符
     */
    @Schema(
            description = "电子合同PDF永久访问地址（仅已签状态有效）",
            example = "https://oss.example.com/contract/pdf/5001_signed.pdf",
            nullable = true,
            maxLength = 500,
            pattern = "^(https?://|oss://).*\\.pdf$" // 仅允许PDF格式链接
    )
    @Size(max = 500, message = "PDF地址长度不能超过500字符")
    @Pattern(regexp = "^(https?://|oss://).*\\.pdf$", message = "PDF地址格式错误（支持HTTP/HTTPS/OSS，且文件后缀为.pdf）", flags = Pattern.Flag.CASE_INSENSITIVE)
    @TableField(value = "contract_pdf_url")
    private String contractPdfUrl;

    /**
     * 创建时间
     * 电子签记录创建时间（链接生成时），数据库自动填充，无需手动传值，只读
     */
    @Schema(
            description = "电子签记录创建时间（数据库自动填充）",
            example = "2025-11-26 09:00:00",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY,
            format = "date-time"
    )
    @TableField(value = "create_time", fill = FieldFill.INSERT) // 声明新增时自动填充
    private LocalDateTime createTime;

    /**
     * 更新时间
     * 电子签信息更新时间（如状态变更、签约时间补充），数据库自动填充，无需手动传值，只读
     */
    @Schema(
            description = "电子签记录更新时间（数据库自动填充）",
            example = "2025-11-27 10:30:00",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY,
            format = "date-time"
    )
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE) // 声明新增/修改时自动填充
    private LocalDateTime updateTime;
}
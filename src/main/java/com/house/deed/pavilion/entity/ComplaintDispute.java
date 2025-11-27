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
 * 投诉与纠纷记录表（租户级数据隔离）
 * <p>
 * 核心业务说明：
 * 1. 流程记录：记录租户内投诉/纠纷的全生命周期信息，支撑纠纷处理、追溯与统计分析；
 * 2. 租户隔离：所有字段绑定tenant_id，仅当前租户可查询/操作自身的投诉纠纷数据；
 * 3. 关联维度：
 *    - 关联合同（related_contract_id）：可绑定具体成交合同（非必填，如无合同纠纷可为null）；
 *    - 投诉人（complainant_id）：关联对应角色表（客户/经纪人，由complainant_type区分）；
 *    - 处理人（handler_id）：关联管理员/店长表，记录纠纷处理责任人；
 * 4. 状态管控：status标识纠纷处理进度（已受理/处理中/已解决/已取消），支持流程化管理；
 * 5. 数据规范：纠纷编号租户内唯一，投诉人电话需为有效手机号，纠纷描述需详细（长度≤500字符）。
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Getter
@Setter
@TableName(value = "complaint_dispute", autoResultMap = true) // 开启自动结果映射（兼容复杂类型）
@Schema(
        name = "ComplaintDispute",
        description = "投诉与纠纷记录实体（租户级数据），记录投诉纠纷全生命周期信息及处理进度",
        example = "{\"tenantId\": 1001, \"disputeNo\": \"DIS20251126001\", \"relatedContractId\": 5001, \"complainantType\": \"CUSTOMER\", \"complainantId\": 4001, \"complainantPhone\": \"13800138000\", \"disputeType\": \"SERVICE\", \"description\": \"经纪人未及时反馈房源过户进度\", \"status\": \"ACCEPTED\", \"handlerId\": 3002, \"createAgentId\": 3001}"
)
public class ComplaintDispute implements Serializable {

    /**
     * 序列化版本号：保证对象序列化/反序列化一致性，避免类结构变更导致反序列化异常
     */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 纠纷记录主键ID
     * 自增策略，唯一标识单条投诉纠纷记录，无业务含义，仅作为数据库主键
     */
    @Schema(
            description = "纠纷记录主键ID（自增）",
            example = "1",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY // 只读：新增时数据库自增，无需传值
    )
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID
     * 关联租户表主键，标识投诉纠纷归属的租户，核心隔离字段，非空
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
     * 纠纷编号
     * 租户内唯一，格式：DIS+年月日+3位流水号（如DIS20251126001），非空，长度≤20字符
     */
    @Schema(
            description = "纠纷编号（租户内唯一，格式：DIS+年月日+3位流水号，如DIS20251126001）",
            example = "DIS20251126001",
            nullable = false,
            maxLength = 20,
            pattern = "^DIS\\d{8}\\d{3}$" // 格式约束
    )
    @NotBlank(message = "纠纷编号不能为空")
    @Size(max = 20, message = "纠纷编号长度不能超过20字符")
    @Pattern(regexp = "^DIS\\d{8}\\d{3}$", message = "纠纷编号格式错误（需为DIS+年月日+3位流水号，如DIS20251126001）")
    @TableField(value = "dispute_no")
    private String disputeNo;

    /**
     * 关联合同ID
     * 关联contract表主键（同租户下的合同），可空（如无合同关联的服务投诉）
     */
    @Schema(
            description = "关联合同ID（关联contract表，仅同租户下的合同有效，无关联合同可填null）",
            example = "5001",
            nullable = true
    )
    @TableField(value = "related_contract_id")
    private Long relatedContractId;

    /**
     * 投诉人类型
     * 枚举值：CUSTOMER-客户，AGENT-经纪人，OTHER-其他，非空（标识投诉人身份）
     */
    @Schema(
            description = "投诉人类型（CUSTOMER=客户，AGENT=经纪人，OTHER=其他）",
            example = "CUSTOMER",
            nullable = false,
            allowableValues = {"CUSTOMER", "AGENT", "OTHER"}
    )
    @NotBlank(message = "投诉人类型不能为空")
    @TableField(value = "complainant_type")
    private String complainantType;

    /**
     * 投诉人ID
     * 关联对应角色表主键（客户表/经纪人表，由complainant_type区分），同租户下有效，非空
     */
    @Schema(
            description = "投诉人ID（关联对应角色表，同租户下有效，由投诉人类型区分）",
            example = "4001",
            nullable = false
    )
    @NotNull(message = "投诉人ID不能为空")
    @TableField(value = "complainant_id")
    private Long complainantId;

    /**
     * 投诉人电话
     * 11位有效手机号，用于联系投诉人，非空
     */
    @Schema(
            description = "投诉人联系电话（11位有效手机号）",
            example = "13800138000",
            nullable = false,
            pattern = "^1[3-9]\\d{9}$"
    )
    @NotBlank(message = "投诉人电话不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式错误（需为11位有效手机号）")
    @TableField(value = "complainant_phone")
    private String complainantPhone;

    /**
     * 纠纷类型
     * 枚举值：SERVICE-服务投诉，CONTRACT-合同纠纷，PROPERTY-房源问题，OTHER-其他纠纷，非空
     */
    @Schema(
            description = "纠纷类型（SERVICE=服务投诉，CONTRACT=合同纠纷，PROPERTY=房源问题，OTHER=其他纠纷）",
            example = "SERVICE",
            nullable = false,
            allowableValues = {"SERVICE", "CONTRACT", "PROPERTY", "OTHER"}
    )
    @NotBlank(message = "纠纷类型不能为空")
    @TableField(value = "dispute_type")
    private String disputeType;

    /**
     * 纠纷描述
     * 详细描述投诉/纠纷的原因、诉求等信息，非空，长度≤500字符
     */
    @Schema(
            description = "纠纷描述（详细说明投诉原因、诉求等信息）",
            example = "经纪人未及时反馈房源过户进度，导致客户无法按时入住",
            nullable = false,
            maxLength = 500
    )
    @NotBlank(message = "纠纷描述不能为空")
    @Size(max = 500, message = "纠纷描述长度不能超过500字符")
    @TableField(value = "description")
    private String description;

    /**
     * 纠纷状态
     * 枚举值：ACCEPTED-已受理，PROCESSING-处理中，RESOLVED-已解决，CANCELED-已取消，非空
     */
    @Schema(
            description = "纠纷状态（ACCEPTED=已受理，PROCESSING=处理中，RESOLVED=已解决，CANCELED=已取消）",
            example = "ACCEPTED",
            nullable = false,
            allowableValues = {"ACCEPTED", "PROCESSING", "RESOLVED", "CANCELED"}
    )
    @NotBlank(message = "纠纷状态不能为空")
    @TableField(value = "status")
    private String status;

    /**
     * 处理人ID
     * 关联管理员/店长表主键（同租户下），可空（未分配处理人时为null）
     */
    @Schema(
            description = "处理人ID（关联管理员/店长表，同租户下有效，未分配时为null）",
            example = "3002",
            nullable = true
    )
    @TableField(value = "handler_id")
    private Long handlerId;

    /**
     * 创建时间
     * 纠纷记录创建时间，数据库自动填充（新增时），无需手动传值，只读
     */
    @Schema(
            description = "纠纷记录创建时间（数据库自动填充）",
            example = "2025-11-26 14:30:00",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY,
            format = "date-time"
    )
    @TableField(value = "create_time", fill = FieldFill.INSERT) // 声明新增时自动填充
    private LocalDateTime createTime;

    /**
     * 更新时间
     * 纠纷记录更新时间（如状态变更、处理人分配），数据库自动填充，无需手动传值，只读
     */
    @Schema(
            description = "纠纷记录更新时间（数据库自动填充）",
            example = "2025-11-26 15:00:00",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY,
            format = "date-time"
    )
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE) // 声明新增/修改时自动填充
    private LocalDateTime updateTime;

    /**
     * 创建人ID（经纪人）
     * 关联agent表主键（同租户下的经纪人），标识纠纷记录的创建者，非空
     */
    @Schema(
            description = "创建人ID（关联agent表，仅同租户下的经纪人有效）",
            example = "3001",
            nullable = false
    )
    @NotNull(message = "创建人ID不能为空")
    @TableField(value = "create_agent_id")
    private Long createAgentId;
}
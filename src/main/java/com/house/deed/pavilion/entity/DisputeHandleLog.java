package com.house.deed.pavilion.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 纠纷处理日志表（租户级数据隔离）
 * <p>
 * 核心业务说明：
 * 1. 流程审计：记录租户内投诉纠纷的每一次处理操作、状态变更轨迹，支撑纠纷处理全流程追溯和责任审计；
 * 2. 租户隔离：所有字段绑定tenant_id，仅当前租户可查询/操作自身纠纷的处理日志；
 * 3. 核心关联：
 *    - dispute_id 关联投诉纠纷主表（complaint_dispute），明确日志归属的纠纷；
 *    - handler_id/handler_name 记录处理人信息（冗余存储，避免关联查询，便于快速查看）；
 * 4. 状态变更核心：
 *    - statusBefore：处理前的纠纷状态（如ACCEPTED）；
 *    - statusAfter：处理后的纠纷状态（如PROCESSING）；
 *    两者共同构成纠纷状态变更轨迹，确保状态流转可追溯；
 * 5. 数据规范：处理内容需详细记录沟通要点、解决方案，文本长度限制在合理范围，处理时间需精准到时分秒。
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Getter
@Setter
@TableName(value = "dispute_handle_log", autoResultMap = true) // 开启自动结果映射（兼容复杂类型）
@Schema(
        name = "DisputeHandleLog",
        description = "纠纷处理日志实体（租户级数据），记录纠纷处理操作、状态变更轨迹及处理详情",
        example = "{\"tenantId\": 1001, \"disputeId\": 1, \"handleTime\": \"2025-11-26 16:00:00\", \"handlerId\": 3002, \"handlerName\": \"李四（店长）\", \"handleContent\": \"已联系客户和经纪人核实情况，客户诉求为尽快反馈过户进度，已协调经纪人24小时内同步最新进展\", \"statusBefore\": \"ACCEPTED\", \"statusAfter\": \"PROCESSING\"}"
)
public class DisputeHandleLog implements Serializable {

    /**
     * 序列化版本号：保证对象序列化/反序列化一致性，避免类结构变更导致反序列化异常
     */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 处理日志主键ID
     * 自增策略，唯一标识单条纠纷处理日志，无业务含义，仅作为数据库主键
     */
    @Schema(
            description = "处理日志主键ID（自增）",
            example = "1",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY // 只读：新增时数据库自增，无需传值
    )
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID
     * 关联租户表主键，标识处理日志归属的租户，核心隔离字段，非空
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
     * 纠纷ID
     * 关联complaint_dispute表主键（同租户下的纠纷），标识日志归属的纠纷，非空（核心关联字段）
     */
    @Schema(
            description = "纠纷ID（关联complaint_dispute表，仅同租户下的纠纷有效）",
            example = "1",
            nullable = false
    )
    @NotNull(message = "纠纷ID不能为空")
    @TableField(value = "dispute_id")
    private Long disputeId;

    /**
     * 处理时间
     * 纠纷处理操作实际发生的时间（如沟通时间、方案确定时间），非空，格式：yyyy-MM-dd HH:mm:ss
     */
    @Schema(
            description = "处理操作实际发生时间（格式：yyyy-MM-dd HH:mm:ss）",
            example = "2025-11-26 16:00:00",
            nullable = false,
            format = "date-time"
    )
    @NotNull(message = "处理时间不能为空")
    @TableField(value = "handle_time")
    private LocalDateTime handleTime;

    /**
     * 处理人ID
     * 关联管理员/店长/经纪人表主键（同租户下），标识执行处理操作的人员，非空
     */
    @Schema(
            description = "处理人ID（关联管理员/店长/经纪人表，仅同租户下有效）",
            example = "3002",
            nullable = false
    )
    @NotNull(message = "处理人ID不能为空")
    @TableField(value = "handler_id")
    private Long handlerId;

    /**
     * 处理人姓名
     * 处理人的真实姓名（冗余存储，避免关联查询），非空，长度≤50字符
     */
    @Schema(
            description = "处理人真实姓名（冗余存储）",
            example = "李四（店长）",
            nullable = false,
            maxLength = 50
    )
    @NotBlank(message = "处理人姓名不能为空")
    @Size(max = 50, message = "处理人姓名长度不能超过50字符")
    @TableField(value = "handler_name")
    private String handlerName;

    /**
     * 处理内容
     * 详细记录处理过程（沟通要点、客户诉求、解决方案、协调结果等），非空，长度≤1000字符
     */
    @Schema(
            description = "处理内容（沟通要点、客户诉求、解决方案等）",
            example = "已联系客户和经纪人核实情况，客户诉求为尽快反馈过户进度，已协调经纪人24小时内同步最新进展，客户表示认可",
            nullable = false,
            maxLength = 1000
    )
    @NotBlank(message = "处理内容不能为空")
    @Size(max = 1000, message = "处理内容长度不能超过1000字符")
    @TableField(value = "handle_content")
    private String handleContent;

    /**
     * 处理前状态
     * 纠纷处理前的状态（与complaint_dispute表status枚举一致），非空
     */
    @Schema(
            description = "处理前的纠纷状态（ACCEPTED=已受理，PROCESSING=处理中，RESOLVED=已解决，CANCELED=已取消）",
            example = "ACCEPTED",
            nullable = false,
            allowableValues = {"ACCEPTED", "PROCESSING", "RESOLVED", "CANCELED"} // 与纠纷主表状态一致
    )
    @NotBlank(message = "处理前状态不能为空")
    @TableField(value = "status_before")
    private String statusBefore;

    /**
     * 处理后状态
     * 纠纷处理后的状态（与complaint_dispute表status枚举一致），非空（需与处理前状态不同）
     */
    @Schema(
            description = "处理后的纠纷状态（ACCEPTED=已受理，PROCESSING=处理中，RESOLVED=已解决，CANCELED=已取消）",
            example = "PROCESSING",
            nullable = false,
            allowableValues = {"ACCEPTED", "PROCESSING", "RESOLVED", "CANCELED"}
    )
    @NotBlank(message = "处理后状态不能为空")
    @TableField(value = "status_after")
    private String statusAfter;

    /**
     * 日志创建时间
     * 处理日志录入系统的时间，数据库自动填充（新增时），无需手动传值，只读
     */
    @Schema(
            description = "日志创建时间（数据库自动填充）",
            example = "2025-11-26 16:05:00",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY,
            format = "date-time"
    )
    @TableField(value = "create_time", fill = FieldFill.INSERT) // 声明新增时自动填充
    private LocalDateTime createTime;
}
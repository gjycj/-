package com.house.deed.pavilion.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 带看记录表（租户级数据隔离）
 * <p>
 * 核心业务说明：
 * 1. 带看全流程追溯：记录租户内客户看房的完整信息（房源、客户、经纪人三方关联），支撑带看效果分析、成交转化追踪及客户需求挖掘；
 * 2. 租户隔离：所有字段绑定tenant_id，仅当前租户可查询/操作自身带看记录，保护客户隐私和业务数据安全；
 * 3. 核心关联：
 *    - 房源关联：house_id 关联房源表（同租户），标识带看的目标房源；
 *    - 客户关联：customer_id 关联客户表（同租户），标识参与带看的客户；
 *    - 经纪人关联：agent_id 关联经纪人表（同租户），标识负责带看的经纪人；
 *    - 合同关联：contract_id 关联合同表（同租户），带看后成交时非空（标记该带看转化为交易）；
 * 4. 带看核心要素：
 *    - 带看方式：OFFLINE-线下带看（实地看房）、VR-VR带看（远程虚拟看房）；
 *    - 意向程度：1-低（无意向）、2-中（一般意向）、3-高（强烈意向），支撑客户需求优先级排序；
 *    - 客户反馈：记录客户对房源的具体评价（如价格、户型、采光等），为后续房源推荐提供依据；
 * 5. 数据规范：带看时间需精准到时分秒，客户反馈需真实有效，关联ID需存在于对应主表，确保带看记录可追溯、可分析。
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Getter
@Setter
@TableName(value = "visit_record", autoResultMap = true) // 开启自动结果映射
@Schema(
        name = "VisitRecord",
        description = "客户带看记录实体（租户级数据），记录房源-客户-经纪人三方关联信息，支撑带看追溯和成交转化分析",
        example = "{\"tenantId\": 1001, \"houseId\": 101, \"customerId\": 4001, \"agentId\": 3001, \"visitTime\": \"2025-12-01 14:30:00\", \"visitType\": \"OFFLINE\", \"customerFeedback\": \"价格偏高，户型和采光满意，意向中等\", \"intentionLevel\": 2, \"contractId\": null}"
)
public class VisitRecord implements Serializable {

    /**
     * 序列化版本号：保证对象序列化/反序列化一致性，避免类结构变更导致反序列化异常
     */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 带看记录主键ID
     * 自增策略，唯一标识单条带看记录，无业务含义，仅作为数据库主键
     */
    @Schema(
            description = "带看记录主键ID（自增）",
            example = "1001",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY // 只读：新增时数据库自增，无需传值
    )
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID
     * 关联租户表主键，标识带看记录归属的租户，核心隔离字段，非空
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
     * 房源ID
     * 关联house表主键（同租户下的房源），标识带看的目标房源，非空（核心关联字段）
     */
    @Schema(
            description = "房源ID（关联house表，仅同租户下的房源有效）",
            example = "101",
            nullable = false
    )
    @NotNull(message = "房源ID不能为空")
    @TableField(value = "house_id")
    private Long houseId;

    /**
     * 客户ID
     * 关联customer表主键（同租户下的客户），标识参与带看的客户，非空（核心关联字段）
     */
    @Schema(
            description = "客户ID（关联customer表，仅同租户下的客户有效）",
            example = "4001",
            nullable = false
    )
    @NotNull(message = "客户ID不能为空")
    @TableField(value = "customer_id")
    private Long customerId;

    /**
     * 带看经纪人ID
     * 关联agent表主键（同租户下的经纪人），标识负责带看的经纪人，非空（核心关联字段）
     */
    @Schema(
            description = "带看经纪人ID（关联agent表，仅同租户下的经纪人有效）",
            example = "3001",
            nullable = false
    )
    @NotNull(message = "带看经纪人ID不能为空")
    @TableField(value = "agent_id")
    private Long agentId;

    /**
     * 带看时间
     * 实际带看的时间（精确到时分秒），非空（核心时间节点），需晚于当前时间（预约带看）或符合实际带看时间
     */
    @Schema(
            description = "带看时间（精确到时分秒，预约带看需晚于当前时间）",
            example = "2025-12-01 14:30:00",
            nullable = false,
            format = "date-time"
    )
    @NotNull(message = "带看时间不能为空")
    @TableField(value = "visit_time")
    private LocalDateTime visitTime;

    /**
     * 带看方式
     * 枚举值：OFFLINE-线下带看（实地看房），VR-VR带看（远程虚拟看房），非空
     */
    @Schema(
            description = "带看方式（OFFLINE=线下带看，VR=VR远程带看）",
            example = "OFFLINE",
            nullable = false,
            allowableValues = {"OFFLINE", "VR"}
    )
    @NotBlank(message = "带看方式不能为空")
    @TableField(value = "visit_type")
    private String visitType;

    /**
     * 客户反馈
     * 客户带看后的真实评价（如价格、户型、采光、配套等），可空，长度≤500字符，建议详细填写
     */
    @Schema(
            description = "客户带看反馈（如价格、户型、采光等评价）",
            example = "价格偏高，户型和采光满意，周边配套齐全，意向中等",
            nullable = true,
            maxLength = 500
    )
    @Size(max = 500, message = "客户反馈长度不能超过500字符")
    @TableField(value = "customer_feedback")
    private String customerFeedback;

    /**
     * 意向程度
     * 枚举值：1-低（无意向购买/租赁），2-中（一般意向），3-高（强烈意向），非空，支撑成交转化分析
     */
    @Schema(
            description = "客户意向程度（1=低，2=中，3=高）",
            example = "2",
            nullable = false,
            allowableValues = {"1", "2", "3"}
    )
    @NotNull(message = "意向程度不能为空")
    @Min(value = 1, message = "意向程度仅支持1（低）、2（中）、3（高）")
    @Max(value = 3, message = "意向程度仅支持1（低）、2（中）、3（高）")
    @TableField(value = "intention_level")
    private Byte intentionLevel;

    /**
     * 关联合同ID
     * 关联contract表主键（同租户下的交易合同），带看后成交时非空，未成交则为null
     */
    @Schema(
            description = "关联合同ID（带看后成交时必填，未成交可为null）",
            example = "5001",
            nullable = true
    )
    @TableField(value = "contract_id")
    private Long contractId;

    /**
     * 创建时间
     * 带看记录录入系统的时间，数据库自动填充，无需手动传值，只读
     */
    @Schema(
            description = "带看记录创建时间（数据库自动填充）",
            example = "2025-11-30 10:00:00",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY,
            format = "date-time"
    )
    @TableField(value = "create_time", fill = FieldFill.INSERT) // 声明新增时自动填充
    private LocalDateTime createTime;
}
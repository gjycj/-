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
 * 房源状态变更日志表（租户级数据隔离）
 * <p>
 * 核心业务说明：
 * 1. 状态追溯：记录租户内房源状态的每一次变更轨迹（如在售→已预订、已售→下架），支撑状态变更审计和问题追溯；
 * 2. 租户隔离：所有字段绑定tenant_id，仅当前租户可查询/操作自身房源的状态变更记录；
 * 3. 核心关联：
 *    - house_id 关联房源表（同租户），标识状态变更的房源；
 *    - operator_id 关联经纪人表（同租户），标识执行状态变更的操作人；
 * 4. 状态变更核心：
 *    - statusBefore/statusAfter：分别记录变更前后的房源状态（与house表status枚举完全一致），两者不可相同；
 *    - change_reason：状态变更的具体原因（如客户支付诚意金、房源成交、房东下架），需明确且可追溯；
 * 5. 审计规范：操作人ID/姓名（冗余存储）、变更时间为必填审计字段，确保状态变更可追责。
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Getter
@Setter
@TableName(value = "house_status_log", autoResultMap = true) // 开启自动结果映射
@Schema(
        name = "HouseStatusLog",
        description = "房源状态变更日志实体（租户级数据），记录房源状态变更轨迹、原因及操作人，支撑审计追溯",
        example = "{\"tenantId\": 1001, \"houseId\": 101, \"statusBefore\": \"ON_SALE\", \"statusAfter\": \"RESERVED\", \"changeReason\": \"客户支付5万元诚意金，锁定房源\", \"operatorId\": 3001, \"operatorName\": \"张三（经纪人）\"}"
)
public class HouseStatusLog implements Serializable {

    /**
     * 序列化版本号：保证对象序列化/反序列化一致性，避免类结构变更导致反序列化异常
     */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 状态变更日志主键ID
     * 自增策略，唯一标识单条状态变更记录，无业务含义，仅作为数据库主键
     */
    @Schema(
            description = "状态变更日志主键ID（自增）",
            example = "1",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY // 只读：新增时数据库自增，无需传值
    )
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID
     * 关联租户表主键，标识日志归属的租户，核心隔离字段，非空
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
     * 关联house表主键（同租户下的房源），标识状态变更的房源，非空（核心关联字段）
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
     * 变更前状态
     * 房源状态变更前的状态（与house表status枚举一致），非空
     * 枚举值：ON_SALE-在售/在租，RESERVED-已预订，SOLD-已售/已租，OFF_SHELF-下架
     */
    @Schema(
            description = "变更前房源状态（与house表一致：ON_SALE=在售/在租，RESERVED=已预订，SOLD=已售/已租，OFF_SHELF=下架）",
            example = "ON_SALE",
            nullable = false,
            allowableValues = {"ON_SALE", "RESERVED", "SOLD", "OFF_SHELF"}
    )
    @NotBlank(message = "变更前状态不能为空")
    @TableField(value = "status_before")
    private String statusBefore;

    /**
     * 变更后状态
     * 房源状态变更后的状态（与house表status枚举一致），非空（需与变更前状态不同）
     * 枚举值：ON_SALE-在售/在租，RESERVED-已预订，SOLD-已售/已租，OFF_SHELF-下架
     */
    @Schema(
            description = "变更后房源状态（与house表一致：ON_SALE=在售/在租，RESERVED=已预订，SOLD=已售/已租，OFF_SHELF=下架）",
            example = "RESERVED",
            nullable = false,
            allowableValues = {"ON_SALE", "RESERVED", "SOLD", "OFF_SHELF"}
    )
    @NotBlank(message = "变更后状态不能为空")
    @TableField(value = "status_after")
    private String statusAfter;

    /**
     * 变更原因
     * 状态变更的具体业务原因（如客户支付诚意金、房源成交、房东申请下架、违规下架等），非空，长度≤200字符
     */
    @Schema(
            description = "状态变更原因（如客户支付诚意金、房源成交、房东下架等）",
            example = "客户支付5万元诚意金，锁定房源",
            nullable = false,
            maxLength = 200
    )
    @NotBlank(message = "变更原因不能为空")
    @Size(max = 200, message = "变更原因长度不能超过200字符")
    @TableField(value = "change_reason")
    private String changeReason;

    /**
     * 操作人ID
     * 关联经纪人表主键（同租户下），标识执行状态变更的操作人，非空
     */
    @Schema(
            description = "操作人ID（关联经纪人表，仅同租户下的经纪人有效）",
            example = "3001",
            nullable = false
    )
    @NotNull(message = "操作人ID不能为空")
    @TableField(value = "operator_id")
    private Long operatorId;

    /**
     * 操作人姓名
     * 执行状态变更的操作人真实姓名（冗余存储，避免关联查询），非空，长度≤50字符
     */
    @Schema(
            description = "操作人真实姓名（冗余存储）",
            example = "张三（经纪人）",
            nullable = false,
            maxLength = 50
    )
    @NotBlank(message = "操作人姓名不能为空")
    @Size(max = 50, message = "操作人姓名长度不能超过50字符")
    @TableField(value = "operator_name")
    private String operatorName;

    /**
     * 变更时间
     * 房源状态变更的执行时间（精确到时分秒），数据库自动填充，无需手动传值，只读
     */
    @Schema(
            description = "状态变更执行时间（数据库自动填充）",
            example = "2025-11-26 16:00:00",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY,
            format = "date-time"
    )
    @TableField(value = "create_time", fill = FieldFill.INSERT) // 声明新增时自动填充
    private LocalDateTime createTime;
}
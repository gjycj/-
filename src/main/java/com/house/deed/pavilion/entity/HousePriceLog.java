package com.house.deed.pavilion.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 房源价格变动记录表（租户级数据隔离）
 * <p>
 * 核心业务说明：
 * 1. 价格追溯：记录租户内房源挂牌价的每一次调整记录，支撑价格变动轨迹追溯、市场行情分析；
 * 2. 租户隔离：所有字段绑定tenant_id，仅当前租户可查询/操作自身房源的价格变动记录；
 * 3. 核心关联：
 *    - house_id 关联房源表（同租户），标识价格变动的房源；
 *    - operator_id 关联经纪人表（同租户），标识调价操作人；
 * 4. 关键字段：
 *    - price_before/price_after：分别记录调整前后的挂牌价（单位：万元），非负且保留2位小数；
 *    - change_reason：调价原因（房东要求、市场行情、促销活动等），需明确且简洁；
 *    - operator_name：操作人姓名（冗余存储，避免关联查询，便于快速查看）；
 * 5. 数据规范：价格非负，调价原因限制长度，确保价格变动记录真实、可追溯。
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Getter
@Setter
@TableName(value = "house_price_log", autoResultMap = true) // 开启自动结果映射
@Schema(
        name = "HousePriceLog",
        description = "房源价格变动记录实体（租户级数据），记录房源挂牌价调整轨迹、调价原因及操作人信息",
        example = "{\"tenantId\": 1001, \"houseId\": 101, \"priceBefore\": 180.00, \"priceAfter\": 175.00, \"changeReason\": \"房东降价促销，加快成交\", \"operatorId\": 3001, \"operatorName\": \"张三（经纪人）\", \"createTime\": \"2025-11-26 15:00:00\"}"
)
public class HousePriceLog implements Serializable {

    /**
     * 序列化版本号：保证对象序列化/反序列化一致性，避免类结构变更导致反序列化异常
     */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 价格变动日志主键ID
     * 自增策略，唯一标识单条价格变动记录，无业务含义，仅作为数据库主键
     */
    @Schema(
            description = "价格变动日志主键ID（自增）",
            example = "1",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY // 只读：新增时数据库自增，无需传值
    )
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID
     * 关联租户表主键，标识价格变动记录归属的租户，核心隔离字段，非空
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
     * 关联house表主键（同租户下的房源），标识价格变动的房源，非空（核心关联字段）
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
     * 调整前价格
     * 房源调价前的挂牌价（单位：万元），非负，保留2位小数，非空（价格追溯依据）
     */
    @Schema(
            description = "调整前挂牌价（单位：万元），保留2位小数",
            example = "180.00",
            nullable = false,
            minimum = "0.00",
            format = "decimal",
            multipleOf = 0.01
    )
    @NotNull(message = "调整前价格不能为空")
    @DecimalMin(value = "0.00", inclusive = true, message = "调整前价格不能为负数")
    @TableField(value = "price_before")
    private BigDecimal priceBefore;

    /**
     * 调整后价格
     * 房源调价后的挂牌价（单位：万元），非负，保留2位小数，非空（新挂牌价）
     */
    @Schema(
            description = "调整后挂牌价（单位：万元），保留2位小数",
            example = "175.00",
            nullable = false,
            minimum = "0.00",
            format = "decimal",
            multipleOf = 0.01
    )
    @NotNull(message = "调整后价格不能为空")
    @DecimalMin(value = "0.00", inclusive = true, message = "调整后价格不能为负数")
    @TableField(value = "price_after")
    private BigDecimal priceAfter;

    /**
     * 调价原因
     * 价格调整的具体原因（如房东要求、市场行情、促销活动、房源装修升级等），非空，长度≤200字符
     */
    @Schema(
            description = "调价原因（如房东要求、市场行情、促销活动、装修升级等）",
            example = "房东降价促销，加快成交",
            nullable = false,
            maxLength = 200
    )
    @NotBlank(message = "调价原因不能为空")
    @Size(max = 200, message = "调价原因长度不能超过200字符")
    @TableField(value = "change_reason")
    private String changeReason;

    /**
     * 操作人ID
     * 关联经纪人表主键（同租户下），标识执行调价操作的经纪人，非空
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
     * 调价操作人的真实姓名（冗余存储，避免关联查询），非空，长度≤50字符
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
     * 调价时间
     * 价格调整的执行时间（精确到时分秒），数据库自动填充，无需手动传值，只读
     */
    @Schema(
            description = "价格调整执行时间（数据库自动填充）",
            example = "2025-11-26 15:00:00",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY,
            format = "date-time"
    )
    @TableField(value = "create_time", fill = FieldFill.INSERT) // 声明新增时自动填充
    private LocalDateTime createTime;
}
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
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 客户历史成交记录表（租户级数据隔离）
 * <p>
 * 核心业务说明：
 * 1. 成交追溯：记录租户内客户过往所有成交记录（买卖/租赁），支撑客户画像分析、二次营销；
 * 2. 租户隔离：所有字段绑定tenant_id，仅当前租户可查询/操作自身客户的成交历史；
 * 3. 核心关联：
 *    - customer_id 关联客户表，明确成交客户；
 *    - contract_id 关联合同表，追溯成交合同详情；
 * 4. 关键字段：
 *    - deal_time：成交日期（合同签订日期）；
 *    - house_info：成交房源简要信息（无需关联房源表，冗余存储便于快速查看）；
 *    - deal_type：成交类型（买卖/租赁）；
 * 5. 数据规范：房源简要信息简洁明了，成交类型固定枚举，确保数据一致性。
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Getter
@Setter
@TableName(value = "customer_history_deal", autoResultMap = true)
@Schema(
        name = "CustomerHistoryDeal",
        description = "客户历史成交记录实体（租户级数据），记录客户过往成交信息，支撑二次营销",
        example = "{\"tenantId\": 1001, \"customerId\": 1, \"contractId\": 5001, \"dealTime\": \"2025-10-15\", \"houseInfo\": \"滨江花园 3室2厅 120㎡\", \"dealType\": \"SALE\"}"
)
public class CustomerHistoryDeal implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 记录主键ID
     * 自增策略，唯一标识单条历史成交记录，无业务含义，仅作为数据库主键
     */
    @Schema(
            description = "历史成交记录主键ID（自增）",
            example = "1",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY
    )
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID
     * 关联租户表主键，标识成交记录归属的租户，核心隔离字段，非空
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
     * 客户ID
     * 关联customer表主键（同租户下的客户），标识成交客户，非空
     */
    @Schema(
            description = "客户ID（关联customer表，仅同租户下的客户有效）",
            example = "1",
            nullable = false
    )
    @NotNull(message = "客户ID不能为空")
    @TableField(value = "customer_id")
    private Long customerId;

    /**
     * 历史成交合同ID
     * 关联contract表主键（同租户下的合同），追溯成交合同详情，非空
     */
    @Schema(
            description = "成交合同ID（关联contract表，仅同租户下的合同有效）",
            example = "5001",
            nullable = false
    )
    @NotNull(message = "成交合同ID不能为空")
    @TableField(value = "contract_id")
    private Long contractId;

    /**
     * 成交日期
     * 合同签订的日期（成交核心时间节点），非空，格式：yyyy-MM-dd
     */
    @Schema(
            description = "成交日期（合同签订日期，格式：yyyy-MM-dd）",
            example = "2025-10-15",
            nullable = false,
            format = "date"
    )
    @NotNull(message = "成交日期不能为空")
    @TableField(value = "deal_time")
    private LocalDate dealTime;

    /**
     * 成交房源简要信息
     * 冗余存储房源核心信息（如“小区名称 户型 面积”），无需关联房源表，非空，长度≤200字符
     */
    @Schema(
            description = "成交房源简要信息（格式：小区名称 户型 面积，如“滨江花园 3室2厅 120㎡”）",
            example = "滨江花园 3室2厅 120㎡",
            nullable = false,
            maxLength = 200
    )
    @NotBlank(message = "成交房源简要信息不能为空")
    @Size(max = 200, message = "房源简要信息长度不能超过200字符")
    @TableField(value = "house_info")
    private String houseInfo;

    /**
     * 成交类型
     * 枚举值：SALE-买卖成交，RENT-租赁成交，非空
     */
    @Schema(
            description = "成交类型（SALE=买卖成交，RENT=租赁成交）",
            example = "SALE",
            nullable = false,
            allowableValues = {"SALE", "RENT"}
    )
    @NotBlank(message = "成交类型不能为空")
    @TableField(value = "deal_type")
    private String dealType;

    /**
     * 记录创建时间
     * 历史成交记录录入系统的时间，数据库自动填充，无需手动传值，只读
     */
    @Schema(
            description = "记录创建时间（数据库自动填充）",
            example = "2025-10-15 16:00:00",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY,
            format = "date-time"
    )
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
package com.house.deed.pavilion.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
 * 房东委托信息表（租户级数据隔离）
 * <p>
 * 核心业务说明：
 * 1. 委托管理：存储租户内房东对房源的委托代理信息（如独家委托、非独家委托），明确委托周期、权限及双方约定，是中介开展房源交易/租赁的合法依据；
 * 2. 租户隔离：所有字段绑定tenant_id，仅当前租户可查询/操作自身的委托记录，保护委托业务数据隔离性；
 * 3. 核心关联：
 *    - 房东关联：landlord_id 关联房东表（同租户），标识委托方；
 *    - 房源关联：house_id 关联房源表（同租户），标识被委托房源（一对一：一个房源同一时间仅支持一个有效委托）；
 * 4. 委托核心要素：
 *    - 委托类型：EXCLUSIVE-独家委托（仅当前中介可操作）、NON_EXCLUSIVE-非独家委托（多中介可操作）；
 *    - 周期管控：entrust_start_time（开始时间）、entrust_end_time（结束时间），需满足“结束时间≥开始时间”；
 *    - 状态管理：status（1-有效，0-过期/取消），支撑委托生命周期管控（到期自动失效、手动取消）；
 *    - 提醒功能：renew_remind（1-开启到期提醒，0-不开启），辅助中介及时跟进委托续约；
 * 5. 合规规范：委托记录需明确委托类型、周期及备注（特殊约定），确保中介操作房源的合法性。
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Getter
@Setter
@TableName(value = "landlord_entrust", autoResultMap = true) // 开启自动结果映射
@Schema(
        name = "LandlordEntrust",
        description = "房东委托信息实体（租户级数据），记录房东对房源的委托代理关系、周期及约定，支撑合规运营",
        example = "{\"tenantId\": 1001, \"landlordId\": 201, \"houseId\": 101, \"entrustType\": \"EXCLUSIVE\", \"entrustStartTime\": \"2025-12-01\", \"entrustEndTime\": \"2026-12-01\", \"renewRemind\": 1, \"remark\": \"独家委托期间，中介需每月同步房源带看情况\", \"status\": 1}"
)
public class LandlordEntrust implements Serializable {

    /**
     * 序列化版本号：保证对象序列化/反序列化一致性，避免类结构变更导致反序列化异常
     */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 委托记录主键ID
     * 自增策略，唯一标识单条房东委托记录，无业务含义，仅作为数据库主键
     */
    @Schema(
            description = "委托记录主键ID（自增）",
            example = "301",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY // 只读：新增时数据库自增，无需传值
    )
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID
     * 关联租户表主键，标识委托记录归属的租户，核心隔离字段，非空
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
     * 房东ID
     * 关联landlord表主键（同租户下的房东），标识委托方（房东），非空（核心关联字段）
     */
    @Schema(
            description = "房东ID（关联landlord表，仅同租户下的房东有效）",
            example = "201",
            nullable = false
    )
    @NotNull(message = "房东ID不能为空")
    @TableField(value = "landlord_id")
    private Long landlordId;

    /**
     * 房源ID
     * 关联house表主键（同租户下的房源），标识被委托的房源，非空（核心关联字段）
     * 业务约束：同一房源同一时间仅支持1条有效委托记录
     */
    @Schema(
            description = "房源ID（关联house表，仅同租户下的房源有效；同一房源同一时间仅支持1条有效委托）",
            example = "101",
            nullable = false
    )
    @NotNull(message = "房源ID不能为空")
    @TableField(value = "house_id")
    private Long houseId;

    /**
     * 委托类型
     * 枚举值：EXCLUSIVE-独家委托（仅当前中介可操作房源），NON_EXCLUSIVE-非独家委托（多中介可操作），非空
     */
    @Schema(
            description = "委托类型（EXCLUSIVE=独家委托，NON_EXCLUSIVE=非独家委托）",
            example = "EXCLUSIVE",
            nullable = false,
            allowableValues = {"EXCLUSIVE", "NON_EXCLUSIVE"}
    )
    @NotBlank(message = "委托类型不能为空")
    @TableField(value = "entrust_type")
    private String entrustType;

    /**
     * 委托开始时间
     * 委托协议生效的日期（格式：yyyy-MM-dd），非空（核心时间节点）
     */
    @Schema(
            description = "委托开始时间（格式：yyyy-MM-dd）",
            example = "2025-12-01",
            nullable = false,
            format = "date"
    )
    @NotNull(message = "委托开始时间不能为空")
    @TableField(value = "entrust_start_time")
    private LocalDate entrustStartTime;

    /**
     * 委托结束时间
     * 委托协议失效的日期（格式：yyyy-MM-dd），非空，需晚于委托开始时间
     */
    @Schema(
            description = "委托结束时间（格式：yyyy-MM-dd），需晚于委托开始时间",
            example = "2026-12-01",
            nullable = false,
            format = "date"
    )
    @NotNull(message = "委托结束时间不能为空")
    @TableField(value = "entrust_end_time")
    private LocalDate entrustEndTime;

    /**
     * 是否到期提醒
     * 枚举值：1-是（开启委托到期提醒），0-否（不开启），非空（默认1）
     */
    @Schema(
            description = "是否开启委托到期提醒（1=是，0=否）",
            example = "1",
            nullable = false,
            allowableValues = {"0", "1"}
    )
    @NotNull(message = "是否到期提醒不能为空")
    @Min(value = 0, message = "是否到期提醒仅支持0（否）或1（是）")
    @Max(value = 1, message = "是否到期提醒仅支持0（否）或1（是）")
    @TableField(value = "renew_remind")
    private Byte renewRemind;

    /**
     * 委托备注
     * 委托双方的特殊约定（如独家委托佣金比例、房源带看频率要求、禁止转租等），可空，长度≤500字符
     */
    @Schema(
            description = "委托特殊约定（如佣金比例、带看要求等）",
            example = "独家委托期间，中介需每月5日前同步房源带看情况；成交佣金按2%结算",
            nullable = true,
            maxLength = 500
    )
    @Size(max = 500, message = "委托备注长度不能超过500字符")
    @TableField(value = "remark")
    private String remark;

    /**
     * 委托状态
     * 枚举值：1-有效（委托在有效期内），0-过期/取消（委托已到期或手动取消），非空（默认1）
     */
    @Schema(
            description = "委托状态（1=有效，0=过期/取消）",
            example = "1",
            nullable = false,
            allowableValues = {"0", "1"}
    )
    @NotNull(message = "委托状态不能为空")
    @Min(value = 0, message = "委托状态仅支持0（过期/取消）或1（有效）")
    @Max(value = 1, message = "委托状态仅支持0（过期/取消）或1（有效）")
    @TableField(value = "status")
    private Byte status;

    /**
     * 创建时间
     * 委托记录创建时间，数据库自动填充，无需手动传值，只读
     */
    @Schema(
            description = "委托记录创建时间（数据库自动填充）",
            example = "2025-11-26 15:00:00",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY,
            format = "date-time"
    )
    @TableField(value = "create_time", fill = FieldFill.INSERT) // 声明新增时自动填充
    private LocalDateTime createTime;

    /**
     * 更新时间
     * 委托记录更新时间（如状态变更、备注修改），数据库自动填充，无需手动传值，只读
     */
    @Schema(
            description = "委托记录更新时间（数据库自动填充）",
            example = "2025-11-26 15:30:00",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY,
            format = "date-time"
    )
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE) // 声明新增/修改时自动填充
    private LocalDateTime updateTime;
}
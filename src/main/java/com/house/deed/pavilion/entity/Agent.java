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
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 经纪人信息表（租户级数据隔离）
 * <p>
 * 核心业务说明：
 * 1. 存储经纪人基础信息，作为房源/合同操作的核心责任人维度；
 * 2. 租户级隔离：所有字段绑定 tenant_id，仅当前租户可查询/操作自身经纪人数据；
 * 3. 关联关系：store_id 关联 store 表（同租户下的门店），create_agent_id 关联本表（创建人需为同租户经纪人）；
 * 4. 状态管控：status 标识在职/离职，level 区分经纪人等级，入职时间记录从业起点。
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Getter
@Setter
@TableName(value = "agent", autoResultMap = true)
@Schema(
        name = "Agent",
        description = "经纪人信息实体（租户级数据隔离）",
        example = "{\"tenantId\": 1001, \"storeId\": 201, \"agentCode\": \"BJ001\", \"name\": \"张三\", \"phone\": \"13800138000\", \"idCard\": \"330106199001011234\", \"position\": \"经纪人\", \"level\": \"SENIOR\", \"entryTime\": \"2020-01-01\", \"status\": 1, \"createAgentId\": 3001}"
)
public class Agent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 经纪人主键ID
     * 自增策略，唯一标识单个经纪人，无业务含义，仅作为数据库主键
     */
    @Schema(
            description = "经纪人主键ID（自增）",
            example = "1",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY
    )
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID
     * 关联租户表主键，标识经纪人归属的租户，核心隔离字段，非空
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
     * 所属门店ID
     * 关联store表主键（同租户下的门店），经纪人归属的门店，非空
     */
    @Schema(
            description = "所属门店ID（关联store表，仅同租户下的门店有效）",
            example = "201",
            nullable = false
    )
    @NotNull(message = "所属门店ID不能为空")
    @TableField(value = "store_id")
    private Long storeId;

    /**
     * 经纪人工号
     * 租户内唯一，格式建议：BJ+3位数字（如BJ001），长度≤10字符，非空
     */
    @Schema(
            description = "经纪人工号（租户内唯一，格式：BJ+3位数字）",
            example = "BJ001",
            nullable = false,
            maxLength = 10,
            pattern = "^BJ\\d{3}$"
    )
    @NotBlank(message = "经纪人工号不能为空")
    @Size(max = 10, message = "经纪人工号长度不能超过10字符")
    @Pattern(regexp = "^BJ\\d{3}$", message = "经纪人工号格式错误（需为BJ+3位数字，如BJ001）")
    @TableField(value = "agent_code")
    private String agentCode;

    /**
     * 经纪人姓名
     * 真实姓名，长度≤20字符，非空
     */
    @Schema(
            description = "经纪人真实姓名",
            example = "张三",
            nullable = false,
            maxLength = 20
    )
    @NotBlank(message = "经纪人姓名不能为空")
    @Size(max = 20, message = "经纪人姓名长度不能超过20字符")
    @TableField(value = "name")
    private String name;

    /**
     * 联系电话
     * 11位手机号，格式校验：1开头的11位数字，非空
     */
    @Schema(
            description = "经纪人联系电话（11位手机号）",
            example = "13800138000",
            nullable = false,
            pattern = "^1[3-9]\\d{9}$"
    )
    @NotBlank(message = "联系电话不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式错误（需为11位有效手机号）")
    @TableField(value = "phone")
    private String phone;

    /**
     * 身份证号
     * 18位身份证格式，含最后一位X（大小写均可），非空
     */
    @Schema(
            description = "经纪人身份证号（18位，支持最后一位X）",
            example = "330106199001011234",
            nullable = false,
            pattern = "^[1-9]\\d{5}(18|19|20)\\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]$"
    )
    @NotBlank(message = "身份证号不能为空")
    @Pattern(regexp = "^[1-9]\\d{5}(18|19|20)\\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]$",
            message = "身份证号格式错误（需为18位有效身份证号）")
    @TableField(value = "id_card")
    private String idCard;

    /**
     * 职位
     * 如：经纪人、店长、区域经理，长度≤30字符，非空
     */
    @Schema(
            description = "经纪人职位（如：经纪人、店长、区域经理）",
            example = "经纪人",
            nullable = false,
            maxLength = 30
    )
    @NotBlank(message = "职位不能为空")
    @Size(max = 30, message = "职位长度不能超过30字符")
    @TableField(value = "position")
    private String position;

    /**
     * 经纪人等级
     * 枚举值：JUNIOR-初级，SENIOR-高级，STAR-明星，非空
     */
    @Schema(
            description = "经纪人等级（JUNIOR=初级，SENIOR=高级，STAR=明星）", // 直接在description中说明语义
            example = "SENIOR",
            nullable = false,
            allowableValues = {"JUNIOR", "SENIOR", "STAR"} // 标准属性：声明合法值
    )
    @NotBlank(message = "经纪人等级不能为空")
    @Pattern(regexp = "^(JUNIOR|SENIOR|STAR)$", message = "经纪人等级错误（仅支持JUNIOR/SENIOR/STAR）")
    @TableField(value = "level")
    private String level;

    /**
     * 入职时间
     * 格式：yyyy-MM-dd，需为过去的日期，非空
     */
    @Schema(
            description = "经纪人入职时间（格式：yyyy-MM-dd）",
            example = "2020-01-01",
            nullable = false,
            format = "date"
    )
    @NotNull(message = "入职时间不能为空")
    @TableField(value = "entry_time")
    private LocalDate entryTime;

    /**
     * 状态
     * 1=在职，0=离职，非空，仅支持这两个值
     */
    @Schema(
            description = "经纪人状态（1=在职，0=离职）", // 直接在description中说明语义
            example = "1",
            nullable = false,
            allowableValues = {"0", "1"} // 标准属性：声明合法值
    )
    @NotNull(message = "经纪人状态不能为空")
    @TableField(value = "status")
    private Byte status;

    /**
     * 创建时间
     * 数据库自动填充（新增时），无需手动传值
     */
    @Schema(
            description = "经纪人信息创建时间（数据库自动填充）",
            example = "2025-11-26 10:00:00",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY,
            format = "date-time"
    )
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     * 数据库自动填充（新增/修改时），无需手动传值
     */
    @Schema(
            description = "经纪人信息更新时间（数据库自动填充）",
            example = "2025-11-26 11:00:00",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY,
            format = "date-time"
    )
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 创建人ID（经纪人）
     * 关联本表主键（同租户下的经纪人），标识该条记录的创建者，非空
     */
    @Schema(
            description = "创建人ID（关联本表，仅同租户下的经纪人有效）",
            example = "3001",
            nullable = false
    )
    @NotNull(message = "创建人ID不能为空")
    @TableField(value = "create_agent_id")
    private Long createAgentId;
}
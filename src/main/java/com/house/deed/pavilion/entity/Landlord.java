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
 * 房东信息表（租户级数据隔离）
 * <p>
 * 核心业务说明：
 * 1. 核心主体：存储租户内房产房东的基础信息、联系方式及实名认证信息，是租赁/买卖交易中房东端的核心数据；
 * 2. 租户隔离：所有字段绑定tenant_id，仅当前租户可查询/操作自身的房东数据，保护房东隐私；
 * 3. 关联关系：
 *    - 经纪人关联：create_agent_id 关联经纪人表（同租户），标识录入房东信息的责任人；
 *    - 房源关联：通过house_landlord关联表实现“一个房东多套房源”的多对多关系；
 * 4. 合规校验：
 *    - 身份认证：id_card 字段存储房东身份证号（需脱敏展示，完整存储用于合规校验），支持18位身份证（含最后一位X）；
 *    - 联系有效性：phone 字段校验手机号格式，确保交易过程中可正常联系；
 * 5. 数据规范：姓名、手机号、身份证号为必填项，联系地址简洁明了，确保信息真实有效。
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Getter
@Setter
@TableName(value = "landlord", autoResultMap = true) // 开启自动结果映射
@Schema(
        name = "Landlord",
        description = "房东信息实体（租户级数据），存储房东基础信息、联系方式及实名认证信息，支撑交易合规",
        example = "{\"tenantId\": 1001, \"name\": \"张三\", \"phone\": \"13800138000\", \"idCard\": \"330106199001011234\", \"address\": \"浙江省杭州市滨江区江南大道123号\", \"createAgentId\": 3001}"
)
public class Landlord implements Serializable {

    /**
     * 序列化版本号：保证对象序列化/反序列化一致性，避免类结构变更导致反序列化异常
     */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 房东主键ID
     * 自增策略，唯一标识单个房东，无业务含义，仅作为数据库主键
     */
    @Schema(
            description = "房东主键ID（自增）",
            example = "201",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY // 只读：新增时数据库自增，无需传值
    )
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID
     * 关联租户表主键，标识房东归属的租户，核心隔离字段，非空
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
     * 房东姓名
     * 房东真实姓名（与身份证一致），非空，长度≤50字符
     */
    @Schema(
            description = "房东真实姓名（与身份证一致）",
            example = "张三",
            nullable = false,
            maxLength = 50
    )
    @NotBlank(message = "房东姓名不能为空")
    @Size(max = 50, message = "房东姓名长度不能超过50字符")
    @TableField(value = "name")
    private String name;

    /**
     * 联系电话
     * 房东常用手机号（支持11位大陆手机号，含+86前缀），非空，用于交易沟通
     */
    @Schema(
            description = "房东联系电话（支持11位手机号，含+86前缀）",
            example = "13800138000",
            nullable = false,
            maxLength = 20,
            pattern = "^(\\+86)?1[3-9]\\d{9}$" // 支持+86前缀，手机号以13-9开头，共11位
    )
    @NotBlank(message = "联系电话不能为空")
    @Size(max = 20, message = "联系电话长度不能超过20字符")
    @Pattern(regexp = "^(\\+86)?1[3-9]\\d{9}$", message = "联系电话格式错误（支持11位手机号，可带+86前缀）")
    @TableField(value = "phone")
    private String phone;

    /**
     * 身份证号
     * 房东身份证号码（18位，支持最后一位为X/x），非空，用于实名认证和合规校验
     */
    @Schema(
            description = "房东身份证号（18位，支持最后一位X/x）",
            example = "330106199001011234",
            nullable = false,
            maxLength = 18,
            minLength = 18,
            pattern = "^[1-9]\\d{5}(19|20)\\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]$" // 18位身份证正则
    )
    @NotBlank(message = "身份证号不能为空")
    @Size(min = 18, max = 18, message = "身份证号必须为18位")
    @Pattern(regexp = "^[1-9]\\d{5}(19|20)\\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]$", message = "身份证号格式错误")
    @TableField(value = "id_card")
    private String idCard;

    /**
     * 联系地址
     * 房东常用联系地址（如户籍地址、现居住地址），可空，长度≤200字符
     */
    @Schema(
            description = "房东联系地址（如户籍地址、现居住地址）",
            example = "浙江省杭州市滨江区江南大道123号",
            nullable = true,
            maxLength = 200
    )
    @Size(max = 200, message = "联系地址长度不能超过200字符")
    @TableField(value = "address")
    private String address;

    /**
     * 录入经纪人ID
     * 关联agent表主键（同租户下的经纪人），标识录入房东信息的责任人，非空
     */
    @Schema(
            description = "录入经纪人ID（关联agent表，仅同租户下的经纪人有效）",
            example = "3001",
            nullable = false
    )
    @NotNull(message = "录入经纪人ID不能为空")
    @TableField(value = "create_agent_id")
    private Long createAgentId;

    /**
     * 创建时间
     * 房东信息录入系统的时间，数据库自动填充，无需手动传值，只读
     */
    @Schema(
            description = "房东信息录入时间（数据库自动填充）",
            example = "2025-11-26 10:00:00",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY,
            format = "date-time"
    )
    @TableField(value = "create_time", fill = FieldFill.INSERT) // 声明新增时自动填充
    private LocalDateTime createTime;

    /**
     * 更新时间
     * 房东信息更新时间（如联系方式、地址变更），数据库自动填充，无需手动传值，只读
     */
    @Schema(
            description = "房东信息更新时间（数据库自动填充）",
            example = "2025-11-26 14:30:00",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY,
            format = "date-time"
    )
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE) // 声明新增/修改时自动填充
    private LocalDateTime updateTime;
}
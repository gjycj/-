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
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 门店信息表（租户级数据隔离）
 * <p>
 * 核心业务说明：
 * 1. 门店管理核心：存储租户内房产中介门店的基础信息、运营状态及关联关系，支撑经纪人归属、房源分配、客户对接等业务；
 * 2. 租户隔离：所有字段绑定tenant_id，仅当前租户可查询/操作自身门店数据，保护门店运营数据隔离性；
 * 3. 核心关联：
 *    - 区域关联：region_id 关联区域表（region，同租户），标识门店所属行政区域（省/市/区/街道）；
 *    - 人员关联：manager_id 关联经纪人表（agent，同租户），标识门店店长（负责人）；
 * 4. 关键约束：
 *    - 门店编码：tenant_id + store_code 组合唯一（租户内门店编码唯一，建议格式：租户前缀+序号，如T1001-ST001）；
 *    - 运营状态：status（1=营业，0=停业），支撑门店运营管控（停业后不可分配新业务）；
 *    - 基础信息：门店名称、电话、详细地址为运营必备信息，确保客户可联系、可到访；
 * 5. 业务价值：门店作为经纪人的组织单元，关联房源、客户、交易等核心业务数据，是中介机构的基础运营载体。
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Getter
@Setter
@TableName(value = "store", autoResultMap = true) // 开启自动结果映射
@Schema(
        name = "Store",
        description = "门店信息实体（租户级数据），存储中介门店基础信息、运营状态及关联关系，支撑门店运营管理",
        example = "{\"tenantId\": 1001, \"storeCode\": \"T1001-ST001\", \"storeName\": \"链家·杭州滨江江南大道店\", \"regionId\": 330108, \"address\": \"浙江省杭州市滨江区江南大道123号绿城春江明月1楼\", \"managerId\": 3001, \"phone\": \"0571-88888888\", \"status\": 1}"
)
public class Store implements Serializable {

    /**
     * 序列化版本号：保证对象序列化/反序列化一致性，避免类结构变更导致反序列化异常
     */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 门店主键ID
     * 自增策略，唯一标识单个门店，无业务含义，仅作为数据库主键
     */
    @Schema(
            description = "门店主键ID（自增）",
            example = "701",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY // 只读：新增时数据库自增，无需传值
    )
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID
     * 关联租户表主键，标识门店归属的租户，核心隔离字段，非空
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
     * 门店编码
     * 租户内唯一标识（建议格式：租户前缀+序号，如T1001-ST001），非空，长度≤20字符
     */
    @Schema(
            description = "门店编码（租户内唯一，建议格式：租户前缀+序号，如T1001-ST001）",
            example = "T1001-ST001",
            nullable = false,
            maxLength = 20
    )
    @NotBlank(message = "门店编码不能为空")
    @Size(max = 20, message = "门店编码长度不能超过20字符")
    @TableField(value = "store_code")
    private String storeCode;

    /**
     * 门店名称
     * 门店对外展示名称（如“链家·杭州滨江江南大道店”），非空，长度≤100字符
     */
    @Schema(
            description = "门店对外展示名称（如“链家·杭州滨江江南大道店”）",
            example = "链家·杭州滨江江南大道店",
            nullable = false,
            maxLength = 100
    )
    @NotBlank(message = "门店名称不能为空")
    @Size(max = 100, message = "门店名称长度不能超过100字符")
    @TableField(value = "store_name")
    private String storeName;

    /**
     * 所属区域ID
     * 关联region表主键（同租户下的行政区域，建议到区级/街道级），标识门店所在区域，非空
     */
    @Schema(
            description = "所属区域ID（关联region表，仅同租户下的行政区域有效，建议到区级/街道级）",
            example = "330108",
            nullable = false
    )
    @NotNull(message = "所属区域ID不能为空")
    @TableField(value = "region_id")
    private Long regionId;

    /**
     * 详细地址
     * 门店具体联系地址（含省市区街道+门牌号），非空，长度≤200字符，确保客户可到访
     */
    @Schema(
            description = "门店详细地址（含省市区街道+门牌号，确保可到访）",
            example = "浙江省杭州市滨江区江南大道123号绿城春江明月1楼",
            nullable = false,
            maxLength = 200
    )
    @NotBlank(message = "详细地址不能为空")
    @Size(max = 200, message = "详细地址长度不能超过200字符")
    @TableField(value = "address")
    private String address;

    /**
     * 店长ID
     * 关联agent表主键（同租户下的经纪人），标识门店负责人，非空（门店必须指定店长）
     */
    @Schema(
            description = "店长ID（关联agent表，仅同租户下的经纪人有效，为门店负责人）",
            example = "3001",
            nullable = false
    )
    @NotNull(message = "店长ID不能为空")
    @TableField(value = "manager_id")
    private Long managerId;

    /**
     * 门店电话
     * 门店对外联系电话（支持固话/手机号，固话格式：区号-号码，如0571-88888888；手机号支持+86前缀），非空
     */
    @Schema(
            description = "门店联系电话（支持固话：区号-号码，如0571-88888888；手机号：11位数字，可带+86前缀）",
            example = "0571-88888888",
            nullable = false,
            maxLength = 20,
            pattern = "^(\\+86)?1[3-9]\\d{9}$|^0\\d{2,3}-\\d{7,8}$"
    )
    @NotBlank(message = "门店电话不能为空")
    @Size(max = 20, message = "门店电话长度不能超过20字符")
    @Pattern(regexp = "^(\\+86)?1[3-9]\\d{9}$|^0\\d{2,3}-\\d{7,8}$", message = "门店电话格式错误（支持手机号或固话：区号-号码）")
    @TableField(value = "phone")
    private String phone;

    /**
     * 运营状态
     * 枚举值：1=营业（正常开展业务），0=停业（暂停业务，不可分配新单），非空
     */
    @Schema(
            description = "门店运营状态（1=营业，0=停业）",
            example = "1",
            nullable = false,
            allowableValues = {"0", "1"}
    )
    @NotNull(message = "运营状态不能为空")
    @Min(value = 0, message = "运营状态仅支持0（停业）或1（营业）")
    @Max(value = 1, message = "运营状态仅支持0（停业）或1（营业）")
    @TableField(value = "status")
    private Byte status;

    /**
     * 创建时间
     * 门店信息录入系统的时间，数据库自动填充，无需手动传值，只读
     */
    @Schema(
            description = "门店创建时间（数据库自动填充）",
            example = "2025-11-28 16:00:00",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY,
            format = "date-time"
    )
    @TableField(value = "create_time", fill = FieldFill.INSERT) // 声明新增时自动填充
    private LocalDateTime createTime;

    /**
     * 更新时间
     * 门店信息更新时间（如地址变更、店长更换、状态切换），数据库自动填充，无需手动传值，只读
     */
    @Schema(
            description = "门店信息更新时间（数据库自动填充）",
            example = "2025-11-28 16:30:00",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY,
            format = "date-time"
    )
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE) // 声明新增/修改时自动填充
    private LocalDateTime updateTime;
}
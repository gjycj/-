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
 * 租赁合同附加条款表（租户级数据隔离）
 * <p>
 * 核心业务说明：
 * 1. 专属场景：仅关联租赁合同（contract表中contract_type=RENT），存储租赁业务特有附加条款，补充主合同未覆盖的细节约定；
 * 2. 租户隔离：所有字段绑定tenant_id，仅当前租户可查询/操作自身租赁合同的附加条款；
 * 3. 核心条款：
 *    - 权限约定：allow_pet（是否允许养宠物）、allow_sublet（是否允许转租），采用0/1布尔型标识；
 *    - 责任划分：fee_bear（费用承担，如物业费/水电费归属）、furniture_maintenance（家具维修责任）；
 * 4. 数据关联：contract_id 严格关联同租户下的租赁类型合同，确保条款与合同一一对应；
 * 5. 规范要求：文本类条款需明确、无歧义，长度限制在合理范围，便于合同履行和纠纷追溯。
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Getter
@Setter
@TableName(value = "contract_lease_terms", autoResultMap = true) // 开启自动结果映射（兼容复杂类型）
@Schema(
        name = "ContractLeaseTerms",
        description = "租赁合同附加条款实体（租户级数据），存储租赁业务特有附加约定（宠物、转租、费用承担等）",
        example = "{\"tenantId\": 1001, \"contractId\": 2, \"allowPet\": 0, \"allowSublet\": 0, \"feeBear\": \"物业费由房东承担，水电费、燃气费由租户承担\", \"furnitureMaintenance\": \"自然损坏由房东负责维修，人为损坏由租户承担费用\"}"
)
public class ContractLeaseTerms implements Serializable {

    /**
     * 序列化版本号：保证对象序列化/反序列化一致性，避免类结构变更导致反序列化异常
     */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 条款主键ID
     * 自增策略，唯一标识单套租赁附加条款，无业务含义，仅作为数据库主键
     */
    @Schema(
            description = "条款主键ID（自增）",
            example = "1",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY // 只读：新增时数据库自增，无需传值
    )
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID
     * 关联租户表主键，标识附加条款归属的租户，核心隔离字段，非空
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
     * 合同ID
     * 关联contract表主键（同租户下的租赁类型合同，contract_type=RENT），非空（核心关联字段）
     */
    @Schema(
            description = "合同ID（关联contract表，仅同租户下的租赁类型合同有效）",
            example = "2",
            nullable = false
    )
    @NotNull(message = "合同ID不能为空")
    @TableField(value = "contract_id")
    private Long contractId;

    /**
     * 是否允许养宠物
     * 布尔型标识：1=是，0=否，非空（租赁场景核心约定，避免后续纠纷）
     */
    @Schema(
            description = "是否允许养宠物（1=是，0=否）",
            example = "0",
            nullable = false,
            allowableValues = {"0", "1"} // 仅支持0/1取值
    )
    @NotNull(message = "是否允许养宠物不能为空")
    @Pattern(regexp = "^[01]$", message = "是否允许养宠物仅支持0（否）/1（是）")
    @TableField(value = "allow_pet")
    private Byte allowPet;

    /**
     * 是否允许转租
     * 布尔型标识：1=是，0=否，非空（租赁场景核心约定，需明确转租权限）
     */
    @Schema(
            description = "是否允许转租（1=是，0=否）",
            example = "0",
            nullable = false,
            allowableValues = {"0", "1"}
    )
    @NotNull(message = "是否允许转租不能为空")
    @Pattern(regexp = "^[01]$", message = "是否允许转租仅支持0（否）/1（是）")
    @TableField(value = "allow_sublet")
    private Byte allowSublet;

    /**
     * 费用承担约定
     * 明确租赁期间各类费用（物业费、水电费、燃气费等）的承担方，非空，长度≤500字符
     */
    @Schema(
            description = "费用承担约定（明确物业费、水电费、燃气费等费用的承担方）",
            example = "物业费由房东承担，水电费、燃气费由租户承担",
            nullable = false,
            maxLength = 500
    )
    @NotBlank(message = "费用承担约定不能为空")
    @Size(max = 500, message = "费用承担约定长度不能超过500字符")
    @TableField(value = "fee_bear")
    private String feeBear;

    /**
     * 家具维修约定
     * 明确租赁房屋内家具、家电的维修责任（自然损坏/人为损坏的责任划分），非空，长度≤500字符
     */
    @Schema(
            description = "家具维修约定（明确自然损坏/人为损坏的维修责任划分）",
            example = "自然损坏由房东负责维修，人为损坏由租户承担费用",
            nullable = false,
            maxLength = 500
    )
    @NotBlank(message = "家具维修约定不能为空")
    @Size(max = 500, message = "家具维修约定长度不能超过500字符")
    @TableField(value = "furniture_maintenance")
    private String furnitureMaintenance;

    /**
     * 创建时间
     * 附加条款创建时间，数据库自动填充（新增时），无需手动传值，只读
     */
    @Schema(
            description = "附加条款创建时间（数据库自动填充）",
            example = "2025-11-26 15:00:00",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY,
            format = "date-time"
    )
    @TableField(value = "create_time", fill = FieldFill.INSERT) // 声明新增时自动填充
    private LocalDateTime createTime;
}
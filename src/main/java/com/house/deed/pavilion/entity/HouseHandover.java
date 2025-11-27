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
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 房屋交接记录表（租户级数据隔离）
 * <p>
 * 核心业务说明：
 * 1. 租赁专属：仅关联租赁合同（contract_type=RENT），记录房屋「入住交接」和「退租交接」全流程信息，支撑租赁责任划分；
 * 2. 租户隔离：所有字段绑定tenant_id，仅当前租户可查询/操作自身租赁业务的交接记录；
 * 3. 核心维度：
 *    - 关联维度：contract_id（关联租赁合同）、house_id（关联交接房源）、last_maintenance_id（关联维修工单）；
 *    - 交接类型：CHECK_IN（入住交接）、CHECK_OUT（退租交接），决定字段有效性（如退租需填损坏记录）；
 *    - 核心信息：
 *      - 资产核对：appliances_list（家具家电清单，JSON格式）、damage_records（房屋损坏记录）；
 *      - 费用结算：water_meter/electricity_meter/gas_meter（表底数）、settlement_status（费用结算状态）；
 *      - 责任确认：handover_person（房东/代理人）、receiver（租户）、sign_image_url（交接签字图片）；
 *      - 维修关联：maintenance_remark（维修结果）、maintenance_cost（维修费用）、maintenance_bearer（费用承担方）；
 * 4. 状态管控：status（DRAFT=草稿，CONFIRMED=已确认），确认后不可修改，作为费用结算和责任认定的依据。
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Getter
@Setter
@TableName(value = "house_handover", autoResultMap = true) // 开启自动结果映射（兼容JSON字段）
@Schema(
        name = "HouseHandover",
        description = "房屋交接记录实体（租户级数据），记录租赁业务入住/退租交接的资产核对、费用结算及责任确认信息",
        example = "{\"tenantId\": 1001, \"contractId\": 5001, \"houseId\": 101, \"handoverType\": \"CHECK_IN\", \"handoverTime\": \"2025-12-01 14:00:00\", \"settlementStatus\": \"SETTLED\", \"appliancesList\": \"{\\\"冰箱\\\":\\\"海尔\\\",\\\"空调\\\":2,\\\"洗衣机\\\":\\\"美的\\\"}\", \"waterMeter\": 120.00, \"electricityMeter\": 350.00, \"gasMeter\": 80.00, \"damageRecords\": \"无\", \"handoverPerson\": \"张三（房东）\", \"receiver\": \"李四（租户）\", \"signImageUrl\": \"https://oss.example.com/handover/sign/5001.jpg\", \"status\": \"CONFIRMED\", \"maintenanceRemark\": \"无维修记录\", \"maintenanceCost\": 0.00, \"maintenanceBearer\": \"LANDLORD\", \"lastMaintenanceId\": null}"
)
public class HouseHandover implements Serializable {

    /**
     * 序列化版本号：保证对象序列化/反序列化一致性，避免类结构变更导致反序列化异常
     */
    private static final long serialVersionUID = 1L;

    /**
     * 交接记录主键ID
     * 自增策略，唯一标识单条交接记录，无业务含义，仅作为数据库主键
     */
    @Schema(
            description = "交接记录主键ID（自增）",
            example = "1",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY // 只读：新增时数据库自增，无需传值
    )
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID
     * 关联租户表主键，标识交接记录归属的租户，核心隔离字段，非空
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
     * 关联contract表主键（同租户下的租赁合同，contract_type=RENT），非空（核心关联字段）
     */
    @Schema(
            description = "合同ID（关联租赁合同，仅同租户下的租赁类型合同有效）",
            example = "5001",
            nullable = false
    )
    @NotNull(message = "合同ID不能为空")
    @TableField(value = "contract_id")
    private Long contractId;

    /**
     * 房源ID
     * 关联house表主键（同租户下的房源），标识交接的具体房屋，非空（核心关联字段）
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
     * 交接类型
     * 枚举值：CHECK_IN-入住交接，CHECK_OUT-退租交接，非空（决定交接流程和字段有效性）
     */
    @Schema(
            description = "交接类型（CHECK_IN=入住交接，CHECK_OUT=退租交接）",
            example = "CHECK_IN",
            nullable = false,
            allowableValues = {"CHECK_IN", "CHECK_OUT"}
    )
    @NotBlank(message = "交接类型不能为空")
    @TableField(value = "handover_type")
    private String handoverType;

    /**
     * 交接时间
     * 实际完成交接的时间（精确到时分秒），非空（核心时间节点）
     */
    @Schema(
            description = "交接完成时间（格式：yyyy-MM-dd HH:mm:ss）",
            example = "2025-12-01 14:00:00",
            nullable = false,
            format = "date-time"
    )
    @NotNull(message = "交接时间不能为空")
    @TableField(value = "handover_time")
    private LocalDateTime handoverTime;

    /**
     * 费用结算状态
     * 枚举值：UNSETTLED-未结算，SETTLED-已结算，非空（标识水电燃气、押金等费用是否结清）
     */
    @Schema(
            description = "费用结算状态（UNSETTLED=未结算，SETTLED=已结算）",
            example = "SETTLED",
            nullable = false,
            allowableValues = {"UNSETTLED", "SETTLED"}
    )
    @NotBlank(message = "费用结算状态不能为空")
    @TableField(value = "settlement_status")
    private String settlementStatus;

    /**
     * 家具家电清单
     * JSON格式字符串（如{"冰箱":"海尔","空调":2,"洗衣机":"美的"}），记录交接时房屋内的资产，非空
     */
    @Schema(
            description = "家具家电清单（JSON格式，如{\"冰箱\":\"海尔\",\"空调\":2,\"洗衣机\":\"美的\"}）",
            example = "{\"冰箱\":\"海尔\",\"空调\":2,\"洗衣机\":\"美的\"}",
            nullable = false,
            maxLength = 1000,
            pattern = "^\\{.*\\}$" // 简单校验JSON格式（{}包裹）
    )
    @NotBlank(message = "家具家电清单不能为空")
    @Size(max = 1000, message = "家具家电清单长度不能超过1000字符")
    @Pattern(regexp = "^\\{.*\\}$", message = "家具家电清单需为JSON格式（如{\"冰箱\":\"海尔\"}）")
    @TableField(value = "appliances_list")
    private String appliancesList;

    /**
     * 水表底数
     * 交接时的水表读数（单位：吨），非负，保留2位小数，非空（费用结算依据）
     */
    @Schema(
            description = "水表底数（单位：吨），保留2位小数",
            example = "120.00",
            nullable = false,
            minimum = "0.00",
            format = "decimal",
            multipleOf = 0.01
    )
    @NotNull(message = "水表底数不能为空")
    @DecimalMin(value = "0.00", inclusive = true, message = "水表底数不能为负数")
    @TableField(value = "water_meter")
    private BigDecimal waterMeter;

    /**
     * 电表底数
     * 交接时的电表读数（单位：度），非负，保留2位小数，非空（费用结算依据）
     */
    @Schema(
            description = "电表底数（单位：度），保留2位小数",
            example = "350.00",
            nullable = false,
            minimum = "0.00",
            format = "decimal",
            multipleOf = 0.01
    )
    @NotNull(message = "电表底数不能为空")
    @DecimalMin(value = "0.00", inclusive = true, message = "电表底数不能为负数")
    @TableField(value = "electricity_meter")
    private BigDecimal electricityMeter;

    /**
     * 燃气表底数
     * 交接时的燃气表读数（单位：立方米），非负，保留2位小数，非空（费用结算依据）
     */
    @Schema(
            description = "燃气表底数（单位：立方米），保留2位小数",
            example = "80.00",
            nullable = false,
            minimum = "0.00",
            format = "decimal",
            multipleOf = 0.01
    )
    @NotNull(message = "燃气表底数不能为空")
    @DecimalMin(value = "0.00", inclusive = true, message = "燃气表底数不能为负数")
    @TableField(value = "gas_meter")
    private BigDecimal gasMeter;

    /**
     * 房屋损坏记录
     * 交接时发现的房屋/家具损坏情况（如墙面划痕、空调故障），可空（无损坏填“无”），长度≤500字符
     */
    @Schema(
            description = "房屋损坏记录（无损坏填“无”）",
            example = "无",
            nullable = true,
            maxLength = 500
    )
    @Size(max = 500, message = "房屋损坏记录长度不能超过500字符")
    @TableField(value = "damage_records")
    private String damageRecords;

    /**
     * 交接人
     * 房东或其授权代理人姓名，非空，长度≤50字符
     */
    @Schema(
            description = "交接人（房东或其授权代理人姓名）",
            example = "张三（房东）",
            nullable = false,
            maxLength = 50
    )
    @NotBlank(message = "交接人不能为空")
    @Size(max = 50, message = "交接人姓名长度不能超过50字符")
    @TableField(value = "handover_person")
    private String handoverPerson;

    /**
     * 接收人
     * 租户姓名（租赁业务的实际使用人），非空，长度≤50字符
     */
    @Schema(
            description = "接收人（租户姓名）",
            example = "李四（租户）",
            nullable = false,
            maxLength = 50
    )
    @NotBlank(message = "接收人不能为空")
    @Size(max = 50, message = "接收人姓名长度不能超过50字符")
    @TableField(value = "receiver")
    private String receiver;

    /**
     * 交接确认签字图片URL
     * 双方交接确认后的签字扫描件/照片URL，非空（责任确认依据），长度≤500字符
     */
    @Schema(
            description = "交接确认签字图片URL（支持HTTP/HTTPS/OSS）",
            example = "https://oss.example.com/handover/sign/5001.jpg",
            nullable = false,
            maxLength = 500,
            pattern = "^(https?://|oss://).*$"
    )
    @NotBlank(message = "交接确认签字图片URL不能为空")
    @Size(max = 500, message = "签字图片URL长度不能超过500字符")
    @Pattern(regexp = "^(https?://|oss://).*$", message = "签字图片URL格式错误（支持HTTP/HTTPS/OSS）")
    @TableField(value = "sign_image_url")
    private String signImageUrl;

    /**
     * 交接记录状态
     * 枚举值：DRAFT-草稿（未确认），CONFIRMED-已确认（双方签字生效），非空（管控记录有效性）
     */
    @Schema(
            description = "交接记录状态（DRAFT=草稿，CONFIRMED=已确认）",
            example = "CONFIRMED",
            nullable = false,
            allowableValues = {"DRAFT", "CONFIRMED"}
    )
    @NotBlank(message = "交接记录状态不能为空")
    @TableField(value = "status")
    private String status;

    /**
     * 记录创建时间
     * 交接记录录入系统的时间，数据库自动填充，无需手动传值，只读
     */
    @Schema(
            description = "记录创建时间（数据库自动填充）",
            example = "2025-12-01 14:30:00",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY,
            format = "date-time"
    )
    @TableField(value = "create_time", fill = FieldFill.INSERT) // 声明新增时自动填充
    private LocalDateTime createTime;

    /**
     * 维修结果备注
     * 同步自关联维修工单的处理结果（如“墙面划痕已修复”），可空，长度≤300字符
     */
    @Schema(
            description = "维修结果备注（同步自维修工单）",
            example = "无维修记录",
            nullable = true,
            maxLength = 300
    )
    @Size(max = 300, message = "维修结果备注长度不能超过300字符")
    @TableField(value = "maintenance_remark")
    private String maintenanceRemark;

    /**
     * 维修费用
     * 交接相关的维修总费用（单位：元），非负，保留2位小数，可空（无维修时为0或null）
     */
    @Schema(
            description = "维修费用（单位：元），保留2位小数，无维修时填0.00",
            example = "0.00",
            nullable = true,
            minimum = "0.00",
            format = "decimal",
            multipleOf = 0.01
    )
    @DecimalMin(value = "0.00", inclusive = true, message = "维修费用不能为负数")
    @TableField(value = "maintenance_cost")
    private BigDecimal maintenanceCost;

    /**
     * 维修费用承担方
     * 枚举值：LANDLORD-房东承担，TENANT-租户承担，SHARED-共同承担，可空（无维修时为null）
     */
    @Schema(
            description = "维修费用承担方（LANDLORD=房东，TENANT=租户，SHARED=共同承担）",
            example = "LANDLORD",
            nullable = true,
            allowableValues = {"LANDLORD", "TENANT", "SHARED"}
    )
    @TableField(value = "maintenance_bearer")
    private String maintenanceBearer;

    /**
     * 关联的最后一次维修工单ID
     * 关联维修工单表主键（同租户下），标识交接前的最后一次维修记录，可空（无维修时为null）
     */
    @Schema(
            description = "关联的最后一次维修工单ID（无维修时为null）",
            example = "null",
            nullable = true
    )
    @TableField(value = "last_maintenance_id")
    private Long lastMaintenanceId;
}
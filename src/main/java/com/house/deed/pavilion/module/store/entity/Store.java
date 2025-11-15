package com.house.deed.pavilion.module.store.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.FieldFill;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 门店信息表（租户级数据）
 * 存储租户下属门店的基础信息，包括编码、名称、所属区域、店长等关键信息，支持营业状态管理
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
@Schema(
        description = "门店信息实体，用于管理租户下属门店的基础数据，关联区域表(region)和经纪人表(agent)，支持按租户隔离和状态筛选",
        requiredMode = Schema.RequiredMode.REQUIRED
)
@Getter
@Setter
@TableName("store")
public class Store implements Serializable {

    @Serial
    @Schema(hidden = true)
    private static final long serialVersionUID = 1L;

    /**
     * 门店ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    @Schema(
            description = "门店唯一标识（自增主键）",
            example = "1001",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    private Long id;

    /**
     * 租户ID（归属租户）
     */
    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    @Schema(
            description = "租户ID（数据隔离字段），关联tenant表主键，由系统自动填充当前登录租户ID",
            example = "101",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    private Long tenantId;

    /**
     * 门店编码（租户内唯一）
     */
    @TableField("store_code")
    @Schema(
            description = "门店编码，租户内唯一标识，用于门店快速检索",
            example = "STORE001",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String storeCode;

    /**
     * 门店名称
     */
    @TableField("store_name")
    @Schema(
            description = "门店全称，如'XX中介朝阳路店'",
            example = "幸福家园中介朝阳路店",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String storeName;

    /**
     * 所属区域ID（关联region表，同租户）
     */
    @TableField("region_id")
    @Schema(
            description = "所属区域ID，必须关联当前租户下已存在的区域（关联region表id）",
            example = "3001",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Long regionId;

    /**
     * 详细地址
     */
    @TableField("address")
    @Schema(
            description = "门店详细地址，精确到门牌号",
            example = "北京市朝阳区朝阳路88号1层"
    )
    private String address;

    /**
     * 店长ID（关联agent表，同租户）
     */
    @TableField("manager_id")
    @Schema(
            description = "店长ID，关联当前租户下的经纪人（agent表id），可为null表示未分配店长",
            example = "1"
    )
    private Long managerId;

    /**
     * 门店电话
     */
    @TableField("phone")
    @Schema(
            description = "门店公开联系电话",
            example = "010-12345678"
    )
    private String phone;

    /**
     * 状态（true=营业，false=停业；对应数据库tinyint存储：1=营业，0=停业）
     */
    @TableField(value = "status")
    @Schema(
            description = "门店状态（true表示营业，false表示停业）",
            example = "true",
            allowableValues = {"true", "false"},
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private boolean status;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(
            description = "记录创建时间（系统自动填充）",
            example = "2025-11-07T09:30:00",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    @Schema(
            description = "记录更新时间（系统自动填充，数据变更时更新）",
            example = "2025-11-08T14:20:00",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    private LocalDateTime updateTime;
}
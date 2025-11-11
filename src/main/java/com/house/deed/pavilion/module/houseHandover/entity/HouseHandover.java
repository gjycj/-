package com.house.deed.pavilion.module.houseHandover.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 房屋交接记录表（租户级数据）
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
@Getter
@Setter
@TableName("house_handover")
public class HouseHandover implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 交接ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID（归属租户）
     */
    @TableField("tenant_id")
    private Long tenantId;

    /**
     * 合同ID（关联contract表，同租户，仅租赁）
     */
    @TableField("contract_id")
    private Long contractId;

    /**
     * 房源ID（关联house表，同租户）
     */
    @TableField("house_id")
    private Long houseId;

    /**
     * 交接类型（CHECK_IN-入住，CHECK_OUT-退租）
     */
    @TableField("handover_type")
    private String handoverType;

    /**
     * 交接时间
     */
    @TableField("handover_time")
    private LocalDateTime handoverTime;

    /**
     * 家具家电清单（如{"冰箱":"海尔","空调":2台}）
     */
    @TableField("appliances_list")
    private String appliancesList;

    /**
     * 水表底数（吨）
     */
    @TableField("water_meter")
    private BigDecimal waterMeter;

    /**
     * 电表底数（度）
     */
    @TableField("electricity_meter")
    private BigDecimal electricityMeter;

    /**
     * 燃气表底数（立方米）
     */
    @TableField("gas_meter")
    private BigDecimal gasMeter;

    /**
     * 房屋损坏记录（如墙面划痕）
     */
    @TableField("damage_records")
    private String damageRecords;

    /**
     * 交接人（房东或其代理人）
     */
    @TableField("handover_person")
    private String handoverPerson;

    /**
     * 接收人（租户）
     */
    @TableField("receiver")
    private String receiver;

    /**
     * 交接确认签字图片URL
     */
    @TableField("sign_image_url")
    private String signImageUrl;

    @TableField("create_time")
    private LocalDateTime createTime;
}

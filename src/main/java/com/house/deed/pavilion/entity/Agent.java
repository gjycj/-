package com.house.deed.pavilion.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 经纪人信息表（租户级数据）
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Getter
@Setter
@TableName("agent")
@ApiModel(value = "Agent对象", description = "经纪人信息表（租户级数据）")
public class Agent implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("经纪人ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("租户ID（归属租户）")
    @TableField("tenant_id")
    private Long tenantId;

    @ApiModelProperty("所属门店ID（关联store表，同租户）")
    @TableField("store_id")
    private Long storeId;

    @ApiModelProperty("工号（租户内唯一）")
    @TableField("agent_code")
    private String agentCode;

    @ApiModelProperty("姓名")
    @TableField("name")
    private String name;

    @ApiModelProperty("联系电话")
    @TableField("phone")
    private String phone;

    @ApiModelProperty("身份证号")
    @TableField("id_card")
    private String idCard;

    @ApiModelProperty("职位（经纪人/店长等）")
    @TableField("position")
    private String position;

    @ApiModelProperty("级别（JUNIOR-初级，SENIOR-高级，STAR-明星）")
    @TableField("level")
    private String level;

    @ApiModelProperty("入职时间")
    @TableField("entry_time")
    private LocalDate entryTime;

    @ApiModelProperty("状态（1-在职，0-离职）")
    @TableField("status")
    private Byte status;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @ApiModelProperty("创建人ID（经纪人，同租户）")
    @TableField("create_agent_id")
    private Long createAgentId;
}

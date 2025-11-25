package com.house.deed.pavilion.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Getter
@Setter
@TableName("flyway_schema_history")
@ApiModel(value = "FlywaySchemaHistory对象", description = "")
public class FlywaySchemaHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "installed_rank", type = IdType.AUTO)
    private Integer installedRank;

    @TableField("version")
    private String version;

    @TableField("description")
    private String description;

    @TableField("type")
    private String type;

    @TableField("script")
    private String script;

    @TableField("checksum")
    private Integer checksum;

    @TableField("installed_by")
    private String installedBy;

    @TableField("installed_on")
    private LocalDateTime installedOn;

    @TableField("execution_time")
    private Integer executionTime;

    @TableField("success")
    private Boolean success;
}

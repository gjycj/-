package com.house.deed.pavilion.module.operationLog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 系统操作日志表（租户级审计）
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
@Getter
@Setter
@TableName("operation_log")
public class OperationLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 日志ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID（归属租户，0为系统操作）
     */
    @TableField("tenant_id")
    private Long tenantId;

    /**
     * 操作模块（房源管理/客户管理等）
     */
    @TableField("module")
    private String module;

    /**
     * 操作类型（ADD-新增，UPDATE-修改等）
     */
    @TableField("operation_type")
    private String operationType;

    /**
     * 操作内容（如修改房源价格从100万到105万）
     */
    @TableField("operation_content")
    private String operationContent;

    /**
     * 操作人ID（经纪人/管理员，同租户）
     */
    @TableField("operator_id")
    private Long operatorId;

    /**
     * 操作人姓名
     */
    @TableField("operator_name")
    private String operatorName;

    /**
     * 操作IP地址
     */
    @TableField("ip_address")
    private String ipAddress;

    @TableField("create_time")
    private LocalDateTime createTime;
}

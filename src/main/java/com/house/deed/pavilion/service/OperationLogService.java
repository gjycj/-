package com.house.deed.pavilion.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.house.deed.pavilion.entity.OperationLog;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 系统操作日志表（租户级审计） 服务类
 * </p>
 * 核心业务：审计日志的录入、查询、批量操作（遵循日志不可篡改特性，限制更新/删除）
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
public interface OperationLogService extends IService<OperationLog> {

    // ==================== 基础CRUD（审计日志特性：限制更新/删除） ====================
    /**
     * 新增操作日志（严格校验实体类约束：枚举值、IP格式、长度等）
     * @param log 操作日志实体
     * @return 是否新增成功
     */
    boolean saveOperationLog(OperationLog log);

    /**
     * 日志更新（仅允许修改操作人姓名，禁止篡改核心审计字段）
     * @param log 操作日志实体
     * @return 是否更新成功
     */
    boolean updateOperationLogById(OperationLog log);

    /**
     * 删除操作日志（仅允许删除指定时间前的非系统级日志，系统级日志禁止删除）
     * @param id 日志ID
     * @param tenantId 租户ID（0表示系统级操作）
     * @return 是否删除成功
     */
    boolean removeOperationLogById(Long id, Long tenantId);

    /**
     * 根据ID查询操作日志（租户隔离：普通租户仅查自身，系统级日志需管理员权限）
     * @param id 日志ID
     * @param tenantId 租户ID（0表示查询系统级日志）
     * @return 操作日志实体
     */
    OperationLog getOperationLogById(Long id, Long tenantId);

    // ==================== 多条件查询（贴合实体类字段+审计场景） ====================
    /**
     * 分页查询操作日志（多条件+租户隔离）
     * @param page 分页参数
     * @param queryParams 查询条件（module/operationType/tenantId/operatorId/时间范围等）
     * @param tenantId 租户ID（0表示查询系统级日志）
     * @return 分页结果
     */
    IPage<OperationLog> pageQuery(Page<OperationLog> page, Map<String, Object> queryParams, Long tenantId);

    /**
     * 多条件查询日志列表（租户隔离）
     * @param queryParams 查询条件
     * @param tenantId 租户ID（0表示查询系统级日志）
     * @return 日志列表
     */
    List<OperationLog> listByConditions(Map<String, Object> queryParams, Long tenantId);

    /**
     * 按操作模块+类型查询日志（租户隔离）
     * @param module 操作模块
     * @param operationType 操作类型
     * @param tenantId 租户ID
     * @return 日志列表
     */
    List<OperationLog> listByModuleAndType(String module, String operationType, Long tenantId);

    /**
     * 按操作人ID查询日志（租户隔离）
     * @param operatorId 操作人ID
     * @param tenantId 租户ID
     * @return 日志列表
     */
    List<OperationLog> listByOperatorId(Long operatorId, Long tenantId);

    // ==================== 批量操作（审计日志特性：仅批量新增/删除/查询） ====================
    /**
     * 批量新增操作日志（同一租户，事务保证）
     * @param logList 日志列表
     * @return 是否批量新增成功
     */
    boolean batchSaveOperationLogs(List<OperationLog> logList);

    /**
     * 批量删除操作日志（仅允许删除指定时间前的非系统级日志）
     * @param ids 日志ID列表
     * @param tenantId 租户ID
     * @param beforeTime 仅删除该时间前的日志（防止误删最新日志）
     * @return 是否批量删除成功
     */
    boolean batchRemoveOperationLogs(List<Long> ids, Long tenantId, LocalDateTime beforeTime);

    /**
     * 校验日志ID列表是否属于当前租户（系统级日志需管理员权限）
     * @param tenantId 租户ID（0表示系统级）
     * @param logIds 日志ID列表
     */
    void validateLogIdsBelongToTenant(Long tenantId, List<Long> logIds);
}
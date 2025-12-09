package com.house.deed.pavilion.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.house.deed.pavilion.entity.DisputeHandleLog;
import java.util.List;

/**
 * <p>
 * 纠纷处理日志表（租户级数据） 服务类
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
public interface DisputeHandleLogService extends IService<DisputeHandleLog> {

    // ==================== 单条CRUD（增强租户校验） ====================
    /**
     * 新增纠纷处理日志（带租户校验）
     * @param log 纠纷处理日志实体
     * @return 是否新增成功
     */
    boolean saveLog(DisputeHandleLog log);

    /**
     * 更新纠纷处理日志（带租户校验）
     * @param log 纠纷处理日志实体
     * @return 是否更新成功
     */
    boolean updateLogById(DisputeHandleLog log);

    /**
     * 删除纠纷处理日志（带租户校验）
     * @param id 日志ID
     * @param tenantId 租户ID
     * @return 是否删除成功
     */
    boolean removeLogById(Long id, Long tenantId);

    /**
     * 按ID查询纠纷处理日志（带租户隔离）
     * @param id 日志ID
     * @param tenantId 租户ID
     * @return 纠纷处理日志实体
     */
    DisputeHandleLog getLogById(Long id, Long tenantId);

    // ==================== 多条件查询 ====================
    /**
     * 分页查询纠纷处理日志（多条件+租户隔离）
     * @param page 分页参数
     * @param query 查询条件（含租户ID）
     * @return 分页结果
     */
    IPage<DisputeHandleLog> pageQuery(Page<DisputeHandleLog> page, DisputeHandleLog query);

    /**
     * 多条件查询纠纷处理日志列表（租户隔离）
     * @param query 查询条件（含租户ID）
     * @return 日志列表
     */
    List<DisputeHandleLog> listByConditions(DisputeHandleLog query);

    /**
     * 按纠纷ID查询处理日志（租户隔离）
     * @param disputeId 纠纷ID
     * @param tenantId 租户ID
     * @return 日志列表（按处理时间倒序）
     */
    List<DisputeHandleLog> listByDisputeId(Long disputeId, Long tenantId);

    // ==================== 批量操作 ====================
    /**
     * 批量新增纠纷处理日志（同一租户）
     * @param logList 日志列表
     * @return 是否批量新增成功
     */
    boolean batchSaveLogs(List<DisputeHandleLog> logList);

    /**
     * 批量删除纠纷处理日志（租户隔离）
     * @param ids 日志ID列表
     * @param tenantId 租户ID
     * @return 是否批量删除成功
     */
    boolean batchRemoveLogs(List<Long> ids, Long tenantId);
}
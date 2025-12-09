package com.house.deed.pavilion.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.house.deed.pavilion.entity.HousePriceLog;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 房源价格变动记录表（租户级数据） 服务接口
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
public interface HousePriceLogService extends IService<HousePriceLog> {

    // ==================== 基础CRUD（增强租户隔离） ====================
    /**
     * 新增价格变动记录（强制租户绑定）
     * @param log 价格变动记录实体
     * @param tenantId 租户ID
     * @return 是否新增成功
     */
    boolean savePriceLog(HousePriceLog log, Long tenantId);

    /**
     * 更新价格变动记录（校验租户归属）
     * @param log 价格变动记录实体
     * @param tenantId 租户ID
     * @return 是否更新成功
     */
    boolean updatePriceLogById(HousePriceLog log, Long tenantId);

    /**
     * 删除价格变动记录（校验租户归属）
     * @param id 记录ID
     * @param tenantId 租户ID
     * @return 是否删除成功
     */
    boolean removePriceLogById(Long id, Long tenantId);

    /**
     * 按ID查询价格变动记录（租户隔离）
     * @param id 记录ID
     * @param tenantId 租户ID
     * @return 价格变动记录实体
     */
    HousePriceLog getPriceLogById(Long id, Long tenantId);

    // ==================== 多条件查询 ====================
    /**
     * 分页查询价格变动记录（多条件+租户隔离）
     * @param page 分页参数
     * @param queryParams 查询条件（房源ID/价格变动类型/时间范围等）
     * @param tenantId 租户ID
     * @return 分页结果
     */
    IPage<HousePriceLog> pageQuery(Page<HousePriceLog> page, Map<String, Object> queryParams, Long tenantId);

    /**
     * 多条件查询价格变动记录列表（租户隔离）
     * @param queryParams 查询条件
     * @param tenantId 租户ID
     * @return 价格变动记录列表
     */
    List<HousePriceLog> listByConditions(Map<String, Object> queryParams, Long tenantId);

    /**
     * 按房源ID查询价格变动记录（租户隔离）
     * @param houseId 房源ID
     * @param tenantId 租户ID
     * @return 价格变动记录列表（按变动时间倒序）
     */
    List<HousePriceLog> listByHouseId(Long houseId, Long tenantId);

    // ==================== 批量操作 ====================
    /**
     * 批量新增价格变动记录（同一租户）
     * @param logList 价格变动记录列表
     * @param tenantId 租户ID
     * @return 是否批量新增成功
     */
    boolean batchSavePriceLogs(List<HousePriceLog> logList, Long tenantId);

    /**
     * 批量删除价格变动记录（租户隔离）
     * @param ids 记录ID列表
     * @param tenantId 租户ID
     * @return 是否批量删除成功
     */
    boolean batchRemovePriceLogs(List<Long> ids, Long tenantId);
}
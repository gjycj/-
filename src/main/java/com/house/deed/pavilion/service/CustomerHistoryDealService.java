package com.house.deed.pavilion.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.house.deed.pavilion.entity.CustomerHistoryDeal;
import java.util.List;

/**
 * <p>
 * 客户历史成交记录表（租户级数据） 服务类
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
public interface CustomerHistoryDealService extends IService<CustomerHistoryDeal> {

    // ==================== 单条CRUD（增强租户校验） ====================
    /**
     * 新增客户历史成交记录（带租户校验）
     * @param historyDeal 成交记录实体
     * @return 是否新增成功
     */
    boolean saveHistoryDeal(CustomerHistoryDeal historyDeal);

    /**
     * 更新客户历史成交记录（带租户校验）
     * @param historyDeal 成交记录实体
     * @return 是否更新成功
     */
    boolean updateHistoryDealById(CustomerHistoryDeal historyDeal);

    /**
     * 删除客户历史成交记录（带租户校验）
     * @param id 记录ID
     * @param tenantId 租户ID
     * @return 是否删除成功
     */
    boolean removeHistoryDealById(Long id, Long tenantId);

    /**
     * 按ID查询客户历史成交记录（带租户隔离）
     * @param id 记录ID
     * @param tenantId 租户ID
     * @return 成交记录实体
     */
    CustomerHistoryDeal getHistoryDealById(Long id, Long tenantId);

    // ==================== 多条件查询 ====================
    /**
     * 分页查询客户历史成交记录（多条件+租户隔离）
     * @param page 分页参数
     * @param query 查询条件（含租户ID）
     * @return 分页结果
     */
    IPage<CustomerHistoryDeal> pageQuery(Page<CustomerHistoryDeal> page, CustomerHistoryDeal query);

    /**
     * 多条件查询客户历史成交记录列表（租户隔离）
     * @param query 查询条件（含租户ID）
     * @return 成交记录列表
     */
    List<CustomerHistoryDeal> listByConditions(CustomerHistoryDeal query);

    /**
     * 按客户ID查询历史成交记录（租户隔离）
     * @param customerId 客户ID
     * @param tenantId 租户ID
     * @return 成交记录列表（按成交时间倒序）
     */
    List<CustomerHistoryDeal> listByCustomerId(Long customerId, Long tenantId);

    /**
     * 按合同ID查询历史成交记录（租户隔离）
     * @param contractId 合同ID
     * @param tenantId 租户ID
     * @return 成交记录列表
     */
    List<CustomerHistoryDeal> listByContractId(Long contractId, Long tenantId);

    // ==================== 批量操作 ====================
    /**
     * 批量新增客户历史成交记录（同一租户）
     * @param historyDealList 成交记录列表
     * @return 是否批量新增成功
     */
    boolean batchSaveHistoryDeals(List<CustomerHistoryDeal> historyDealList);

    /**
     * 批量删除客户历史成交记录（租户隔离）
     * @param ids 记录ID列表
     * @param tenantId 租户ID
     * @return 是否批量删除成功
     */
    boolean batchRemoveHistoryDeals(List<Long> ids, Long tenantId);
}
package com.house.deed.pavilion.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.house.deed.pavilion.entity.CustomerFollowUp;
import java.util.List;

/**
 * <p>
 * 客户跟进记录表（租户级数据） 服务类
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
public interface CustomerFollowUpService extends IService<CustomerFollowUp> {

    // ==================== 单条CRUD（增强租户校验） ====================
    /**
     * 新增跟进记录（带租户校验）
     * @param followUp 跟进记录实体
     * @return 是否新增成功
     */
    boolean saveFollowUp(CustomerFollowUp followUp);

    /**
     * 更新跟进记录（带租户校验）
     * @param followUp 跟进记录实体
     * @return 是否更新成功
     */
    boolean updateFollowUpById(CustomerFollowUp followUp);

    /**
     * 删除跟进记录（带租户校验）
     * @param id 记录ID
     * @param tenantId 租户ID
     * @return 是否删除成功
     */
    boolean removeFollowUpById(Long id, Long tenantId);

    /**
     * 按ID查询跟进记录（带租户隔离）
     * @param id 记录ID
     * @param tenantId 租户ID
     * @return 跟进记录实体
     */
    CustomerFollowUp getFollowUpById(Long id, Long tenantId);

    // ==================== 多条件查询 ====================
    /**
     * 分页查询跟进记录（多条件+租户隔离）
     * @param page 分页参数
     * @param query 查询条件（含租户ID）
     * @return 分页结果
     */
    IPage<CustomerFollowUp> pageQuery(Page<CustomerFollowUp> page, CustomerFollowUp query);

    /**
     * 多条件查询跟进记录列表（租户隔离）
     * @param query 查询条件（含租户ID）
     * @return 跟进记录列表
     */
    List<CustomerFollowUp> listByConditions(CustomerFollowUp query);

    /**
     * 按客户ID查询跟进记录（租户隔离）
     * @param customerId 客户ID
     * @param tenantId 租户ID
     * @return 跟进记录列表（按跟进时间倒序）
     */
    List<CustomerFollowUp> listByCustomerId(Long customerId, Long tenantId);

    // ==================== 批量操作 ====================
    /**
     * 批量新增跟进记录（同一租户）
     * @param followUpList 跟进记录列表
     * @return 是否批量新增成功
     */
    boolean batchSaveFollowUps(List<CustomerFollowUp> followUpList);

    /**
     * 批量删除跟进记录（租户隔离）
     * @param ids 记录ID列表
     * @param tenantId 租户ID
     * @return 是否批量删除成功
     */
    boolean batchRemoveFollowUps(List<Long> ids, Long tenantId);
}
package com.house.deed.pavilion.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.house.deed.pavilion.entity.Contract;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 交易合同表（租户核心业务数据） 服务类
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
public interface ContractService extends IService<Contract> {

    // ==================== 基础CRUD（IService已提供，此处仅强调使用方式） ====================
    // 新增：save(Contract entity)
    // 删除：removeById(Long id)
    // 更新：updateById(Contract entity)
    // 单查：getById(Long id)


    // ==================== 多条件分页查询（带租户隔离） ====================
    /**
     * 分页查询合同记录（支持多条件+租户隔离）
     * @param page 分页参数（页码、每页条数）
     * @param queryParams 查询条件（键值对，支持：contractNo、status、startTime、endTime等）
     * @param tenantId 租户ID（强制隔离）
     * @return 分页结果（含数据列表和分页信息）
     */
    IPage<Contract> pageQuery(Page<Contract> page, Map<String, Object> queryParams, Long tenantId);


    // ==================== 多条件查询列表（带租户隔离） ====================
    /**
     * 多条件查询合同列表（租户隔离）
     * @param queryParams 查询条件（键值对，支持：contractNo、status等）
     * @param tenantId 租户ID（强制隔离）
     * @return 符合条件的合同列表
     */
    List<Contract> listByConditions(Map<String, Object> queryParams, Long tenantId);


    // ==================== 批量操作 ====================
    /**
     * 批量新增合同（带租户校验）
     * @param contracts 合同列表（需包含租户ID）
     * @return 批量新增是否成功
     */
    boolean batchSaveContracts(List<Contract> contracts);

    /**
     * 批量更新合同状态（带租户校验）
     * @param ids 合同ID列表
     * @param status 目标状态
     * @param tenantId 租户ID（强制隔离）
     * @return 批量更新是否成功
     */
    boolean batchUpdateStatus(List<Long> ids, String status, Long tenantId);

    /**
     * 批量删除合同（带租户校验）
     * @param ids 合同ID列表
     * @param tenantId 租户ID（强制隔离）
     * @return 批量删除是否成功
     */
    boolean batchRemoveContracts(List<Long> ids, Long tenantId);

}
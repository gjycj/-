package com.house.deed.pavilion.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.house.deed.pavilion.entity.CustomerBackup;
import java.util.List;

/**
 * <p>
 * 客户删除备份表（租户级存档） 服务类
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
public interface CustomerBackupService extends IService<CustomerBackup> {

    // ==================== 多条件分页查询（租户隔离） ====================
    /**
     * 分页查询客户备份记录（支持多条件+租户隔离）
     * @param page 分页参数（页码、每页条数）
     * @param query 查询条件实体（含租户ID和其他筛选条件）
     * @return 分页结果
     */
    IPage<CustomerBackup> pageQuery(Page<CustomerBackup> page, CustomerBackup query);

    // ==================== 多条件查询列表（租户隔离） ====================
    /**
     * 多条件查询客户备份列表（租户隔离）
     * @param query 查询条件实体（含租户ID）
     * @return 符合条件的备份列表
     */
    List<CustomerBackup> listByConditions(CustomerBackup query);

    // ==================== 批量操作 ====================
    /**
     * 批量创建客户备份记录（从客户主表删除时同步调用）
     * @param backupList 备份记录列表（需统一租户ID）
     * @return 是否创建成功
     */
    boolean batchCreate(List<CustomerBackup> backupList);

    /**
     * 根据原客户ID批量查询备份记录（租户隔离）
     * @param originalIds 原客户ID列表
     * @param tenantId 租户ID
     * @return 备份记录列表
     */
    List<CustomerBackup> getByOriginalIds(List<Long> originalIds, Long tenantId);

    /**
     * 恢复指定原ID的客户备份（恢复到客户主表并删除备份）
     * @param originalId 原客户ID
     * @param tenantId 租户ID
     * @return 是否恢复成功
     */
    boolean restore(Long originalId, Long tenantId);

    /**
     * 批量删除备份记录（物理删除，租户隔离）
     * @param ids 备份记录ID列表
     * @param tenantId 租户ID
     * @return 是否删除成功
     */
    boolean batchDelete(List<Long> ids, Long tenantId);
}
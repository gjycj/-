package com.house.deed.pavilion.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.house.deed.pavilion.entity.AgentBackup;

import java.util.List;

/**
 * <p>
 * 经纪人删除备份表（租户级存档） 服务类
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
public interface AgentBackupService extends IService<AgentBackup> {

    /**
     * 多条件分页查询备份记录（强制租户隔离）
     * @param page 分页参数
     * @param query 查询条件（需包含租户ID）
     * @return 分页结果
     */
    IPage<AgentBackup> pageQuery(Page<AgentBackup> page, AgentBackup query);

    /**
     * 批量创建备份记录（从经纪人主表删除时同步调用）
     * @param backupList 备份记录列表（需统一租户ID）
     * @return 是否创建成功
     */
    boolean batchCreate(List<AgentBackup> backupList);

    /**
     * 根据原经纪人ID批量查询备份记录（租户隔离）
     * @param originalIds 原经纪人ID列表
     * @param tenantId 租户ID
     * @return 备份记录列表
     */
    List<AgentBackup> getByOriginalIds(List<Long> originalIds, Long tenantId);

    /**
     * 恢复指定原ID的经纪人备份（恢复到agent表并删除备份）
     * @param originalId 原经纪人ID
     * @param tenantId 租户ID
     * @return 是否恢复成功
     */
    boolean restore(Long originalId, Long tenantId);

    /**
     * 批量删除备份记录（物理删除，支持按条件）
     * @param ids 备份记录ID列表
     * @param tenantId 租户ID（强制校验，防止跨租户删除）
     * @return 是否删除成功
     */
    boolean batchDelete(List<Long> ids, Long tenantId);
}
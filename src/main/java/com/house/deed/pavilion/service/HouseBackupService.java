package com.house.deed.pavilion.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.house.deed.pavilion.entity.HouseBackup;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 房源删除备份表（租户级存档） 服务类
 * </p>
 * <p>
 * 负责房源删除记录的备份管理，当房源数据被删除时，系统将自动创建备份记录以供后续审计和恢复。
 * 所有备份记录均与原始租户绑定，确保租户级数据隔离和完整性。
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
public interface HouseBackupService extends IService<HouseBackup> {

    // ==================== 基础CRUD操作 ====================

    /**
     * 保存房源删除备份记录
     *
     * @param entity 房源备份实体对象，包含原始房源信息和删除相关信息
     * @return boolean 保存成功返回true，失败返回false
     * @throws IllegalArgumentException 当参数校验失败时抛出
     *
     * 业务场景：
     * 1. 房源删除操作时自动调用
     * 2. 手动创建备份记录用于特殊审计场景
     *
     * 约束条件：
     * 1. 备份记录必须包含原始房源ID
     * 2. 必须记录删除操作人和操作时间
     */
    boolean saveBackup(HouseBackup entity);

    /**
     * 根据备份ID查询备份记录（租户隔离）
     *
     * @param backupId 备份记录ID
     * @param tenantId 租户ID，用于数据隔离
     * @return HouseBackup 房源备份实体对象，不存在时返回null
     * @throws IllegalArgumentException 当备份ID或租户ID为空时抛出
     *
     * 说明：此方法强制租户数据隔离，确保租户只能访问自己的备份记录
     */
    HouseBackup getById(Long backupId, Long tenantId);

    /**
     * 根据原始房源ID查询备份记录
     *
     * @param originalId 原始房源ID（被删除的房源ID）
     * @param tenantId 租户ID，用于数据隔离
     * @return HouseBackup 房源备份实体对象，不存在时返回null
     * @throws IllegalArgumentException 当原始房源ID或租户ID为空时抛出
     *
     * 使用场景：
     * 1. 查看特定房源的历史删除记录
     * 2. 恢复删除前的数据参考
     */
    HouseBackup getByOriginalId(Long originalId, Long tenantId);

    /**
     * 删除备份记录
     *
     * @param backupId 备份记录ID
     * @param tenantId 租户ID，用于权限校验
     * @return boolean 删除成功返回true，失败返回false
     * @throws IllegalArgumentException 当备份ID或租户ID为空时抛出
     * @throws IllegalStateException 当记录不存在或权限不足时抛出
     *
     * 注意：备份记录通常用于审计目的，不建议频繁删除
     */
    boolean removeBackup(Long backupId, Long tenantId);

    // ==================== 多条件查询操作 ====================

    /**
     * 多条件分页查询备份记录
     *
     * @param page 分页参数对象，包含页码、每页大小等信息
     * @param query 查询条件实体对象
     * @return IPage<HouseBackup> 分页结果对象，包含数据列表和分页信息
     * @throws IllegalArgumentException 当分页参数或查询条件无效时抛出
     *
     * 支持查询条件：
     * 1. 原始房源ID
     * 2. 删除操作人
     * 3. 删除时间范围
     * 4. 备份原因
     * 5. 租户ID（自动包含）
     */
    IPage<HouseBackup> pageQuery(Page<HouseBackup> page, HouseBackup query);

    /**
     * 多条件列表查询备份记录
     *
     * @param queryParams 查询条件Map，支持灵活的条件组合
     * @param tenantId 租户ID，用于数据隔离
     * @return List<HouseBackup> 符合条件的备份记录列表
     * @throws IllegalArgumentException 当租户ID为空时抛出
     *
     * 常用查询参数：
     * - originalHouseId: 原始房源ID
     * - operatorId: 操作人ID
     * - deleteReason: 删除原因（模糊匹配）
     * - startTime/endTime: 删除时间范围
     */
    List<HouseBackup> listByConditions(Map<String, Object> queryParams, Long tenantId);

    // ==================== 批量操作接口 ====================

    /**
     * 批量创建备份记录
     *
     * @param backupList 备份记录列表
     * @return boolean 批量创建成功返回true，失败返回false（事务回滚）
     * @throws IllegalArgumentException 当参数为空或记录格式无效时抛出
     *
     * 使用场景：
     * 1. 批量删除房源时的批量备份
     * 2. 数据迁移时的批量备份
     * 3. 系统定期归档
     *
     * 注意事项：
     * 1. 批量操作使用事务保证一致性
     * 2. 批量记录需属于同一租户
     */
    boolean batchCreate(List<HouseBackup> backupList);

    /**
     * 批量删除备份记录
     *
     * @param backupIds 备份记录ID列表
     * @param tenantId 租户ID，用于权限校验
     * @return boolean 批量删除成功返回true，失败返回false（事务回滚）
     * @throws IllegalArgumentException 当参数为空或存在跨租户记录时抛出
     *
     * 安全机制：
     * 1. 强制租户ID校验，防止跨租户删除
     * 2. 批量删除前验证所有记录属于当前租户
     */
    boolean batchRemove(List<Long> backupIds, Long tenantId);

    /**
     * 批量根据原始房源ID查询备份记录
     *
     * @param originalIds 原始房源ID列表
     * @param tenantId 租户ID，用于数据隔离
     * @return List<HouseBackup> 备份记录列表，按原始ID顺序返回
     * @throws IllegalArgumentException 当原始房源ID列表或租户ID为空时抛出
     *
     * 使用场景：
     * 1. 批量恢复多个房源时查询历史备份
     * 2. 批量审计操作时查询相关备份记录
     * 3. 数据一致性校验
     */
    List<HouseBackup> batchGetByOriginalIds(List<Long> originalIds, Long tenantId);
}
package com.house.deed.pavilion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.entity.HouseBackup;
import com.house.deed.pavilion.mapper.HouseBackupMapper;
import com.house.deed.pavilion.service.HouseBackupService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * 房源删除备份表（租户级存档） 服务实现类
 * </p>
 * <p>
 * 负责房源删除记录的备份管理和查询功能实现。当房源数据被删除时，系统自动将原始数据
 * 备份至此表中，以便后续审计追踪、数据恢复等操作。
 *
 * 核心特点：
 * 1. 强制租户数据隔离：所有操作均需验证租户ID，确保跨租户数据不可见
 * 2. 数据完整性保障：备份记录包含原始房源的所有关键信息和删除相关信息
 * 3. 审计追踪支持：记录删除操作人、删除时间、删除原因等审计信息
 * 4. 与房源主表同步：遵循统一的备份恢复逻辑规范
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Service
public class HouseBackupServiceImpl extends ServiceImpl<HouseBackupMapper, HouseBackup> implements HouseBackupService {

    @Resource
    private HouseBackupMapper houseBackupMapper;

    // ==================== 基础CRUD方法 ====================

    /**
     * 新增房源删除备份记录
     *
     * @param entity 备份实体对象，包含原始房源信息和删除相关数据
     * @return boolean 保存成功返回true，失败返回false
     * @throws IllegalArgumentException 当必要参数为空时抛出
     *
     * 执行流程：
     * 1. 参数校验：租户ID、原始房源ID、删除操作人不能为空
     * 2. 事务保障：使用@Transactional确保数据一致性
     * 3. 调用MyBatis-Plus保存方法持久化备份记录
     *
     * 使用场景：
     * 1. 房源主表数据删除时的自动备份
     * 2. 手动创建备份记录用于特殊审计需求
     *
     * 注意事项：
     * 1. 备份记录一旦创建，通常不建议修改
     * 2. 删除操作人应为系统当前登录用户或系统标识
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveBackup(HouseBackup entity) {
        // 1. 核心参数校验
        Assert.notNull(entity.getTenantId(), "租户ID不能为空");
        Assert.notNull(entity.getOriginalId(), "原始房源ID不能为空");
        Assert.hasText(entity.getDeleteOperator(), "删除操作人不能为空");

        // 2. 执行保存操作
        return save(entity);
    }

    /**
     * 根据备份ID查询备份记录（租户隔离）
     *
     * @param backupId 备份记录的唯一标识
     * @param tenantId 租户ID，用于数据隔离验证
     * @return HouseBackup 备份实体对象，不存在时返回null
     * @throws IllegalArgumentException 当备份ID或租户ID为空时抛出
     *
     * 说明：
     * 1. 强制添加租户ID查询条件，确保租户数据隔离
     * 2. 使用QueryWrapper构建精确查询条件
     * 3. 只能查询到当前租户下的备份记录
     */
    @Override
    public HouseBackup getById(Long backupId, Long tenantId) {
        // 参数校验
        Assert.notNull(backupId, "备份ID不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");

        // 构建查询条件（备份ID + 租户ID双重验证）
        QueryWrapper<HouseBackup> wrapper = new QueryWrapper<>();
        wrapper.eq("id", backupId)
                .eq("tenant_id", tenantId);
        return getOne(wrapper);
    }

    /**
     * 根据原始房源ID查询备份记录（租户隔离）
     *
     * @param originalId 原始房源的唯一标识（被删除的房源ID）
     * @param tenantId 租户ID，用于数据隔离验证
     * @return HouseBackup 备份实体对象，不存在时返回null
     * @throws IllegalArgumentException 当原始房源ID或租户ID为空时抛出
     *
     * 使用场景：
     * 1. 查看特定房源的历史删除记录
     * 2. 数据恢复前获取原始数据参考
     *
     * 说明：
     * 1. 一个原始房源ID可能对应多个备份记录（多次删除）
     * 2. 使用limit 1获取最新或最相关的备份记录
     * 3. 按业务需求，也可返回列表供用户选择
     */
    @Override
    public HouseBackup getByOriginalId(Long originalId, Long tenantId) {
        // 参数校验
        Assert.notNull(originalId, "原始房源ID不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");

        // 构建查询条件（原始ID + 租户ID）
        QueryWrapper<HouseBackup> wrapper = new QueryWrapper<>();
        wrapper.eq("original_id", originalId)
                .eq("tenant_id", tenantId)
                .last("limit 1"); // 限制返回一条记录
        return getOne(wrapper);
    }

    /**
     * 物理删除备份记录
     *
     * @param backupId 备份记录的唯一标识
     * @param tenantId 租户ID，用于权限验证
     * @return boolean 删除成功返回true，失败返回false
     * @throws IllegalArgumentException 当备份ID或租户ID为空时抛出
     * @throws IllegalStateException 当备份记录不存在或不属于当前租户时抛出
     *
     * 执行流程：
     * 1. 参数非空校验
     * 2. 记录存在性及租户归属校验
     * 3. 执行物理删除操作
     *
     * 注意事项：
     * 1. 备份记录通常用于审计目的，建议谨慎删除
     * 2. 删除前应确保已满足相关审计要求
     * 3. 数据一旦删除不可恢复，请确认操作
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeBackup(Long backupId, Long tenantId) {
        // 1. 参数校验
        Assert.notNull(backupId, "备份ID不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");

        // 2. 校验记录存在且属于当前租户
        HouseBackup backup = getById(backupId, tenantId);
        Assert.notNull(backup, "备份记录不存在或不属于当前租户");

        // 3. 执行删除操作
        return removeById(backupId);
    }

    // ==================== 多条件查询方法 ====================

    /**
     * 多条件分页查询备份记录
     *
     * @param page 分页参数对象，包含页码、每页大小等信息
     * @param query 查询条件实体对象，支持多个字段组合查询
     * @return IPage<HouseBackup> 分页结果对象，包含数据列表和分页信息
     * @throws IllegalArgumentException 当租户ID为空时抛出
     *
     * 说明：
     * 1. 强制要求租户ID，确保租户数据隔离
     * 2. 使用实体对象封装查询条件，避免Map的松散类型
     * 3. 默认按删除时间倒序排列（最新删除记录在前）
     */
    @Override
    public IPage<HouseBackup> pageQuery(Page<HouseBackup> page, HouseBackup query) {
        // 租户ID必填校验
        Assert.notNull(query.getTenantId(), "租户ID不能为空");

        // 构建查询条件
        QueryWrapper<HouseBackup> wrapper = buildQueryWrapper(query);
        return houseBackupMapper.selectPage(page, wrapper);
    }

    /**
     * 多条件列表查询备份记录
     *
     * @param queryParams 查询参数Map，支持灵活的查询条件组合
     * @param tenantId 租户ID，用于数据隔离
     * @return List<HouseBackup> 符合条件的备份记录列表
     * @throws IllegalArgumentException 当租户ID为空时抛出
     *
     * 支持的查询参数：
     * - originalId: 原始房源ID（精确匹配）
     * - houseNo: 房源编号（模糊匹配）
     * - startDeleteTime/endDeleteTime: 删除时间范围（时间区间）
     * - status: 房源状态（精确匹配，备份时的原状态）
     *
     * 默认排序：
     * 按删除时间倒序排列，最新删除的记录在前
     */
    @Override
    public List<HouseBackup> listByConditions(Map<String, Object> queryParams, Long tenantId) {
        // 租户ID必填校验
        Assert.notNull(tenantId, "租户ID不能为空");

        QueryWrapper<HouseBackup> wrapper = new QueryWrapper<>();
        wrapper.eq("tenant_id", tenantId); // 强制租户隔离

        // 动态添加查询条件
        if (!ObjectUtils.isEmpty(queryParams)) {
            // 原始房源ID查询（精确匹配）
            if (queryParams.containsKey("originalId") && queryParams.get("originalId") != null) {
                wrapper.eq("original_id", queryParams.get("originalId"));
            }
            // 房源编号查询（模糊匹配，支持部分匹配）
            if (queryParams.containsKey("houseNo") && StringUtils.hasText(queryParams.get("houseNo").toString())) {
                wrapper.like("house_no", queryParams.get("houseNo"));
            }
            // 删除时间范围查询（大于等于开始时间）
            if (queryParams.containsKey("startDeleteTime") && queryParams.get("startDeleteTime") != null) {
                wrapper.ge("delete_time", queryParams.get("startDeleteTime"));
            }
            // 删除时间范围查询（小于等于结束时间）
            if (queryParams.containsKey("endDeleteTime") && queryParams.get("endDeleteTime") != null) {
                wrapper.le("delete_time", queryParams.get("endDeleteTime"));
            }
            // 房源状态查询（精确匹配，备份时的原始状态）
            if (queryParams.containsKey("status") && queryParams.get("status") != null) {
                wrapper.eq("status", queryParams.get("status"));
            }
        }

        // 默认排序规则：按删除时间倒序（最新删除的在前）
        wrapper.orderByDesc("delete_time");
        return list(wrapper);
    }

    /**
     * 构建基于实体对象的查询条件封装器
     *
     * @param query 查询条件实体对象
     * @return QueryWrapper<HouseBackup> 查询条件封装器
     *
     * 支持的查询条件：
     * 1. 租户ID（必填，强制隔离）
     * 2. 原始房源ID（精确匹配）
     * 3. 房源编号（模糊匹配）
     * 4. 删除操作人（模糊匹配）
     * 5. 删除时间（大于等于指定时间）
     * 6. 房源类型（精确匹配）
     *
     * 说明：此方法使用实体对象封装查询条件，类型安全且易于扩展
     */
    private QueryWrapper<HouseBackup> buildQueryWrapper(HouseBackup query) {
        QueryWrapper<HouseBackup> wrapper = new QueryWrapper<>();
        wrapper.eq("tenant_id", query.getTenantId()); // 强制租户隔离

        // 原始房源ID精确查询
        if (query.getOriginalId() != null) {
            wrapper.eq("original_id", query.getOriginalId());
        }
        // 房源编号模糊查询（支持部分匹配）
        if (StringUtils.hasText(query.getHouseNo())) {
            wrapper.like("house_no", query.getHouseNo());
        }
        // 删除操作人模糊查询（支持姓名模糊查询）
        if (StringUtils.hasText(query.getDeleteOperator())) {
            wrapper.like("delete_operator", query.getDeleteOperator());
        }
        // 删除时间范围查询（大于等于指定时间）
        if (query.getDeleteTime() != null) {
            wrapper.ge("delete_time", query.getDeleteTime());
        }
        // 房源类型精确查询
        if (StringUtils.hasText(query.getHouseType())) {
            wrapper.eq("house_type", query.getHouseType());
        }

        return wrapper;
    }

    // ==================== 批量操作方法 ====================

    /**
     * 批量创建备份记录
     *
     * @param backupList 备份记录列表
     * @return boolean 批量创建成功返回true，失败返回false（事务回滚）
     * @throws IllegalArgumentException 当列表为空或租户ID不一致时抛出
     *
     * 执行流程：
     * 1. 列表非空校验
     * 2. 租户一致性校验（批量记录必须属于同一租户）
     * 3. 执行批量保存操作（事务保障）
     *
     * 使用场景：
     * 1. 批量删除房源时的批量备份
     * 2. 数据迁移或系统维护时的批量备份
     * 3. 定期数据归档
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchCreate(List<HouseBackup> backupList) {
        // 1. 列表非空校验
        Assert.notEmpty(backupList, "备份列表不能为空");

        // 2. 租户一致性校验
        Long tenantId = backupList.get(0).getTenantId();
        Assert.notNull(tenantId, "租户ID不能为空");
        boolean hasInvalidTenant = backupList.stream()
                .anyMatch(backup -> !Objects.equals(backup.getTenantId(), tenantId));
        Assert.isTrue(!hasInvalidTenant, "批量备份必须属于同一租户");

        // 3. 执行批量保存
        return saveBatch(backupList);
    }

    /**
     * 批量删除备份记录
     *
     * @param backupIds 备份记录ID列表
     * @param tenantId 租户ID，用于权限验证
     * @return boolean 批量删除成功返回true，失败返回false（事务回滚）
     * @throws IllegalArgumentException 当参数为空或存在跨租户记录时抛出
     *
     * 执行流程：
     * 1. 参数非空校验
     * 2. 跨租户记录校验（防止越权删除）
     * 3. 执行批量删除操作（事务保障）
     *
     * 安全机制：
     * 1. 强制租户ID校验，确保只能删除自己租户的数据
     * 2. 批量操作前验证所有记录归属，防止部分成功部分失败
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchRemove(List<Long> backupIds, Long tenantId) {
        // 1. 参数非空校验
        Assert.notEmpty(backupIds, "备份ID列表不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");

        // 2. 跨租户记录校验
        QueryWrapper<HouseBackup> wrapper = new QueryWrapper<>();
        wrapper.in("id", backupIds)
                .ne("tenant_id", tenantId);
        long invalidCount = count(wrapper);
        Assert.isTrue(invalidCount == 0, "存在不属于当前租户的备份记录，无法删除");

        // 3. 执行批量删除
        return removeByIds(backupIds);
    }

    /**
     * 批量查询原始房源ID对应的备份记录
     *
     * @param originalIds 原始房源ID列表
     * @param tenantId 租户ID，用于数据隔离
     * @return List<HouseBackup> 备份记录列表
     * @throws IllegalArgumentException 当参数为空时抛出
     *
     * 使用场景：
     * 1. 批量恢复多个房源时查询历史备份
     * 2. 批量审计操作时查询相关备份记录
     * 3. 数据一致性校验和比对
     *
     * 说明：
     * 1. 返回列表包含所有匹配的备份记录，按查询条件自然排序
     * 2. 一个原始房源ID可能对应多个备份记录
     */
    @Override
    public List<HouseBackup> batchGetByOriginalIds(List<Long> originalIds, Long tenantId) {
        // 参数校验
        Assert.notEmpty(originalIds, "原始房源ID列表不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");

        // 构建批量查询条件
        QueryWrapper<HouseBackup> wrapper = new QueryWrapper<>();
        wrapper.eq("tenant_id", tenantId)
                .in("original_id", originalIds);
        return list(wrapper);
    }
}
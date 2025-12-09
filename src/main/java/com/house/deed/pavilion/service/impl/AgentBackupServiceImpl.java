package com.house.deed.pavilion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.entity.Agent;
import com.house.deed.pavilion.entity.AgentBackup;
import com.house.deed.pavilion.mapper.AgentBackupMapper;
import com.house.deed.pavilion.service.AgentBackupService;
import com.house.deed.pavilion.service.AgentService;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

/**
 * <p>
 * 经纪人删除备份表（租户级存档） 服务实现类
 * </p>
 *
 * <p>
 * 本服务类负责经纪人数据的备份管理，包括：
 * - 经纪人删除时的数据备份存储
 * - 备份记录的分页查询与条件检索
 * - 经纪人数据的恢复操作
 * - 备份记录的批量删除管理
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Service
public class AgentBackupServiceImpl extends ServiceImpl<AgentBackupMapper, AgentBackup> implements AgentBackupService {

    @Resource
    private AgentService agentService;

    /**
     * 多条件分页查询备份记录（强制租户隔离）
     *
     * <p>
     * 支持以下查询条件：
     * - 租户ID（必填，确保数据隔离）
     * - 原经纪人姓名模糊匹配
     * - 删除时间范围筛选
     * - 原经纪人ID精确查询
     * - 门店ID筛选
     * - 经纪人等级筛选
     * </p>
     *
     * @param page 分页参数对象，包含页码、页大小等信息
     * @param query 查询条件对象，包含租户ID、姓名、删除时间等筛选条件
     * @return IPage<AgentBackup> 分页结果对象，包含当前页数据和分页信息
     * @throws IllegalArgumentException 当租户ID为空时抛出异常
     */
    @Override
    public IPage<AgentBackup> pageQuery(Page<AgentBackup> page, AgentBackup query) {
        // 参数校验：租户ID为必填项，确保多租户数据隔离
        Assert.notNull(query.getTenantId(), "租户ID不能为空");

        QueryWrapper<AgentBackup> wrapper = new QueryWrapper<>();
        // 基础租户隔离条件：强制限制只能访问当前租户的数据
        wrapper.eq("tenant_id", query.getTenantId());

        // 按原经纪人姓名模糊查询：支持中文姓名、英文名的模糊匹配
        if (StringUtils.hasText(query.getName())) {
            wrapper.like("name", query.getName());
        }

        // 按删除时间范围查询：用于筛选特定时间段内删除的经纪人记录
        if (query.getDeleteTime() != null) {
            wrapper.ge("delete_time", query.getDeleteTime());
        }

        // 按原经纪人ID精确查询：用于快速定位特定经纪人的备份记录
        if (query.getOriginalId() != null) {
            wrapper.eq("original_id", query.getOriginalId());
        }

        // 按门店ID查询：支持按门店维度筛选经纪人备份数据
        if (query.getStoreId() != null) {
            wrapper.eq("store_id", query.getStoreId());
        }

        // 按经纪人等级查询：支持按经纪人等级（如初级、中级、高级）筛选
        if (StringUtils.hasText(query.getLevel())) {
            wrapper.eq("level", query.getLevel());
        }

        // 执行分页查询并返回结果
        return baseMapper.selectPage(page, wrapper);
    }

    /**
     * 批量创建备份记录
     *
     * <p>
     * 从经纪人主表删除数据时同步调用此方法，确保数据删除前完成备份。
     * 适用于批量删除经纪人场景，保证数据可恢复性。
     * </p>
     *
     * @param backupList 备份记录列表，包含要备份的经纪人数据
     * @return boolean 批量保存结果，true表示全部保存成功，false表示保存失败
     * @throws IllegalArgumentException 当备份记录列表为空、租户ID为空或租户ID不一致时抛出异常
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchCreate(List<AgentBackup> backupList) {
        // 空列表校验：如果备份列表为空，直接返回成功，避免不必要的数据库操作
        if (backupList.isEmpty()) {
            return true;
        }

        // 租户一致性校验：确保批量备份的所有记录属于同一租户，防止数据越权
        Long tenantId = backupList.get(0).getTenantId();
        Assert.notNull(tenantId, "备份记录租户ID不能为空");
        boolean hasInvalidTenant = backupList.stream()
                .anyMatch(backup -> !Objects.equals(backup.getTenantId(), tenantId));
        Assert.isTrue(!hasInvalidTenant, "批量备份记录必须属于同一租户");

        // 执行批量保存操作，使用数据库的批量插入功能提升性能
        return saveBatch(backupList);
    }

    /**
     * 根据原经纪人ID批量查询备份记录（租户隔离）
     *
     * <p>
     * 主要用于数据恢复前的验证、批量操作前的数据检查等场景。
     * 严格限制在当前租户范围内查询，确保数据安全。
     * </p>
     *
     * @param originalIds 原经纪人ID列表，支持批量查询多个经纪人的备份记录
     * @param tenantId 租户ID，用于数据隔离和权限控制
     * @return List<AgentBackup> 符合条件的备份记录列表
     * @throws IllegalArgumentException 当租户ID为空或原经纪人ID列表为空时抛出异常
     */
    @Override
    public List<AgentBackup> getByOriginalIds(List<Long> originalIds, Long tenantId) {
        // 参数校验：确保租户ID和经纪人ID列表的有效性
        Assert.notNull(tenantId, "租户ID不能为空");
        Assert.notEmpty(originalIds, "原经纪人ID列表不能为空");

        // 构建查询条件：租户隔离 + 原经纪人ID批量匹配
        QueryWrapper<AgentBackup> wrapper = new QueryWrapper<>();
        wrapper.eq("tenant_id", tenantId)
                .in("original_id", originalIds);

        return baseMapper.selectList(wrapper);
    }

    /**
     * 恢复指定原ID的经纪人备份
     *
     * <p>
     * 将备份表中的数据恢复到经纪人主表，并删除对应的备份记录。
     * 这是一个事务性操作，确保数据恢复和备份删除的原子性。
     * 主要用于误删除数据的恢复、数据迁移等场景。
     * </p>
     *
     * @param originalId 原经纪人ID，用于定位要恢复的备份记录
     * @param tenantId 租户ID，确保只能恢复本租户的数据
     * @return boolean 恢复操作结果，true表示恢复成功，false表示恢复失败
     * @throws IllegalArgumentException 当原经纪人ID或租户ID为空时抛出异常
     * @throws IllegalStateException 当未找到对应的备份记录时抛出异常
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean restore(Long originalId, Long tenantId) {
        // 参数校验：确保必要的查询参数有效
        Assert.notNull(originalId, "原经纪人ID不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");

        // 查询备份记录：使用租户隔离条件，确保数据安全
        AgentBackup backup = getOne(new QueryWrapper<AgentBackup>()
                .eq("tenant_id", tenantId)
                .eq("original_id", originalId)
                .last("limit 1"));
        Assert.notNull(backup, "未找到对应的经纪人备份记录");

        // 数据转换：将备份记录转换为经纪人实体，准备恢复数据
        Agent agent = new Agent();
        BeanUtils.copyProperties(backup, agent);
        agent.setId(null); // 重置主键ID，避免与现有数据冲突，由数据库重新生成
        agent.setStatus(backup.getStatus()); // 保持经纪人状态不变

        // 事务操作：先保存到主表，成功后删除备份记录
        boolean saveSuccess = agentService.save(agent);
        if (saveSuccess) {
            return removeById(backup.getId());
        }
        return false;
    }

    /**
     * 批量删除备份记录（物理删除）
     *
     * <p>
     * 永久删除指定的备份记录，适用于数据清理、存储空间释放等场景。
     * 操作不可逆，请谨慎使用。严格限制在当前租户范围内删除。
     * </p>
     *
     * @param ids 要删除的备份记录ID列表
     * @param tenantId 租户ID，确保只能删除本租户的数据
     * @return boolean 删除操作结果，true表示删除成功，false表示删除失败
     * @throws IllegalArgumentException 当租户ID为空或备份记录ID列表为空时抛出异常
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchDelete(List<Long> ids, Long tenantId) {
        // 参数校验：确保删除操作的安全性
        Assert.notNull(tenantId, "租户ID不能为空");
        Assert.notEmpty(ids, "备份记录ID列表不能为空");

        // 构建删除条件：租户隔离 + 指定ID列表
        QueryWrapper<AgentBackup> wrapper = new QueryWrapper<>();
        wrapper.eq("tenant_id", tenantId)
                .in("id", ids);

        // 执行物理删除操作
        return remove(wrapper);
    }
}
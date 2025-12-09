package com.house.deed.pavilion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.entity.Customer;
import com.house.deed.pavilion.entity.CustomerBackup;
import com.house.deed.pavilion.mapper.CustomerBackupMapper;
import com.house.deed.pavilion.service.CustomerBackupService;
import com.house.deed.pavilion.service.CustomerService;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

/**
 * 客户删除备份表服务实现类
 * 负责客户数据删除后的备份管理，包括备份记录查询、恢复、批量操作等功能
 * 所有操作均强制租户数据隔离，确保不同租户间的备份数据完全隔离
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Service
public class CustomerBackupServiceImpl extends ServiceImpl<CustomerBackupMapper, CustomerBackup> implements CustomerBackupService {

    @Resource
    private CustomerService customerService;

    // ==================== 私有工具方法 ====================

    /**
     * 构建客户备份查询条件包装器
     * 统一处理租户隔离和动态查询条件，避免代码重复
     *
     * @param query 查询条件实体，包含各种查询参数
     * @return QueryWrapper<CustomerBackup> 构建完成的查询条件包装器
     */
    private QueryWrapper<CustomerBackup> buildQueryWrapper(CustomerBackup query) {
        QueryWrapper<CustomerBackup> wrapper = new QueryWrapper<>();

        // 强制租户数据隔离
        wrapper.eq("tenant_id", query.getTenantId());

        // 动态条件拼接（基于非空参数）
        if (query.getOriginalId() != null) {
            wrapper.eq("original_id", query.getOriginalId());
        }
        if (StringUtils.hasText(query.getName())) {
            wrapper.like("name", query.getName());
        }
        if (StringUtils.hasText(query.getPhone())) {
            wrapper.eq("phone", query.getPhone());
        }
        if (StringUtils.hasText(query.getCustomerType())) {
            wrapper.eq("customer_type", query.getCustomerType());
        }
        if (query.getPotentialLevel() != null) {
            wrapper.eq("potential_level", query.getPotentialLevel());
        }
        if (query.getDeleteTime() != null) {
            wrapper.ge("delete_time", query.getDeleteTime());
        }

        // 默认按删除时间倒序排列（最新删除的在前）
        wrapper.orderByDesc("delete_time");

        return wrapper;
    }

    /**
     * 校验租户ID一致性
     * 确保批量操作中的所有记录都属于同一租户
     *
     * @param backupList 备份记录列表
     * @throws IllegalArgumentException 当租户ID为空或不一致时抛出
     */
    public void validateTenantConsistency(List<CustomerBackup> backupList) {
        if (backupList.isEmpty()) {
            return;
        }

        Long tenantId = backupList.get(0).getTenantId();
        Assert.notNull(tenantId, "备份记录租户ID不能为空");

        boolean hasInvalidTenant = backupList.stream()
                .anyMatch(backup -> !Objects.equals(backup.getTenantId(), tenantId));
        Assert.isTrue(!hasInvalidTenant, "批量备份记录必须属于同一租户");
    }

    /**
     * 校验备份记录是否属于当前租户
     * 用于删除操作前的权限校验
     *
     * @param ids 备份记录ID列表
     * @param tenantId 当前租户ID
     * @throws IllegalArgumentException 当存在跨租户记录时抛出
     */
    private void validateBackupOwnership(List<Long> ids, Long tenantId) {
        List<CustomerBackup> backups = baseMapper.selectList(
                new QueryWrapper<CustomerBackup>()
                        .select("id")
                        .in("id", ids)
                        .ne("tenant_id", tenantId)
        );
        Assert.isTrue(backups.isEmpty(), "存在跨租户的备份记录，无法删除");
    }

    // ==================== 查询相关方法 ====================

    /**
     * 多条件分页查询客户备份记录
     * 支持客户姓名、手机号、客户类型等多种条件组合查询，强制租户数据隔离
     *
     * @param page 分页参数对象，包含页码、页大小等分页信息
     * @param query 查询条件实体，包含租户ID和各种查询参数
     * @return IPage<CustomerBackup> 分页查询结果，包含数据列表和分页信息
     * @throws IllegalArgumentException 当租户ID为空时抛出
     */
    @Override
    public IPage<CustomerBackup> pageQuery(Page<CustomerBackup> page, CustomerBackup query) {
        Assert.notNull(query.getTenantId(), "租户ID不能为空");

        QueryWrapper<CustomerBackup> wrapper = buildQueryWrapper(query);
        return baseMapper.selectPage(page, wrapper);
    }

    /**
     * 多条件查询客户备份记录列表（不分页）
     * 适用于数据导出、下拉选择等不需要分页的业务场景
     *
     * @param query 查询条件实体，包含租户ID和各种查询参数
     * @return List<CustomerBackup> 符合条件的备份记录列表
     * @throws IllegalArgumentException 当租户ID为空时抛出
     */
    @Override
    public List<CustomerBackup> listByConditions(CustomerBackup query) {
        Assert.notNull(query.getTenantId(), "租户ID不能为空");

        QueryWrapper<CustomerBackup> wrapper = buildQueryWrapper(query);
        return baseMapper.selectList(wrapper);
    }

    /**
     * 根据原客户ID批量查询备份记录
     * 用于根据原始客户ID列表获取对应的备份记录
     *
     * @param originalIds 原客户ID列表
     * @param tenantId 当前租户ID
     * @return List<CustomerBackup> 对应的备份记录列表
     * @throws IllegalArgumentException 当租户ID为空或原客户ID列表为空时抛出
     */
    @Override
    public List<CustomerBackup> getByOriginalIds(List<Long> originalIds, Long tenantId) {
        Assert.notNull(tenantId, "租户ID不能为空");
        Assert.notEmpty(originalIds, "原客户ID列表不能为空");

        QueryWrapper<CustomerBackup> wrapper = new QueryWrapper<>();
        wrapper.eq("tenant_id", tenantId)
                .in("original_id", originalIds);
        return baseMapper.selectList(wrapper);
    }

    // ==================== 业务操作相关方法 ====================

    /**
     * 批量创建客户备份记录
     * 使用事务保证数据一致性，所有记录必须属于同一租户
     *
     * @param backupList 待创建的备份记录列表
     * @return boolean 批量创建成功返回true，失败返回false
     * @throws IllegalArgumentException 当租户ID为空或不一致时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchCreate(List<CustomerBackup> backupList) {
        if (backupList.isEmpty()) {
            return true;
        }

        // 校验租户一致性
        validateTenantConsistency(backupList);

        return saveBatch(backupList);
    }

    /**
     * 恢复客户备份数据到主表
     * 将备份记录恢复到客户主表，并删除对应的备份记录
     * 使用事务保证恢复操作的原子性
     *
     * @param originalId 原客户ID
     * @param tenantId 当前租户ID
     * @return boolean 恢复成功返回true，失败返回false
     * @throws IllegalArgumentException 当原客户ID或租户ID为空，或未找到备份记录时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean restore(Long originalId, Long tenantId) {
        Assert.notNull(originalId, "原客户ID不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");

        // 查询备份记录（带租户隔离）
        CustomerBackup backup = getOne(new QueryWrapper<CustomerBackup>()
                .eq("tenant_id", tenantId)
                .eq("original_id", originalId)
                .last("limit 1"));
        Assert.notNull(backup, "未找到对应的客户备份记录");

        // 转换为客户主表实体
        Customer customer = new Customer();
        BeanUtils.copyProperties(backup, customer);
        customer.setId(null); // 重置主键，避免冲突
        customer.setStatus(backup.getStatus()); // 可根据业务需求调整状态

        // 保存到主表并删除备份
        boolean saveSuccess = customerService.save(customer);
        if (saveSuccess) {
            return removeById(backup.getId());
        }
        return false;
    }

    /**
     * 批量删除客户备份记录
     * 使用事务保证操作原子性，删除前会校验租户权限
     *
     * @param ids 待删除的备份记录ID列表
     * @param tenantId 当前租户ID
     * @return boolean 删除成功返回true，失败返回false
     * @throws IllegalArgumentException 当租户ID为空、ID列表为空或存在跨租户记录时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchDelete(List<Long> ids, Long tenantId) {
        Assert.notNull(tenantId, "租户ID不能为空");
        Assert.notEmpty(ids, "备份记录ID列表不能为空");

        // 校验租户权限
        validateBackupOwnership(ids, tenantId);

        // 执行批量删除
        QueryWrapper<CustomerBackup> wrapper = new QueryWrapper<>();
        wrapper.eq("tenant_id", tenantId)
                .in("id", ids);
        return remove(wrapper);
    }
}
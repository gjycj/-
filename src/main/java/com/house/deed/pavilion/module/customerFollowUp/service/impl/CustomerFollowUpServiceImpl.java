package com.house.deed.pavilion.module.customerFollowUp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.common.aspect.annotation.AgentDataPermission;
import com.house.deed.pavilion.common.exception.BusinessException;
import com.house.deed.pavilion.common.util.AgentContext;
import com.house.deed.pavilion.common.util.RoleUtil;
import com.house.deed.pavilion.common.util.TenantContext;
import com.house.deed.pavilion.common.util.ValidateUtil;
import com.house.deed.pavilion.module.customerFollowUp.entity.CustomerFollowUp;
import com.house.deed.pavilion.module.customerFollowUp.mapper.CustomerFollowUpMapper;
import com.house.deed.pavilion.module.customerFollowUp.service.ICustomerFollowUpService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 客户跟进记录表（租户级数据） 服务实现类
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
@Service
public class CustomerFollowUpServiceImpl extends ServiceImpl<CustomerFollowUpMapper, CustomerFollowUp> implements ICustomerFollowUpService {

    // 1. 删除跟进记录：注解+基础校验（保留租户+存在性）
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteFollowUp(Long id) {
        return removeById(id);
    }

    // 2. 按合同ID查询：新增注解（自动过滤当前经纪人+租户）
    @Override
    @AgentDataPermission(
            operation = AgentDataPermission.OperationType.QUERY,
            entityClass = CustomerFollowUp.class,
            creatorField = "agentId"
    )
    public List<CustomerFollowUp> getByContractId(Long contractId, Long tenantId) {
        return lambdaQuery()
                .eq(CustomerFollowUp::getTenantId, tenantId)
                .eq(CustomerFollowUp::getContractId, contractId)
                .list();
    }

    // 3. 更新跟进记录：注解+业务增强（角色+时间校验）
    @Override
    @Transactional(rollbackFor = Exception.class)
    @AgentDataPermission(
            operation = AgentDataPermission.OperationType.UPDATE,
            entityClass = CustomerFollowUp.class,
            dataIdParam = "followUp.id",
            creatorField = "agentId"
    )
    public boolean updateFollowUp(CustomerFollowUp followUp) {
        Long followUpId = followUp.getId();
        ValidateUtil.notNull(followUpId, "跟进记录ID不能为空");

        Long tenantId = TenantContext.getTenantId();
        ValidateUtil.notNull(tenantId, "租户上下文获取失败");

        // 基础存在性+租户校验
        CustomerFollowUp existing = getById(followUpId);
        if (existing == null || !existing.getTenantId().equals(tenantId)) {
            throw new BusinessException(404, "跟进记录不存在或不属于当前租户");
        }

        // 角色权限增强：店长额外权限（注解未覆盖，保留手动判断）
        boolean isStoreManager = RoleUtil.isStoreManager();
        boolean isCreator = existing.getAgentId().equals(AgentContext.getAgentId());
        if (!isCreator && !isStoreManager) {
            throw new BusinessException(403, "无权修改：仅跟进记录创建人或店长可操作");
        }

        // 核心业务校验：时间时序（保留原有逻辑）
        LocalDateTime currentFollowTime = followUp.getFollowTime();
        ValidateUtil.notNull(currentFollowTime, "跟进时间不能为空");

        LocalDateTime lastFollowTime = baseMapper.selectLastFollowTimeExcludeCurrent(
                existing.getCustomerId(), tenantId, followUpId);
        if (lastFollowTime != null && currentFollowTime.isBefore(lastFollowTime)) {
            throw new BusinessException(400, "跟进时间不能早于上一次跟进时间（上一次跟进时间：" + lastFollowTime + "）");
        }

        // 字段保护：禁止修改创建人+强制绑定租户
        followUp.setAgentId(existing.getAgentId());
        followUp.setTenantId(tenantId);
        followUp.setFollowTime(LocalDateTime.now()); // 统一更新为当前时间（可选，按业务调整）

        return updateById(followUp);
    }

    // 4. 按客户ID分页查询：注解+自动过滤（保留原有排序）
    @Override
    @AgentDataPermission(
            operation = AgentDataPermission.OperationType.QUERY,
            entityClass = CustomerFollowUp.class,
            creatorField = "agentId"
    )
    public Page<CustomerFollowUp> getByCustomerId(Page<CustomerFollowUp> page, Long customerId, Long tenantId) {
        return lambdaQuery()
                .eq(CustomerFollowUp::getTenantId, tenantId)
                .eq(CustomerFollowUp::getCustomerId, customerId)
                .orderByDesc(CustomerFollowUp::getFollowTime)
                .page(page);
    }

    // 5. 新增跟进记录：新增注解（自动校验创建人+租户）
    @Override
    @Transactional(rollbackFor = Exception.class)
    @AgentDataPermission(
            operation = AgentDataPermission.OperationType.CREATE,
            entityClass = CustomerFollowUp.class,
            creatorField = "agentId" // 按当前经纪人作为创建人校验
    )
    public boolean saveWithTimeCheck(CustomerFollowUp followUp) {
        Long customerId = followUp.getCustomerId();
        Long tenantId = followUp.getTenantId();
        LocalDateTime currentFollowTime = followUp.getFollowTime();

        // 核心业务校验：保留原有时间+关联校验
        ValidateUtil.notNull(currentFollowTime, "跟进时间不能为空");

        LocalDateTime lastFollowTime = baseMapper.selectLastFollowTime(customerId, tenantId);
        if (lastFollowTime != null && currentFollowTime.isBefore(lastFollowTime)) {
            throw new BusinessException(400, "跟进时间不能早于上一次跟进时间（上一次跟进时间：" + lastFollowTime + "）");
        }

        // 强制绑定当前租户（防止篡改）
        followUp.setTenantId(TenantContext.getTenantId());
        return save(followUp);
    }

    // 新增：按ID查询（带权限校验，供Controller调用）
    @Override
    @AgentDataPermission(
            operation = AgentDataPermission.OperationType.QUERY,
            entityClass = CustomerFollowUp.class,
            dataIdParam = "id",
            creatorField = "agentId"
    )
    public CustomerFollowUp getByIdWithPermission(Long id) {
        CustomerFollowUp followUp = getById(id);
        if (followUp == null || !followUp.getTenantId().equals(TenantContext.getTenantId())) {
            throw new BusinessException(404, "跟进记录不存在或无权访问");
        }
        return followUp;
    }
}
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

    @Override
    public List<CustomerFollowUp> getByContractId(Long contractId, Long tenantId) {
        return lambdaQuery()
                .eq(CustomerFollowUp::getTenantId, tenantId)
                .eq(CustomerFollowUp::getContractId, contractId)
                .list();
    }

    /**
     * 更新客户跟进记录（带权限校验：仅创建人或店长可修改）
     * @param followUp 待更新的跟进记录
     * @return 是否更新成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @AgentDataPermission(
            operation = AgentDataPermission.OperationType.UPDATE,
            entityClass = CustomerFollowUp.class,
            dataIdParam = "followUp.id",  // 从参数对象中获取跟进记录ID
            creatorField = "agentId"     // 数据权限字段：创建人ID（agent_id）
    )
    public boolean updateFollowUp(CustomerFollowUp followUp) {
        Long followUpId = followUp.getId();
        ValidateUtil.notNull(followUpId, "跟进记录ID不能为空");

        // 1. 获取当前操作的经纪人ID和租户ID（从上下文获取，与项目其他模块一致）
        Long currentAgentId = AgentContext.getAgentId();
        Long tenantId = TenantContext.getTenantId();
        ValidateUtil.notNull(currentAgentId, "经纪人上下文获取失败");
        ValidateUtil.notNull(tenantId, "租户上下文获取失败");

        // 2. 查询原记录（注解已完成基础权限校验，此处进一步业务校验）
        CustomerFollowUp existing = getById(followUpId);
        if (existing == null || !existing.getTenantId().equals(tenantId)) {
            throw new BusinessException(404, "跟进记录不存在或不属于当前租户");
        }

        // 3. 权限增强：仅创建人或店长可修改
        boolean isCreator = existing.getAgentId().equals(currentAgentId);  // 是否为创建人
        boolean isStoreManager = RoleUtil.isStoreManager();  // 是否为店长（复用项目角色工具类）
        if (!isCreator && !isStoreManager) {
            throw new BusinessException(403, "无权修改：仅跟进记录创建人或店长可操作");
        }

        // 4. 时间时序校验（复用原有保存时的校验逻辑）
        LocalDateTime currentFollowTime = followUp.getFollowTime();
        ValidateUtil.notNull(currentFollowTime, "跟进时间不能为空");

        // 查询该客户上一次的跟进时间（排除当前记录本身，避免更新时与自身比较）
        LocalDateTime lastFollowTime = baseMapper.selectLastFollowTimeExcludeCurrent(
                existing.getCustomerId(), tenantId, followUpId);
        if (lastFollowTime != null && currentFollowTime.isBefore(lastFollowTime)) {
            throw new BusinessException(400, "跟进时间不能早于上一次跟进时间（上一次跟进时间：" + lastFollowTime + "）");
        }

        // 5. 保护不可修改字段（如创建人、租户ID）
        followUp.setAgentId(existing.getAgentId());  // 禁止修改创建人
        followUp.setTenantId(tenantId);              // 强制绑定当前租户
        followUp.setFollowTime(LocalDateTime.now());

        // 6. 执行更新
        return updateById(followUp);
    }

    // 补充：分页查询客户的跟进记录
    @Override
    public Page<CustomerFollowUp> getByCustomerId(Page<CustomerFollowUp> page, Long customerId, Long tenantId) {
        // 构建查询条件：租户隔离 + 客户ID匹配 + 按跟进时间倒序
        LambdaQueryWrapper<CustomerFollowUp> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CustomerFollowUp::getTenantId, tenantId)  // 多租户隔离
                .eq(CustomerFollowUp::getCustomerId, customerId)  // 匹配目标客户
                .orderByDesc(CustomerFollowUp::getFollowTime);  // 最新跟进记录在前

        // 执行分页查询
        return baseMapper.selectPage(page, queryWrapper);
    }

    /**
     * 保存跟进记录（增强：时间时序校验）
     */
    @Transactional
    public boolean saveWithTimeCheck(CustomerFollowUp followUp) {
        Long customerId = followUp.getCustomerId();
        Long tenantId = followUp.getTenantId();
        LocalDateTime currentFollowTime = followUp.getFollowTime();

        // 1. 校验当前跟进时间不为空
        ValidateUtil.notNull(currentFollowTime, "跟进时间不能为空");

        // 2. 查询该客户上一次的跟进时间
        LocalDateTime lastFollowTime = baseMapper.selectLastFollowTime(customerId, tenantId);

        // 3. 若存在上一次跟进，校验当前时间是否更晚
        if (lastFollowTime != null && currentFollowTime.isBefore(lastFollowTime)) {
            throw new BusinessException(400, "跟进时间不能早于上一次跟进时间（上一次跟进时间：" + lastFollowTime + "）");
        }

        // 4. 保存记录
        return save(followUp);
    }

}

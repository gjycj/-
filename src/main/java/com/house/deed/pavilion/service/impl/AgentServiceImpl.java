package com.house.deed.pavilion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.entity.Agent;
import com.house.deed.pavilion.mapper.AgentMapper;
import com.house.deed.pavilion.service.AgentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * <p>
 * 经纪人信息表（租户级数据） 服务实现类
 * 实现说明：基于实体类字段修正逻辑，匹配状态类型、删除方式等核心约束
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Service
public class AgentServiceImpl extends ServiceImpl<AgentMapper, Agent> implements AgentService {

    /**
     * 新增经纪人
     * 校验逻辑：
     * 1. 租户内手机号必须唯一（实体类phone字段非空且唯一）
     * 2. 租户内经纪人工号必须唯一（实体类agentCode字段租户内唯一）
     */
    @Override
    public boolean saveAgent(Agent agent) {
        // 1. 校验租户内手机号唯一性
        LambdaQueryWrapper<Agent> phoneWrapper = new LambdaQueryWrapper<>();
        phoneWrapper.eq(Agent::getTenantId, agent.getTenantId())
                .eq(Agent::getPhone, agent.getPhone());
        long phoneCount = baseMapper.selectCount(phoneWrapper);
        if (phoneCount > 0) {
            throw new IllegalArgumentException("当前租户下手机号已存在：" + agent.getPhone());
        }

        // 2. 校验租户内工号唯一性（实体类agentCode有格式约束，需确保租户内唯一）
        LambdaQueryWrapper<Agent> codeWrapper = new LambdaQueryWrapper<>();
        codeWrapper.eq(Agent::getTenantId, agent.getTenantId())
                .eq(Agent::getAgentCode, agent.getAgentCode());
        long codeCount = baseMapper.selectCount(codeWrapper);
        if (codeCount > 0) {
            throw new IllegalArgumentException("当前租户下工号已存在：" + agent.getAgentCode());
        }

        // 3. 无需手动设置createTime（实体类通过@TableField(fill = FieldFill.INSERT)自动填充）
        return baseMapper.insert(agent) > 0;
    }

    /**
     * 根据ID更新经纪人信息
     * 校验逻辑：
     * 1. 数据必须存在且属于当前租户
     * 2. 若更新手机号/工号，需校验新值在租户内的唯一性
     */
    @Override
    public boolean updateAgentById(Agent agent) {
        // 1. 校验数据存在且属于当前租户
        Agent existAgent = baseMapper.selectById(agent.getId());
        if (existAgent == null || !existAgent.getTenantId().equals(agent.getTenantId())) {
            throw new IllegalArgumentException("经纪人不存在或无权限操作");
        }

        // 2. 若更新手机号，校验新手机号唯一性
        if (agent.getPhone() != null) {
            LambdaQueryWrapper<Agent> phoneWrapper = new LambdaQueryWrapper<>();
            phoneWrapper.eq(Agent::getTenantId, agent.getTenantId())
                    .eq(Agent::getPhone, agent.getPhone())
                    .ne(Agent::getId, agent.getId()); // 排除自身
            long phoneCount = baseMapper.selectCount(phoneWrapper);
            if (phoneCount > 0) {
                throw new IllegalArgumentException("新手机号已存在：" + agent.getPhone());
            }
        }

        // 3. 若更新工号，校验新工号唯一性
        if (agent.getAgentCode() != null) {
            LambdaQueryWrapper<Agent> codeWrapper = new LambdaQueryWrapper<>();
            codeWrapper.eq(Agent::getTenantId, agent.getTenantId())
                    .eq(Agent::getAgentCode, agent.getAgentCode())
                    .ne(Agent::getId, agent.getId()); // 排除自身
            long codeCount = baseMapper.selectCount(codeWrapper);
            if (codeCount > 0) {
                throw new IllegalArgumentException("新工号已存在：" + agent.getAgentCode());
            }
        }

        // 4. 无需手动设置updateTime（实体类通过@TableField(fill = FieldFill.INSERT_UPDATE)自动填充）
        return baseMapper.updateById(agent) > 0;
    }

    /**
     * 删除经纪人（物理删除，因实体类无is_deleted字段）
     * 校验逻辑：必须存在且属于当前租户
     */
    @Override
    public boolean removeAgentById(Long id, Long tenantId) {
        // 校验数据存在且属于当前租户
        Agent existAgent = baseMapper.selectById(id);
        if (existAgent == null || !existAgent.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("经纪人不存在或无权限操作");
        }
        return baseMapper.deleteById(id) > 0;
    }

    /**
     * 根据ID查询经纪人
     * 数据隔离：仅返回当前租户的数据
     */
    @Override
    public Agent getAgentById(Long id, Long tenantId) {
        LambdaQueryWrapper<Agent> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Agent::getId, id)
                .eq(Agent::getTenantId, tenantId);
        return baseMapper.selectOne(queryWrapper);
    }

    /**
     * 多条件分页查询
     * 条件构建：基于实体类字段，支持姓名模糊、手机号精确、门店ID、状态等筛选
     */
    @Override
    public IPage<Agent> pageQuery(Page<Agent> page, Agent agent, Long tenantId) {
        LambdaQueryWrapper<Agent> queryWrapper = new LambdaQueryWrapper<>();
        // 1. 强制租户隔离
        queryWrapper.eq(Agent::getTenantId, tenantId);

        // 2. 动态拼接查询条件（非空字段才参与筛选）
        if (agent.getName() != null) {
            queryWrapper.like(Agent::getName, agent.getName()); // 姓名模糊查询
        }
        if (agent.getPhone() != null) {
            queryWrapper.eq(Agent::getPhone, agent.getPhone()); // 手机号精确匹配
        }
        if (agent.getStoreId() != null) {
            queryWrapper.eq(Agent::getStoreId, agent.getStoreId()); // 所属门店筛选
        }
        if (agent.getStatus() != null) {
            queryWrapper.eq(Agent::getStatus, agent.getStatus()); // 状态筛选（1=在职，0=离职）
        }
        if (agent.getLevel() != null) {
            queryWrapper.eq(Agent::getLevel, agent.getLevel()); // 等级筛选（JUNIOR/SENIOR/STAR）
        }

        // 3. 按创建时间降序（最新的在前面）
        queryWrapper.orderByDesc(Agent::getCreateTime);

        // 4. 分页查询
        return baseMapper.selectPage(page, queryWrapper);
    }

    /**
     * 批量新增经纪人
     * 事务保证：批量操作原子性，全成功或全失败
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchSaveAgents(List<Agent> agents) {
        if (CollectionUtils.isEmpty(agents)) {
            return false;
        }

        // 批量校验手机号和工号唯一性
        for (Agent agent : agents) {
            // 手机号校验（同saveAgent逻辑）
            LambdaQueryWrapper<Agent> phoneWrapper = new LambdaQueryWrapper<>();
            phoneWrapper.eq(Agent::getTenantId, agent.getTenantId())
                    .eq(Agent::getPhone, agent.getPhone());
            if (baseMapper.selectCount(phoneWrapper) > 0) {
                throw new IllegalArgumentException("批量新增失败：手机号已存在" + agent.getPhone());
            }

            // 工号校验（同saveAgent逻辑）
            LambdaQueryWrapper<Agent> codeWrapper = new LambdaQueryWrapper<>();
            codeWrapper.eq(Agent::getTenantId, agent.getTenantId())
                    .eq(Agent::getAgentCode, agent.getAgentCode());
            if (baseMapper.selectCount(codeWrapper) > 0) {
                throw new IllegalArgumentException("批量新增失败：工号已存在" + agent.getAgentCode());
            }
        }

        // 批量保存（依赖MyBatis-Plus的批量插入）
        return saveBatch(agents);
    }

    /**
     * 批量更新经纪人状态（1=在职，0=离职）
     * 事务保证：确保所有更新原子性
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchUpdateStatus(List<Long> ids, Byte status, Long tenantId) {
        if (CollectionUtils.isEmpty(ids)) {
            return false;
        }

        // 1. 校验所有ID都属于当前租户
        validateAgentIdsBelongToTenant(tenantId, ids);

        // 2. 批量更新状态
        Agent updateAgent = new Agent();
        updateAgent.setStatus(status);

        LambdaQueryWrapper<Agent> updateWrapper = new LambdaQueryWrapper<>();
        updateWrapper.in(Agent::getId, ids)
                .eq(Agent::getTenantId, tenantId);

        return baseMapper.update(updateAgent, updateWrapper) > 0;
    }

    /**
     * 批量删除经纪人（物理删除）
     * 事务保证：批量操作原子性
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchRemoveAgents(List<Long> ids, Long tenantId) {
        if (CollectionUtils.isEmpty(ids)) {
            return false;
        }

        // 1. 校验所有ID都属于当前租户
        validateAgentIdsBelongToTenant(tenantId, ids);

        // 2. 批量删除
        return baseMapper.deleteBatchIds(ids) > 0;
    }

    /**
     * 验证经纪人ID列表是否全部属于当前租户
     *
     * @param tenantId 当前租户ID
     * @param agentIds 待验证的经纪人ID列表
     * @throws IllegalArgumentException 当ID不存在或不属于当前租户时抛出
     */
    @Override
    public void validateAgentIdsBelongToTenant(Long tenantId, List<Long> agentIds) {
        if (agentIds == null || agentIds.isEmpty()) {
            return;
        }

        // 1. 查询数据库中存在的经纪人ID及其对应的租户ID
        List<Agent> agents = baseMapper.selectList(
                new LambdaQueryWrapper<Agent>()
                        .select(Agent::getId, Agent::getTenantId)
                        .in(Agent::getId, agentIds)
        );

        // 2. 检查是否存在未查询到的ID（即不存在的ID）
        List<Long> existingIds = agents.stream().map(Agent::getId).toList();
        List<Long> nonExistentIds = agentIds.stream()
                .filter(id -> !existingIds.contains(id))
                .toList();

        if (!nonExistentIds.isEmpty()) {
            throw new IllegalArgumentException("经纪人ID不存在: " + nonExistentIds);
        }

        // 3. 检查存在的ID是否属于当前租户
        List<Long> invalidIds = agents.stream()
                .filter(agent -> !agent.getTenantId().equals(tenantId))
                .map(Agent::getId)
                .toList();

        if (!invalidIds.isEmpty()) {
            throw new IllegalArgumentException("存在无权限操作的经纪人");
        }
    }
}
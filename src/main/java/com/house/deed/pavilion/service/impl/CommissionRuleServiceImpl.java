package com.house.deed.pavilion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.entity.CommissionRule;
import com.house.deed.pavilion.mapper.CommissionRuleMapper;
import com.house.deed.pavilion.service.CommissionRuleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 佣金计算规则表（租户级数据） 服务实现类
 * </p>
 *
 * <p>
 * 本服务类负责佣金计算规则的全生命周期管理，包括：
 * - 佣金规则的增删改查操作
 * - 租户级数据隔离和安全控制
 * - 规则名称唯一性校验
 * - 批量操作的事务保证
 * - 多条件动态查询支持
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Service
public class CommissionRuleServiceImpl extends ServiceImpl<CommissionRuleMapper, CommissionRule> implements CommissionRuleService {

    /**
     * 新增佣金规则
     *
     * <p>
     * 创建新的佣金计算规则，系统会自动校验租户内规则名称的唯一性。
     * 适用于创建新的佣金政策、促销活动规则等场景。
     * </p>
     *
     * @param rule 佣金规则实体对象，包含规则名称、佣金比例、适用类型等业务参数
     * @return boolean 创建结果，true表示创建成功，false表示创建失败
     * @throws IllegalArgumentException 当规则名称为空、租户ID为空或规则名称在租户内已存在时抛出
     */
    @Override
    public boolean saveCommissionRule(CommissionRule rule) {
        // 租户内规则名称唯一性校验：确保同一租户下规则名称不重复
        LambdaQueryWrapper<CommissionRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommissionRule::getTenantId, rule.getTenantId())
                .eq(CommissionRule::getRuleName, rule.getRuleName());
        long count = baseMapper.selectCount(wrapper);
        if (count > 0) {
            throw new IllegalArgumentException("当前租户下规则名称已存在：" + rule.getRuleName());
        }

        // 执行插入操作，createTime字段通过MyBatis-Plus自动填充
        return baseMapper.insert(rule) > 0;
    }

    /**
     * 更新佣金规则
     *
     * <p>
     * 更新现有佣金规则信息，系统会校验数据权限和规则名称唯一性。
     * 支持部分字段更新，未设置的字段保持原值不变。
     * </p>
     *
     * @param rule 佣金规则实体对象，必须包含ID和租户ID用于权限校验
     * @return boolean 更新结果，true表示更新成功，false表示更新失败
     * @throws IllegalArgumentException 当规则不存在、无操作权限或新规则名称已存在时抛出
     */
    @Override
    public boolean updateCommissionRuleById(CommissionRule rule) {
        // 数据存在性及租户权限校验：确保只能操作本租户的数据
        CommissionRule existRule = baseMapper.selectById(rule.getId());
        if (existRule == null || !existRule.getTenantId().equals(rule.getTenantId())) {
            throw new IllegalArgumentException("规则不存在或无权限操作");
        }

        // 规则名称更新时的唯一性校验：排除自身后检查名称是否重复
        if (rule.getRuleName() != null) {
            LambdaQueryWrapper<CommissionRule> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(CommissionRule::getTenantId, rule.getTenantId())
                    .eq(CommissionRule::getRuleName, rule.getRuleName())
                    .ne(CommissionRule::getId, rule.getId()); // 排除当前规则自身
            long count = baseMapper.selectCount(wrapper);
            if (count > 0) {
                throw new IllegalArgumentException("新规则名称已存在：" + rule.getRuleName());
            }
        }

        // 执行更新操作，updateTime字段通过MyBatis-Plus自动填充
        return baseMapper.updateById(rule) > 0;
    }

    /**
     * 删除佣金规则
     *
     * <p>
     * 删除指定的佣金规则，系统会校验数据存在性和租户操作权限。
     * 删除操作为物理删除，请谨慎使用。
     * </p>
     *
     * @param id 要删除的规则主键ID
     * @param tenantId 当前操作租户ID，用于权限校验
     * @return boolean 删除结果，true表示删除成功，false表示删除失败
     * @throws IllegalArgumentException 当规则不存在或无操作权限时抛出
     */
    @Override
    public boolean removeCommissionRuleById(Long id, Long tenantId) {
        // 数据存在性及租户权限校验
        CommissionRule existRule = baseMapper.selectById(id);
        if (existRule == null || !existRule.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("规则不存在或无权限操作");
        }
        return baseMapper.deleteById(id) > 0;
    }

    /**
     * 按ID查询规则详情
     *
     * <p>
     * 根据规则ID查询详细信息，系统会自动进行租户隔离校验。
     * 只能查询到当前租户下的规则信息。
     * </p>
     *
     * @param id 规则主键ID
     * @param tenantId 当前操作租户ID
     * @return CommissionRule 佣金规则实体对象，未找到时返回null
     */
    @Override
    public CommissionRule getCommissionRuleById(Long id, Long tenantId) {
        LambdaQueryWrapper<CommissionRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommissionRule::getId, id)
                .eq(CommissionRule::getTenantId, tenantId);
        return baseMapper.selectOne(wrapper);
    }

    /**
     * 多条件分页查询
     *
     * <p>
     * 支持多种条件的佣金规则分页查询，系统强制租户隔离。
     * 适用于管理后台的规则列表展示、数据筛选等场景。
     * </p>
     *
     * @param page 分页参数对象，包含页码、页大小、排序等信息
     * @param queryMap 查询条件映射表，支持适用类型、状态、规则名称、佣金比例范围等条件
     * @param tenantId 当前操作租户ID
     * @return IPage<CommissionRule> 分页结果对象，包含数据列表和分页信息
     */
    @Override
    public IPage<CommissionRule> pageQuery(Page<CommissionRule> page, Map<String, Object> queryMap, Long tenantId) {
        QueryWrapper<CommissionRule> wrapper = buildQueryWrapper(queryMap, tenantId);
        return baseMapper.selectPage(page, wrapper);
    }

    /**
     * 多条件列表查询
     *
     * <p>
     * 支持多种条件的佣金规则列表查询，不进行分页处理。
     * 适用于下拉选择、数据导出、批量处理等场景。
     * </p>
     *
     * @param queryMap 查询条件映射表
     * @param tenantId 当前操作租户ID
     * @return List<CommissionRule> 符合条件的规则列表
     */
    @Override
    public List<CommissionRule> listByConditions(Map<String, Object> queryMap, Long tenantId) {
        QueryWrapper<CommissionRule> wrapper = buildQueryWrapper(queryMap, tenantId);
        return baseMapper.selectList(wrapper);
    }

    /**
     * 批量新增规则
     *
     * <p>
     * 批量创建佣金规则，使用事务保证数据一致性。
     * 系统会校验每个规则的名称在租户内的唯一性。
     * 适用于数据初始化、批量导入等场景。
     * </p>
     *
     * @param rules 佣金规则实体对象列表
     * @return boolean 批量创建结果，true表示全部创建成功，false表示创建失败
     * @throws IllegalArgumentException 当规则列表为空或存在重复规则名称时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchSaveCommissionRules(List<CommissionRule> rules) {
        // 空列表校验
        if (CollectionUtils.isEmpty(rules)) {
            return false;
        }

        // 批量校验租户内规则名称唯一性
        for (CommissionRule rule : rules) {
            LambdaQueryWrapper<CommissionRule> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(CommissionRule::getTenantId, rule.getTenantId())
                    .eq(CommissionRule::getRuleName, rule.getRuleName());
            if (baseMapper.selectCount(wrapper) > 0) {
                throw new IllegalArgumentException("批量新增失败：规则名称已存在" + rule.getRuleName());
            }
        }

        // 执行批量保存操作
        return saveBatch(rules);
    }

    /**
     * 批量更新规则状态
     *
     * <p>
     * 批量更新多个佣金规则的状态，使用事务保证数据一致性。
     * 系统会校验所有规则ID都属于当前租户。
     * 适用于批量启用、禁用规则等场景。
     * </p>
     *
     * @param ids 要更新的规则ID列表
     * @param status 目标状态（1=生效，0=失效）
     * @param tenantId 当前操作租户ID
     * @return boolean 批量更新结果，true表示更新成功，false表示更新失败
     * @throws IllegalArgumentException 当规则ID列表为空或存在无权限操作的规则时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchUpdateStatus(List<Long> ids, Byte status, Long tenantId) {
        // 空列表校验
        if (CollectionUtils.isEmpty(ids)) {
            return false;
        }

        // 租户权限校验：确保所有规则都属于当前租户
        validateRuleIdsBelongToTenant(tenantId, ids);

        // 构建更新对象并执行批量更新
        CommissionRule updateRule = new CommissionRule();
        updateRule.setStatus(status);

        LambdaQueryWrapper<CommissionRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(CommissionRule::getId, ids)
                .eq(CommissionRule::getTenantId, tenantId);

        return baseMapper.update(updateRule, wrapper) > 0;
    }

    /**
     * 批量删除规则
     *
     * <p>
     * 批量删除多个佣金规则，使用事务保证数据一致性。
     * 系统会校验所有规则ID都属于当前租户。
     * 适用于批量清理过期规则等场景。
     * </p>
     *
     * @param ids 要删除的规则ID列表
     * @param tenantId 当前操作租户ID
     * @return boolean 批量删除结果，true表示删除成功，false表示删除失败
     * @throws IllegalArgumentException 当规则ID列表为空或存在无权限操作的规则时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchRemoveCommissionRules(List<Long> ids, Long tenantId) {
        // 空列表校验
        if (CollectionUtils.isEmpty(ids)) {
            return false;
        }

        // 租户权限校验：确保所有规则都属于当前租户
        validateRuleIdsBelongToTenant(tenantId, ids);

        // 执行批量删除操作
        return baseMapper.deleteBatchIds(ids) > 0;
    }

    /**
     * 校验规则ID列表是否属于当前租户
     *
     * <p>
     * 内部校验方法，用于验证给定的规则ID列表是否全部属于指定租户。
     * 会同时校验规则存在性和租户归属权限。
     * </p>
     *
     * @param tenantId 目标租户ID
     * @param ruleIds 待校验的规则ID列表
     * @throws IllegalArgumentException 当存在不存在的规则ID或无权限操作的规则时抛出
     */
    @Override
    public void validateRuleIdsBelongToTenant(Long tenantId, List<Long> ruleIds) {
        if (CollectionUtils.isEmpty(ruleIds)) {
            return;
        }

        // 使用 QueryWrapper 替代 LambdaQueryWrapper
        List<CommissionRule> rules = baseMapper.selectList(
                new QueryWrapper<CommissionRule>()
                        .select("id", "tenant_id")  // 修正字段名
                        .in("id", ruleIds)
        );

        // 检查不存在的ID：确保所有传入的ID都对应实际存在的规则
        List<Long> existingIds = rules.stream().map(CommissionRule::getId).toList();
        List<Long> nonExistentIds = ruleIds.stream()
                .filter(id -> !existingIds.contains(id))
                .toList();
        if (!nonExistentIds.isEmpty()) {
            throw new IllegalArgumentException("规则ID不存在: " + nonExistentIds);
        }

        // 检查租户归属：确保所有规则都属于当前操作租户
        List<Long> invalidIds = rules.stream()
                .filter(rule -> !rule.getTenantId().equals(tenantId))
                .map(CommissionRule::getId)
                .toList();
        if (!invalidIds.isEmpty()) {
            throw new IllegalArgumentException("无权限操作规则ID: " + invalidIds);
        }
    }

    /**
     * 构建动态查询条件包装器
     *
     * <p>
     * 内部方法，用于构建统一的查询条件，消除重复代码。
     * 支持多种条件的动态拼接，所有查询都强制租户隔离。
     * </p>
     *
     * @param queryMap 查询条件映射表，支持以下键：
     *                - applicableType: 适用类型筛选
     *                - status: 状态筛选（1=生效，0=失效）
     *                - ruleName: 规则名称模糊查询
     *                - minRate: 最小佣金比例
     *                - maxRate: 最大佣金比例
     * @param tenantId 当前操作租户ID
     * @return QueryWrapper<CommissionRule> 构建好的查询条件包装器
     */
    private QueryWrapper<CommissionRule> buildQueryWrapper(Map<String, Object> queryMap, Long tenantId) {
        QueryWrapper<CommissionRule> wrapper = new QueryWrapper<>();
        // 强制租户隔离：所有查询都必须限制在当前租户范围内
        wrapper.eq("tenant_id", tenantId);

        // 动态拼接查询条件：只有非空的参数才会参与查询
        if (!ObjectUtils.isEmpty(queryMap)) {
            // 适用类型筛选：如新房、二手房、租赁等业务类型
            if (queryMap.containsKey("applicableType") && queryMap.get("applicableType") != null) {
                wrapper.eq("applicable_type", queryMap.get("applicableType"));
            }
            // 状态筛选：1=生效（可用），0=失效（不可用）
            if (queryMap.containsKey("status") && queryMap.get("status") != null) {
                wrapper.eq("status", queryMap.get("status"));
            }
            // 规则名称模糊查询：支持中文、英文名称的模糊匹配
            if (queryMap.containsKey("ruleName") && queryMap.get("ruleName") != null) {
                wrapper.like("rule_name", queryMap.get("ruleName"));
            }
            // 佣金比例范围查询：用于筛选特定佣金比例区间的规则
            if (queryMap.containsKey("minRate") && queryMap.get("minRate") != null) {
                wrapper.ge("commission_rate", queryMap.get("minRate"));
            }
            if (queryMap.containsKey("maxRate") && queryMap.get("maxRate") != null) {
                wrapper.le("commission_rate", queryMap.get("maxRate"));
            }
        }

        // 默认排序：按更新时间倒序，确保最新修改的规则显示在最前面
        wrapper.orderByDesc("update_time");
        return wrapper;
    }
}
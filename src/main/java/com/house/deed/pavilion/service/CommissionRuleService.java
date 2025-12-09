package com.house.deed.pavilion.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.house.deed.pavilion.entity.CommissionRule;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 佣金计算规则表（租户级数据） 服务类
 * 核心业务：佣金规则的增删改查、多条件筛选、批量操作，支撑佣金自动计算逻辑
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
public interface CommissionRuleService extends IService<CommissionRule> {

    /**
     * 新增佣金规则
     * 业务说明：保存规则信息，自动填充创建时间，校验租户内规则名称唯一性
     *
     * @param rule 佣金规则实体（包含租户ID、规则名称等核心信息）
     * @return 新增是否成功
     */
    boolean saveCommissionRule(CommissionRule rule);

    /**
     * 根据ID更新佣金规则
     * 业务说明：支持部分字段更新，忽略null值，校验租户权限
     *
     * @param rule 佣金规则实体（必须包含ID和租户ID）
     * @return 更新是否成功
     */
    boolean updateCommissionRuleById(CommissionRule rule);

    /**
     * 根据ID删除佣金规则
     * 业务说明：物理删除，需校验租户权限
     *
     * @param id       规则ID
     * @param tenantId 租户ID（用于数据隔离校验）
     * @return 删除是否成功
     */
    boolean removeCommissionRuleById(Long id, Long tenantId);

    /**
     * 根据ID查询佣金规则详情
     * 业务说明：仅返回租户内的规则信息
     *
     * @param id       规则ID
     * @param tenantId 租户ID（用于数据隔离）
     * @return 佣金规则实体（null表示不存在或无权限）
     */
    CommissionRule getCommissionRuleById(Long id, Long tenantId);

    /**
     * 多条件分页查询佣金规则
     * 业务说明：支持按适用类型、状态、规则名称等条件筛选，仅查询当前租户数据
     *
     * @param page     分页参数（页码、每页条数）
     * @param queryMap 查询条件（键值对，支持：applicableType、status、ruleName等）
     * @param tenantId 租户ID（强制筛选，确保数据隔离）
     * @return 分页结果（包含总条数、当前页数据）
     */
    IPage<CommissionRule> pageQuery(Page<CommissionRule> page, Map<String, Object> queryMap, Long tenantId);

    /**
     * 多条件查询佣金规则列表
     * 业务说明：支持按适用类型、状态等条件组合筛选，仅返回当前租户数据
     *
     * @param queryMap 查询条件（键值对）
     * @param tenantId 租户ID（数据隔离）
     * @return 符合条件的规则列表
     */
    List<CommissionRule> listByConditions(Map<String, Object> queryMap, Long tenantId);

    /**
     * 批量新增佣金规则
     * 业务说明：一次性保存多条规则，自动校验租户内规则名称唯一性，批量操作加事务
     *
     * @param rules 规则列表（每条需包含租户ID）
     * @return 批量新增是否成功（全部成功返回true，否则false）
     */
    boolean batchSaveCommissionRules(List<CommissionRule> rules);

    /**
     * 批量更新规则状态
     * 业务说明：用于批量启用/禁用规则（1=生效，0=失效），需校验租户权限
     *
     * @param ids      规则ID列表
     * @param status   目标状态（1=生效，0=失效）
     * @param tenantId 租户ID（用于数据隔离校验）
     * @return 批量更新是否成功
     */
    boolean batchUpdateStatus(List<Long> ids, Byte status, Long tenantId);

    /**
     * 批量删除佣金规则
     * 业务说明：物理删除，需校验租户权限，加事务保证原子性
     *
     * @param ids      规则ID列表
     * @param tenantId 租户ID（用于数据隔离校验）
     * @return 批量删除是否成功
     */
    boolean batchRemoveCommissionRules(List<Long> ids, Long tenantId);

    /**
     * 校验规则ID列表是否均属于当前租户
     * @param tenantId 当前租户ID
     * @param ruleIds 规则ID列表
     * @throws IllegalArgumentException 当存在不属于当前租户的ID时抛出
     */
    void validateRuleIdsBelongToTenant(Long tenantId, List<Long> ruleIds);
}
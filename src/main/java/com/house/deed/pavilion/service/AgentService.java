package com.house.deed.pavilion.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.house.deed.pavilion.entity.Agent;

import java.util.List;

/**
 * <p>
 * 经纪人信息表（租户级数据） 服务类
 * 核心业务：经纪人信息的增删改查、多条件筛选、批量操作，支撑门店人员管理
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
public interface AgentService extends IService<Agent> {

    /**
     * 新增经纪人
     * 业务说明：保存经纪人基础信息，自动填充创建时间，需校验租户内手机号和工号唯一
     *
     * @param agent 经纪人实体（包含租户ID、姓名、手机号等核心信息）
     * @return 新增是否成功
     */
    boolean saveAgent(Agent agent);

    /**
     * 根据ID更新经纪人信息
     * 业务说明：支持部分字段更新（如手机号、状态），忽略null值，校验租户权限
     *
     * @param agent 经纪人实体（必须包含ID和租户ID）
     * @return 更新是否成功
     */
    boolean updateAgentById(Agent agent);

    /**
     * 根据ID删除经纪人
     * 业务说明：物理删除（实体类无逻辑删除字段），需校验租户权限
     *
     * @param id       经纪人ID
     * @param tenantId 租户ID（用于数据隔离校验）
     * @return 删除是否成功
     */
    boolean removeAgentById(Long id, Long tenantId);

    /**
     * 根据ID查询经纪人详情
     * 业务说明：仅返回租户内的经纪人信息
     *
     * @param id       经纪人ID
     * @param tenantId 租户ID（用于数据隔离）
     * @return 经纪人实体（null表示不存在或无权限）
     */
    Agent getAgentById(Long id, Long tenantId);

    /**
     * 多条件分页查询经纪人
     * 业务说明：支持按姓名、手机号、门店ID、状态等条件组合筛选，仅查询当前租户数据
     *
     * @param page     分页参数（页码、每页条数）
     * @param agent    查询条件（非空字段作为筛选条件）
     * @param tenantId 租户ID（强制筛选，确保数据隔离）
     * @return 分页结果（包含总条数、当前页数据）
     */
    IPage<Agent> pageQuery(Page<Agent> page, Agent agent, Long tenantId);

    /**
     * 批量新增经纪人
     * 业务说明：一次性保存多条经纪人信息，自动校验工号和手机号唯一性，批量操作加事务
     *
     * @param agents 经纪人列表（每条需包含租户ID）
     * @return 批量新增是否成功（全部成功返回true，否则false）
     */
    boolean batchSaveAgents(List<Agent> agents);

    /**
     * 批量更新经纪人状态
     * 业务说明：用于批量启用/禁用经纪人（1=在职，0=离职），需校验租户权限
     *
     * @param ids      经纪人ID列表
     * @param status   目标状态（1=在职，0=离职）
     * @param tenantId 租户ID（用于数据隔离校验）
     * @return 批量更新是否成功
     */
    boolean batchUpdateStatus(List<Long> ids, Byte status, Long tenantId);

    /**
     * 批量删除经纪人
     * 业务说明：物理删除，需校验租户权限，加事务保证原子性
     *
     * @param ids      经纪人ID列表
     * @param tenantId 租户ID（用于数据隔离校验）
     * @return 批量删除是否成功
     */
    boolean batchRemoveAgents(List<Long> ids, Long tenantId);

    /**
     * 校验经纪人ID列表是否均属于当前租户
     * @param tenantId 当前租户ID
     * @param agentIds 经纪人ID列表
     * @throws IllegalArgumentException 当存在不属于当前租户的ID时抛出
     */
    void validateAgentIdsBelongToTenant(Long tenantId, List<Long> agentIds);
}
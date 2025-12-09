package com.house.deed.pavilion.service;

import com.house.deed.pavilion.entity.HouseMaintainPlan;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 房源维护计划表（租户级数据） 服务接口
 * </p>
 * <p>
 * 负责房源维护计划的全生命周期管理，包括维护计划的创建、查询、更新、删除等功能。
 * 房源维护计划是房源持续管理的重要环节，包括定期检查、维修保养、设施更新等计划任务。
 * 所有操作均强制进行租户数据隔离校验，确保数据安全性和业务完整性。
 * </p>
 * <p>
 * 核心业务功能：
 * 1. 维护计划的制定和跟踪管理
 * 2. 按房源、执行人、时间等维度查询计划
 * 3. 批量操作支持，提升管理效率
 * 4. 严格的租户数据隔离和安全控制
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
public interface HouseMaintainPlanService extends IService<HouseMaintainPlan> {

    // ==================== 基础CRUD操作 ====================

    /**
     * 创建房源维护计划记录
     *
     * @param plan 维护计划实体对象，包含计划名称、房源、执行人、时间等信息
     * @return boolean 创建成功返回true，失败返回false
     * @throws IllegalArgumentException 当参数校验失败时抛出
     *
     * 核心字段要求：
     * 1. 租户ID、房源ID、计划名称、计划类型不能为空
     * 2. 计划开始时间不能为空且必须合理
     * 3. 执行人ID必须为有效用户ID
     *
     * 业务场景：
     * 1. 创建定期房源检查计划
     * 2. 制定房源维修保养方案
     * 3. 安排设施更新或改造计划
     */
    boolean saveHouseMaintainPlan(HouseMaintainPlan plan);

    /**
     * 更新房源维护计划记录
     *
     * @param plan 更新后的维护计划实体对象
     * @return boolean 更新成功返回true，失败返回false
     * @throws IllegalArgumentException 当参数校验失败时抛出
     * @throws IllegalStateException 当记录不存在或状态不允许修改时抛出
     *
     * 更新规则：
     * 1. 已完成的维护计划不允许修改核心内容
     * 2. 执行中的计划只能更新进度和状态信息
     * 3. 计划时间调整需考虑与其他计划的冲突
     *
     * 使用场景：
     * 1. 调整计划执行时间
     * 2. 更新计划执行进度
     * 3. 修改计划负责人或执行人
     */
    boolean updateHouseMaintainPlan(HouseMaintainPlan plan);

    /**
     * 删除房源维护计划记录
     *
     * @param id 维护计划记录的唯一标识
     * @param tenantId 租户ID，用于权限校验
     * @return boolean 删除成功返回true，失败返回false
     * @throws IllegalArgumentException 当ID或租户ID为空时抛出
     * @throws IllegalStateException 当记录不存在或权限不足时抛出
     *
     * 删除策略：
     * 1. 执行中的维护计划不建议直接删除，建议取消或标记作废
     * 2. 删除前需校验是否存在关联的执行记录
     * 3. 建议记录删除操作人和删除原因用于审计
     */
    boolean removeHouseMaintainPlan(Long id, Long tenantId);

    /**
     * 根据ID查询维护计划记录（租户隔离）
     *
     * @param id 维护计划记录的唯一标识
     * @param tenantId 租户ID，用于数据隔离
     * @return HouseMaintainPlan 维护计划实体对象，不存在时返回null
     * @throws IllegalArgumentException 当ID或租户ID为空时抛出
     *
     * 说明：
     * 1. 强制添加租户ID条件，确保租户只能访问自己的数据
     * 2. 返回的记录包含完整的计划信息和关联数据
     * 3. 用于计划详情查看和编辑前数据加载
     */
    HouseMaintainPlan getHouseMaintainPlanById(Long id, Long tenantId);

    // ==================== 多条件查询操作 ====================

    /**
     * 多条件分页查询维护计划记录
     *
     * @param page 分页参数对象，包含页码、每页大小等信息
     * @param queryParams 查询参数Map，支持灵活的条件组合
     * @param tenantId 租户ID，用于数据隔离
     * @return IPage<HouseMaintainPlan> 分页结果对象，包含数据列表和分页信息
     * @throws IllegalArgumentException 当租户ID为空时抛出
     *
     * 支持的查询条件：
     * 1. houseId: 房源ID（精确匹配）
     * 2. executorId: 执行人ID（精确匹配）
     * 3. planType: 计划类型（精确匹配）
     * 4. planStatus: 计划状态（精确匹配：待执行/执行中/已完成/已取消）
     * 5. planName: 计划名称（模糊匹配）
     * 6. startTime/endTime: 计划时间范围
     *
     * 默认排序：按计划开始时间升序排列（即将执行的计划在前）
     */
    IPage<HouseMaintainPlan> pageQuery(Page<HouseMaintainPlan> page, Map<String, Object> queryParams, Long tenantId);

    /**
     * 多条件列表查询维护计划记录
     *
     * @param queryParams 查询条件Map，支持灵活的条件组合
     * @param tenantId 租户ID，用于数据隔离
     * @return List<HouseMaintainPlan> 符合条件的维护计划列表
     * @throws IllegalArgumentException 当租户ID为空时抛出
     *
     * 常用查询参数：
     * - houseId: 房源ID（精确匹配）
     * - executorId: 执行人ID（精确匹配）
     * - planType: 计划类型（精确匹配）
     * - planStatus: 计划状态（精确匹配）
     * - planName: 计划名称（模糊匹配）
     * - startTime/endTime: 计划时间范围
     *
     * 使用场景：
     * 1. 导出维护计划报表
     * 2. 统计分析维护数据
     * 3. 批量处理维护计划
     */
    List<HouseMaintainPlan> listByConditions(Map<String, Object> queryParams, Long tenantId);

    /**
     * 根据房源ID查询维护计划记录
     *
     * @param houseId 房源ID
     * @param tenantId 租户ID，用于数据隔离
     * @return List<HouseMaintainPlan> 该房源的所有维护计划列表，按计划开始时间升序排列
     * @throws IllegalArgumentException 当房源ID或租户ID为空时抛出
     *
     * 业务用途：
     * 1. 查看房源的历史维护记录
     * 2. 安排房源的未来维护计划
     * 3. 分析房源的维护频率和成本
     *
     * 返回说明：
     * 1. 返回列表包含该房源的所有维护计划
     * 2. 按计划开始时间升序排列，即将执行的计划在前
     * 3. 包含各种状态的记录（待执行、执行中、已完成、已取消）
     */
    List<HouseMaintainPlan> listByHouseId(Long houseId, Long tenantId);

    /**
     * 根据执行人ID查询维护计划记录
     *
     * @param executorId 执行人ID（用户ID）
     * @param tenantId 租户ID，用于数据隔离
     * @return List<HouseMaintainPlan> 该执行人的所有维护计划列表，按计划开始时间升序排列
     * @throws IllegalArgumentException 当执行人ID或租户ID为空时抛出
     *
     * 业务用途：
     * 1. 查看员工的工作任务分配
     * 2. 统计执行人的工作量和工作效率
     * 3. 安排执行人的工作日程
     *
     * 返回说明：
     * 1. 返回列表包含该执行人的所有维护计划
     * 2. 按计划开始时间升序排列，即将执行的任务在前
     * 3. 可用于任务提醒和进度跟踪
     */
    List<HouseMaintainPlan> listByExecutorId(Long executorId, Long tenantId);

    // ==================== 批量操作接口 ====================

    /**
     * 批量创建维护计划记录
     *
     * @param plans 维护计划记录列表
     * @return boolean 批量创建成功返回true，失败返回false（事务回滚）
     * @throws IllegalArgumentException 当参数为空或记录格式无效时抛出
     *
     * 使用场景：
     * 1. 批量导入历史维护计划数据
     * 2. 批量创建相似房源的维护计划
     * 3. 数据迁移时的批量创建
     *
     * 约束条件：
     * 1. 批量记录需属于同一租户
     * 2. 每个记录必须包含必填字段
     * 3. 房源ID必须在当前租户下存在
     * 4. 执行人ID必须为有效用户ID
     *
     * 事务保障：批量操作使用事务，确保全部成功或全部回滚
     */
    boolean batchSaveHouseMaintainPlans(List<HouseMaintainPlan> plans);

    /**
     * 批量删除维护计划记录
     *
     * @param ids 维护计划记录ID列表
     * @param tenantId 租户ID，用于权限校验
     * @return boolean 批量删除成功返回true，失败返回false（事务回滚）
     * @throws IllegalArgumentException 当参数为空或存在跨租户记录时抛出
     *
     * 安全机制：
     * 1. 强制租户ID校验，防止跨租户删除
     * 2. 批量删除前验证所有记录属于当前租户
     * 3. 仅允许删除特定状态的记录（如待执行状态）
     *
     * 注意事项：
     * 1. 批量删除前建议先备份数据
     * 2. 执行中的维护计划不建议批量删除
     * 3. 记录删除操作日志用于审计
     */
    boolean batchRemoveHouseMaintainPlans(List<Long> ids, Long tenantId);
}
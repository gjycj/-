package com.house.deed.pavilion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.entity.HouseMaintainPlan;
import com.house.deed.pavilion.mapper.HouseMaintainPlanMapper;
import com.house.deed.pavilion.service.HouseMaintainPlanService;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * 房源维护计划表（租户级数据） 服务实现类
 * </p>
 * <p>
 * 负责房源维护计划的全生命周期管理，包括计划的新增、更新、删除、查询及批量操作。
 * 房源维护计划是房源持续管理的关键环节，支持定期检查、维修保养、设施更新等各类维护任务。
 * 所有操作严格遵循租户数据隔离规则，并校验维护计划的业务约束（如时间范围、周期规则等）。
 * </p>
 * <p>
 * 核心特性：
 * 1. 租户数据隔离：所有操作必须验证租户ID，确保跨租户数据不可见
 * 2. 业务规则校验：严格校验维护计划的时间、周期、状态等业务规则
 * 3. 计划唯一性保障：防止同一房源重复创建相同类型和周期的维护计划
 * 4. 批量操作优化：提供批量增删功能，支持事务一致性保障
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Service
public class HouseMaintainPlanServiceImpl extends ServiceImpl<HouseMaintainPlanMapper, HouseMaintainPlan> implements HouseMaintainPlanService {

    // ==================== 基础CRUD实现 ====================

    /**
     * 新增房源维护计划
     *
     * @param plan 维护计划实体对象，包含计划所有信息
     * @return boolean 新增成功返回true，失败返回false
     * @throws IllegalArgumentException 当参数校验失败或业务规则不满足时抛出
     *
     * 执行流程：
     * 1. 基础参数校验（租户ID、房源ID、维护类型等必填字段）
     * 2. 业务规则校验（时间范围、周期逻辑等）
     * 3. 租户内计划唯一性校验（防止重复创建）
     * 4. 调用MyBatis-Plus插入方法持久化数据
     *
     * 业务约束：
     * 1. 开始日期不能早于当前日期
     * 2. 周期性计划必须设置结束日期，且结束日期不能早于开始日期
     * 3. 同一房源在相同租户下，相同类型和周期的计划不能重复
     *
     * 事务说明：此方法没有显式声明事务，依赖调用方的事务控制
     */
    @Override
    public boolean saveHouseMaintainPlan(HouseMaintainPlan plan) {
        // 1. 基础参数校验
        validatePlanBaseParams(plan);
        // 2. 业务规则校验（时间范围、周期匹配等）
        validatePlanBusinessRules(plan);
        // 3. 租户内唯一性校验（同一房源+类型+周期的计划不可重复）
        validatePlanUniqueness(plan);

        return baseMapper.insert(plan) > 0;
    }

    /**
     * 更新房源维护计划
     *
     * @param plan 更新后的维护计划实体对象，必须包含ID和租户ID
     * @return boolean 更新成功返回true，失败返回false
     * @throws IllegalArgumentException 当参数校验失败时抛出
     * @throws IllegalStateException 当记录不存在或权限不足时抛出
     *
     * 执行流程：
     * 1. 基础参数校验（计划ID和租户ID不能为空）
     * 2. 记录存在性及租户归属校验
     * 3. 若涉及时间或周期字段更新，重新校验业务规则
     * 4. 执行数据库更新操作
     *
     * 更新限制：
     * 1. 已完成的维护计划通常不允许修改核心内容
     * 2. 执行中的计划只能更新进度和状态信息
     * 3. 计划时间调整需考虑与其他计划的冲突
     *
     * 注意事项：
     * 1. 部分字段更新时需要重新校验业务规则
     * 2. 不支持跨租户迁移维护计划
     */
    @Override
    public boolean updateHouseMaintainPlan(HouseMaintainPlan plan) {
        // 1. 基础参数校验
        Assert.notNull(plan.getId(), "计划ID不能为空");
        Assert.notNull(plan.getTenantId(), "租户ID不能为空");

        // 2. 校验计划存在且归属当前租户
        HouseMaintainPlan existPlan = baseMapper.selectById(plan.getId());
        Assert.notNull(existPlan, "维护计划不存在");
        Assert.isTrue(Objects.equals(existPlan.getTenantId(), plan.getTenantId()),
                "无权限操作其他租户的维护计划");

        // 3. 业务规则校验（仅当相关字段变更时）
        if (plan.getStartDate() != null || plan.getEndDate() != null || plan.getCycle() != null) {
            validatePlanBusinessRules(plan);
        }

        return baseMapper.updateById(plan) > 0;
    }

    /**
     * 删除房源维护计划
     *
     * @param id 维护计划记录的唯一标识
     * @param tenantId 租户ID，用于权限校验
     * @return boolean 删除成功返回true，失败返回false
     * @throws IllegalArgumentException 当参数为空时抛出
     * @throws IllegalStateException 当记录不存在或权限不足时抛出
     *
     * 执行流程：
     * 1. 参数非空校验
     * 2. 记录存在性及租户归属校验
     * 3. 执行物理删除操作
     *
     * 删除策略：
     * 1. 执行中的维护计划不建议直接删除，建议取消或标记作废
     * 2. 删除前需校验是否存在关联的执行记录
     * 3. 建议记录删除操作人和删除原因用于审计
     */
    @Override
    public boolean removeHouseMaintainPlan(Long id, Long tenantId) {
        // 1. 基础参数校验
        Assert.notNull(id, "计划ID不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");

        // 2. 校验计划归属
        HouseMaintainPlan existPlan = baseMapper.selectById(id);
        Assert.notNull(existPlan, "维护计划不存在");
        Assert.isTrue(Objects.equals(existPlan.getTenantId(), tenantId),
                "无权限操作其他租户的维护计划");

        // 3. 执行删除操作
        return baseMapper.deleteById(id) > 0;
    }

    /**
     * 按ID查询维护计划（租户隔离）
     *
     * @param id 维护计划记录的唯一标识
     * @param tenantId 租户ID，用于数据隔离
     * @return HouseMaintainPlan 维护计划实体对象，不存在时返回null
     * @throws IllegalArgumentException 当参数为空时抛出
     *
     * 说明：
     * 1. 强制添加租户ID查询条件，确保租户数据隔离
     * 2. 返回包含计划所有字段的完整信息
     * 3. 主要用于计划详情查看和编辑前数据加载
     */
    @Override
    public HouseMaintainPlan getHouseMaintainPlanById(Long id, Long tenantId) {
        // 参数校验
        Assert.notNull(id, "计划ID不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");

        return baseMapper.selectOne(new LambdaQueryWrapper<HouseMaintainPlan>()
                .eq(HouseMaintainPlan::getId, id)
                .eq(HouseMaintainPlan::getTenantId, tenantId));
    }

    // ==================== 多条件查询实现 ====================

    /**
     * 分页查询维护计划
     *
     * @param page 分页参数对象，包含页码、每页大小等信息
     * @param queryParams 查询参数Map，支持动态条件组合
     * @param tenantId 租户ID，用于数据隔离
     * @return IPage<HouseMaintainPlan> 分页结果对象，包含数据列表和分页信息
     * @throws IllegalArgumentException 当租户ID为空时抛出
     *
     * 支持的查询条件：
     * 1. houseId: 房源ID（精确匹配）
     * 2. maintainType: 维护类型（精确匹配）
     * 3. cycle: 执行周期（精确匹配）
     * 4. status: 计划状态（精确匹配：待执行/执行中/已完成/已取消）
     * 5. executorId: 执行人ID（精确匹配）
     * 6. startDateMin/startDateMax: 开始日期范围查询
     *
     * 默认排序：按开始日期倒序排列（最新开始的计划在前）
     */
    @Override
    public IPage<HouseMaintainPlan> pageQuery(Page<HouseMaintainPlan> page, Map<String, Object> queryParams, Long tenantId) {
        // 租户ID必填校验
        Assert.notNull(tenantId, "租户ID不能为空");

        // 构建查询条件
        LambdaQueryWrapper<HouseMaintainPlan> wrapper = buildQueryWrapper(queryParams, tenantId);
        return baseMapper.selectPage(page, wrapper);
    }

    /**
     * 多条件查询维护计划列表（租户隔离）
     *
     * @param queryParams 查询参数Map，支持动态条件组合
     * @param tenantId 租户ID，用于数据隔离
     * @return List<HouseMaintainPlan> 符合条件的维护计划列表
     * @throws IllegalArgumentException 当租户ID为空时抛出
     *
     * 说明：
     * 1. 此方法与分页查询使用相同的查询逻辑，但不进行分页处理
     * 2. 适用于需要获取所有匹配记录的场景
     * 3. 按开始日期倒序排列，最新计划在前
     */
    @Override
    public List<HouseMaintainPlan> listByConditions(Map<String, Object> queryParams, Long tenantId) {
        // 租户ID必填校验
        Assert.notNull(tenantId, "租户ID不能为空");

        // 构建查询条件
        LambdaQueryWrapper<HouseMaintainPlan> wrapper = buildQueryWrapper(queryParams, tenantId);
        return baseMapper.selectList(wrapper);
    }

    /**
     * 按房源ID查询维护计划
     *
     * @param houseId 房源ID
     * @param tenantId 租户ID，用于数据隔离
     * @return List<HouseMaintainPlan> 该房源的所有维护计划列表，按开始日期倒序排列
     * @throws IllegalArgumentException 当房源ID或租户ID为空时抛出
     *
     * 业务用途：
     * 1. 查看房源的历史维护记录
     * 2. 安排房源的未来维护计划
     * 3. 分析房源的维护频率和成本
     *
     * 返回说明：
     * 1. 返回列表包含该房源的所有维护计划
     * 2. 按开始日期倒序排列，最新计划在前
     * 3. 包含各种状态的记录（待执行、执行中、已完成、已取消）
     */
    @Override
    public List<HouseMaintainPlan> listByHouseId(Long houseId, Long tenantId) {
        // 参数校验
        Assert.notNull(houseId, "房源ID不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");

        return baseMapper.selectList(new LambdaQueryWrapper<HouseMaintainPlan>()
                .eq(HouseMaintainPlan::getHouseId, houseId)
                .eq(HouseMaintainPlan::getTenantId, tenantId)
                .orderByDesc(HouseMaintainPlan::getStartDate)); // 按开始日期倒序排列
    }

    /**
     * 按执行人ID查询维护计划
     *
     * @param executorId 执行人ID（用户ID）
     * @param tenantId 租户ID，用于数据隔离
     * @return List<HouseMaintainPlan> 该执行人的所有维护计划列表，按开始日期倒序排列
     * @throws IllegalArgumentException 当执行人ID或租户ID为空时抛出
     *
     * 业务用途：
     * 1. 查看员工的工作任务分配
     * 2. 统计执行人的工作量和工作效率
     * 3. 安排执行人的工作日程
     *
     * 返回说明：
     * 1. 返回列表包含该执行人的所有维护计划
     * 2. 按开始日期倒序排列，最新任务在前
     * 3. 可用于任务提醒和进度跟踪
     */
    @Override
    public List<HouseMaintainPlan> listByExecutorId(Long executorId, Long tenantId) {
        // 参数校验
        Assert.notNull(executorId, "执行人ID不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");

        return baseMapper.selectList(new LambdaQueryWrapper<HouseMaintainPlan>()
                .eq(HouseMaintainPlan::getExecutorId, executorId)
                .eq(HouseMaintainPlan::getTenantId, tenantId)
                .orderByDesc(HouseMaintainPlan::getStartDate)); // 按开始日期倒序排列
    }

    // ==================== 批量操作实现 ====================

    /**
     * 批量新增维护计划
     *
     * @param plans 维护计划记录列表
     * @return boolean 批量新增成功返回true，失败返回false
     * @throws IllegalArgumentException 当列表为空或租户ID不一致时抛出
     *
     * 执行流程：
     * 1. 列表非空校验
     * 2. 租户一致性校验（批量记录必须属于同一租户）
     * 3. 逐条记录基础参数校验
     * 4. 逐条记录业务规则校验
     * 5. 逐条记录计划唯一性校验
     * 6. 批量保存到数据库
     *
     * 使用场景：
     * 1. 批量导入历史维护计划数据
     * 2. 批量创建相似房源的维护计划
     * 3. 数据迁移时的批量创建
     */
    @Override
    public boolean batchSaveHouseMaintainPlans(List<HouseMaintainPlan> plans) {
        // 列表非空校验
        if (CollectionUtils.isEmpty(plans)) {
            return false;
        }

        // 1. 校验所有计划属于同一租户
        Long tenantId = plans.get(0).getTenantId();
        Assert.notNull(tenantId, "租户ID不能为空");
        for (HouseMaintainPlan plan : plans) {
            // 租户一致性校验
            Assert.isTrue(Objects.equals(plan.getTenantId(), tenantId), "批量操作必须属于同一租户");
            // 基础参数校验
            validatePlanBaseParams(plan);
            // 业务规则校验
            validatePlanBusinessRules(plan);
            // 计划唯一性校验
            validatePlanUniqueness(plan);
        }

        // 执行批量保存
        return saveBatch(plans);
    }

    /**
     * 批量删除维护计划
     *
     * @param ids 维护计划记录ID列表
     * @param tenantId 租户ID，用于权限校验
     * @return boolean 批量删除成功返回true，失败返回false
     * @throws IllegalArgumentException 当参数为空或存在跨租户记录时抛出
     *
     * 执行流程：
     * 1. 参数非空校验
     * 2. 跨租户记录校验（防止越权删除）
     * 3. 执行批量删除操作
     *
     * 安全机制：
     * 1. 强制租户ID校验，确保只能删除自己租户的数据
     * 2. 批量操作前验证所有记录归属，防止部分成功部分失败
     */
    @Override
    public boolean batchRemoveHouseMaintainPlans(List<Long> ids, Long tenantId) {
        // 参数非空校验
        if (CollectionUtils.isEmpty(ids)) {
            return false;
        }
        Assert.notNull(tenantId, "租户ID不能为空");

        // 校验所有ID归属当前租户
        long count = baseMapper.selectCount(new LambdaQueryWrapper<HouseMaintainPlan>()
                .in(HouseMaintainPlan::getId, ids)
                .ne(HouseMaintainPlan::getTenantId, tenantId));
        Assert.isTrue(count == 0, "存在不属于当前租户的维护计划，无法批量删除");

        // 执行批量删除
        return removeByIds(ids);
    }

    // ==================== 内部工具方法 ====================

    /**
     * 基础参数校验
     *
     * @param plan 维护计划实体对象
     * @throws IllegalArgumentException 当必填字段为空时抛出
     *
     * 校验规则：
     * 1. 租户ID不能为空（确保数据归属明确）
     * 2. 房源ID不能为空（确保维护对象有效）
     * 3. 维护类型不能为空（确定维护工作类别）
     * 4. 执行周期不能为空（确定维护频率）
     * 5. 开始日期不能为空（确定维护启动时间）
     * 6. 执行人ID不能为空（确定维护负责人）
     * 7. 计划状态不能为空（确定维护当前阶段）
     */
    private void validatePlanBaseParams(HouseMaintainPlan plan) {
        Assert.notNull(plan.getTenantId(), "租户ID不能为空");
        Assert.notNull(plan.getHouseId(), "房源ID不能为空");
        Assert.hasText(plan.getMaintainType(), "维护类型不能为空");
        Assert.hasText(plan.getCycle(), "执行周期不能为空");
        Assert.notNull(plan.getStartDate(), "开始日期不能为空");
        Assert.notNull(plan.getExecutorId(), "执行人ID不能为空");
        Assert.hasText(plan.getStatus(), "计划状态不能为空");
    }

    /**
     * 业务规则校验
     *
     * @param plan 维护计划实体对象
     * @throws IllegalArgumentException 当业务规则不满足时抛出
     *
     * 校验逻辑：
     * 1. 开始日期不能早于当前日期（防止创建过去的计划）
     * 2. 周期性计划（非一次性计划）必须设置结束日期
     * 3. 结束日期不能早于开始日期（时间逻辑校验）
     *
     * 业务意义：
     * 1. 确保维护计划的时间合理性
     * 2. 避免创建无效或冲突的计划
     * 3. 提供明确的计划执行时间窗口
     */
    private void validatePlanBusinessRules(HouseMaintainPlan plan) {
        // 1. 开始日期不能早于当前日期
        LocalDate now = LocalDate.now();
        if (plan.getStartDate() != null && plan.getStartDate().isBefore(now)) {
            throw new IllegalArgumentException("开始日期不能早于当前日期");
        }

        // 2. 周期性计划必须有结束日期，且结束日期不能早于开始日期
        if (!"ONCE".equals(plan.getCycle())) { // 非一次性计划
            Assert.notNull(plan.getEndDate(), "周期性计划必须设置结束日期");
            if (plan.getStartDate() != null && plan.getEndDate() != null &&
                    plan.getEndDate().isBefore(plan.getStartDate())) {
                throw new IllegalArgumentException("结束日期不能早于开始日期");
            }
        }
    }

    /**
     * 租户内计划唯一性校验
     *
     * @param plan 维护计划实体对象
     * @throws IllegalArgumentException 当计划已存在时抛出
     *
     * 校验逻辑：
     * 同一租户下，相同房源、相同维护类型、相同执行周期、相同开始日期的计划不可重复
     *
     * 业务意义：
     * 1. 防止重复创建相同的维护计划
     * 2. 避免计划冲突和资源浪费
     * 3. 确保维护计划的可管理性和清晰度
     */
    private void validatePlanUniqueness(HouseMaintainPlan plan) {
        long count = baseMapper.selectCount(new LambdaQueryWrapper<HouseMaintainPlan>()
                .eq(HouseMaintainPlan::getTenantId, plan.getTenantId())
                .eq(HouseMaintainPlan::getHouseId, plan.getHouseId())
                .eq(HouseMaintainPlan::getMaintainType, plan.getMaintainType())
                .eq(HouseMaintainPlan::getCycle, plan.getCycle())
                .eq(HouseMaintainPlan::getStartDate, plan.getStartDate()));
        Assert.isTrue(count == 0, "同一房源相同类型和周期的维护计划已存在");
    }

    /**
     * 构建多条件查询封装器
     *
     * @param queryParams 查询参数Map，支持动态条件组合
     * @param tenantId 租户ID，用于数据隔离
     * @return LambdaQueryWrapper<HouseMaintainPlan> 查询条件封装器
     *
     * 支持的查询条件：
     * 1. houseId: 房源ID（精确匹配）
     * 2. maintainType: 维护类型（精确匹配）
     * 3. cycle: 执行周期（精确匹配）
     * 4. status: 计划状态（精确匹配）
     * 5. executorId: 执行人ID（精确匹配）
     * 6. startDateMin/startDateMax: 开始日期范围查询
     *
     * 默认排序：按开始日期倒序排列（最新开始的计划在前）
     */
    private LambdaQueryWrapper<HouseMaintainPlan> buildQueryWrapper(Map<String, Object> queryParams, Long tenantId) {
        // 强制添加租户隔离条件
        LambdaQueryWrapper<HouseMaintainPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HouseMaintainPlan::getTenantId, tenantId);

        // 如果查询参数为空，直接返回基本查询条件
        if (CollectionUtils.isEmpty(queryParams)) {
            wrapper.orderByDesc(HouseMaintainPlan::getStartDate);
            return wrapper;
        }

        // 按房源ID查询
        if (queryParams.containsKey("houseId") && queryParams.get("houseId") != null) {
            wrapper.eq(HouseMaintainPlan::getHouseId, queryParams.get("houseId"));
        }

        // 按维护类型查询
        if (queryParams.containsKey("maintainType") && StringUtils.hasText((String) queryParams.get("maintainType"))) {
            wrapper.eq(HouseMaintainPlan::getMaintainType, queryParams.get("maintainType"));
        }

        // 按执行周期查询
        if (queryParams.containsKey("cycle") && StringUtils.hasText((String) queryParams.get("cycle"))) {
            wrapper.eq(HouseMaintainPlan::getCycle, queryParams.get("cycle"));
        }

        // 按状态查询
        if (queryParams.containsKey("status") && StringUtils.hasText((String) queryParams.get("status"))) {
            wrapper.eq(HouseMaintainPlan::getStatus, queryParams.get("status"));
        }

        // 按执行人ID查询
        if (queryParams.containsKey("executorId") && queryParams.get("executorId") != null) {
            wrapper.eq(HouseMaintainPlan::getExecutorId, queryParams.get("executorId"));
        }

        // 按开始日期范围查询
        if (queryParams.containsKey("startDateMin") && queryParams.get("startDateMin") != null) {
            wrapper.ge(HouseMaintainPlan::getStartDate, queryParams.get("startDateMin"));
        }
        if (queryParams.containsKey("startDateMax") && queryParams.get("startDateMax") != null) {
            wrapper.le(HouseMaintainPlan::getStartDate, queryParams.get("startDateMax"));
        }

        // 默认按开始日期倒序（最新开始的计划在前）
        wrapper.orderByDesc(HouseMaintainPlan::getStartDate);
        return wrapper;
    }
}
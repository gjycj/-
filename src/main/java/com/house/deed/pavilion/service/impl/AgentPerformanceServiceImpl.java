package com.house.deed.pavilion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.entity.Agent;
import com.house.deed.pavilion.entity.AgentPerformance;
import com.house.deed.pavilion.mapper.AgentPerformanceMapper;
import com.house.deed.pavilion.service.AgentPerformanceService;
import com.house.deed.pavilion.service.AgentService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * <p>
 * 经纪人业绩记录表（租户级数据） 服务实现类
 * </p>
 *
 * <p>
 * 本服务类负责经纪人业绩数据的全生命周期管理，包括：
 * - 业绩记录的增删改查操作
 * - 业绩数据的统计分析聚合
 * - 租户级数据隔离和安全控制
 * - 业绩状态流转和结算管理
 * - 批量操作的事务一致性保证
 * </p>
 *
 * <p>
 * 核心设计原则：
 * 1. 租户隔离强化：所有数据库操作强制携带tenantId条件，与CustomerBackup、Landlord等实体的隔离逻辑保持一致
 * 2. 查询规范统一：采用QueryWrapper构建动态条件，空值处理使用Spring的ObjectUtils工具类，避免NPE
 * 3. 业务适配优化：
 *    - 时间范围查询统一使用ge/le组合，与VisitRecord等实体的时间筛选逻辑对齐
 *    - 排序规则遵循"最新优先"原则（倒序），特殊场景（如趋势分析）采用正序
 * 4. 性能考量：复杂统计逻辑（如周期求和）委托给Mapper层自定义SQL，减少服务层计算开销
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Service
public class AgentPerformanceServiceImpl extends ServiceImpl<AgentPerformanceMapper, AgentPerformance> implements AgentPerformanceService {

    @Resource
    private AgentService agentService;

    // 业绩月份格式正则（yyyyMM），用于校验月份格式是否符合规范
    private static final Pattern PERFORMANCE_MONTH_PATTERN = Pattern.compile("^\\d{6}$");
    // 业绩状态合法值枚举，定义系统支持的三种业绩状态
    private static final List<String> VALID_STATUS = List.of("UNSETTLED", "SETTLED", "CANCELED");

    /**
     * 多条件分页查询经纪人业绩记录
     *
     * <p>
     * 支持按经纪人ID、业绩类型、时间范围、合同ID等多种条件组合查询，
     * 强制租户隔离确保数据安全，适用于业绩管理后台的列表展示。
     * </p>
     *
     * @param page 分页参数对象，包含页码、页大小、排序等配置信息
     * @param queryParams 查询条件映射表，支持以下参数：
     *                   - agentId: 经纪人ID精确查询
     *                   - performanceType: 业绩类型筛选
     *                   - startDate: 业绩日期范围查询（开始日期）
     *                   - endDate: 业绩日期范围查询（结束日期）
     *                   - contractId: 关联合同ID精确查询
     * @param tenantId 当前操作租户ID，用于数据隔离
     * @return IPage<AgentPerformance> 分页结果对象，包含数据列表和分页信息
     * @throws IllegalArgumentException 当租户ID为空时抛出异常
     */
    @Override
    public IPage<AgentPerformance> pageQuery(Page<AgentPerformance> page, Map<String, Object> queryParams, Long tenantId) {
        Assert.notNull(tenantId, "租户ID不能为空");

        QueryWrapper<AgentPerformance> queryWrapper = new QueryWrapper<>();
        // 租户隔离：必加条件，与Landlord等实体的查询逻辑一致
        queryWrapper.eq("tenant_id", tenantId);

        // 动态拼接业务查询条件，空值不参与查询
        if (!ObjectUtils.isEmpty(queryParams.get("agentId"))) {
            queryWrapper.eq("agent_id", queryParams.get("agentId"));
        }
        if (!ObjectUtils.isEmpty(queryParams.get("performanceType"))) {
            queryWrapper.eq("performance_type", queryParams.get("performanceType"));
        }
        if (!ObjectUtils.isEmpty(queryParams.get("startDate"))) {
            queryWrapper.ge("performance_date", queryParams.get("startDate"));
        }
        if (!ObjectUtils.isEmpty(queryParams.get("endDate"))) {
            queryWrapper.le("performance_date", queryParams.get("endDate"));
        }
        if (!ObjectUtils.isEmpty(queryParams.get("contractId"))) {
            queryWrapper.eq("contract_id", queryParams.get("contractId"));
        }

        // 排序规则：按业绩日期倒序，与VisitRecord的createTime排序逻辑一致
        queryWrapper.orderByDesc("performance_date");

        return baseMapper.selectPage(page, queryWrapper);
    }

    /**
     * 根据经纪人ID查询业绩记录列表
     *
     * <p>
     * 查询指定经纪人在特定时间范围内的所有业绩记录，
     * 按业绩日期正序排列，便于观察业绩变化趋势和生成业绩图表。
     * </p>
     *
     * @param agentId 经纪人ID，用于精确查询特定经纪人的业绩
     * @param startTime 查询开始时间，包含该时间点
     * @param endTime 查询结束时间，包含该时间点
     * @param tenantId 当前操作租户ID，用于数据隔离
     * @return List<AgentPerformance> 符合条件的业绩记录列表，按业绩日期正序排列
     * @throws IllegalArgumentException 当租户ID或经纪人ID为空时抛出异常
     */
    @Override
    public List<AgentPerformance> listByAgentId(Long agentId, LocalDate startTime, LocalDate endTime, Long tenantId) {
        Assert.notNull(tenantId, "租户ID不能为空");
        Assert.notNull(agentId, "经纪人ID不能为空");

        QueryWrapper<AgentPerformance> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("tenant_id", tenantId)
                .eq("agent_id", agentId)
                .ge("performance_date", startTime)
                .le("performance_date", endTime)
                .orderByAsc("performance_date"); // 正序排列，便于趋势分析
        return baseMapper.selectList(queryWrapper);
    }

    /**
     * 按时间周期统计经纪人业绩总和
     *
     * <p>
     * 按指定周期类型（如月度、季度、年度）统计各经纪人的业绩总额，
     * 返回经纪人ID到业绩总和的映射，便于业绩排名和奖金计算。
     * </p>
     *
     * @param cycleType 统计周期类型，如"MONTH"、"QUARTER"、"YEAR"等
     * @param statisticDate 统计基准日期，用于确定具体的统计周期
     * @param tenantId 当前操作租户ID，用于数据隔离
     * @return Map<Long, BigDecimal> 经纪人ID到业绩总额的映射表
     * @throws IllegalArgumentException 当租户ID、周期类型或统计日期为空时抛出异常
     */
    @Override
    public Map<Long, BigDecimal> sumByCycle(String cycleType, LocalDate statisticDate, Long tenantId) {
        Assert.notNull(tenantId, "租户ID不能为空");
        Assert.hasText(cycleType, "统计周期类型不能为空");
        Assert.notNull(statisticDate, "统计基准日期不能为空");

        // 委托Mapper层处理周期统计（需在AgentPerformanceMapper中定义sumPerformanceByCycle方法）
        List<Map<String, Object>> sumList = baseMapper.sumPerformanceByCycle(cycleType, statisticDate, tenantId);

        // 转换结果格式，与接口返回值要求匹配
        Map<Long, BigDecimal> resultMap = new HashMap<>(sumList.size());
        for (Map<String, Object> item : sumList) {
            Long agentId = Long.valueOf(item.get("agent_id").toString());
            BigDecimal totalAmount = new BigDecimal(item.get("total_amount").toString());
            resultMap.put(agentId, totalAmount);
        }
        return resultMap;
    }

    /**
     * 根据业绩类型查询记录
     *
     * <p>
     * 按业绩类型筛选记录，如新房业绩、二手房业绩、租赁业绩等，
     * 按创建时间倒序排列，便于查看最新的业绩记录。
     * </p>
     *
     * @param performanceType 业绩类型，如"NEW_HOUSE"、"SECOND_HAND"、"RENT"等
     * @param tenantId 当前操作租户ID，用于数据隔离
     * @return List<AgentPerformance> 指定类型的业绩记录列表，按创建时间倒序排列
     * @throws IllegalArgumentException 当租户ID或业绩类型为空时抛出异常
     */
    @Override
    public List<AgentPerformance> listByType(String performanceType, Long tenantId) {
        Assert.notNull(tenantId, "租户ID不能为空");
        Assert.hasText(performanceType, "业绩类型不能为空");

        QueryWrapper<AgentPerformance> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("tenant_id", tenantId)
                .eq("performance_type", performanceType)
                .orderByDesc("create_time"); // 按创建时间倒序，与实体类的记录新增逻辑匹配
        return baseMapper.selectList(queryWrapper);
    }

    /**
     * 根据合同ID查询关联的业绩记录
     *
     * <p>
     * 查询与指定合同相关的所有业绩记录，用于合同详情页展示关联业绩，
     * 或用于业绩核算时的数据核对。
     * </p>
     *
     * @param contractId 合同ID，用于精确查询关联业绩
     * @param tenantId 当前操作租户ID，用于数据隔离
     * @return List<AgentPerformance> 关联指定合同的业绩记录列表，按主键倒序排列
     * @throws IllegalArgumentException 当租户ID或合同ID为空时抛出异常
     */
    @Override
    public List<AgentPerformance> listByContractId(Long contractId, Long tenantId) {
        Assert.notNull(tenantId, "租户ID不能为空");
        Assert.notNull(contractId, "合同ID不能为空");

        QueryWrapper<AgentPerformance> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("tenant_id", tenantId)
                .eq("contract_id", contractId)
                .orderByDesc("id"); // 按主键倒序，确保最新记录在前
        return baseMapper.selectList(queryWrapper);
    }

    /**
     * 添加单条经纪人业绩记录
     *
     * <p>
     * 创建新的经纪人业绩记录，系统会自动校验数据完整性和业务规则，
     * 包括租户一致性、关联数据合法性、字段格式约束等。
     * </p>
     *
     * @param performance 业绩记录实体对象，包含经纪人ID、合同ID、业绩金额等业务数据
     * @param tenantId 当前操作租户ID，用于权限校验和数据隔离
     * @return boolean 创建结果，true表示创建成功，false表示创建失败
     * @throws IllegalArgumentException 当业绩记录为空、租户ID为空、租户不一致或字段校验失败时抛出
     */
    @Override
    public boolean saveAgentPerformance(AgentPerformance performance, Long tenantId) {
        // 1. 基础参数校验
        Assert.notNull(performance, "业绩记录不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");
        Assert.isTrue(tenantId.equals(performance.getTenantId()), "业绩记录的租户ID与操作租户不一致");

        // 2. 关联数据校验（经纪人必须存在且属于当前租户）
        Agent agent = agentService.getAgentById(performance.getAgentId(), tenantId);
        Assert.notNull(agent, "经纪人不存在或不属于当前租户（agentId=" + performance.getAgentId() + "）");

        // 3. 字段合法性校验（与实体类注解约束保持一致）
        validatePerformanceFields(performance);

        // 4. 移除手动设置的createTime（确保自动填充生效）
        performance.setCreateTime(null);

        return baseMapper.insert(performance) > 0;
    }

    /**
     * 批量添加经纪人业绩记录
     *
     * <p>
     * 批量创建经纪人业绩记录，使用事务保证数据一致性，
     * 适用于数据导入、批量录入等场景，提升数据录入效率。
     * </p>
     *
     * @param performances 业绩记录实体对象列表
     * @param tenantId 当前操作租户ID，用于权限校验和数据隔离
     * @return boolean 批量创建结果，true表示全部创建成功，false表示创建失败
     * @throws IllegalArgumentException 当租户ID为空、记录列表为空、租户不一致或字段校验失败时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchSaveAgentPerformances(List<AgentPerformance> performances, Long tenantId) {
        // 1. 基础参数校验
        Assert.notNull(tenantId, "租户ID不能为空");
        Assert.isTrue(!CollectionUtils.isEmpty(performances), "业绩记录列表不能为空");

        // 2. 批量校验租户一致性
        boolean hasInvalidTenant = performances.stream()
                .anyMatch(p -> !tenantId.equals(p.getTenantId()));
        Assert.isTrue(!hasInvalidTenant, "存在不属于当前租户的业绩记录");

        // 3. 提取经纪人ID列表，批量校验合法性（复用AgentService的校验方法）
        List<Long> agentIds = performances.stream()
                .map(AgentPerformance::getAgentId)
                .distinct()
                .toList();
        agentService.validateAgentIdsBelongToTenant(tenantId, agentIds);

        // 4. 逐条校验字段约束
        performances.forEach(this::validatePerformanceFields);

        // 5. 统一清除手动设置的createTime
        performances.forEach(p -> p.setCreateTime(null));

        return saveBatch(performances);
    }

    /**
     * 批量删除业绩记录
     *
     * <p>
     * 批量删除指定的业绩记录，使用事务保证操作原子性，
     * 系统会校验所有记录都属于当前租户，防止跨租户删除。
     * </p>
     *
     * @param ids 要删除的业绩记录ID列表
     * @param tenantId 当前操作租户ID，用于权限校验
     * @return boolean 批量删除结果，true表示删除成功，false表示删除失败
     * @throws IllegalArgumentException 当租户ID为空或存在不属于当前租户的记录时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchRemove(List<Long> ids, Long tenantId) {
        // 1. 参数校验
        Assert.notNull(tenantId, "租户ID不能为空");
        if (CollectionUtils.isEmpty(ids)) {
            return false;
        }

        // 2. 校验所有ID属于当前租户（防止跨租户删除）- 使用 QueryWrapper 替代 LambdaQueryWrapper
        QueryWrapper<AgentPerformance> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("id", ids)
                .ne("tenant_id", tenantId);
        long invalidCount = baseMapper.selectCount(queryWrapper);
        if (invalidCount > 0) {
            throw new IllegalArgumentException("存在不属于当前租户的业绩记录，无法删除");
        }

        // 3. 批量删除
        return baseMapper.deleteBatchIds(ids) > 0;
    }

    /**
     * 批量更新业绩状态
     *
     * <p>
     * 批量更新业绩记录的状态，如批量标记为已结算、已取消等，
     * 使用事务保证操作原子性，支持结算时间等关联字段的自动设置。
     * </p>
     *
     * @param ids 要更新的业绩记录ID列表
     * @param status 目标状态（UNSETTLED=未结算，SETTLED=已结算，CANCELED=已取消）
     * @param settleTime 结算时间，当状态为SETTLED时必须提供
     * @param tenantId 当前操作租户ID，用于权限校验
     * @return boolean 批量更新结果，true表示更新成功，false表示更新失败
     * @throws IllegalArgumentException 当租户ID为空、ID列表为空、状态非法或存在跨租户记录时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchUpdateStatus(List<Long> ids, String status, LocalDateTime settleTime, Long tenantId) {
        // 1. 参数校验
        Assert.notNull(tenantId, "租户ID不能为空");
        Assert.notEmpty(ids, "业绩记录ID列表不能为空");
        // 替代 Assert.notBlank(status, "目标状态不能为空")
        if (status == null || status.trim().isEmpty()) {
            throw new IllegalArgumentException("目标状态不能为空");
        }
        // 校验状态合法性（必须是枚举值）
        if (!Arrays.asList("UNSETTLED", "SETTLED", "CANCELED").contains(status)) {
            throw new IllegalArgumentException("业绩状态错误（仅支持UNSETTLED/SETTLED/CANCELED）");
        }
        // 若状态为已结算，必须传入结算时间
        if ("SETTLED".equals(status) && settleTime == null) {
            throw new IllegalArgumentException("状态为已结算时，结算时间不能为空");
        }

        // 2. 校验所有ID属于当前租户 - 使用 QueryWrapper 替代 LambdaQueryWrapper
        QueryWrapper<AgentPerformance> validateWrapper = new QueryWrapper<>();
        validateWrapper.in("id", ids)
                .ne("tenant_id", tenantId);
        long invalidCount = baseMapper.selectCount(validateWrapper);
        if (invalidCount > 0) {
            throw new IllegalArgumentException("存在不属于当前租户的业绩记录，无法更新");
        }

        // 3. 构建更新参数
        AgentPerformance updateEntity = new AgentPerformance();
        updateEntity.setPerformanceStatus(status);
        // 仅当状态为已结算时设置结算时间
        if ("SETTLED".equals(status)) {
            updateEntity.setSettleTime(settleTime);
        }

        // 4. 批量更新（仅更新当前租户的指定ID）- 使用 QueryWrapper 替代 LambdaQueryWrapper
        QueryWrapper<AgentPerformance> updateWrapper = new QueryWrapper<>();
        updateWrapper.in("id", ids)
                .eq("tenant_id", tenantId);

        return baseMapper.update(updateEntity, updateWrapper) > 0;
    }

    /**
     * 业绩记录字段合法性校验
     *
     * <p>
     * 内部校验方法，用于验证业绩记录实体对象的字段合法性，
     * 与实体类AgentPerformance的注解约束严格匹配，确保数据质量。
     * </p>
     *
     * @param performance 待校验的业绩记录实体对象
     * @throws IllegalArgumentException 当任何字段校验失败时抛出异常
     */
    private void validatePerformanceFields(AgentPerformance performance) {
        // 3.1 经纪人ID非空校验（实体类@NotNull）
        Assert.notNull(performance.getAgentId(), "经纪人ID不能为空");

        // 3.2 合同ID非空校验（实体类@NotNull）
        Assert.notNull(performance.getContractId(), "合同ID不能为空");

        // 3.3 业绩月份校验（格式yyyyMM，非空）
        Assert.hasText(performance.getPerformanceMonth(), "业绩月份不能为空");
        Assert.isTrue(PERFORMANCE_MONTH_PATTERN.matcher(performance.getPerformanceMonth()).matches(),
                "业绩月份格式错误（需为yyyyMM，如202511）");

        // 3.4 成交金额校验（非负，保留2位小数）
        Assert.notNull(performance.getDealAmount(), "成交金额不能为空");
        Assert.isTrue(performance.getDealAmount().compareTo(BigDecimal.ZERO) >= 0, "成交金额不能为负数");
        Assert.isTrue(performance.getDealAmount().scale() <= 2, "成交金额最多保留2位小数");

        // 3.5 佣金金额校验（非负，保留2位小数）
        Assert.notNull(performance.getCommissionAmount(), "佣金金额不能为空");
        Assert.isTrue(performance.getCommissionAmount().compareTo(BigDecimal.ZERO) >= 0, "佣金金额不能为负数");
        Assert.isTrue(performance.getCommissionAmount().scale() <= 2, "佣金金额最多保留2位小数");

        // 3.6 业绩状态校验（合法枚举值）
        Assert.hasText(performance.getPerformanceStatus(), "业绩状态不能为空");
        Assert.isTrue(VALID_STATUS.contains(performance.getPerformanceStatus()),
                "业绩状态错误（仅支持UNSETTLED/SETTLED/CANCELED）");

        // 3.7 结算时间校验（状态为SETTLED时必填，否则必须为null）
        if ("SETTLED".equals(performance.getPerformanceStatus())) {
            Assert.notNull(performance.getSettleTime(), "业绩状态为已结算时，结算时间不能为空");
        } else {
            Assert.isNull(performance.getSettleTime(), "业绩状态非已结算时，结算时间必须为null");
        }
    }
}
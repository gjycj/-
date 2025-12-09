package com.house.deed.pavilion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.entity.MaintenanceOrder;
import com.house.deed.pavilion.mapper.MaintenanceOrderMapper;
import com.house.deed.pavilion.service.MaintenanceOrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 房源维修工单服务实现类
 *
 * <p>实现维修工单的增删改查及批量操作，所有方法均包含租户级数据隔离和严格的业务校验</p>
 * <p>业务特点：</p>
 * <ul>
 *   <li>支持租赁和退租两种维修场景</li>
 *   <li>完整的状态流转控制（SUBMITTED→ASSIGNED→REPAIRING→COMPLETED/CANCELED）</li>
 *   <li>紧急程度分级（1=低，2=中，3=高）</li>
 *   <li>维修费用管理（包含费用承担方：LANDLORD/TENANT/SHARED）</li>
 * </ul>
 *
 * <p>技术要点：</p>
 * <ul>
 *   <li>所有枚举值校验完全匹配实体类allowableValues配置</li>
 *   <li>自动填充创建时间和更新时间（通过MyBatis Plus字段填充器）</li>
 *   <li>修复LambdaQueryWrapper排序方法的语法错误</li>
 *   <li>使用Spring Assert进行参数校验</li>
 * </ul>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Service
public class MaintenanceOrderServiceImpl extends ServiceImpl<MaintenanceOrderMapper, MaintenanceOrder> implements MaintenanceOrderService {

    // ==================== 实体类枚举常量（完全匹配allowableValues） ====================

    /**
     * 合法报修人类型集合
     * <p>与实体类allowableValues配置保持一致，包括：</p>
     * <ul>
     *   <li>TENANT - 租户</li>
     *   <li>LANDLORD - 房东</li>
     *   <li>AGENT - 经纪人</li>
     *   <li>OTHER - 其他</li>
     * </ul>
     */
    private static final Set<String> VALID_REPORTER_TYPES = Set.of("TENANT", "LANDLORD", "AGENT", "OTHER");

    /**
     * 合法维修类型集合
     * <p>与实体类allowableValues配置保持一致，包括：</p>
     * <ul>
     *   <li>WATER - 水修</li>
     *   <li>APPLIANCE - 电器维修</li>
     *   <li>WALL - 墙面维修</li>
     *   <li>PIPELINE - 管道维修</li>
     *   <li>DOOR_WINDOW - 门窗维修</li>
     *   <li>OTHER - 其他维修</li>
     * </ul>
     */
    private static final Set<String> VALID_MAINTENANCE_TYPES = Set.of("WATER", "APPLIANCE", "WALL", "PIPELINE", "DOOR_WINDOW", "OTHER");

    /**
     * 合法工单状态集合
     * <p>与实体类allowableValues配置保持一致，包括：</p>
     * <ul>
     *   <li>SUBMITTED - 已提交</li>
     *   <li>ASSIGNED - 已派单</li>
     *   <li>REPAIRING - 维修中</li>
     *   <li>COMPLETED - 已完成</li>
     *   <li>CANCELED - 已取消</li>
     * </ul>
     */
    private static final Set<String> VALID_ORDER_STATUSES = Set.of("SUBMITTED", "ASSIGNED", "REPAIRING", "COMPLETED", "CANCELED");

    /**
     * 合法费用承担方集合
     * <p>与实体类allowableValues配置保持一致，包括：</p>
     * <ul>
     *   <li>LANDLORD - 房东承担</li>
     *   <li>TENANT - 租户承担</li>
     *   <li>SHARED - 双方分摊</li>
     * </ul>
     */
    private static final Set<String> VALID_COST_BEARERS = Set.of("LANDLORD", "TENANT", "SHARED");

    /**
     * 合法紧急程度集合
     * <p>紧急程度为字节类型，取值：</p>
     * <ul>
     *   <li>1 - 低</li>
     *   <li>2 - 中</li>
     *   <li>3 - 高</li>
     * </ul>
     */
    private static final Set<Byte> VALID_URGENCY_LEVELS = Set.of((byte)1, (byte)2, (byte)3);

    /**
     * 手机号正则表达式
     * <p>匹配实体类Pattern注解配置，支持格式：</p>
     * <ul>
     *   <li>11位手机号（1开头）</li>
     *   <li>可选的+86前缀</li>
     * </ul>
     */
    private static final Pattern PHONE_PATTERN = Pattern.compile("^(\\+86)?1[3-9]\\d{9}$");

    // ==================== 状态流转规则（贴合实体类业务说明） ====================

    /**
     * 工单状态流转规则
     * <p>定义工单状态之间的合法流转关系：</p>
     * <ul>
     *   <li>SUBMITTED → ASSIGNED, CANCELED</li>
     *   <li>ASSIGNED → REPAIRING, CANCELED</li>
     *   <li>REPAIRING → COMPLETED, CANCELED</li>
     *   <li>COMPLETED → 不可变更</li>
     *   <li>CANCELED → 不可变更</li>
     * </ul>
     */
    private static final Map<String, Set<String>> STATUS_FLOW_RULES = new HashMap<>();
    static {
        STATUS_FLOW_RULES.put("SUBMITTED", Set.of("ASSIGNED", "CANCELED"));       // 已提交→已派单/已取消
        STATUS_FLOW_RULES.put("ASSIGNED", Set.of("REPAIRING", "CANCELED"));       // 已派单→维修中/已取消
        STATUS_FLOW_RULES.put("REPAIRING", Set.of("COMPLETED", "CANCELED"));      // 维修中→已完成/已取消
        STATUS_FLOW_RULES.put("COMPLETED", Collections.emptySet());               // 已完成→不可变更
        STATUS_FLOW_RULES.put("CANCELED", Collections.emptySet());                // 已取消→不可变更
    }

    // ==================== 基础CRUD实现 ====================

    /**
     * 新增维修工单
     *
     * <p>业务校验：</p>
     * <ul>
     *   <li>基础字段非空校验（租户ID、房源ID、工单编号等）</li>
     *   <li>枚举值合法性校验（报修人类型、维修类型、工单状态等）</li>
     *   <li>格式校验（手机号格式、字段长度等）</li>
     *   <li>场景化关联字段校验（租赁/退租场景）</li>
     *   <li>租户内工单编号唯一性校验</li>
     * </ul>
     * <p>技术实现：自动填充createTime（通过MyBatis Plus字段填充器）</p>
     *
     * @param order 维修工单实体对象，需包含必填信息
     * @return 新增成功返回true，否则返回false
     * @throws IllegalArgumentException 当违反任何业务规则时抛出
     */
    @Override
    public boolean saveMaintenanceOrder(MaintenanceOrder order) {
        // 1. 基础字段+枚举+格式校验（完全匹配实体类注解）
        validateEntityBaseConstraints(order);

        // 2. 场景化关联字段校验（租赁/退租场景）
        validateSceneRelatedFields(order);

        // 3. 租户内工单编号唯一性校验
        validateOrderNoUnique(order.getTenantId(), order.getOrderNo(), null);

        // 4. 新增工单默认状态为已提交，禁止手动指定其他初始状态
        if (!"SUBMITTED".equals(order.getStatus())) {
            throw new IllegalArgumentException("新增工单仅允许初始状态为已提交（SUBMITTED）");
        }

        // 5. 执行新增操作（createTime自动填充）
        return baseMapper.insert(order) > 0;
    }

    /**
     * 根据ID更新维修工单
     *
     * <p>业务校验：</p>
     * <ul>
     *   <li>数据必须存在且属于当前租户</li>
     *   <li>禁止修改核心不可变字段（房源ID、报修人类型、维修类型等）</li>
     *   <li>工单编号修改时校验唯一性</li>
     *   <li>状态变更时校验流转规则和联动字段</li>
     * </ul>
     * <p>技术实现：自动填充updateTime（通过MyBatis Plus字段填充器）</p>
     *
     * @param order 维修工单实体对象，需包含主键ID和需要更新的字段
     * @return 更新成功返回true，否则返回false
     * @throws IllegalArgumentException 当违反任何业务规则时抛出
     */
    @Override
    public boolean updateMaintenanceOrderById(MaintenanceOrder order) {
        // 1. 基础参数校验
        Assert.notNull(order.getId(), "工单ID不能为空");
        Assert.notNull(order.getTenantId(), "租户ID不能为空");

        // 2. 校验工单存在且归属当前租户
        MaintenanceOrder existOrder = getMaintenanceOrderById(order.getId(), order.getTenantId());
        Assert.notNull(existOrder, "维修工单不存在或无权限操作");

        // 3. 禁止修改核心不可变字段
        validateImmutableFields(existOrder, order);

        // 4. 工单编号修改时校验唯一性
        if (order.getOrderNo() != null && !order.getOrderNo().equals(existOrder.getOrderNo())) {
            validateOrderNoUnique(order.getTenantId(), order.getOrderNo(), order.getId());
        }

        // 5. 状态变更校验（流转规则+联动字段）
        if (order.getStatus() != null && !order.getStatus().equals(existOrder.getStatus())) {
            validateStatusFlow(existOrder.getStatus(), order.getStatus());
            // 填充状态联动字段（如完成时间、预约时间等）
            fillStatusRelatedFields(order, existOrder.getStatus());
        }

        // 6. 完成状态额外校验（费用/承担方/备注/完成时间）
        if ("COMPLETED".equals(order.getStatus())) {
            validateCompletedStatusFields(order);
        }

        // 7. 派单状态额外校验（维修师傅/预约时间）
        if ("ASSIGNED".equals(order.getStatus())) {
            validateAssignedStatusFields(order);
        }

        // 8. 执行更新操作（updateTime自动填充）
        return baseMapper.updateById(order) > 0;
    }

    /**
     * 根据ID物理删除维修工单
     *
     * <p>业务限制：仅已提交（SUBMITTED）和已派单（ASSIGNED）状态的工单可删除</p>
     *
     * @param id 维修工单主键ID
     * @param tenantId 租户ID，用于数据隔离验证
     * @return 删除成功返回true，否则返回false
     * @throws IllegalArgumentException 当数据不存在、无权限操作或工单状态不可删除时抛出
     */
    @Override
    public boolean removeMaintenanceOrderById(Long id, Long tenantId) {
        // 1. 基础参数校验
        Assert.notNull(id, "工单ID不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");

        // 2. 校验工单存在且归属当前租户
        MaintenanceOrder existOrder = getMaintenanceOrderById(id, tenantId);
        Assert.notNull(existOrder, "维修工单不存在或无权限操作");

        // 3. 仅已提交/已派单状态可删除
        if (!Set.of("SUBMITTED", "ASSIGNED").contains(existOrder.getStatus())) {
            throw new IllegalArgumentException("仅已提交/已派单状态的工单可删除，当前状态：" + existOrder.getStatus());
        }

        return baseMapper.deleteById(id) > 0;
    }

    /**
     * 根据ID查询维修工单详细信息（租户隔离）
     *
     * <p>查询时自动应用租户隔离条件，确保只能查询到当前租户的数据</p>
     *
     * @param id 维修工单主键ID
     * @param tenantId 租户ID，用于数据隔离
     * @return 符合条件的维修工单实体对象，未找到返回null
     */
    @Override
    public MaintenanceOrder getMaintenanceOrderById(Long id, Long tenantId) {
        Assert.notNull(id, "工单ID不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");

        return baseMapper.selectOne(new LambdaQueryWrapper<MaintenanceOrder>()
                .eq(MaintenanceOrder::getId, id)
                .eq(MaintenanceOrder::getTenantId, tenantId));
    }

    // ==================== 多条件查询实现 ====================

    /**
     * 多条件分页查询维修工单
     *
     * <p>支持以下查询条件：</p>
     * <ul>
     *   <li>房源ID精确查询</li>
     *   <li>合同ID精确查询</li>
     *   <li>房屋交接ID精确查询</li>
     *   <li>工单状态精确查询</li>
     *   <li>维修类型精确查询</li>
     *   <li>报修人类型精确查询</li>
     *   <li>报修人ID精确查询</li>
     *   <li>维修师傅ID精确查询</li>
     *   <li>紧急程度精确查询</li>
     *   <li>创建时间范围查询</li>
     *   <li>完成时间范围查询</li>
     * </ul>
     * <p>排序规则：创建时间倒序 → 紧急程度降序（3=高优先） → 维修中状态优先</p>
     *
     * @param page 分页参数对象，包含页码和每页大小
     * @param queryParams 查询参数映射表，key为字段名，value为查询值
     * @param tenantId 租户ID，用于数据隔离
     * @return 分页查询结果，包含维修工单列表和分页信息
     */
    @Override
    public IPage<MaintenanceOrder> pageQuery(Page<MaintenanceOrder> page, Map<String, Object> queryParams, Long tenantId) {
        Assert.notNull(tenantId, "租户ID不能为空");
        LambdaQueryWrapper<MaintenanceOrder> wrapper = buildQueryWrapper(queryParams, tenantId);

        // 排序逻辑：创建时间倒序 → 紧急程度降序 → 状态升序（REPAIRING优先）
        wrapper.orderByDesc(MaintenanceOrder::getCreateTime)
                .orderByDesc(MaintenanceOrder::getUrgencyLevel)
                .orderByAsc(MaintenanceOrder::getStatus);

        return baseMapper.selectPage(page, wrapper);
    }

    /**
     * 多条件查询维修工单列表（不分页）
     *
     * <p>查询条件与分页查询方法保持一致，但不进行分页处理</p>
     *
     * @param queryParams 查询参数映射表，key为字段名，value为查询值
     * @param tenantId 租户ID，用于数据隔离
     * @return 符合条件的维修工单实体对象列表
     */
    @Override
    public List<MaintenanceOrder> listByConditions(Map<String, Object> queryParams, Long tenantId) {
        Assert.notNull(tenantId, "租户ID不能为空");
        return baseMapper.selectList(buildQueryWrapper(queryParams, tenantId));
    }

    /**
     * 根据房源ID查询维修工单列表
     *
     * <p>按创建时间倒序排列，最新的工单在前</p>
     *
     * @param houseId 房源ID
     * @param tenantId 租户ID，用于数据隔离
     * @return 符合条件的维修工单实体对象列表
     */
    @Override
    public List<MaintenanceOrder> listByHouseId(Long houseId, Long tenantId) {
        Assert.notNull(houseId, "房源ID不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");

        return baseMapper.selectList(new LambdaQueryWrapper<MaintenanceOrder>()
                .eq(MaintenanceOrder::getHouseId, houseId)
                .eq(MaintenanceOrder::getTenantId, tenantId)
                .orderByDesc(MaintenanceOrder::getCreateTime));
    }

    /**
     * 根据报修人信息查询维修工单列表
     *
     * <p>按创建时间倒序排列，最新的工单在前</p>
     *
     * @param reporterId 报修人ID
     * @param reporterType 报修人类型（必须在VALID_REPORTER_TYPES中）
     * @param tenantId 租户ID，用于数据隔离
     * @return 符合条件的维修工单实体对象列表
     * @throws IllegalArgumentException 当报修人类型无效时抛出
     */
    @Override
    public List<MaintenanceOrder> listByReporter(Long reporterId, String reporterType, Long tenantId) {
        Assert.notNull(reporterId, "报修人ID不能为空");
        Assert.notNull(reporterType, "报修人类型不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");
        Assert.isTrue(VALID_REPORTER_TYPES.contains(reporterType), "无效报修人类型：" + reporterType);

        return baseMapper.selectList(new LambdaQueryWrapper<MaintenanceOrder>()
                .eq(MaintenanceOrder::getReporterId, reporterId)
                .eq(MaintenanceOrder::getReporterType, reporterType)
                .eq(MaintenanceOrder::getTenantId, tenantId)
                .orderByDesc(MaintenanceOrder::getCreateTime));
    }

    // ==================== 批量操作实现 ====================

    /**
     * 批量新增维修工单（事务保证）
     *
     * <p>在单个事务中执行批量新增，任一记录校验失败或保存失败将导致整个操作回滚</p>
     * <p>批量校验包括：</p>
     * <ul>
     *   <li>租户一致性校验（所有工单必须属于同一租户）</li>
     *   <li>工单编号唯一性校验（批量内部去重+租户内已存在检查）</li>
     *   <li>实体约束校验（复用单条新增的校验逻辑）</li>
     * </ul>
     *
     * @param orderList 维修工单实体对象列表
     * @return 批量新增成功返回true，否则返回false
     * @throws IllegalArgumentException 当违反任何业务规则时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchSaveMaintenanceOrders(List<MaintenanceOrder> orderList) {
        if (CollectionUtils.isEmpty(orderList)) {
            return false;
        }

        // 1. 校验租户一致性
        Long tenantId = orderList.get(0).getTenantId();
        Assert.notNull(tenantId, "租户ID不能为空");
        boolean hasInvalidTenant = orderList.stream()
                .anyMatch(order -> !Objects.equals(order.getTenantId(), tenantId));
        Assert.isTrue(!hasInvalidTenant, "批量新增的工单必须属于同一租户");

        // 2. 校验工单编号唯一性（批量去重+租户内已存在检查）
        List<String> orderNos = orderList.stream().map(MaintenanceOrder::getOrderNo).collect(Collectors.toList());
        if (orderNos.size() != orderNos.stream().distinct().count()) {
            throw new IllegalArgumentException("批量工单中存在重复的工单编号");
        }
        List<String> existNos = baseMapper.selectList(new LambdaQueryWrapper<MaintenanceOrder>()
                        .select(MaintenanceOrder::getOrderNo)
                        .eq(MaintenanceOrder::getTenantId, tenantId)
                        .in(MaintenanceOrder::getOrderNo, orderNos))
                .stream().map(MaintenanceOrder::getOrderNo).collect(Collectors.toList());
        if (!existNos.isEmpty()) {
            throw new IllegalArgumentException("以下工单编号已存在：" + existNos);
        }

        // 3. 逐条校验实体约束
        for (MaintenanceOrder order : orderList) {
            validateEntityBaseConstraints(order);
            validateSceneRelatedFields(order);
            Assert.isTrue("SUBMITTED".equals(order.getStatus()),
                    "批量新增工单仅允许初始状态为已提交（SUBMITTED），违规编号：" + order.getOrderNo());
        }

        // 执行批量保存（事务保证）
        return saveBatch(orderList);
    }

    /**
     * 批量更新工单状态（事务保证）
     *
     * <p>在单个事务中执行批量状态更新，主要用于批量派单、批量完成等操作</p>
     * <p>校验逻辑：</p>
     * <ul>
     *   <li>所有工单必须属于当前租户</li>
     *   <li>目标状态必须合法</li>
     *   <li>状态流转必须符合规则</li>
     *   <li>完成状态需要额外校验费用、承担方、备注等字段</li>
     * </ul>
     *
     * @param ids 待更新的工单ID列表
     * @param targetStatus 目标状态值，必须在VALID_ORDER_STATUSES中
     * @param tenantId 租户ID，用于数据隔离验证
     * @return 批量更新成功返回true，否则返回false
     * @throws IllegalArgumentException 当违反任何业务规则时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchUpdateOrderStatus(List<Long> ids, String targetStatus, Long tenantId) {
        // 1. 基础参数校验
        Assert.notEmpty(ids, "工单ID列表不能为空");
        Assert.hasText(targetStatus, "目标状态不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");
        Assert.isTrue(VALID_ORDER_STATUSES.contains(targetStatus), "无效目标状态：" + targetStatus);

        // 2. 校验所有工单归属当前租户
        validateOrderIdsBelongToTenant(tenantId, ids);

        // 3. 查询所有工单当前状态，校验流转规则
        List<MaintenanceOrder> orders = baseMapper.selectList(new LambdaQueryWrapper<MaintenanceOrder>()
                .select(MaintenanceOrder::getId, MaintenanceOrder::getStatus)
                .in(MaintenanceOrder::getId, ids));

        List<Long> invalidIds = new ArrayList<>();
        for (MaintenanceOrder order : orders) {
            if (!STATUS_FLOW_RULES.get(order.getStatus()).contains(targetStatus)) {
                invalidIds.add(order.getId());
            }
        }
        Assert.isTrue(invalidIds.isEmpty(),
                "以下工单不允许变更为" + targetStatus + "状态：" + invalidIds);

        // 4. 构建更新实体（状态+联动字段）
        MaintenanceOrder updateEntity = new MaintenanceOrder();
        updateEntity.setStatus(targetStatus);

        // 5. 填充状态联动字段
        fillBatchStatusRelatedFields(updateEntity, targetStatus);

        // 6. 完成状态额外校验
        if ("COMPLETED".equals(targetStatus)) {
            Assert.notNull(updateEntity.getCostAmount(), "批量完成工单必须传入维修费用");
            Assert.notNull(updateEntity.getCostBearer(), "批量完成工单必须传入费用承担方");
            Assert.notNull(updateEntity.getRemark(), "批量完成工单必须传入维修结果备注");
            Assert.isTrue(VALID_COST_BEARERS.contains(updateEntity.getCostBearer()), "无效费用承担方");
            Assert.isTrue(updateEntity.getCostAmount().compareTo(BigDecimal.ZERO) >= 0, "维修费用不能为负数");
        }

        // 7. 执行批量更新
        return baseMapper.update(updateEntity, new LambdaQueryWrapper<MaintenanceOrder>()
                .in(MaintenanceOrder::getId, ids)
                .eq(MaintenanceOrder::getTenantId, tenantId)) > 0;
    }

    /**
     * 批量删除维修工单（事务保证）
     *
     * <p>在单个事务中执行批量删除，任一记录校验失败或删除失败将导致整个操作回滚</p>
     * <p>业务限制：仅已提交（SUBMITTED）和已派单（ASSIGNED）状态的工单可删除</p>
     *
     * @param ids 待删除的工单ID列表
     * @param tenantId 租户ID，用于数据隔离验证
     * @return 批量删除成功返回true，否则返回false
     * @throws IllegalArgumentException 当存在不可删除状态的工单时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchRemoveMaintenanceOrders(List<Long> ids, Long tenantId) {
        // 1. 基础参数校验
        Assert.notEmpty(ids, "工单ID列表不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");

        // 2. 校验所有工单归属当前租户
        validateOrderIdsBelongToTenant(tenantId, ids);

        // 3. 校验所有工单均为可删除状态（已提交/已派单）
        long nonDeletableCount = baseMapper.selectCount(new LambdaQueryWrapper<MaintenanceOrder>()
                .in(MaintenanceOrder::getId, ids)
                .eq(MaintenanceOrder::getTenantId, tenantId)
                .notIn(MaintenanceOrder::getStatus, "SUBMITTED", "ASSIGNED"));
        Assert.isTrue(nonDeletableCount == 0, "存在非已提交/已派单状态的工单，不允许批量删除");

        return baseMapper.deleteBatchIds(ids) > 0;
    }

    /**
     * 验证工单ID列表是否全部属于当前租户
     *
     * <p>两步验证：</p>
     * <ol>
     *   <li>检查ID是否存在（是否存在未查询到的ID）</li>
     *   <li>检查存在的ID是否属于当前租户</li>
     * </ol>
     * <p>验证失败时抛出具体的异常信息，便于定位问题</p>
     *
     * @param tenantId 租户ID
     * @param orderIds 待验证的工单ID列表
     * @throws IllegalArgumentException 当存在不存在的ID或不属于当前租户的ID时抛出
     */
    @Override
    public void validateOrderIdsBelongToTenant(Long tenantId, List<Long> orderIds) {
        if (CollectionUtils.isEmpty(orderIds)) {
            return;
        }

        // 1. 查询存在的工单ID及租户ID
        List<MaintenanceOrder> orders = baseMapper.selectList(new LambdaQueryWrapper<MaintenanceOrder>()
                .select(MaintenanceOrder::getId, MaintenanceOrder::getTenantId)
                .in(MaintenanceOrder::getId, orderIds));

        // 2. 检查不存在的ID
        Set<Long> existingIds = orders.stream().map(MaintenanceOrder::getId).collect(Collectors.toSet());
        List<Long> nonExistentIds = orderIds.stream()
                .filter(id -> !existingIds.contains(id))
                .collect(Collectors.toList());
        Assert.isTrue(nonExistentIds.isEmpty(), "以下工单ID不存在：" + nonExistentIds);

        // 3. 检查租户权限
        List<Long> invalidIds = orders.stream()
                .filter(order -> !Objects.equals(order.getTenantId(), tenantId))
                .map(MaintenanceOrder::getId)
                .collect(Collectors.toList());
        Assert.isTrue(invalidIds.isEmpty(), "无权限操作以下工单ID：" + invalidIds);
    }

    // ==================== 私有工具方法（完全匹配实体类约束） ====================

    /**
     * 构建查询条件（严格匹配实体类字段）
     *
     * <p>根据查询参数动态构建查询条件，支持以下参数：</p>
     * <ul>
     *   <li>houseId - 房源ID精确查询</li>
     *   <li>contractId - 合同ID精确查询</li>
     *   <li>houseHandoverId - 房屋交接ID精确查询</li>
     *   <li>status - 工单状态精确查询</li>
     *   <li>maintenanceType - 维修类型精确查询</li>
     *   <li>reporterType - 报修人类型精确查询</li>
     *   <li>reporterId - 报修人ID精确查询</li>
     *   <li>repairmanId - 维修师傅ID精确查询</li>
     *   <li>urgencyLevel - 紧急程度精确查询</li>
     *   <li>startCreateTime/endCreateTime - 创建时间范围查询</li>
     *   <li>startCompleteTime/endCompleteTime - 完成时间范围查询</li>
     * </ul>
     * <p>所有查询均自动添加租户隔离条件</p>
     *
     * @param queryParams 查询参数映射表
     * @param tenantId 租户ID，用于数据隔离
     * @return 构建完成的LambdaQueryWrapper对象
     */
    private LambdaQueryWrapper<MaintenanceOrder> buildQueryWrapper(Map<String, Object> queryParams, Long tenantId) {
        LambdaQueryWrapper<MaintenanceOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MaintenanceOrder::getTenantId, tenantId);

        if (ObjectUtils.isEmpty(queryParams)) {
            return wrapper;
        }

        // 房源ID精确查询
        if (queryParams.containsKey("houseId") && queryParams.get("houseId") != null) {
            wrapper.eq(MaintenanceOrder::getHouseId, queryParams.get("houseId"));
        }
        // 合同ID精确查询
        if (queryParams.containsKey("contractId") && queryParams.get("contractId") != null) {
            wrapper.eq(MaintenanceOrder::getContractId, queryParams.get("contractId"));
        }
        // 房屋交接ID精确查询
        if (queryParams.containsKey("houseHandoverId") && queryParams.get("houseHandoverId") != null) {
            wrapper.eq(MaintenanceOrder::getHouseHandoverId, queryParams.get("houseHandoverId"));
        }
        // 工单状态精确查询
        if (queryParams.containsKey("status") && queryParams.get("status") != null) {
            wrapper.eq(MaintenanceOrder::getStatus, queryParams.get("status").toString());
        }
        // 维修类型精确查询
        if (queryParams.containsKey("maintenanceType") && queryParams.get("maintenanceType") != null) {
            wrapper.eq(MaintenanceOrder::getMaintenanceType, queryParams.get("maintenanceType").toString());
        }
        // 报修人类型精确查询
        if (queryParams.containsKey("reporterType") && queryParams.get("reporterType") != null) {
            wrapper.eq(MaintenanceOrder::getReporterType, queryParams.get("reporterType").toString());
        }
        // 报修人ID精确查询
        if (queryParams.containsKey("reporterId") && queryParams.get("reporterId") != null) {
            wrapper.eq(MaintenanceOrder::getReporterId, queryParams.get("reporterId"));
        }
        // 维修师傅ID精确查询
        if (queryParams.containsKey("repairmanId") && queryParams.get("repairmanId") != null) {
            wrapper.eq(MaintenanceOrder::getRepairmanId, queryParams.get("repairmanId"));
        }
        // 紧急程度精确查询
        if (queryParams.containsKey("urgencyLevel") && queryParams.get("urgencyLevel") != null) {
            wrapper.eq(MaintenanceOrder::getUrgencyLevel, queryParams.get("urgencyLevel"));
        }
        // 创建时间范围查询
        if (queryParams.containsKey("startCreateTime") && queryParams.get("startCreateTime") != null) {
            wrapper.ge(MaintenanceOrder::getCreateTime, queryParams.get("startCreateTime"));
        }
        if (queryParams.containsKey("endCreateTime") && queryParams.get("endCreateTime") != null) {
            wrapper.le(MaintenanceOrder::getCreateTime, queryParams.get("endCreateTime"));
        }
        // 完成时间范围查询
        if (queryParams.containsKey("startCompleteTime") && queryParams.get("startCompleteTime") != null) {
            wrapper.ge(MaintenanceOrder::getCompleteTime, queryParams.get("startCompleteTime"));
        }
        if (queryParams.containsKey("endCompleteTime") && queryParams.get("endCompleteTime") != null) {
            wrapper.le(MaintenanceOrder::getCompleteTime, queryParams.get("endCompleteTime"));
        }

        return wrapper;
    }

    /**
     * 校验实体类基础约束（NotNull/NotBlank/Pattern/Size/DecimalMin等）
     *
     * <p>完全匹配实体类注解配置，确保数据完整性</p>
     * <p>校验内容包括：</p>
     * <ul>
     *   <li>非空字段校验</li>
     *   <li>字段长度校验</li>
     *   <li>格式校验（手机号格式）</li>
     *   <li>枚举值合法性校验</li>
     *   <li>费用非负校验</li>
     * </ul>
     *
     * @param order 待校验的维修工单实体对象
     * @throws IllegalArgumentException 当任何校验失败时抛出，包含具体的错误信息
     */
    private void validateEntityBaseConstraints(MaintenanceOrder order) {
        // 1. 非空字段校验（匹配实体类@NotNull/@NotBlank）
        Assert.notNull(order.getTenantId(), "租户ID不能为空");
        Assert.notNull(order.getHouseId(), "房源ID不能为空");
        Assert.hasLength(order.getOrderNo(), "工单编号不能为空");
        Assert.hasLength(order.getReporterType(), "报修人类型不能为空");
        Assert.notNull(order.getReporterId(), "报修人ID不能为空");
        Assert.hasLength(order.getReporterPhone(), "报修人电话不能为空");
        Assert.hasLength(order.getMaintenanceType(), "维修类型不能为空");
        Assert.hasLength(order.getDescription(), "故障描述不能为空");
        Assert.notNull(order.getUrgencyLevel(), "紧急程度不能为空");
        Assert.hasLength(order.getStatus(), "工单状态不能为空");

        // 2. 长度校验（匹配实体类@Size）
        Assert.isTrue(order.getOrderNo().length() <= 20, "工单编号长度不能超过20字符");
        Assert.isTrue(order.getReporterPhone().length() <= 20, "报修人电话长度不能超过20字符");
        Assert.isTrue(order.getDescription().length() <= 500, "故障描述长度不能超过500字符");
        if (StringUtils.hasText(order.getRemark())) {
            Assert.isTrue(order.getRemark().length() <= 300, "维修结果备注长度不能超过300字符");
        }

        // 3. 格式校验（匹配实体类@Pattern）
        Assert.isTrue(PHONE_PATTERN.matcher(order.getReporterPhone()).matches(),
                "报修人电话格式错误（支持11位手机号，可带+86前缀）");

        // 4. 枚举值校验（匹配实体类allowableValues）
        Assert.isTrue(VALID_REPORTER_TYPES.contains(order.getReporterType()),
                "无效报修人类型：" + order.getReporterType() + "，允许值：" + VALID_REPORTER_TYPES);
        Assert.isTrue(VALID_MAINTENANCE_TYPES.contains(order.getMaintenanceType()),
                "无效维修类型：" + order.getMaintenanceType() + "，允许值：" + VALID_MAINTENANCE_TYPES);
        Assert.isTrue(VALID_URGENCY_LEVELS.contains(order.getUrgencyLevel()),
                "紧急程度仅支持1（低）、2（中）、3（高）");
        Assert.isTrue(VALID_ORDER_STATUSES.contains(order.getStatus()),
                "无效工单状态：" + order.getStatus() + "，允许值：" + VALID_ORDER_STATUSES);

        // 5. 费用非负校验（匹配实体类@DecimalMin）
        if (order.getCostAmount() != null) {
            Assert.isTrue(order.getCostAmount().compareTo(BigDecimal.ZERO) >= 0, "维修费用不能为负数");
        }

        // 6. 费用承担方枚举校验
        if (StringUtils.hasText(order.getCostBearer())) {
            Assert.isTrue(VALID_COST_BEARERS.contains(order.getCostBearer()),
                    "无效费用承担方：" + order.getCostBearer() + "，允许值：" + VALID_COST_BEARERS);
        }
    }

    /**
     * 校验场景化关联字段（租赁/退租场景）
     *
     * <p>业务规则：</p>
     * <ul>
     *   <li>租赁场景（租户报修）：必须填写合同ID</li>
     *   <li>退租维修场景：必须填写合同ID和房屋交接ID</li>
     * </ul>
     *
     * @param order 待校验的维修工单实体对象
     * @throws IllegalArgumentException 当场景字段不完整时抛出
     */
    private void validateSceneRelatedFields(MaintenanceOrder order) {
        // 租赁场景：contractId必填（报修人类型为租户时，默认是租赁场景）
        if ("TENANT".equals(order.getReporterType()) && order.getContractId() == null) {
            throw new IllegalArgumentException("租户报修（租赁场景）必须填写关联合同ID");
        }

        // 退租维修场景：houseHandoverId必填（可通过业务标识判断，此处简化为有houseHandoverId时校验）
        if (order.getHouseHandoverId() != null && order.getContractId() == null) {
            throw new IllegalArgumentException("退租维修场景必须填写关联合同ID");
        }
    }

    /**
     * 校验租户内工单编号唯一性
     *
     * @param tenantId 租户ID
     * @param orderNo 工单编号
     * @param excludeId 排除的ID（更新场景下排除自身）
     * @throws IllegalArgumentException 当工单编号已存在时抛出
     */
    private void validateOrderNoUnique(Long tenantId, String orderNo, Long excludeId) {
        LambdaQueryWrapper<MaintenanceOrder> wrapper = new LambdaQueryWrapper<MaintenanceOrder>()
                .eq(MaintenanceOrder::getTenantId, tenantId)
                .eq(MaintenanceOrder::getOrderNo, orderNo);
        if (excludeId != null) {
            wrapper.ne(MaintenanceOrder::getId, excludeId);
        }
        long count = baseMapper.selectCount(wrapper);
        if (count > 0) {
            throw new IllegalArgumentException("当前租户下工单编号已存在：" + orderNo);
        }
    }

    /**
     * 校验不可修改的核心字段
     *
     * <p>以下字段在创建后不允许修改：</p>
     * <ul>
     *   <li>房源ID（houseId）</li>
     *   <li>报修人类型（reporterType）</li>
     *   <li>报修人ID（reporterId）</li>
     *   <li>维修类型（maintenanceType）</li>
     *   <li>紧急程度（urgencyLevel）</li>
     * </ul>
     *
     * @param existOrder 数据库中已存在的工单记录
     * @param updateOrder 待更新的工单对象
     * @throws IllegalArgumentException 当尝试修改不可变字段时抛出
     */
    private void validateImmutableFields(MaintenanceOrder existOrder, MaintenanceOrder updateOrder) {
        // 核心关联字段不允许修改
        if (updateOrder.getHouseId() != null && !updateOrder.getHouseId().equals(existOrder.getHouseId())) {
            throw new IllegalArgumentException("房源ID不允许修改");
        }
        if (updateOrder.getReporterType() != null && !updateOrder.getReporterType().equals(existOrder.getReporterType())) {
            throw new IllegalArgumentException("报修人类型不允许修改");
        }
        if (updateOrder.getReporterId() != null && !updateOrder.getReporterId().equals(existOrder.getReporterId())) {
            throw new IllegalArgumentException("报修人ID不允许修改");
        }
        if (updateOrder.getMaintenanceType() != null && !updateOrder.getMaintenanceType().equals(existOrder.getMaintenanceType())) {
            throw new IllegalArgumentException("维修类型不允许修改");
        }
        if (updateOrder.getUrgencyLevel() != null && !updateOrder.getUrgencyLevel().equals(existOrder.getUrgencyLevel())) {
            throw new IllegalArgumentException("紧急程度不允许修改");
        }
    }

    /**
     * 校验状态流转规则
     *
     * <p>根据STATUS_FLOW_RULES定义的规则校验状态变更的合法性</p>
     *
     * @param currentStatus 当前状态
     * @param targetStatus 目标状态
     * @throws IllegalArgumentException 当状态流转不合法时抛出
     */
    private void validateStatusFlow(String currentStatus, String targetStatus) {
        if (!STATUS_FLOW_RULES.get(currentStatus).contains(targetStatus)) {
            throw new IllegalArgumentException(
                    "工单状态不允许从" + currentStatus + "变更为" + targetStatus +
                            "，允许的目标状态：" + STATUS_FLOW_RULES.get(currentStatus)
            );
        }
    }

    /**
     * 填充状态联动字段（单个工单）
     *
     * <p>根据状态变更自动设置相关字段：</p>
     * <ul>
     *   <li>ASSIGNED状态：自动填充预约时间（如未传则使用当前时间）</li>
     *   <li>COMPLETED状态：自动填充完成时间</li>
     *   <li>CANCELED状态：清空派单/维修相关字段</li>
     * </ul>
     *
     * @param order 待更新的工单对象
     * @param oldStatus 旧状态（用于判断是否发生了状态变更）
     */
    private void fillStatusRelatedFields(MaintenanceOrder order, String oldStatus) {
        String newStatus = order.getStatus();
        switch (newStatus) {
            case "ASSIGNED":
                // 已派单：自动填充预约时间（未传则填当前时间）
                if (order.getAppointmentTime() == null) {
                    order.setAppointmentTime(LocalDateTime.now());
                }
                break;
            case "COMPLETED":
                // 已完成：自动填充完成时间
                if (order.getCompleteTime() == null) {
                    order.setCompleteTime(LocalDateTime.now());
                }
                break;
            case "CANCELED":
                // 已取消：清空派单/维修相关字段
                order.setRepairmanId(null);
                order.setAppointmentTime(null);
                order.setCompleteTime(null);
                order.setCostAmount(null);
                order.setCostBearer(null);
                order.setRemark(null);
                break;
            default:
                break;
        }
    }

    /**
     * 批量填充状态联动字段
     *
     * <p>用于批量更新状态时的字段自动填充</p>
     *
     * @param updateEntity 批量更新实体
     * @param targetStatus 目标状态
     */
    private void fillBatchStatusRelatedFields(MaintenanceOrder updateEntity, String targetStatus) {
        switch (targetStatus) {
            case "ASSIGNED":
                updateEntity.setAppointmentTime(LocalDateTime.now());
                break;
            case "COMPLETED":
                updateEntity.setCompleteTime(LocalDateTime.now());
                break;
            case "CANCELED":
                updateEntity.setRepairmanId(null);
                updateEntity.setAppointmentTime(null);
                updateEntity.setCompleteTime(null);
                updateEntity.setCostAmount(null);
                updateEntity.setCostBearer(null);
                updateEntity.setRemark(null);
                break;
            default:
                break;
        }
    }

    /**
     * 校验已完成状态的必填字段
     *
     * <p>COMPLETED状态必须包含以下字段：</p>
     * <ul>
     *   <li>完成时间（completeTime）</li>
     *   <li>维修费用（costAmount）</li>
     *   <li>费用承担方（costBearer）</li>
     *   <li>维修结果备注（remark）</li>
     * </ul>
     *
     * @param order 待校验的工单对象
     * @throws IllegalArgumentException 当必填字段缺失时抛出
     */
    private void validateCompletedStatusFields(MaintenanceOrder order) {
        Assert.notNull(order.getCompleteTime(), "已完成状态必须填写完成时间");
        Assert.notNull(order.getCostAmount(), "已完成状态必须填写维修费用");
        Assert.hasLength(order.getCostBearer(), "已完成状态必须填写费用承担方");
        Assert.hasLength(order.getRemark(), "已完成状态必须填写维修结果备注");
        Assert.isTrue(VALID_COST_BEARERS.contains(order.getCostBearer()), "无效费用承担方");
    }

    /**
     * 校验已派单状态的必填字段
     *
     * <p>ASSIGNED状态必须包含以下字段：</p>
     * <ul>
     *   <li>维修师傅ID（repairmanId）</li>
     *   <li>预约维修时间（appointmentTime）</li>
     * </ul>
     *
     * @param order 待校验的工单对象
     * @throws IllegalArgumentException 当必填字段缺失时抛出
     */
    private void validateAssignedStatusFields(MaintenanceOrder order) {
        Assert.notNull(order.getRepairmanId(), "已派单状态必须填写维修师傅ID");
        Assert.notNull(order.getAppointmentTime(), "已派单状态必须填写预约维修时间");
    }
}
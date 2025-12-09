package com.house.deed.pavilion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.entity.DisputeHandleLog;
import com.house.deed.pavilion.mapper.DisputeHandleLogMapper;
import com.house.deed.pavilion.service.DisputeHandleLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

/**
 * <p>
 * 纠纷处理日志表（租户级数据）服务实现类
 * </p>
 *
 * <p>
 * 本服务类实现纠纷处理日志的完整生命周期管理，记录纠纷状态变更的完整审计轨迹。
 * 作为关键的审计跟踪表，本服务强调数据的不可篡改性，确保纠纷处理过程的透明和可追溯。
 * </p>
 *
 * <p><b>核心设计原则：</b></p>
 * <ul>
 *   <li><b>审计完整性</b>：记录纠纷状态变更的完整轨迹，形成不可篡改的审计日志</li>
 *   <li><b>租户隔离</b>：所有操作强制校验租户归属，确保多租户数据安全</li>
 *   <li><b>数据不可变性</b>：关键审计字段（状态变更、处理时间等）在更新时禁止修改</li>
 *   <li><b>时间顺序性</b>：所有日志按处理时间倒序排列，确保最新处理记录在前</li>
 * </ul>
 *
 * <p><b>主要功能模块：</b></p>
 * <ul>
 *   <li>纠纷处理日志的创建、查询和删除</li>
 *   <li>按纠纷ID查询完整处理历史</li>
 *   <li>多条件组合查询与分页展示</li>
 *   <li>批量日志操作支持</li>
 * </ul>
 *
 * <p><b>关键业务规则：</b></p>
 * <ul>
 *   <li>处理前后状态必须不同，确保日志的变更价值</li>
 *   <li>处理时间必须准确记录，用于追踪处理时效</li>
 *   <li>更新操作禁止修改核心审计字段，保持审计完整性</li>
 *   <li>批量操作要求所有记录属于同一租户</li>
 * </ul>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Service
public class DisputeHandleLogServiceImpl extends ServiceImpl<DisputeHandleLogMapper, DisputeHandleLog> implements DisputeHandleLogService {

    /**
     * 新增纠纷处理日志
     *
     * <p><b>业务规则：</b></p>
     * <ol>
     *   <li>租户ID、纠纷ID、处理时间、处理人ID、处理人姓名为必填字段</li>
     *   <li>处理内容和处理前后状态必须非空，记录完整的变更信息</li>
     *   <li>处理前后状态必须不同，确保日志记录了有效的状态变更</li>
     *   <li>系统会自动记录创建时间和主键ID</li>
     * </ol>
     *
     * <p><b>审计要求：</b></p>
     * <ul>
     *   <li>每条日志都构成纠纷处理历史的一部分，必须完整准确</li>
     *   <li>处理时间应记录实际操作时间，用于后续时效分析</li>
     *   <li>处理内容应详细描述处理措施和依据</li>
     * </ul>
     *
     * @param log 纠纷处理日志实体，必须包含所有必填字段
     * @return 保存成功返回true，失败返回false或抛出异常
     * @throws IllegalArgumentException 当参数校验失败时抛出
     */
    @Override
    public boolean saveLog(DisputeHandleLog log) {
        // 基础字段校验：租户ID和纠纷ID等核心审计字段
        Assert.notNull(log.getTenantId(), "租户ID不能为空");
        Assert.notNull(log.getDisputeId(), "纠纷ID不能为空");
        Assert.notNull(log.getHandleTime(), "处理时间不能为空");
        Assert.notNull(log.getHandlerId(), "处理人ID不能为空");
        Assert.hasText(log.getHandlerName(), "处理人姓名不能为空");
        Assert.hasText(log.getHandleContent(), "处理内容不能为空");
        Assert.hasText(log.getStatusBefore(), "处理前状态不能为空");
        Assert.hasText(log.getStatusAfter(), "处理后状态不能为空");
        // 业务规则校验：处理前后状态必须不同，确保记录有效的状态变更
        Assert.isTrue(!log.getStatusBefore().equals(log.getStatusAfter()),
                "处理前后状态不能相同");

        return save(log);
    }

    /**
     * 更新纠纷处理日志
     *
     * <p><b>安全与审计机制：</b></p>
     * <ol>
     *   <li>验证日志ID和租户ID不能为空</li>
     *   <li>查询数据库确认日志存在性</li>
     *   <li>验证当前租户是否有权限操作该日志</li>
     *   <li><b>强制保护核心审计字段</b>：禁止修改状态变更、纠纷ID、处理时间等关键字段</li>
     * </ol>
     *
     * <p><b>设计理念：</b></p>
     * <ul>
     *   <li>纠纷处理日志作为审计记录，核心内容一旦创建不可更改</li>
     *   <li>允许更新的是非核心字段（如备注、附件等补充信息）</li>
     *   <li>通过技术手段防止人为篡改处理历史</li>
     * </ul>
     *
     * @param log 纠纷处理日志实体，必须包含ID和租户ID
     * @return 更新成功返回true，失败返回false或抛出异常
     * @throws IllegalArgumentException 当参数校验失败时抛出
     * @throws IllegalStateException 当权限验证失败时抛出
     */
    @Override
    public boolean updateLogById(DisputeHandleLog log) {
        // 基础参数校验
        Assert.notNull(log.getId(), "日志ID不能为空");
        Assert.notNull(log.getTenantId(), "租户ID不能为空");

        // 权限校验：验证日志存在且属于当前租户
        DisputeHandleLog exist = getById(log.getId());
        Assert.notNull(exist, "纠纷处理日志不存在");
        Assert.isTrue(Objects.equals(exist.getTenantId(), log.getTenantId()),
                "无权限操作此日志，日志属于其他租户");

        // 审计保护：强制设置核心审计字段为原始值，防止篡改
        log.setStatusBefore(exist.getStatusBefore());
        log.setStatusAfter(exist.getStatusAfter());
        log.setDisputeId(exist.getDisputeId());
        log.setHandleTime(exist.getHandleTime());

        // 执行更新操作（仅更新非保护字段）
        return updateById(log);
    }

    /**
     * 删除纠纷处理日志
     *
     * <p><b>风险提示：</b>纠纷处理日志作为重要的审计记录，删除操作需谨慎执行。</p>
     *
     * <p><b>安全机制：</b></p>
     * <ul>
     *   <li>双重验证：先验证日志存在，再验证租户归属</li>
     *   <li>防止误删其他租户的审计日志</li>
     *   <li>建议在实际业务中限制删除权限</li>
     * </ul>
     *
     * <p><b>业务影响：</b></p>
     * <ul>
     *   <li>物理删除，数据无法恢复</li>
     *   <li>可能导致纠纷处理历史不完整</li>
     *   <li>影响后续审计和问题追溯</li>
     * </ul>
     *
     * @param id 日志主键ID
     * @param tenantId 当前操作租户ID
     * @return 删除成功返回true，失败返回false或抛出异常
     * @throws IllegalArgumentException 当参数校验失败时抛出
     * @throws IllegalStateException 当权限验证失败时抛出
     */
    @Override
    public boolean removeLogById(Long id, Long tenantId) {
        // 参数校验
        Assert.notNull(id, "日志ID不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");

        // 存在性校验和权限校验
        DisputeHandleLog exist = getById(id);
        Assert.notNull(exist, "纠纷处理日志不存在");
        Assert.isTrue(Objects.equals(exist.getTenantId(), tenantId),
                "无权限操作此日志，日志属于其他租户");

        // 执行删除操作
        return removeById(id);
    }

    /**
     * 根据ID查询纠纷处理日志（租户隔离版本）
     *
     * <p>提供安全的单记录查询，自动添加租户ID条件，确保不会查询到其他租户的审计日志。</p>
     *
     * <p><b>设计特点：</b></p>
     * <ul>
     *   <li>返回null而不是抛出异常，符合查询操作的预期行为</li>
     *   <li>适用于查看单条处理记录的详细内容</li>
     *   <li>查询条件同时包含ID和租户ID，确保数据隔离</li>
     * </ul>
     *
     * @param id 日志主键ID
     * @param tenantId 当前操作租户ID
     * @return 符合条件的纠纷处理日志，如果不存在或不属于当前租户则返回null
     * @throws IllegalArgumentException 当参数为空时抛出
     */
    @Override
    public DisputeHandleLog getLogById(Long id, Long tenantId) {
        // 参数校验
        Assert.notNull(id, "日志ID不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");

        // 构建查询条件：ID匹配且租户ID匹配
        return getOne(new LambdaQueryWrapper<DisputeHandleLog>()
                .eq(DisputeHandleLog::getId, id)
                .eq(DisputeHandleLog::getTenantId, tenantId));
    }

    /**
     * 多条件分页查询纠纷处理日志
     *
     * <p>支持按纠纷ID、处理前后状态、处理人、处理时间、处理内容等条件组合查询。</p>
     *
     * <p><b>查询特性：</b></p>
     * <ul>
     *   <li>结果按处理时间倒序排列（最新的处理记录在前）</li>
     *   <li>处理内容支持模糊查询，便于检索关键信息</li>
     *   <li>处理时间条件为"大于等于"查询，用于筛选某个时间点之后的处理记录</li>
     *   <li>强制租户隔离，不会查询到其他租户数据</li>
     * </ul>
     *
     * <p><b>使用场景：</b></p>
     * <ul>
     *   <li>纠纷处理历史查看页面</li>
     *   <li>处理效率统计分析</li>
     *   <li>审计和合规检查</li>
     * </ul>
     *
     * @param page MyBatis-Plus分页参数，包含页码、每页条数等信息
     * @param query 查询条件实体，tenantId字段必须非空
     * @return 分页查询结果，包含纠纷处理日志列表和分页信息
     * @throws IllegalArgumentException 当tenantId为空时抛出
     */
    @Override
    public IPage<DisputeHandleLog> pageQuery(Page<DisputeHandleLog> page, DisputeHandleLog query) {
        // 租户ID必须非空，确保数据隔离
        Assert.notNull(query.getTenantId(), "租户ID不能为空");

        // 构建查询条件并执行分页查询
        LambdaQueryWrapper<DisputeHandleLog> wrapper = buildQueryWrapper(query);
        return baseMapper.selectPage(page, wrapper);
    }

    /**
     * 多条件列表查询纠纷处理日志
     *
     * <p>与分页查询使用相同的查询条件构建逻辑，但不进行分页，返回所有匹配记录。</p>
     *
     * <p><b>性能考虑：</b>纠纷处理日志可能随时间增长，建议对时间范围加以限制。</p>
     *
     * <p><b>使用场景：</b></p>
     * <ul>
     *   <li>导出纠纷处理历史到Excel</li>
     *   <li>生成审计报告</li>
     *   <li>批量数据分析和处理</li>
     * </ul>
     *
     * @param query 查询条件实体，tenantId字段必须非空
     * @return 符合条件的纠纷处理日志列表，如果没有匹配记录则返回空列表
     * @throws IllegalArgumentException 当tenantId为空时抛出
     */
    @Override
    public List<DisputeHandleLog> listByConditions(DisputeHandleLog query) {
        // 租户ID必须非空，确保数据隔离
        Assert.notNull(query.getTenantId(), "租户ID不能为空");

        // 构建查询条件并执行查询
        LambdaQueryWrapper<DisputeHandleLog> wrapper = buildQueryWrapper(query);
        return baseMapper.selectList(wrapper);
    }

    /**
     * 根据纠纷ID查询处理日志
     *
     * <p>提供便捷的接口，用于查询特定纠纷的完整处理历史。</p>
     *
     * <p><b>业务价值：</b></p>
     * <ul>
     *   <li>完整展示纠纷从创建到解决的全过程</li>
     *   <li>帮助分析处理效率和问题点</li>
     *   <li>为后续类似纠纷提供处理参考</li>
     * </ul>
     *
     * <p><b>排序规则：</b>按处理时间倒序排列，最新的处理记录在前。</p>
     *
     * @param disputeId 纠纷ID
     * @param tenantId 当前操作租户ID
     * @return 该纠纷的所有处理日志列表，按处理时间倒序排列
     * @throws IllegalArgumentException 当参数为空时抛出
     */
    @Override
    public List<DisputeHandleLog> listByDisputeId(Long disputeId, Long tenantId) {
        // 参数校验
        Assert.notNull(disputeId, "纠纷ID不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");

        // 构建查询条件：指定租户和纠纷
        DisputeHandleLog query = new DisputeHandleLog();
        query.setTenantId(tenantId);
        query.setDisputeId(disputeId);
        return listByConditions(query);
    }

    /**
     * 批量新增纠纷处理日志
     *
     * <p><b>业务规则：</b></p>
     * <ol>
     *   <li>批量记录必须属于同一租户，防止数据混乱</li>
     *   <li>每条记录都需要进行核心字段校验</li>
     *   <li>每条记录的处理前后状态必须不同</li>
     *   <li>使用数据库事务保证原子性：要么全部成功，要么全部失败</li>
     * </ol>
     *
     * <p><b>使用场景：</b></p>
     * <ul>
     *   <li>批量导入历史处理记录</li>
     *   <li>系统间数据迁移</li>
     *   <li>批量生成测试数据</li>
     * </ul>
     *
     * <p><b>性能建议：</b>建议单次批量操作不超过500条记录。</p>
     *
     * @param logList 待新增的纠纷处理日志列表
     * @return 保存成功返回true，失败时抛出异常并回滚
     * @throws IllegalArgumentException 当校验失败时抛出
     * @Transactional 声明式事务，异常时自动回滚所有操作
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchSaveLogs(List<DisputeHandleLog> logList) {
        // 空列表快速返回，避免不必要的处理
        if (CollectionUtils.isEmpty(logList)) {
            return true;
        }

        // 租户一致性校验：批量记录必须属于同一租户
        Long tenantId = logList.get(0).getTenantId();
        Assert.notNull(tenantId, "租户ID不能为空");
        boolean hasInvalidTenant = logList.stream()
                .anyMatch(log -> !Objects.equals(log.getTenantId(), tenantId));
        Assert.isTrue(!hasInvalidTenant, "批量日志必须属于同一租户，存在不一致的租户ID");

        // 逐条记录校验核心业务字段
        for (DisputeHandleLog log : logList) {
            Assert.notNull(log.getDisputeId(), "纠纷ID不能为空");
            Assert.notNull(log.getHandleTime(), "处理时间不能为空");
            Assert.hasText(log.getStatusBefore(), "处理前状态不能为空");
            Assert.hasText(log.getStatusAfter(), "处理后状态不能为空");
            // 业务规则校验：处理前后状态必须不同
            Assert.isTrue(!log.getStatusBefore().equals(log.getStatusAfter()),
                    "处理前后状态不能相同，日志ID: " + log.getId());
        }

        // 批量保存
        return saveBatch(logList);
    }

    /**
     * 批量删除纠纷处理日志
     *
     * <p><b>风险提示：</b>批量删除审计日志是高危操作，应在严格管控下执行。</p>
     *
     * <p><b>安全机制：</b></p>
     * <ul>
     *   <li>检查所有待删除记录是否都属于当前租户</li>
     *   <li>使用数据库事务保证原子性</li>
     *   <li>防止批量删除时误删其他租户数据</li>
     * </ul>
     *
     * <p><b>适用场景：</b></p>
     * <ul>
     *   <li>数据归档前的清理</li>
     *   <li>测试数据清除</li>
     *   <li>合规要求下的数据删除</li>
     * </ul>
     *
     * @param ids 要删除的日志ID列表
     * @param tenantId 当前操作租户ID
     * @return 删除成功返回true，失败返回false或抛出异常
     * @throws IllegalArgumentException 当参数校验失败时抛出
     * @Transactional 声明式事务，异常时自动回滚
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchRemoveLogs(List<Long> ids, Long tenantId) {
        // 参数校验
        Assert.notNull(tenantId, "租户ID不能为空");
        Assert.notEmpty(ids, "日志ID列表不能为空");

        // 权限校验：确保所有待删除日志都属于当前租户
        long invalidCount = baseMapper.selectCount(new LambdaQueryWrapper<DisputeHandleLog>()
                .in(DisputeHandleLog::getId, ids)
                .ne(DisputeHandleLog::getTenantId, tenantId));
        Assert.isTrue(invalidCount == 0, "存在跨租户日志ID，无法删除");

        // 执行批量删除
        return removeByIds(ids);
    }

    /**
     * 构建动态查询条件包装器
     *
     * <p><b>私有方法，用于统一查询条件构建逻辑</b></p>
     *
     * <p>本方法根据传入的查询条件实体动态构建查询条件：</p>
     * <ol>
     *   <li>强制添加租户ID等于条件，确保数据隔离</li>
     *   <li>根据query对象的字段值动态添加其他查询条件</li>
     *   <li>设置默认排序：按处理时间倒序（最新处理记录在前）</li>
     * </ol>
     *
     * <p><b>条件类型说明：</b></p>
     * <ul>
     *   <li>纠纷ID：精确查询，用于查看特定纠纷的处理历史</li>
     *   <li>处理前后状态：精确匹配，用于分析状态流转</li>
     *   <li>处理人ID：精确匹配，用于统计个人处理工作量</li>
     *   <li>处理时间：大于等于查询，用于时效分析和时间范围筛选</li>
     *   <li>处理内容：模糊查询，便于检索特定处理措施</li>
     * </ul>
     *
     * @param query 查询条件实体，tenantId字段必须非空
     * @return 构建完成的LambdaQueryWrapper对象
     */
    private LambdaQueryWrapper<DisputeHandleLog> buildQueryWrapper(DisputeHandleLog query) {
        LambdaQueryWrapper<DisputeHandleLog> wrapper = new LambdaQueryWrapper<>();

        // 强制租户隔离：必须条件，确保查询结果仅限当前租户
        wrapper.eq(DisputeHandleLog::getTenantId, query.getTenantId());

        // 动态条件构建：根据传入的查询参数添加相应条件
        if (query.getDisputeId() != null) {
            wrapper.eq(DisputeHandleLog::getDisputeId, query.getDisputeId()); // 纠纷ID精确查询
        }
        if (StringUtils.hasText(query.getStatusBefore())) {
            wrapper.eq(DisputeHandleLog::getStatusBefore, query.getStatusBefore()); // 处理前状态筛选
        }
        if (StringUtils.hasText(query.getStatusAfter())) {
            wrapper.eq(DisputeHandleLog::getStatusAfter, query.getStatusAfter()); // 处理后状态筛选
        }
        if (query.getHandlerId() != null) {
            wrapper.eq(DisputeHandleLog::getHandlerId, query.getHandlerId()); // 处理人ID筛选
        }
        if (query.getHandleTime() != null) {
            wrapper.ge(DisputeHandleLog::getHandleTime, query.getHandleTime()); // 处理时间筛选（大于等于）
        }
        if (StringUtils.hasText(query.getHandleContent())) {
            wrapper.like(DisputeHandleLog::getHandleContent, query.getHandleContent()); // 处理内容模糊查询
        }

        // 默认排序：按处理时间倒序，确保最新的处理记录在前
        wrapper.orderByDesc(DisputeHandleLog::getHandleTime);
        return wrapper;
    }
}
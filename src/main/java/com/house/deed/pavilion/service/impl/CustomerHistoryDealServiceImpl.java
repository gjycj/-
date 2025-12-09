package com.house.deed.pavilion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.entity.CustomerHistoryDeal;
import com.house.deed.pavilion.mapper.CustomerHistoryDealMapper;
import com.house.deed.pavilion.service.CustomerHistoryDealService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

/**
 * <p>
 * 客户历史成交记录表（租户级数据） 服务实现类
 * </p>
 *
 * <p>
 * 本服务类实现客户历史成交记录的所有业务操作，包括增删改查和批量操作。
 * 所有操作均强制进行租户数据隔离，确保数据安全性和多租户系统的数据独立性。
 * </p>
 *
 * <p><b>核心特性：</b></p>
 * <ul>
 *   <li>数据隔离：所有查询和操作均强制校验租户ID，防止跨租户数据访问</li>
 *   <li>参数校验：使用Spring Assert进行严格的前置参数校验，确保数据完整性</li>
 *   <li>业务规则：对成交类型等关键字段进行枚举值校验</li>
 *   <li>批量操作：支持批量新增和删除，包含租户一致性校验</li>
 * </ul>
 *
 * <p><b>重要约束：</b></p>
 * <ul>
 *   <li>成交类型(dealType)只允许"SALE"(买卖)或"RENT"(租赁)两种值</li>
 *   <li>批量操作时，所有记录必须属于同一租户</li>
 *   <li>更新和删除操作会验证记录的原始归属，防止越权操作</li>
 * </ul>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Service
public class CustomerHistoryDealServiceImpl extends ServiceImpl<CustomerHistoryDealMapper, CustomerHistoryDeal>
        implements CustomerHistoryDealService {

    /**
     * 新增单条成交记录
     *
     * <p><b>业务规则：</b></p>
     * <ul>
     *   <li>强制要求租户ID、客户ID、合同ID、成交日期、房源信息、成交类型等核心字段非空</li>
     *   <li>成交类型必须为"SALE"(买卖)或"RENT"(租赁)，通过枚举值校验保证数据规范性</li>
     *   <li>记录将按MyBatis-Plus的默认规则自动生成主键ID和创建时间</li>
     * </ul>
     *
     * @param historyDeal 待新增的成交记录实体，必须包含所有必填字段
     * @return 保存成功返回true，失败返回false或抛出异常
     * @throws IllegalArgumentException 当参数校验失败时抛出，包含具体的错误信息
     */
    @Override
    public boolean saveHistoryDeal(CustomerHistoryDeal historyDeal) {
        // 第一层校验：租户ID和核心业务字段非空校验
        Assert.notNull(historyDeal.getTenantId(), "租户ID不能为空");
        Assert.notNull(historyDeal.getCustomerId(), "客户ID不能为空");
        Assert.notNull(historyDeal.getContractId(), "合同ID不能为空");
        Assert.notNull(historyDeal.getDealTime(), "成交日期不能为空");
        Assert.hasText(historyDeal.getHouseInfo(), "成交房源信息不能为空");
        Assert.hasText(historyDeal.getDealType(), "成交类型不能为空");

        // 第二层校验：成交类型枚举值校验，确保数据规范性
        Assert.isTrue("SALE".equals(historyDeal.getDealType()) || "RENT".equals(historyDeal.getDealType()),
                "成交类型必须为SALE（买卖）或RENT（租赁）");

        // 调用MyBatis-Plus的保存方法，返回操作结果
        return save(historyDeal);
    }

    /**
     * 根据ID更新成交记录
     *
     * <p><b>安全机制：</b></p>
     * <ul>
     *   <li>首先校验记录ID和租户ID非空</li>
     *   <li>查询数据库确认记录存在，防止更新不存在的记录</li>
     *   <li>验证请求租户ID与记录原始租户ID一致，防止跨租户越权操作</li>
     *   <li>如果更新成交类型字段，会重新校验枚举值</li>
     * </ul>
     *
     * @param historyDeal 待更新的成交记录实体，必须包含ID和租户ID
     * @return 更新成功返回true，失败返回false或抛出异常
     * @throws IllegalArgumentException 当参数校验或权限校验失败时抛出
     * @throws IllegalStateException 当记录不存在时抛出
     */
    @Override
    public boolean updateHistoryDealById(CustomerHistoryDeal historyDeal) {
        // 基本参数校验：记录ID和租户ID不能为空
        Assert.notNull(historyDeal.getId(), "记录ID不能为空");
        Assert.notNull(historyDeal.getTenantId(), "租户ID不能为空");

        // 权限校验：查询记录并验证租户归属
        CustomerHistoryDeal exist = getById(historyDeal.getId());
        Assert.notNull(exist, "成交记录不存在");
        Assert.isTrue(Objects.equals(exist.getTenantId(), historyDeal.getTenantId()),
                "无权限操作此记录，记录属于其他租户");

        // 业务规则校验：如果更新了成交类型，需要重新校验枚举值
        if (StringUtils.hasText(historyDeal.getDealType())) {
            Assert.isTrue("SALE".equals(historyDeal.getDealType()) || "RENT".equals(historyDeal.getDealType()),
                    "成交类型必须为SALE（买卖）或RENT（租赁）");
        }

        // 执行更新操作
        return updateById(historyDeal);
    }

    /**
     * 根据ID删除成交记录
     *
     * <p><b>安全机制：</b></p>
     * <ul>
     *   <li>双重验证：先校验记录存在，再校验租户归属</li>
     *   <li>防止删除不存在或不属于当前租户的记录</li>
     *   <li>物理删除操作，数据将被永久移除（如有需要可考虑逻辑删除）</li>
     * </ul>
     *
     * @param id 要删除的记录主键ID
     * @param tenantId 当前操作租户ID，用于权限验证
     * @return 删除成功返回true，失败返回false或抛出异常
     * @throws IllegalArgumentException 当参数校验失败时抛出
     * @throws IllegalStateException 当权限验证失败时抛出
     */
    @Override
    public boolean removeHistoryDealById(Long id, Long tenantId) {
        // 参数校验
        Assert.notNull(id, "记录ID不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");

        // 存在性校验和权限校验
        CustomerHistoryDeal exist = getById(id);
        Assert.notNull(exist, "成交记录不存在");
        Assert.isTrue(Objects.equals(exist.getTenantId(), tenantId),
                "无权限操作此记录，记录属于其他租户");

        // 执行删除操作
        return removeById(id);
    }

    /**
     * 根据ID查询成交记录（租户隔离版本）
     *
     * <p>本方法提供安全的单记录查询，自动添加租户ID条件，确保不会查询到其他租户的数据。</p>
     *
     * <p><b>注意：</b>如果ID存在但不属于当前租户，将返回null而不是抛出异常，这是查询操作的特殊设计。</p>
     *
     * @param id 要查询的记录主键ID
     * @param tenantId 当前操作租户ID
     * @return 符合条件的成交记录，如果不存在或不属于当前租户则返回null
     * @throws IllegalArgumentException 当参数为空时抛出
     */
    @Override
    public CustomerHistoryDeal getHistoryDealById(Long id, Long tenantId) {
        // 参数校验
        Assert.notNull(id, "记录ID不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");

        // 构建查询条件：ID匹配且租户ID匹配
        return getOne(new LambdaQueryWrapper<CustomerHistoryDeal>()
                .eq(CustomerHistoryDeal::getId, id)
                .eq(CustomerHistoryDeal::getTenantId, tenantId));
    }

    /**
     * 多条件分页查询成交记录
     *
     * <p>支持根据客户ID、合同ID、成交日期范围、成交类型、房源信息等进行组合查询。</p>
     *
     * <p><b>查询特性：</b></p>
     * <ul>
     *   <li>结果按成交时间倒序排列（最新的记录在前）</li>
     *   <li>房源信息支持模糊查询</li>
     *   <li>成交日期条件为"大于等于"查询</li>
     *   <li>强制租户隔离，不会查询到其他租户数据</li>
     * </ul>
     *
     * @param page MyBatis-Plus分页参数，包含页码、每页条数等信息
     * @param query 查询条件实体，tenantId字段必须非空
     * @return 分页查询结果，包含数据列表和分页信息
     * @throws IllegalArgumentException 当tenantId为空时抛出
     */
    @Override
    public IPage<CustomerHistoryDeal> pageQuery(Page<CustomerHistoryDeal> page, CustomerHistoryDeal query) {
        // 租户ID必须非空，确保数据隔离
        Assert.notNull(query.getTenantId(), "租户ID不能为空");

        // 构建查询条件并执行分页查询
        LambdaQueryWrapper<CustomerHistoryDeal> wrapper = buildQueryWrapper(query);
        return baseMapper.selectPage(page, wrapper);
    }

    /**
     * 多条件列表查询成交记录
     *
     * <p>与分页查询使用相同的查询条件构建逻辑，但不进行分页，返回所有匹配记录。</p>
     *
     * <p><b>使用场景：</b></p>
     * <ul>
     *   <li>导出数据</li>
     *   <li>统计报表</li>
     *   <li>需要获取所有匹配记录的场景</li>
     * </ul>
     *
     * @param query 查询条件实体，tenantId字段必须非空
     * @return 符合条件的成交记录列表，如果没有匹配记录则返回空列表
     * @throws IllegalArgumentException 当tenantId为空时抛出
     */
    @Override
    public List<CustomerHistoryDeal> listByConditions(CustomerHistoryDeal query) {
        // 租户ID必须非空，确保数据隔离
        Assert.notNull(query.getTenantId(), "租户ID不能为空");

        // 构建查询条件并执行查询
        LambdaQueryWrapper<CustomerHistoryDeal> wrapper = buildQueryWrapper(query);
        return baseMapper.selectList(wrapper);
    }

    /**
     * 根据客户ID查询其所有成交记录
     *
     * <p>用于查看特定客户的历史成交情况。</p>
     *
     * @param customerId 客户ID
     * @param tenantId 租户ID
     * @return 该客户的所有成交记录列表，按成交时间倒序排列
     * @throws IllegalArgumentException 当参数为空时抛出
     */
    @Override
    public List<CustomerHistoryDeal> listByCustomerId(Long customerId, Long tenantId) {
        // 参数校验
        Assert.notNull(customerId, "客户ID不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");

        // 构建查询条件：指定租户和客户
        CustomerHistoryDeal query = new CustomerHistoryDeal();
        query.setTenantId(tenantId);
        query.setCustomerId(customerId);
        return listByConditions(query);
    }

    /**
     * 根据合同ID查询成交记录
     *
     * <p>用于查看特定合同对应的成交记录。</p>
     *
     * @param contractId 合同ID
     * @param tenantId 租户ID
     * @return 该合同对应的成交记录列表，通常为单条记录
     * @throws IllegalArgumentException 当参数为空时抛出
     */
    @Override
    public List<CustomerHistoryDeal> listByContractId(Long contractId, Long tenantId) {
        // 参数校验
        Assert.notNull(contractId, "合同ID不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");

        // 构建查询条件：指定租户和合同
        CustomerHistoryDeal query = new CustomerHistoryDeal();
        query.setTenantId(tenantId);
        query.setContractId(contractId);
        return listByConditions(query);
    }

    /**
     * 批量新增成交记录
     *
     * <p><b>业务规则：</b></p>
     * <ul>
     *   <li>批量记录必须属于同一租户，防止数据混乱</li>
     *   <li>每条记录都需要进行完整的字段校验</li>
     *   <li>使用数据库事务保证操作的原子性：要么全部成功，要么全部失败</li>
     * </ul>
     *
     * <p><b>性能考虑：</b>适用于中小批量数据导入，不建议单次操作超过1000条记录。</p>
     *
     * @param historyDealList 待新增的成交记录列表
     * @return 保存成功返回true，失败时抛出异常并回滚
     * @throws IllegalArgumentException 当校验失败时抛出
     * @Transactional 声明式事务，异常时自动回滚
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchSaveHistoryDeals(List<CustomerHistoryDeal> historyDealList) {
        // 空列表快速返回
        if (CollectionUtils.isEmpty(historyDealList)) {
            return true;
        }

        // 租户一致性校验：批量记录必须属于同一租户
        Long tenantId = historyDealList.get(0).getTenantId();
        Assert.notNull(tenantId, "租户ID不能为空");
        boolean hasInvalidTenant = historyDealList.stream()
                .anyMatch(deal -> !Objects.equals(deal.getTenantId(), tenantId));
        Assert.isTrue(!hasInvalidTenant, "批量记录必须属于同一租户，存在不一致的租户ID");

        // 逐条记录校验核心业务字段
        historyDealList.forEach(deal -> {
            Assert.notNull(deal.getCustomerId(), "客户ID不能为空");
            Assert.notNull(deal.getContractId(), "合同ID不能为空");
            Assert.notNull(deal.getDealTime(), "成交日期不能为空");
            Assert.hasText(deal.getHouseInfo(), "成交房源信息不能为空");
            Assert.hasText(deal.getDealType(), "成交类型不能为空");
            Assert.isTrue("SALE".equals(deal.getDealType()) || "RENT".equals(deal.getDealType()),
                    "成交类型必须为SALE（买卖）或RENT（租赁）");
        });

        // 批量保存，使用MyBatis-Plus的saveBatch方法
        return saveBatch(historyDealList);
    }

    /**
     * 批量删除成交记录
     *
     * <p><b>安全机制：</b></p>
     * <ul>
     *   <li>检查所有待删除记录是否都属于当前租户</li>
     *   <li>使用数据库事务保证原子性</li>
     *   <li>防止批量删除时误删其他租户数据</li>
     * </ul>
     *
     * @param ids 要删除的记录ID列表
     * @param tenantId 当前操作租户ID
     * @return 删除成功返回true，失败时抛出异常并回滚
     * @throws IllegalArgumentException 当参数校验失败时抛出
     * @throws IllegalStateException 当存在跨租户记录时抛出
     * @Transactional 声明式事务，异常时自动回滚
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchRemoveHistoryDeals(List<Long> ids, Long tenantId) {
        // 参数校验
        Assert.notNull(tenantId, "租户ID不能为空");
        Assert.notEmpty(ids, "记录ID列表不能为空");

        // 权限校验：确保所有待删除记录都属于当前租户
        long count = baseMapper.selectCount(new LambdaQueryWrapper<CustomerHistoryDeal>()
                .in(CustomerHistoryDeal::getId, ids)
                .ne(CustomerHistoryDeal::getTenantId, tenantId));
        Assert.isTrue(count == 0, "存在跨租户记录，无法删除，请检查数据权限");

        // 批量删除
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
     *   <li>设置默认排序：按成交时间倒序（最新成交在前）</li>
     * </ol>
     *
     * <p><b>条件说明：</b></p>
     * <ul>
     *   <li>客户ID：精确匹配</li>
     *   <li>合同ID：精确匹配</li>
     *   <li>成交日期：大于等于查询，用于筛选某时间点之后的成交</li>
     *   <li>成交类型：精确匹配，用于筛选买卖或租赁类型</li>
     *   <li>房源信息：模糊查询，支持部分匹配</li>
     * </ul>
     *
     * @param query 查询条件实体，tenantId字段必须非空
     * @return 构建完成的LambdaQueryWrapper对象
     */
    private LambdaQueryWrapper<CustomerHistoryDeal> buildQueryWrapper(CustomerHistoryDeal query) {
        LambdaQueryWrapper<CustomerHistoryDeal> wrapper = new LambdaQueryWrapper<>();

        // 强制租户隔离：必须条件
        wrapper.eq(CustomerHistoryDeal::getTenantId, query.getTenantId());

        // 动态条件构建
        if (query.getCustomerId() != null) {
            wrapper.eq(CustomerHistoryDeal::getCustomerId, query.getCustomerId());
        }
        if (query.getContractId() != null) {
            wrapper.eq(CustomerHistoryDeal::getContractId, query.getContractId());
        }
        if (query.getDealTime() != null) {
            wrapper.ge(CustomerHistoryDeal::getDealTime, query.getDealTime()); // 大于等于指定成交日期
        }
        if (StringUtils.hasText(query.getDealType())) {
            wrapper.eq(CustomerHistoryDeal::getDealType, query.getDealType()); // 成交类型精确匹配
        }
        if (StringUtils.hasText(query.getHouseInfo())) {
            wrapper.like(CustomerHistoryDeal::getHouseInfo, query.getHouseInfo()); // 房源信息模糊查询
        }

        // 默认排序：按成交时间倒序，确保最新数据在前
        wrapper.orderByDesc(CustomerHistoryDeal::getDealTime);
        return wrapper;
    }
}
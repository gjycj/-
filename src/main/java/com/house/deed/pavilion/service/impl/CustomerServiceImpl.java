package com.house.deed.pavilion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.entity.Customer;
import com.house.deed.pavilion.mapper.CustomerMapper;
import com.house.deed.pavilion.service.CustomerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

/**
 * <p>
 * 客户信息表（租户级数据隔离）服务实现类
 * </p>
 *
 * <p>
 * 本服务类实现客户信息（CRM）的核心业务操作，支持完整的增删改查和批量处理功能。
 * 采用多租户架构设计，所有操作均强制进行租户数据隔离，确保不同租户间的数据完全独立。
 * </p>
 *
 * <p><b>核心设计原则：</b></p>
 * <ul>
 *   <li><b>租户隔离</b>：所有查询和操作必须包含租户ID条件，防止数据越权访问</li>
 *   <li><b>数据完整性</b>：严格校验必填字段，确保业务数据的完整性</li>
 *   <li><b>唯一性约束</b>：租户内手机号唯一性校验，防止数据重复</li>
 *   <li><b>事务一致性</b>：批量操作和关键业务使用事务保证数据一致性</li>
 * </ul>
 *
 * <p><b>主要功能模块：</b></p>
 * <ul>
 *   <li>客户信息增删改查（CRUD）</li>
 *   <li>多条件组合查询与分页</li>
 *   <li>客户状态管理</li>
 *   <li>批量导入与批量操作</li>
 *   <li>客户数据统计与分析</li>
 * </ul>
 *
 * <p><b>关键业务规则：</b></p>
 * <ul>
 *   <li>客户姓名和手机号为必填字段</li>
 *   <li>同一租户下客户手机号必须唯一</li>
 *   <li>更新操作需验证记录所属租户</li>
 *   <li>批量操作要求所有记录属于同一租户</li>
 * </ul>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Service
public class CustomerServiceImpl extends ServiceImpl<CustomerMapper, Customer> implements CustomerService {

    /**
     * 新增客户信息
     *
     * <p><b>业务规则：</b></p>
     * <ol>
     *   <li>租户ID必须提供，用于数据隔离</li>
     *   <li>客户姓名和手机号为必填字段</li>
     *   <li>手机号在租户内必须保持唯一性，防止重复客户</li>
     *   <li>系统会自动记录创建时间和创建人（如有配置）</li>
     * </ol>
     *
     * <p><b>校验逻辑：</b></p>
     * <ul>
     *   <li>使用Spring Assert进行前置校验</li>
     *   <li>执行数据库查询验证手机号唯一性</li>
     *   <li>校验失败时提供明确错误信息</li>
     * </ul>
     *
     * @param customer 客户信息实体，必须包含租户ID、姓名、手机号等核心字段
     * @return 保存成功返回true，失败返回false或抛出异常
     * @throws IllegalArgumentException 当参数校验失败时抛出
     * @throws IllegalStateException 当手机号已存在时抛出
     */
    @Override
    public boolean saveCustomer(Customer customer) {
        // 租户ID必须提供，这是多租户系统的核心要求
        Assert.notNull(customer.getTenantId(), "租户ID不能为空");
        // 核心业务字段校验：姓名和手机号不能为空
        Assert.hasText(customer.getName(), "客户姓名不能为空");
        Assert.hasText(customer.getPhone(), "客户手机号不能为空");

        // 租户内手机号唯一性校验：查询当前租户下是否已存在相同手机号
        long phoneCount = baseMapper.selectCount(new LambdaQueryWrapper<Customer>()
                .eq(Customer::getTenantId, customer.getTenantId())
                .eq(Customer::getPhone, customer.getPhone()));
        Assert.isTrue(phoneCount == 0, "当前租户下该手机号已存在：" + customer.getPhone());

        // 执行保存操作，MyBatis-Plus会自动处理ID生成和时间戳
        return save(customer);
    }

    /**
     * 更新客户信息
     *
     * <p><b>安全机制：</b></p>
     * <ol>
     *   <li>验证客户ID和租户ID不能为空</li>
     *   <li>查询数据库确认客户存在性</li>
     *   <li>验证当前租户是否有权限操作该客户</li>
     *   <li>如果更新手机号，需要校验新手机号在租户内的唯一性</li>
     * </ol>
     *
     * <p><b>注意事项：</b></p>
     * <ul>
     *   <li>仅更新传入的非空字段，遵循MyBatis-Plus的更新策略</li>
     *   <li>如果手机号不变，不进行唯一性校验</li>
     *   <li>更新时间字段会自动更新</li>
     * </ul>
     *
     * @param customer 客户信息实体，必须包含ID和租户ID
     * @return 更新成功返回true，失败返回false或抛出异常
     * @throws IllegalArgumentException 当参数校验失败时抛出
     * @throws IllegalStateException 当权限验证或唯一性校验失败时抛出
     */
    @Override
    public boolean updateCustomerById(Customer customer) {
        // 基础参数校验
        Assert.notNull(customer.getId(), "客户ID不能为空");
        Assert.notNull(customer.getTenantId(), "租户ID不能为空");

        // 权限校验：验证客户存在且属于当前租户
        Customer exist = getById(customer.getId());
        Assert.notNull(exist, "客户不存在");
        Assert.isTrue(Objects.equals(exist.getTenantId(), customer.getTenantId()),
                "无权限操作此客户，客户属于其他租户");

        // 如果更新手机号，需要校验新手机号在租户内的唯一性
        if (StringUtils.hasText(customer.getPhone()) && !customer.getPhone().equals(exist.getPhone())) {
            long phoneCount = baseMapper.selectCount(new LambdaQueryWrapper<Customer>()
                    .eq(Customer::getTenantId, customer.getTenantId())
                    .eq(Customer::getPhone, customer.getPhone())
                    .ne(Customer::getId, customer.getId()));
            Assert.isTrue(phoneCount == 0, "新手机号已存在：" + customer.getPhone());
        }

        // 执行更新操作
        return updateById(customer);
    }

    /**
     * 删除客户信息
     *
     * <p><b>安全机制：</b></p>
     * <ul>
     *   <li>双重验证：先验证客户存在，再验证租户归属</li>
     *   <li>防止误删其他租户的客户数据</li>
     *   <li>删除前不检查关联数据，需要调用方确保业务完整性</li>
     * </ul>
     *
     * <p><b>业务影响：</b></p>
     * <ul>
     *   <li>物理删除，数据无法恢复（如需回收站功能需改为逻辑删除）</li>
     *   <li>可能影响关联的成交记录、跟进记录等</li>
     *   <li>建议在删除前进行关联数据检查</li>
     * </ul>
     *
     * @param id 客户主键ID
     * @param tenantId 当前操作租户ID
     * @return 删除成功返回true，失败返回false或抛出异常
     * @throws IllegalArgumentException 当参数校验失败时抛出
     * @throws IllegalStateException 当权限验证失败时抛出
     */
    @Override
    public boolean removeCustomerById(Long id, Long tenantId) {
        // 参数校验
        Assert.notNull(id, "客户ID不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");

        // 存在性校验和权限校验
        Customer exist = getById(id);
        Assert.notNull(exist, "客户不存在");
        Assert.isTrue(Objects.equals(exist.getTenantId(), tenantId),
                "无权限操作此客户，客户属于其他租户");

        // 执行删除操作
        return removeById(id);
    }

    /**
     * 根据ID查询客户信息（租户隔离版本）
     *
     * <p>提供安全的单记录查询，自动添加租户ID条件，确保不会查询到其他租户的数据。</p>
     *
     * <p><b>设计特点：</b></p>
     * <ul>
     *   <li>返回null而不是抛出异常，符合查询操作的预期行为</li>
     *   <li>适用于根据ID查找单个客户的场景</li>
     *   <li>查询条件同时包含ID和租户ID，确保数据隔离</li>
     * </ul>
     *
     * @param id 客户主键ID
     * @param tenantId 当前操作租户ID
     * @return 符合条件的客户实体，如果不存在或不属于当前租户则返回null
     * @throws IllegalArgumentException 当参数为空时抛出
     */
    @Override
    public Customer getCustomerById(Long id, Long tenantId) {
        // 参数校验
        Assert.notNull(id, "客户ID不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");

        // 构建查询条件：ID匹配且租户ID匹配
        return getOne(new LambdaQueryWrapper<Customer>()
                .eq(Customer::getId, id)
                .eq(Customer::getTenantId, tenantId));
    }

    /**
     * 多条件分页查询客户信息
     *
     * <p>支持客户姓名、手机号、状态、来源、类型、潜力等级、意向区域等条件组合查询。</p>
     *
     * <p><b>查询特性：</b></p>
     * <ul>
     *   <li>结果按创建时间倒序排列（最新的客户在前）</li>
     *   <li>姓名支持模糊查询，手机号支持精确查询</li>
     *   <li>创建时间条件为"大于等于"查询，用于筛选某个时间点之后的客户</li>
     *   <li>强制租户隔离，不会查询到其他租户数据</li>
     * </ul>
     *
     * <p><b>使用场景：</b></p>
     * <ul>
     *   <li>客户管理列表页</li>
     *   <li>客户筛选与搜索</li>
     *   <li>数据导出前的预览</li>
     * </ul>
     *
     * @param page MyBatis-Plus分页参数，包含页码、每页条数等信息
     * @param query 查询条件实体，tenantId字段必须非空
     * @return 分页查询结果，包含客户数据列表和分页信息
     * @throws IllegalArgumentException 当tenantId为空时抛出
     */
    @Override
    public IPage<Customer> pageQuery(Page<Customer> page, Customer query) {
        // 租户ID必须非空，确保数据隔离
        Assert.notNull(query.getTenantId(), "租户ID不能为空");

        // 构建查询条件并执行分页查询
        LambdaQueryWrapper<Customer> wrapper = buildQueryWrapper(query);
        return baseMapper.selectPage(page, wrapper);
    }

    /**
     * 多条件列表查询客户信息
     *
     * <p>与分页查询使用相同的查询条件构建逻辑，但不进行分页，返回所有匹配记录。</p>
     *
     * <p><b>性能考虑：</b>当预期结果集较大时，建议使用分页查询避免内存溢出。</p>
     *
     * <p><b>使用场景：</b></p>
     * <ul>
     *   <li>导出客户数据到Excel</li>
     *   <li>批量发送营销短信</li>
     *   <li>生成客户统计报表</li>
     *   <li>需要获取所有匹配记录的批量操作</li>
     * </ul>
     *
     * @param query 查询条件实体，tenantId字段必须非空
     * @return 符合条件的客户列表，如果没有匹配记录则返回空列表
     * @throws IllegalArgumentException 当tenantId为空时抛出
     */
    @Override
    public List<Customer> listByConditions(Customer query) {
        // 租户ID必须非空，确保数据隔离
        Assert.notNull(query.getTenantId(), "租户ID不能为空");

        // 构建查询条件并执行查询
        LambdaQueryWrapper<Customer> wrapper = buildQueryWrapper(query);
        return baseMapper.selectList(wrapper);
    }

    /**
     * 根据姓名模糊查询客户
     *
     * <p>提供简化的姓名模糊查询接口，封装了查询条件的构建过程。</p>
     *
     * <p><b>注意事项：</b></p>
     * <ul>
     *   <li>使用SQL LIKE语法，支持前后模糊匹配</li>
     *   <li>当name参数过长时，可能影响查询性能</li>
     *   <li>结果集过大时建议使用分页查询</li>
     * </ul>
     *
     * @param name 客户姓名（支持模糊匹配）
     * @param tenantId 当前操作租户ID
     * @return 符合姓名条件的客户列表，按创建时间倒序排列
     * @throws IllegalArgumentException 当参数为空时抛出
     */
    @Override
    public List<Customer> listByNameLike(String name, Long tenantId) {
        // 参数校验
        Assert.hasText(name, "客户姓名不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");

        // 构建查询条件
        Customer query = new Customer();
        query.setTenantId(tenantId);
        query.setName(name);
        return listByConditions(query);
    }

    /**
     * 批量新增客户信息
     *
     * <p><b>业务规则：</b></p>
     * <ol>
     *   <li>批量记录必须属于同一租户，防止数据混乱</li>
     *   <li>每条记录都需要进行姓名和手机号校验</li>
     *   <li>批量检查手机号唯一性，防止部分成功部分失败</li>
     *   <li>使用数据库事务保证原子性：要么全部成功，要么全部失败</li>
     * </ol>
     *
     * <p><b>性能优化：</b></p>
     * <ul>
     *   <li>建议单次批量操作不超过1000条记录</li>
     *   <li>可在循环外统一查询已存在手机号，减少数据库查询次数</li>
     *   <li>适用于客户数据导入场景</li>
     * </ul>
     *
     * @param customerList 待新增的客户信息列表
     * @return 保存成功返回true，失败时抛出异常并回滚
     * @throws IllegalArgumentException 当校验失败时抛出
     * @Transactional 声明式事务，异常时自动回滚所有操作
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchSaveCustomers(List<Customer> customerList) {
        // 空列表快速返回，避免不必要的处理
        if (CollectionUtils.isEmpty(customerList)) {
            return true;
        }

        // 租户一致性校验：批量记录必须属于同一租户
        Long tenantId = customerList.get(0).getTenantId();
        Assert.notNull(tenantId, "租户ID不能为空");
        boolean hasInvalidTenant = customerList.stream()
                .anyMatch(customer -> !Objects.equals(customer.getTenantId(), tenantId));
        Assert.isTrue(!hasInvalidTenant, "批量客户必须属于同一租户，存在不一致的租户ID");

        // 逐条记录校验核心业务字段和手机号唯一性
        for (Customer customer : customerList) {
            Assert.hasText(customer.getName(), "客户姓名不能为空");
            Assert.hasText(customer.getPhone(), "客户手机号不能为空");

            // 检查手机号在租户内是否已存在
            long phoneCount = baseMapper.selectCount(new LambdaQueryWrapper<Customer>()
                    .eq(Customer::getTenantId, tenantId)
                    .eq(Customer::getPhone, customer.getPhone()));
            Assert.isTrue(phoneCount == 0, "批量新增失败，手机号已存在：" + customer.getPhone());
        }

        // 批量保存，使用MyBatis-Plus的saveBatch方法
        return saveBatch(customerList);
    }

    /**
     * 批量更新客户状态
     *
     * <p><b>使用场景：</b></p>
     * <ul>
     *   <li>批量激活/禁用客户账号</li>
     *   <li>批量标记客户为已联系/未联系</li>
     *   <li>批量转移客户到其他分类</li>
     * </ul>
     *
     * <p><b>安全机制：</b></p>
     * <ol>
     *   <li>验证所有客户ID都属于当前租户</li>
     *   <li>使用一条SQL语句批量更新，提高性能</li>
     *   <li>事务保证操作的原子性</li>
     * </ol>
     *
     * <p><b>性能考虑：</b>当ID数量过多时，需注意SQL语句长度限制。</p>
     *
     * @param ids 要更新的客户ID列表
     * @param status 目标状态值
     * @param tenantId 当前操作租户ID
     * @return 更新成功返回true，失败返回false或抛出异常
     * @throws IllegalArgumentException 当参数校验失败时抛出
     * @Transactional 声明式事务，异常时自动回滚
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchUpdateStatus(List<Long> ids, String status, Long tenantId) {
        // 参数校验
        Assert.notNull(tenantId, "租户ID不能为空");
        Assert.notEmpty(ids, "客户ID列表不能为空");
        Assert.notNull(status, "目标状态不能为空");

        // 权限校验：确保所有待更新客户都属于当前租户
        long invalidCount = baseMapper.selectCount(new LambdaQueryWrapper<Customer>()
                .in(Customer::getId, ids)
                .ne(Customer::getTenantId, tenantId));
        Assert.isTrue(invalidCount == 0, "存在跨租户客户ID，无法更新");

        // 构建更新实体和条件，执行批量更新
        Customer update = new Customer();
        update.setStatus(status);
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<Customer>()
                .in(Customer::getId, ids)
                .eq(Customer::getTenantId, tenantId);
        return baseMapper.update(update, wrapper) > 0;
    }

    /**
     * 批量删除客户信息
     *
     * <p><b>安全机制：</b></p>
     * <ul>
     *   <li>检查所有待删除记录是否都属于当前租户</li>
     *   <li>使用数据库事务保证原子性</li>
     *   <li>防止批量删除时误删其他租户数据</li>
     * </ul>
     *
     * <p><b>注意事项：</b></p>
     * <ul>
     *   <li>物理删除操作，数据无法恢复</li>
     *   <li>删除前建议检查关联数据，防止产生孤儿数据</li>
     *   <li>适用于批量清理测试数据或无效数据</li>
     * </ul>
     *
     * @param ids 要删除的客户ID列表
     * @param tenantId 当前操作租户ID
     * @return 删除成功返回true，失败返回false或抛出异常
     * @throws IllegalArgumentException 当参数校验失败时抛出
     * @Transactional 声明式事务，异常时自动回滚
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchRemoveCustomers(List<Long> ids, Long tenantId) {
        // 参数校验
        Assert.notNull(tenantId, "租户ID不能为空");
        Assert.notEmpty(ids, "客户ID列表不能为空");

        // 权限校验：确保所有待删除客户都属于当前租户
        long invalidCount = baseMapper.selectCount(new LambdaQueryWrapper<Customer>()
                .in(Customer::getId, ids)
                .ne(Customer::getTenantId, tenantId));
        Assert.isTrue(invalidCount == 0, "存在跨租户客户ID，无法删除");

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
     *   <li>设置默认排序：按创建时间倒序（最新客户在前）</li>
     * </ol>
     *
     * <p><b>条件类型说明：</b></p>
     * <ul>
     *   <li>姓名：模糊查询（LIKE），支持部分匹配</li>
     *   <li>手机号：精确查询，用于查找特定客户</li>
     *   <li>状态/来源/类型：精确匹配，用于分类筛选</li>
     *   <li>潜力等级：精确匹配，用于客户分级管理</li>
     *   <li>意向区域：精确匹配，用于区域统计分析</li>
     *   <li>创建经纪人：精确匹配，用于业绩归属查询</li>
     *   <li>创建时间：大于等于查询，用于时间范围筛选</li>
     * </ul>
     *
     * @param query 查询条件实体，tenantId字段必须非空
     * @return 构建完成的LambdaQueryWrapper对象
     */
    private LambdaQueryWrapper<Customer> buildQueryWrapper(Customer query) {
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<>();

        // 强制租户隔离：必须条件，确保查询结果仅限当前租户
        wrapper.eq(Customer::getTenantId, query.getTenantId());

        // 动态条件构建：根据传入的查询参数添加相应条件
        if (StringUtils.hasText(query.getName())) {
            wrapper.like(Customer::getName, query.getName()); // 姓名模糊查询
        }
        if (StringUtils.hasText(query.getPhone())) {
            wrapper.eq(Customer::getPhone, query.getPhone()); // 手机号精确查询
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(Customer::getStatus, query.getStatus()); // 状态筛选（如：活跃、禁用、已联系等）
        }
        if (StringUtils.hasText(query.getSource())) {
            wrapper.eq(Customer::getSource, query.getSource()); // 客户来源筛选（如：线下、线上、转介绍等）
        }
        if (StringUtils.hasText(query.getCustomerType())) {
            wrapper.eq(Customer::getCustomerType, query.getCustomerType()); // 客户类型筛选（如：个人、企业、VIP等）
        }
        if (query.getPotentialLevel() != null) {
            wrapper.eq(Customer::getPotentialLevel, query.getPotentialLevel()); // 潜力等级筛选（用于客户分级管理）
        }
        if (query.getIntendedRegionId() != null) {
            wrapper.eq(Customer::getIntendedRegionId, query.getIntendedRegionId()); // 意向区域筛选
        }
        if (query.getCreateAgentId() != null) {
            wrapper.eq(Customer::getCreateAgentId, query.getCreateAgentId()); // 创建经纪人筛选（用于业绩归属查询）
        }
        if (query.getCreateTime() != null) {
            wrapper.ge(Customer::getCreateTime, query.getCreateTime()); // 创建时间筛选（大于等于，用于查询某个时间点之后的客户）
        }

        // 默认排序：按创建时间倒序，确保最新创建的客户在前
        wrapper.orderByDesc(Customer::getCreateTime);
        return wrapper;
    }
}
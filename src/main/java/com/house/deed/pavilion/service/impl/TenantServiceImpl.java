package com.house.deed.pavilion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.entity.Tenant;
import com.house.deed.pavilion.mapper.TenantMapper;
import com.house.deed.pavilion.service.TenantService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 多租户核心信息表（租户隔离根表）服务实现类
 *
 * <p>管理系统中所有租户的核心信息，包括租户编码、名称、状态、过期时间等关键属性。
 * 作为租户隔离的基础表，为系统中的数据隔离提供依据。</p>
 *
 * <p><b>核心业务规则：</b></p>
 * <ul>
 *   <li>租户编码（tenant_code）全局唯一，不可重复</li>
 *   <li>租户编码一旦创建不可修改</li>
 *   <li>租户状态管理：0-禁用，1-正常，2-过期</li>
 *   <li>提供丰富的查询条件支持分页查询</li>
 *   <li>支持批量操作以提高处理效率</li>
 * </ul>
 *
 * @author yuquanxi
 * @since 2025-11-26
 * @version 1.0.0
 */
@Service
public class TenantServiceImpl extends ServiceImpl<TenantMapper, Tenant> implements TenantService {

    /**
     * 新增租户信息
     * <p>执行租户编码唯一性校验，确保系统中租户编码全局唯一</p>
     *
     * @param tenant 租户实体对象，必须包含租户编码、租户名称等必填字段
     * @return boolean 新增操作结果，true-成功 false-失败
     * @throws IllegalArgumentException 当租户编码已存在时抛出异常
     *
     * <p><b>业务约束：</b></p>
     * <ul>
     *   <li>租户编码（tenant_code）必须唯一</li>
     *   <li>创建时间（create_time）由数据库自动填充</li>
     *   <li>操作在事务中执行，保证数据一致性</li>
     * </ul>
     *
     * <p><b>执行流程：</b></p>
     * <ol>
     *   <li>校验租户编码唯一性</li>
     *   <li>执行数据持久化操作</li>
     *   <li>返回操作结果</li>
     * </ol>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveTenant(Tenant tenant) {
        // 唯一性校验：租户编码全局唯一
        if (checkTenantCodeExists(tenant.getTenantCode())) {
            throw new IllegalArgumentException("租户编码已存在：" + tenant.getTenantCode());
        }
        // 执行新增操作，创建时间由数据库自动填充
        return save(tenant);
    }

    /**
     * 根据主键ID查询租户信息
     * <p>通过主键ID精确查询租户信息，ID不存在时返回null</p>
     *
     * @param id 租户主键ID，不能为null
     * @return Tenant 匹配的租户实体，未找到时返回null
     *
     * <p><b>使用场景：</b></p>
     * <ul>
     *   <li>租户详情查看</li>
     *   <li>数据编辑前的数据获取</li>
     *   <li>关联数据查询</li>
     * </ul>
     */
    @Override
    public Tenant getTenantById(Long id) {
        return getById(id);
    }

    /**
     * 根据租户编码查询租户信息
     * <p>通过租户编码精确查询，用于登录、授权等场景</p>
     *
     * @param tenantCode 租户编码，不能为null或空字符串
     * @return Tenant 匹配的租户实体，未找到时返回null
     * @throws IllegalArgumentException 当租户编码为空时抛出异常
     *
     * <p><b>查询优化：</b></p>
     * <ul>
     *   <li>使用LambdaQueryWrapper构建查询条件</li>
     *   <li>采用eq精确匹配</li>
     *   <li>租户编码通常已建立唯一索引，查询效率高</li>
     * </ul>
     */
    @Override
    public Tenant getTenantByCode(String tenantCode) {
        LambdaQueryWrapper<Tenant> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Tenant::getTenantCode, tenantCode);
        return getOne(queryWrapper);
    }

    /**
     * 更新租户信息
     * <p>更新租户基本信息，但租户编码不可修改</p>
     *
     * @param tenant 待更新的租户实体，必须包含id主键
     * @return boolean 更新操作结果，true-成功 false-失败
     * @throws IllegalArgumentException 当租户不存在或尝试修改租户编码时抛出异常
     *
     * <p><b>更新限制：</b></p>
     * <table border="1">
     *   <tr><th>字段</th><th>是否可更新</th><th>说明</th></tr>
     *   <tr><td>tenant_code</td><td>❌ 不可更新</td><td>租户标识，创建后不可修改</td></tr>
     *   <tr><td>tenant_name</td><td>✅ 可更新</td><td>租户显示名称</td></tr>
     *   <tr><td>status</td><td>✅ 可更新</td><td>租户状态</td></tr>
     *   <tr><td>expire_time</td><td>✅ 可更新</td><td>过期时间</td></tr>
     *   <tr><td>update_time</td><td>⏱️ 自动更新</td><td>数据库自动填充</td></tr>
     * </table>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateTenant(Tenant tenant) {
        // 参数校验：ID不能为空
        if (tenant.getId() == null) {
            throw new IllegalArgumentException("租户ID不能为空");
        }

        // 数据存在性校验
        Tenant existing = getById(tenant.getId());
        if (existing == null) {
            throw new IllegalArgumentException("租户不存在：" + tenant.getId());
        }

        // 业务约束校验：租户编码不可修改
        if (!existing.getTenantCode().equals(tenant.getTenantCode())) {
            throw new IllegalArgumentException("租户编码不可修改");
        }

        // 执行更新操作，更新时间由数据库自动填充
        return updateById(tenant);
    }

    /**
     * 根据主键ID删除租户
     * <p>执行物理删除操作，删除前会级联删除相关数据（依赖数据库外键约束）</p>
     *
     * @param id 租户主键ID，不能为null
     * @return boolean 删除操作结果，true-成功 false-失败
     *
     * <p><b>删除影响：</b></p>
     * <ul>
     *   <li>删除租户基本信息</li>
     *   <li>依赖外键约束级联删除相关数据</li>
     *   <li>操作不可逆，需谨慎使用</li>
     * </ul>
     *
     * <p><b>注意事项：</b></p>
     * <ul>
     *   <li>生产环境建议使用逻辑删除</li>
     *   <li>删除前应检查是否存在关联数据</li>
     *   <li>重要数据建议备份后再删除</li>
     * </ul>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeTenantById(Long id) {
        return removeById(id);
    }

    /**
     * 多条件分页查询租户信息
     * <p>支持多条件组合查询，返回分页结果，适用于管理后台列表展示</p>
     *
     * @param page 分页参数对象，包含页码、每页大小等信息
     * @param tenantName 租户名称，支持模糊查询
     * @param status 租户状态，精确匹配（0-禁用，1-正常，2-过期）
     * @param expireTime 过期时间，查询小于该时间的租户
     * @param createTimeStart 创建时间起始范围
     * @param createTimeEnd 创建时间结束范围
     * @return IPage<Tenant> 分页查询结果，包含数据列表和分页信息
     *
     * <p><b>查询条件说明：</b></p>
     * <table border="1">
     *   <tr><th>参数</th><th>查询方式</th><th>为空处理</th><th>说明</th></tr>
     *   <tr><td>tenantName</td><td>LIKE 模糊查询</td><td>跳过该条件</td><td>支持部分匹配</td></tr>
     *   <tr><td>status</td><td>EQ 精确查询</td><td>跳过该条件</td><td>0/1/2三种状态</td></tr>
     *   <tr><td>expireTime</td><td>LT 小于查询</td><td>跳过该条件</td><td>查询已过期租户</td></tr>
     *   <tr><td>createTimeStart</td><td>GE 大于等于</td><td>跳过该条件</td><td>起始时间</td></tr>
     *   <tr><td>createTimeEnd</td><td>LE 小于等于</td><td>跳过该条件</td><td>结束时间</td></tr>
     * </table>
     *
     * <p><b>排序规则：</b>按创建时间倒序排列（最新创建的在前）</p>
     */
    @Override
    public IPage<Tenant> pageTenants(Page<Tenant> page, String tenantName, Byte status,
                                     LocalDateTime expireTime, LocalDateTime createTimeStart,
                                     LocalDateTime createTimeEnd) {
        LambdaQueryWrapper<Tenant> queryWrapper = new LambdaQueryWrapper<>();

        // 租户名称模糊查询（非空校验）
        if (StringUtils.hasText(tenantName)) {
            queryWrapper.like(Tenant::getTenantName, tenantName);
        }

        // 状态精确查询
        if (status != null) {
            queryWrapper.eq(Tenant::getStatus, status);
        }

        // 过期时间查询：查找已过期租户
        if (expireTime != null) {
            queryWrapper.lt(Tenant::getExpireTime, expireTime);
        }

        // 创建时间范围查询
        if (createTimeStart != null) {
            queryWrapper.ge(Tenant::getCreateTime, createTimeStart);
        }
        if (createTimeEnd != null) {
            queryWrapper.le(Tenant::getCreateTime, createTimeEnd);
        }

        // 排序规则：按创建时间倒序排列
        queryWrapper.orderByDesc(Tenant::getCreateTime);

        return page(page, queryWrapper);
    }

    /**
     * 批量新增租户
     * <p>批量插入租户信息，插入前校验租户编码唯一性</p>
     *
     * @param tenants 租户实体列表，不能为null或空
     * @return boolean 批量新增结果，true-成功 false-失败
     * @throws IllegalArgumentException 当参数非法或租户编码重复时抛出异常
     *
     * <p><b>事务保证：</b></p>
     * <ul>
     *   <li>整体事务：任一记录失败则全部回滚</li>
     *   <li>原子操作：保证数据一致性</li>
     *   <li>性能优化：使用批量插入提高效率</li>
     * </ul>
     *
     * <p><b>执行流程：</b></p>
     * <ol>
     *   <li>参数合法性校验</li>
     *   <li>批量唯一性校验</li>
     *   <li>执行批量插入</li>
     *   <li>返回操作结果</li>
     * </ol>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchSaveTenants(List<Tenant> tenants) {
        // 参数校验：非空检查
        if (tenants == null) {
            throw new IllegalArgumentException("租户列表不能为null");
        }

        if (tenants.isEmpty()) {
            throw new IllegalArgumentException("租户列表不能为空");
        }

        // 批量唯一性校验：检查租户编码是否重复
        for (Tenant tenant : tenants) {
            if (checkTenantCodeExists(tenant.getTenantCode())) {
                throw new IllegalArgumentException("租户编码已存在：" + tenant.getTenantCode());
            }
        }

        // 执行批量插入操作
        return saveBatch(tenants);
    }

    /**
     * 批量更新租户状态
     * <p>批量修改指定租户的状态信息，支持状态值校验</p>
     *
     * @param ids 租户ID列表，为空时视为操作成功（无需更新）
     * @param status 目标状态值，仅允许0、1、2三个值
     * @return boolean 操作结果，true-成功 false-失败
     * @throws IllegalArgumentException 当状态值非法时抛出异常
     *
     * <p><b>状态值说明：</b></p>
     * <table border="1">
     *   <tr><th>状态值</th><th>状态说明</th><th>业务含义</th></tr>
     *   <tr><td>0</td><td>禁用</td><td>租户无法使用系统</td></tr>
     *   <tr><td>1</td><td>正常</td><td>租户可正常使用系统</td></tr>
     *   <tr><td>2</td><td>过期</td><td>租户服务已到期</td></tr>
     * </table>
     *
     * <p><b>特殊处理：</b></p>
     * <ul>
     *   <li>空ID列表：记录日志并返回成功</li>
     *   <li>ID不存在：静默处理，不抛出异常</li>
     *   <li>更新0行：视为操作成功</li>
     * </ul>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchUpdateStatus(List<Long> ids, Byte status) {
        // 空列表检查：无需更新操作
        if (ids == null || ids.isEmpty()) {
            log.warn("批量更新状态：租户ID列表为空，无需更新");
            return true;
        }

        // 状态值合法性校验
        if (status == null || (status != 0 && status != 1 && status != 2)) {
            throw new IllegalArgumentException("状态值无效，仅支持0、1、2");
        }

        // 构建更新实体和条件
        Tenant updateEntity = new Tenant();
        updateEntity.setStatus(status);

        LambdaQueryWrapper<Tenant> updateWrapper = new LambdaQueryWrapper<>();
        updateWrapper.in(Tenant::getId, ids);

        // 执行批量更新
        update(updateEntity, updateWrapper);

        // 操作成功返回true（即使更新0行）
        return true;
    }

    /**
     * 批量删除租户
     * <p>根据ID列表批量删除租户信息，操作不可逆</p>
     *
     * @param ids 租户ID列表，不能为null或空
     * @return boolean 批量删除结果，true-成功 false-失败
     * @throws IllegalArgumentException 当参数非法时抛出异常
     *
     * <p><b>风险提示：</b></p>
     * <ul>
     *   <li>操作不可恢复，删除前请确认</li>
     *   <li>会级联删除所有关联数据</li>
     *   <li>建议先进行数据备份</li>
     * </ul>
     *
     * <p><b>执行流程：</b></p>
     * <ol>
     *   <li>参数合法性校验</li>
     *   <li>执行批量删除</li>
     *   <li>返回操作结果</li>
     * </ol>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchRemoveTenants(List<Long> ids) {
        // 参数校验
        if (ids == null) {
            throw new IllegalArgumentException("租户ID列表不能为null");
        }

        if (ids.isEmpty()) {
            throw new IllegalArgumentException("租户ID列表不能为空");
        }

        // 执行批量删除
        return removeByIds(ids);
    }

    /**
     * 内部校验方法：检查租户编码是否存在
     * <p>验证指定租户编码是否已在系统中存在</p>
     *
     * @param tenantCode 待校验的租户编码
     * @return boolean true-已存在 false-不存在
     *
     * <p><b>查询逻辑：</b></p>
     * <pre>
     * SELECT COUNT(*) FROM tenant WHERE tenant_code = #{tenantCode}
     * </pre>
     *
     * <p><b>性能优化：</b></p>
     * <ul>
     *   <li>租户编码字段通常建立唯一索引</li>
     *   <li>使用count查询而非查询完整实体</li>
     *   <li>空编码直接返回false</li>
     * </ul>
     */
    private boolean checkTenantCodeExists(String tenantCode) {
        // 空值处理：空编码视为不存在
        if (!StringUtils.hasText(tenantCode)) {
            return false;
        }

        LambdaQueryWrapper<Tenant> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Tenant::getTenantCode, tenantCode);
        return count(queryWrapper) > 0;
    }
}
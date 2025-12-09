package com.house.deed.pavilion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.entity.Property;
import com.house.deed.pavilion.mapper.PropertyMapper;
import com.house.deed.pavilion.service.PropertyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 楼盘信息服务实现类
 *
 * <p>实现楼盘信息的增删改查及批量操作，所有方法均包含租户级数据隔离和严格的业务校验</p>
 * <p>业务特点：</p>
 * <ul>
 *   <li>租户内楼盘名称必须唯一</li>
 *   <li>核心字段（租户ID、创建人ID）禁止修改</li>
 *   <li>创建/更新时间由数据库自动填充，禁止手动修改</li>
 *   <li>支持多种条件组合查询和范围查询</li>
 * </ul>
 * <p>技术实现：</p>
 * <ul>
 *   <li>所有字段约束完全匹配实体类注解配置</li>
 *   <li>使用QueryWrapper构建动态查询条件</li>
 *   <li>批量操作包含事务保证，确保数据一致性</li>
 *   <li>使用Spring Assert进行参数校验</li>
 * </ul>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Service
public class PropertyServiceImpl extends ServiceImpl<PropertyMapper, Property> implements PropertyService {

    // ==================== 实体类约束常量（匹配注解） ====================

    /**
     * 楼盘名称最大长度（匹配实体类@Size注解）
     */
    private static final int PROPERTY_NAME_MAX_LENGTH = 50;

    /**
     * 地址最大长度（匹配实体类@Size注解）
     */
    private static final int ADDRESS_MAX_LENGTH = 200;

    /**
     * 开发商/物业公司最大长度（匹配实体类@Size注解）
     */
    private static final int COMPANY_MAX_LENGTH = 50;

    /**
     * 绿化率最小值（匹配实体类@Min/@Max注解）
     */
    private static final BigDecimal GREEN_RATE_MIN = BigDecimal.ZERO;

    /**
     * 绿化率最大值（匹配实体类@Min/@Max注解）
     */
    private static final BigDecimal GREEN_RATE_MAX = new BigDecimal("100");

    /**
     * 建成年份最小值（匹配实体类@Min/@Max注解）
     */
    private static final int COMPLETION_YEAR_MIN = 1900;

    /**
     * 建成年份最大值（动态可配置，此处暂写2025，匹配实体类@Min/@Max注解）
     */
    private static final int COMPLETION_YEAR_MAX = 2025;

    // ==================== 数据库字段常量 ====================

    private static final String COL_ID = "id";
    private static final String COL_TENANT_ID = "tenant_id";
    private static final String COL_PROPERTY_NAME = "property_name";
    private static final String COL_REGION_ID = "region_id";
    private static final String COL_ADDRESS = "address";
    private static final String COL_DEVELOPER = "developer";
    private static final String COL_GREEN_RATE = "green_rate";
    private static final String COL_COMPLETION_YEAR = "completion_year";
    private static final String COL_PROPERTY_MANAGEMENT = "property_management";
    private static final String COL_CREATE_AGENT_ID = "create_agent_id";
    private static final String COL_CREATE_TIME = "create_time";
    private static final String COL_UPDATE_TIME = "update_time";

    // ==================== 基础CRUD实现 ====================

    /**
     * 新增楼盘信息
     *
     * <p>业务校验：</p>
     * <ul>
     *   <li>基础字段非空校验（租户ID、楼盘名称、区域ID等）</li>
     *   <li>字段长度校验（楼盘名称、地址、开发商名称等）</li>
     *   <li>数值范围校验（绿化率、建成年份）</li>
     *   <li>租户内楼盘名称唯一性校验</li>
     * </ul>
     * <p>技术实现：自动填充创建时间和更新时间（通过MyBatis Plus字段填充器）</p>
     *
     * @param property 楼盘信息实体对象，需包含必填信息
     * @return 新增成功返回true，否则返回false
     * @throws IllegalArgumentException 当违反任何业务规则时抛出
     */
    @Override
    public boolean saveProperty(Property property) {
        // 1. 基础字段约束校验（完全匹配实体类注解）
        validateEntityBaseConstraints(property);

        // 2. 租户内楼盘名称唯一性校验（避免同租户下楼盘名称重复）
        validatePropertyNameUnique(property.getTenantId(), property.getPropertyName(), null);

        // 3. 执行新增操作（createTime和updateTime自动填充）
        return baseMapper.insert(property) > 0;
    }

    /**
     * 根据ID更新楼盘信息
     *
     * <p>业务限制：</p>
     * <ul>
     *   <li>核心字段（租户ID、创建人ID）禁止修改</li>
     *   <li>创建时间和更新时间禁止手动修改</li>
     * </ul>
     * <p>业务校验：</p>
     * <ul>
     *   <li>数据必须存在且属于当前租户</li>
     *   <li>若修改楼盘名称，需校验租户内唯一性</li>
     *   <li>修改字段的约束校验（长度、数值范围等）</li>
     * </ul>
     *
     * @param property 楼盘信息实体对象，需包含主键ID、租户ID及需要更新的字段
     * @return 更新成功返回true，否则返回false
     * @throws IllegalArgumentException 当违反任何业务规则时抛出
     */
    @Override
    public boolean updatePropertyById(Property property) {
        // 1. 基础参数校验
        Assert.notNull(property.getId(), "楼盘ID不能为空");
        Assert.notNull(property.getTenantId(), "租户ID不能为空");

        // 2. 校验楼盘存在且归属当前租户
        Property existProperty = getPropertyById(property.getId(), property.getTenantId());
        Assert.notNull(existProperty, "楼盘信息不存在或无权限操作");

        // 3. 禁止修改核心不可变字段
        validateImmutableFields(existProperty, property);

        // 4. 若修改楼盘名称，校验租户内唯一性
        if (property.getPropertyName() != null && !property.getPropertyName().equals(existProperty.getPropertyName())) {
            validatePropertyNameUnique(property.getTenantId(), property.getPropertyName(), property.getId());
        }

        // 5. 校验修改字段的约束（长度、数值范围等）
        validateUpdateFieldConstraints(property);

        // 6. 执行更新（updateTime自动填充）
        return baseMapper.updateById(property) > 0;
    }

    /**
     * 根据ID物理删除楼盘信息
     *
     * <p>业务校验：数据必须存在且属于当前租户</p>
     *
     * @param id 楼盘信息主键ID
     * @param tenantId 租户ID，用于数据隔离验证
     * @return 删除成功返回true，否则返回false
     * @throws IllegalArgumentException 当数据不存在或无权限操作时抛出
     */
    @Override
    public boolean removePropertyById(Long id, Long tenantId) {
        // 1. 基础参数校验
        Assert.notNull(id, "楼盘ID不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");

        // 2. 校验楼盘存在且归属当前租户
        Property existProperty = getPropertyById(id, tenantId);
        Assert.notNull(existProperty, "楼盘信息不存在或无权限操作");

        // 3. 执行删除
        return baseMapper.deleteById(id) > 0;
    }

    /**
     * 根据ID查询楼盘详细信息（租户隔离）
     *
     * <p>查询时自动应用租户隔离条件，确保只能查询到当前租户的数据</p>
     *
     * @param id 楼盘信息主键ID
     * @param tenantId 租户ID，用于数据隔离
     * @return 符合条件的楼盘信息实体对象，未找到返回null
     */
    @Override
    public Property getPropertyById(Long id, Long tenantId) {
        Assert.notNull(id, "楼盘ID不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");

        // 租户隔离查询：仅查询当前租户的楼盘
        QueryWrapper<Property> wrapper = new QueryWrapper<>();
        wrapper.eq(COL_ID, id);
        wrapper.eq(COL_TENANT_ID, tenantId);
        return baseMapper.selectOne(wrapper);
    }

    // ==================== 多条件查询实现 ====================

    /**
     * 多条件分页查询楼盘信息
     *
     * <p>支持以下查询条件：</p>
     * <ul>
     *   <li>楼盘名称模糊查询</li>
     *   <li>区域ID精确查询</li>
     *   <li>开发商名称模糊查询</li>
     *   <li>绿化率范围查询</li>
     *   <li>建成年份范围查询</li>
     *   <li>物业公司名称模糊查询</li>
     *   <li>创建人ID精确查询</li>
     *   <li>创建时间范围查询</li>
     * </ul>
     * <p>排序规则：按创建时间倒序（最新创建的楼盘在前）</p>
     *
     * @param page 分页参数对象，包含页码和每页大小
     * @param queryParams 查询参数映射表，key为字段名，value为查询值
     * @param tenantId 租户ID，用于数据隔离
     * @return 分页查询结果，包含楼盘信息列表和分页信息
     */
    @Override
    public IPage<Property> pageQuery(Page<Property> page, Map<String, Object> queryParams, Long tenantId) {
        Assert.notNull(tenantId, "租户ID不能为空");
        QueryWrapper<Property> wrapper = buildQueryWrapper(queryParams, tenantId);

        // 默认排序：创建时间倒序（最新创建的楼盘优先）
        wrapper.orderByDesc(COL_CREATE_TIME);

        return baseMapper.selectPage(page, wrapper);
    }

    /**
     * 多条件查询楼盘信息列表（不分页）
     *
     * <p>查询条件与分页查询方法保持一致，但不进行分页处理</p>
     *
     * @param queryParams 查询参数映射表，key为字段名，value为查询值
     * @param tenantId 租户ID，用于数据隔离
     * @return 符合条件的楼盘信息实体对象列表
     */
    @Override
    public List<Property> listByConditions(Map<String, Object> queryParams, Long tenantId) {
        Assert.notNull(tenantId, "租户ID不能为空");
        return baseMapper.selectList(buildQueryWrapper(queryParams, tenantId));
    }

    /**
     * 根据区域ID查询楼盘列表
     *
     * <p>按创建时间倒序排列，最新的楼盘在前</p>
     *
     * @param regionId 区域ID
     * @param tenantId 租户ID，用于数据隔离
     * @return 符合条件的楼盘信息实体对象列表
     */
    @Override
    public List<Property> listByRegionId(Long regionId, Long tenantId) {
        Assert.notNull(regionId, "区域ID不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");

        QueryWrapper<Property> wrapper = new QueryWrapper<>();
        wrapper.eq(COL_REGION_ID, regionId);
        wrapper.eq(COL_TENANT_ID, tenantId);
        wrapper.orderByDesc(COL_CREATE_TIME);

        return baseMapper.selectList(wrapper);
    }

    /**
     * 根据开发商名称查询楼盘列表
     *
     * <p>按创建时间倒序排列，最新的楼盘在前</p>
     * <p>使用模糊匹配，支持部分名称查询</p>
     *
     * @param developer 开发商名称
     * @param tenantId 租户ID，用于数据隔离
     * @return 符合条件的楼盘信息实体对象列表
     */
    @Override
    public List<Property> listByDeveloper(String developer, Long tenantId) {
        Assert.hasLength(developer, "开发商名称不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");

        QueryWrapper<Property> wrapper = new QueryWrapper<>();
        wrapper.like(COL_DEVELOPER, developer); // 模糊匹配开发商名称
        wrapper.eq(COL_TENANT_ID, tenantId);
        wrapper.orderByDesc(COL_CREATE_TIME);

        return baseMapper.selectList(wrapper);
    }

    /**
     * 根据建成年份范围查询楼盘列表
     *
     * <p>按建成年份升序排列，年份较早的楼盘在前</p>
     *
     * @param startYear 起始年份（包含）
     * @param endYear 结束年份（包含）
     * @param tenantId 租户ID，用于数据隔离
     * @return 符合条件的楼盘信息实体对象列表
     * @throws IllegalArgumentException 当年份范围无效时抛出
     */
    @Override
    public List<Property> listByCompletionYearRange(Integer startYear, Integer endYear, Long tenantId) {
        Assert.notNull(startYear, "起始年份不能为空");
        Assert.notNull(endYear, "结束年份不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");
        Assert.isTrue(startYear >= COMPLETION_YEAR_MIN && startYear <= COMPLETION_YEAR_MAX, "起始年份超出合法范围");
        Assert.isTrue(endYear >= COMPLETION_YEAR_MIN && endYear <= COMPLETION_YEAR_MAX, "结束年份超出合法范围");
        Assert.isTrue(startYear <= endYear, "起始年份不能大于结束年份");

        QueryWrapper<Property> wrapper = new QueryWrapper<>();
        wrapper.ge(COL_COMPLETION_YEAR, startYear);
        wrapper.le(COL_COMPLETION_YEAR, endYear);
        wrapper.eq(COL_TENANT_ID, tenantId);
        wrapper.orderByAsc(COL_COMPLETION_YEAR); // 按建成年份升序

        return baseMapper.selectList(wrapper);
    }

    // ==================== 批量操作实现 ====================

    /**
     * 批量新增楼盘信息（事务保证）
     *
     * <p>在单个事务中执行批量新增，任一记录校验失败或保存失败将导致整个操作回滚</p>
     * <p>批量校验：</p>
     * <ul>
     *   <li>租户一致性校验（所有楼盘必须属于同一租户）</li>
     *   <li>楼盘名称唯一性校验（批量内部去重+租户内已存在检查）</li>
     *   <li>实体约束校验（复用单条新增的校验逻辑）</li>
     * </ul>
     *
     * @param propertyList 楼盘信息实体对象列表
     * @return 批量新增成功返回true，否则返回false
     * @throws IllegalArgumentException 当违反任何业务规则时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchSaveProperties(List<Property> propertyList) {
        if (CollectionUtils.isEmpty(propertyList)) {
            return false;
        }

        // 1. 校验租户一致性（批量楼盘需属于同一租户）
        Long tenantId = propertyList.get(0).getTenantId();
        Assert.notNull(tenantId, "租户ID不能为空");
        boolean hasInvalidTenant = propertyList.stream()
                .anyMatch(p -> !Objects.equals(p.getTenantId(), tenantId));
        Assert.isTrue(!hasInvalidTenant, "批量新增的楼盘必须属于同一租户");

        // 2. 校验楼盘名称唯一性（批量去重）
        List<String> propertyNames = propertyList.stream().map(Property::getPropertyName).collect(Collectors.toList());
        if (propertyNames.size() != propertyNames.stream().distinct().count()) {
            throw new IllegalArgumentException("批量楼盘中存在重复的楼盘名称");
        }
        // 校验租户内已存在的名称
        QueryWrapper<Property> nameCheckWrapper = new QueryWrapper<>();
        nameCheckWrapper.select(COL_PROPERTY_NAME);
        nameCheckWrapper.eq(COL_TENANT_ID, tenantId);
        nameCheckWrapper.in(COL_PROPERTY_NAME, propertyNames);

        List<String> existNames = baseMapper.selectList(nameCheckWrapper)
                .stream().map(Property::getPropertyName).collect(Collectors.toList());
        if (!existNames.isEmpty()) {
            throw new IllegalArgumentException("以下楼盘名称已存在：" + existNames);
        }

        // 3. 逐条校验实体约束
        for (Property property : propertyList) {
            validateEntityBaseConstraints(property);
        }

        // 执行批量保存（事务保证）
        return saveBatch(propertyList);
    }

    /**
     * 批量更新楼盘信息（事务保证）
     *
     * <p>在单个事务中执行批量更新，任一记录校验失败或更新失败将导致整个操作回滚</p>
     * <p>批量校验：</p>
     * <ul>
     *   <li>所有楼盘必须属于当前租户</li>
     *   <li>禁止修改核心字段（租户ID、创建人ID、创建时间、更新时间）</li>
     *   <li>修改字段的约束校验</li>
     *   <li>楼盘名称修改时校验唯一性</li>
     * </ul>
     *
     * @param propertyList 楼盘信息实体对象列表
     * @param tenantId 租户ID，用于数据隔离验证
     * @return 批量更新成功返回true，否则返回false
     * @throws IllegalArgumentException 当违反任何业务规则时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchUpdateProperties(List<Property> propertyList, Long tenantId) {
        if (CollectionUtils.isEmpty(propertyList)) {
            return false;
        }
        Assert.notNull(tenantId, "租户ID不能为空");

        // 1. 校验所有楼盘归属当前租户
        List<Long> propertyIds = propertyList.stream().map(Property::getId).collect(Collectors.toList());
        validatePropertyIdsBelongToTenant(tenantId, propertyIds);

        // 2. 逐条校验修改约束
        for (Property property : propertyList) {
            Property existProperty = getPropertyById(property.getId(), tenantId);
            // 禁止修改核心字段
            validateImmutableFields(existProperty, property);
            // 校验修改字段约束
            validateUpdateFieldConstraints(property);
            // 楼盘名称修改时校验唯一性
            if (property.getPropertyName() != null && !property.getPropertyName().equals(existProperty.getPropertyName())) {
                validatePropertyNameUnique(tenantId, property.getPropertyName(), property.getId());
            }
        }

        // 执行批量更新（事务保证）
        return updateBatchById(propertyList);
    }

    /**
     * 批量删除楼盘信息（事务保证）
     *
     * <p>在单个事务中执行批量删除，任一记录校验失败或删除失败将导致整个操作回滚</p>
     * <p>业务校验：所有楼盘必须属于当前租户</p>
     *
     * @param ids 待删除的楼盘ID列表
     * @param tenantId 租户ID，用于数据隔离验证
     * @return 批量删除成功返回true，否则返回false
     * @throws IllegalArgumentException 当存在不属于当前租户的楼盘时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchRemoveProperties(List<Long> ids, Long tenantId) {
        if (CollectionUtils.isEmpty(ids)) {
            return false;
        }
        Assert.notNull(tenantId, "租户ID不能为空");

        // 1. 校验所有楼盘归属当前租户
        validatePropertyIdsBelongToTenant(tenantId, ids);

        // 2. 执行批量删除（事务保证）
        return baseMapper.deleteBatchIds(ids) > 0;
    }

    /**
     * 验证楼盘ID列表是否全部属于当前租户
     *
     * <p>两步验证：</p>
     * <ol>
     *   <li>检查ID是否存在（是否存在未查询到的ID）</li>
     *   <li>检查存在的ID是否属于当前租户</li>
     * </ol>
     * <p>验证失败时抛出具体的异常信息，便于定位问题</p>
     *
     * @param tenantId 租户ID
     * @param propertyIds 待验证的楼盘ID列表
     * @throws IllegalArgumentException 当存在不存在的ID或不属于当前租户的ID时抛出
     */
    @Override
    public void validatePropertyIdsBelongToTenant(Long tenantId, List<Long> propertyIds) {
        if (CollectionUtils.isEmpty(propertyIds)) {
            return;
        }

        // 1. 查询存在的楼盘ID及租户ID
        QueryWrapper<Property> wrapper = new QueryWrapper<>();
        wrapper.select(COL_ID, COL_TENANT_ID);
        wrapper.in(COL_ID, propertyIds);
        List<Property> properties = baseMapper.selectList(wrapper);

        // 2. 检查不存在的ID
        Set<Long> existingIds = properties.stream().map(Property::getId).collect(Collectors.toSet());
        List<Long> nonExistentIds = propertyIds.stream()
                .filter(id -> !existingIds.contains(id))
                .collect(Collectors.toList());
        Assert.isTrue(nonExistentIds.isEmpty(), "以下楼盘ID不存在：" + nonExistentIds);

        // 3. 检查租户权限
        List<Long> invalidIds = properties.stream()
                .filter(p -> !Objects.equals(p.getTenantId(), tenantId))
                .map(Property::getId)
                .collect(Collectors.toList());
        Assert.isTrue(invalidIds.isEmpty(), "无权限操作以下楼盘ID：" + invalidIds);
    }

    // ==================== 私有工具方法（匹配实体类约束） ====================

    /**
     * 构建查询条件（严格匹配实体类字段）
     *
     * <p>根据查询参数动态构建查询条件，支持以下参数：</p>
     * <ul>
     *   <li>propertyName - 楼盘名称模糊查询</li>
     *   <li>regionId - 区域ID精确查询</li>
     *   <li>developer - 开发商名称模糊查询</li>
     *   <li>minGreenRate/maxGreenRate - 绿化率范围查询</li>
     *   <li>minCompletionYear/maxCompletionYear - 建成年份范围查询</li>
     *   <li>propertyManagement - 物业公司名称模糊查询</li>
     *   <li>createAgentId - 创建人ID精确查询</li>
     *   <li>startCreateTime/endCreateTime - 创建时间范围查询</li>
     * </ul>
     * <p>所有查询均自动添加租户隔离条件</p>
     *
     * @param queryParams 查询参数映射表
     * @param tenantId 租户ID，用于数据隔离
     * @return 构建完成的QueryWrapper对象
     */
    private QueryWrapper<Property> buildQueryWrapper(Map<String, Object> queryParams, Long tenantId) {
        QueryWrapper<Property> wrapper = new QueryWrapper<>();
        // 租户隔离：所有查询均过滤租户ID
        wrapper.eq(COL_TENANT_ID, tenantId);

        if (ObjectUtils.isEmpty(queryParams)) {
            return wrapper;
        }

        // 楼盘名称模糊查询
        if (queryParams.containsKey("propertyName") && StringUtils.hasText(queryParams.get("propertyName").toString())) {
            wrapper.like(COL_PROPERTY_NAME, queryParams.get("propertyName"));
        }

        // 区域ID精确查询
        if (queryParams.containsKey("regionId") && queryParams.get("regionId") != null) {
            wrapper.eq(COL_REGION_ID, queryParams.get("regionId"));
        }

        // 开发商名称模糊查询
        if (queryParams.containsKey("developer") && StringUtils.hasText(queryParams.get("developer").toString())) {
            wrapper.like(COL_DEVELOPER, queryParams.get("developer"));
        }

        // 绿化率范围查询
        if (queryParams.containsKey("minGreenRate") && queryParams.get("minGreenRate") != null) {
            BigDecimal minGreenRate = new BigDecimal(queryParams.get("minGreenRate").toString());
            Assert.isTrue(minGreenRate.compareTo(GREEN_RATE_MIN) >= 0 && minGreenRate.compareTo(GREEN_RATE_MAX) <= 0, "最小绿化率超出合法范围");
            wrapper.ge(COL_GREEN_RATE, minGreenRate);
        }
        if (queryParams.containsKey("maxGreenRate") && queryParams.get("maxGreenRate") != null) {
            BigDecimal maxGreenRate = new BigDecimal(queryParams.get("maxGreenRate").toString());
            Assert.isTrue(maxGreenRate.compareTo(GREEN_RATE_MIN) >= 0 && maxGreenRate.compareTo(GREEN_RATE_MAX) <= 0, "最大绿化率超出合法范围");
            wrapper.le(COL_GREEN_RATE, maxGreenRate);
        }

        // 建成年份范围查询
        if (queryParams.containsKey("minCompletionYear") && queryParams.get("minCompletionYear") != null) {
            Integer minYear = Integer.parseInt(queryParams.get("minCompletionYear").toString());
            Assert.isTrue(minYear >= COMPLETION_YEAR_MIN && minYear <= COMPLETION_YEAR_MAX, "最小建成年份超出合法范围");
            wrapper.ge(COL_COMPLETION_YEAR, minYear);
        }
        if (queryParams.containsKey("maxCompletionYear") && queryParams.get("maxCompletionYear") != null) {
            Integer maxYear = Integer.parseInt(queryParams.get("maxCompletionYear").toString());
            Assert.isTrue(maxYear >= COMPLETION_YEAR_MIN && maxYear <= COMPLETION_YEAR_MAX, "最大建成年份超出合法范围");
            wrapper.le(COL_COMPLETION_YEAR, maxYear);
        }

        // 物业公司名称模糊查询
        if (queryParams.containsKey("propertyManagement") && StringUtils.hasText(queryParams.get("propertyManagement").toString())) {
            wrapper.like(COL_PROPERTY_MANAGEMENT, queryParams.get("propertyManagement"));
        }

        // 创建人ID精确查询
        if (queryParams.containsKey("createAgentId") && queryParams.get("createAgentId") != null) {
            wrapper.eq(COL_CREATE_AGENT_ID, queryParams.get("createAgentId"));
        }

        // 创建时间范围查询
        if (queryParams.containsKey("startCreateTime") && queryParams.get("startCreateTime") != null) {
            wrapper.ge(COL_CREATE_TIME, queryParams.get("startCreateTime"));
        }
        if (queryParams.containsKey("endCreateTime") && queryParams.get("endCreateTime") != null) {
            wrapper.le(COL_CREATE_TIME, queryParams.get("endCreateTime"));
        }

        return wrapper;
    }

    /**
     * 校验实体类基础约束（非空、长度、数值范围）
     *
     * <p>完全匹配实体类注解配置，确保数据完整性</p>
     * <p>校验内容包括：</p>
     * <ul>
     *   <li>非空字段校验（租户ID、楼盘名称、区域ID、地址等）</li>
     *   <li>字段长度校验（楼盘名称、地址、开发商名称、物业公司名称）</li>
     *   <li>数值范围校验（绿化率、建成年份）</li>
     * </ul>
     *
     * @param property 待校验的楼盘信息实体对象
     * @throws IllegalArgumentException 当任何校验失败时抛出，包含具体的错误信息
     */
    private void validateEntityBaseConstraints(Property property) {
        // 1. 非空字段校验（匹配实体类@NotNull/@NotBlank）
        Assert.notNull(property.getTenantId(), "租户ID不能为空");
        Assert.hasLength(property.getPropertyName(), "楼盘名称不能为空");
        Assert.notNull(property.getRegionId(), "所属区域ID不能为空");
        Assert.hasLength(property.getAddress(), "楼盘详细地址不能为空");
        Assert.hasLength(property.getDeveloper(), "开发商名称不能为空");
        Assert.notNull(property.getGreenRate(), "绿化率不能为空");
        Assert.notNull(property.getCompletionYear(), "建成年份不能为空");
        Assert.hasLength(property.getPropertyManagement(), "物业公司名称不能为空");
        Assert.notNull(property.getCreateAgentId(), "创建人ID不能为空");

        // 2. 长度校验（匹配实体类@Size）
        Assert.isTrue(property.getPropertyName().length() <= PROPERTY_NAME_MAX_LENGTH,
                "楼盘名称长度不能超过" + PROPERTY_NAME_MAX_LENGTH + "字符");
        Assert.isTrue(property.getAddress().length() <= ADDRESS_MAX_LENGTH,
                "楼盘详细地址长度不能超过" + ADDRESS_MAX_LENGTH + "字符");
        Assert.isTrue(property.getDeveloper().length() <= COMPANY_MAX_LENGTH,
                "开发商名称长度不能超过" + COMPANY_MAX_LENGTH + "字符");
        Assert.isTrue(property.getPropertyManagement().length() <= COMPANY_MAX_LENGTH,
                "物业公司名称长度不能超过" + COMPANY_MAX_LENGTH + "字符");

        // 3. 数值范围校验（匹配实体类@Min/@Max）
        Assert.isTrue(property.getGreenRate().compareTo(GREEN_RATE_MIN) >= 0 && property.getGreenRate().compareTo(GREEN_RATE_MAX) <= 0,
                "绿化率必须在0~100之间");
        Assert.isTrue(property.getCompletionYear() >= COMPLETION_YEAR_MIN && property.getCompletionYear() <= COMPLETION_YEAR_MAX,
                "建成年份必须在" + COMPLETION_YEAR_MIN + "~" + COMPLETION_YEAR_MAX + "之间");
    }

    /**
     * 校验租户内楼盘名称唯一性
     *
     * @param tenantId 租户ID
     * @param propertyName 楼盘名称
     * @param excludeId 排除的ID（更新场景下排除自身）
     * @throws IllegalArgumentException 当楼盘名称已存在时抛出
     */
    private void validatePropertyNameUnique(Long tenantId, String propertyName, Long excludeId) {
        QueryWrapper<Property> wrapper = new QueryWrapper<>();
        wrapper.eq(COL_TENANT_ID, tenantId);
        wrapper.eq(COL_PROPERTY_NAME, propertyName);
        // 更新场景排除自身ID
        if (excludeId != null) {
            wrapper.ne(COL_ID, excludeId);
        }
        long count = baseMapper.selectCount(wrapper);
        if (count > 0) {
            throw new IllegalArgumentException("当前租户下楼盘名称已存在：" + propertyName);
        }
    }

    /**
     * 校验不可修改的核心字段
     *
     * <p>以下字段在创建后不允许修改：</p>
     * <ul>
     *   <li>租户ID（tenantId）</li>
     *   <li>创建人ID（createAgentId）</li>
     *   <li>创建时间（createTime）</li>
     *   <li>更新时间（updateTime）</li>
     * </ul>
     *
     * @param existProperty 数据库中已存在的楼盘记录
     * @param updateProperty 待更新的楼盘对象
     * @throws IllegalArgumentException 当尝试修改不可变字段时抛出
     */
    private void validateImmutableFields(Property existProperty, Property updateProperty) {
        // 核心隔离/创建字段禁止修改
        if (updateProperty.getTenantId() != null && !updateProperty.getTenantId().equals(existProperty.getTenantId())) {
            throw new IllegalArgumentException("租户ID禁止修改");
        }
        if (updateProperty.getCreateAgentId() != null && !updateProperty.getCreateAgentId().equals(existProperty.getCreateAgentId())) {
            throw new IllegalArgumentException("创建人ID禁止修改");
        }
        // 自动填充字段禁止手动修改
        if (updateProperty.getCreateTime() != null) {
            throw new IllegalArgumentException("创建时间禁止修改");
        }
        if (updateProperty.getUpdateTime() != null) {
            throw new IllegalArgumentException("更新时间禁止手动修改");
        }
    }

    /**
     * 校验更新字段的约束（仅校验非空的修改字段）
     *
     * <p>用于更新操作，仅对传入的非空字段进行约束校验</p>
     *
     * @param property 待校验的楼盘对象
     * @throws IllegalArgumentException 当修改字段违反约束时抛出
     */
    private void validateUpdateFieldConstraints(Property property) {
        // 楼盘名称长度
        if (property.getPropertyName() != null) {
            Assert.isTrue(property.getPropertyName().length() <= PROPERTY_NAME_MAX_LENGTH,
                    "楼盘名称长度不能超过" + PROPERTY_NAME_MAX_LENGTH + "字符");
        }
        // 地址长度
        if (property.getAddress() != null) {
            Assert.isTrue(property.getAddress().length() <= ADDRESS_MAX_LENGTH,
                    "楼盘详细地址长度不能超过" + ADDRESS_MAX_LENGTH + "字符");
        }
        // 开发商长度
        if (property.getDeveloper() != null) {
            Assert.isTrue(property.getDeveloper().length() <= COMPANY_MAX_LENGTH,
                    "开发商名称长度不能超过" + COMPANY_MAX_LENGTH + "字符");
        }
        // 绿化率范围
        if (property.getGreenRate() != null) {
            Assert.isTrue(property.getGreenRate().compareTo(GREEN_RATE_MIN) >= 0 && property.getGreenRate().compareTo(GREEN_RATE_MAX) <= 0,
                    "绿化率必须在0~100之间");
        }
        // 建成年份范围
        if (property.getCompletionYear() != null) {
            Assert.isTrue(property.getCompletionYear() >= COMPLETION_YEAR_MIN && property.getCompletionYear() <= COMPLETION_YEAR_MAX,
                    "建成年份必须在" + COMPLETION_YEAR_MIN + "~" + COMPLETION_YEAR_MAX + "之间");
        }
        // 物业公司长度
        if (property.getPropertyManagement() != null) {
            Assert.isTrue(property.getPropertyManagement().length() <= COMPANY_MAX_LENGTH,
                    "物业公司名称长度不能超过" + COMPANY_MAX_LENGTH + "字符");
        }
    }
}
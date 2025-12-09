package com.house.deed.pavilion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.entity.HouseLandlord;
import com.house.deed.pavilion.mapper.HouseLandlordMapper;
import com.house.deed.pavilion.service.HouseLandlordService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * 房源与房东关联表（租户级数据） 服务实现类
 * </p>
 * <p>
 * 负责房源与房东关联关系的全生命周期管理，包括关联关系的创建、查询、更新、删除等核心操作。
 * 房源与房东关联是多对多关系，支持一个房源对应多个房东，一个房东拥有多个房源。
 * 所有操作均强制进行租户数据隔离校验，确保数据安全性和业务完整性。
 * </p>
 * <p>
 * 核心特性：
 * 1. 租户数据隔离：所有操作必须验证租户ID，确保跨租户数据不可见
 * 2. 关联唯一性保障：确保同一房源与房东在同一租户下只能关联一次
 * 3. 双向查询支持：支持按房源查询房东，也支持按房东查询房源
 * 4. 批量操作优化：提供批量增删功能，支持事务一致性保障
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Service
public class HouseLandlordServiceImpl extends ServiceImpl<HouseLandlordMapper, HouseLandlord> implements HouseLandlordService {

    // ==================== 基础CRUD操作 ====================

    /**
     * 新增房源与房东关联关系
     *
     * @param houseLandlord 关联关系实体对象，必须包含房源ID、房东ID和租户ID
     * @return boolean 新增成功返回true，失败返回false
     * @throws IllegalArgumentException 当参数校验失败或关联已存在时抛出
     *
     * 执行流程：
     * 1. 校验核心必填字段（租户ID、房源ID、房东ID）
     * 2. 校验关联关系唯一性（同一房源与房东在同一租户下只能关联一次）
     * 3. 使用MyBatis-Plus保存方法持久化数据
     *
     * 业务约束：
     * 1. 房源ID和房东ID必须在当前租户下存在有效数据
     * 2. 关联关系是租户级别的，不同租户可以有相同的房源-房东关联
     * 3. 关联记录一旦创建，核心字段通常不允许变更
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveHouseLandlord(HouseLandlord houseLandlord) {
        // 1. 校验核心必填字段
        validateRequiredFields(houseLandlord);
        // 2. 校验关联关系唯一性（同一房源与房东只能关联一次）
        validateRelationUniqueness(houseLandlord, null);
        // 3. 保存数据
        return save(houseLandlord);
    }

    /**
     * 更新关联关系信息
     *
     * @param houseLandlord 关联关系实体对象，必须包含ID和租户ID
     * @return boolean 更新成功返回true，失败返回false
     * @throws IllegalArgumentException 当参数校验失败时抛出
     * @throws IllegalStateException 当记录不存在或权限不足时抛出
     *
     * 执行流程：
     * 1. 基础参数校验（ID和租户ID不能为空）
     * 2. 记录存在性及租户归属校验
     * 3. 若修改房源ID或房东ID，重新校验关联关系唯一性
     * 4. 执行数据库更新操作
     *
     * 更新限制：
     * 1. 不支持跨租户迁移关联关系
     * 2. 核心字段（房源ID、房东ID）变更时需重新验证唯一性
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateHouseLandlordById(HouseLandlord houseLandlord) {
        // 1. 基础参数校验
        Assert.notNull(houseLandlord.getId(), "关联ID不能为空");
        Assert.notNull(houseLandlord.getTenantId(), "租户ID不能为空");

        // 2. 校验数据归属
        HouseLandlord existing = getById(houseLandlord.getId());
        Assert.notNull(existing, "关联关系不存在");
        Assert.isTrue(Objects.equals(existing.getTenantId(), houseLandlord.getTenantId()),
                "无权操作其他租户的关联数据");

        // 3. 若修改房源或房东ID，需校验唯一性
        if (!Objects.equals(existing.getHouseId(), houseLandlord.getHouseId()) ||
                !Objects.equals(existing.getLandlordId(), houseLandlord.getLandlordId())) {
            validateRelationUniqueness(houseLandlord, houseLandlord.getId());
        }

        return updateById(houseLandlord);
    }

    /**
     * 删除房源与房东关联关系
     *
     * @param id 关联关系的唯一标识
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
     * 注意事项：
     * 1. 删除关联关系不会影响房源和房东的主表数据
     * 2. 删除操作不可逆，建议先确认业务影响
     * 3. 支持事务回滚，确保操作原子性
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeHouseLandlordById(Long id, Long tenantId) {
        // 1. 参数非空校验
        Assert.notNull(id, "关联ID不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");

        // 2. 校验数据归属
        HouseLandlord existing = getById(id);
        Assert.notNull(existing, "关联关系不存在");
        Assert.isTrue(Objects.equals(existing.getTenantId(), tenantId),
                "无权删除其他租户的关联数据");

        // 3. 执行删除操作
        return removeById(id);
    }

    /**
     * 按ID查询关联关系（租户隔离）
     *
     * @param id 关联关系的唯一标识
     * @param tenantId 租户ID，用于数据隔离
     * @return HouseLandlord 关联关系实体对象，不存在时返回null
     * @throws IllegalArgumentException 当参数为空时抛出
     *
     * 说明：
     * 1. 强制添加租户ID查询条件，确保租户数据隔离
     * 2. 返回包含关联关系所有字段的完整信息
     * 3. 主要用于关联详情查看和编辑前数据加载
     */
    @Override
    public HouseLandlord getHouseLandlordById(Long id, Long tenantId) {
        // 参数校验
        Assert.notNull(id, "关联ID不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");

        return getOne(new LambdaQueryWrapper<HouseLandlord>()
                .eq(HouseLandlord::getId, id)
                .eq(HouseLandlord::getTenantId, tenantId));
    }

    // ==================== 多条件查询方法 ====================

    /**
     * 分页查询关联关系
     *
     * @param page 分页参数对象，包含页码、每页大小等信息
     * @param queryParams 查询参数Map，支持动态条件组合
     * @param tenantId 租户ID，用于数据隔离
     * @return IPage<HouseLandlord> 分页结果对象，包含数据列表和分页信息
     * @throws IllegalArgumentException 当租户ID为空时抛出
     *
     * 支持的查询条件：
     * 1. houseId: 房源ID（精确匹配）
     * 2. landlordId: 房东ID（精确匹配）
     *
     * 使用场景：
     * 1. 关联关系管理列表展示
     * 2. 按房源或房东筛选关联关系
     * 3. 支持分页加载大量关联数据
     */
    @Override
    public IPage<HouseLandlord> pageQuery(Page<HouseLandlord> page, Map<String, Object> queryParams, Long tenantId) {
        // 租户ID必填校验
        Assert.notNull(tenantId, "租户ID不能为空");

        // 构建查询条件
        LambdaQueryWrapper<HouseLandlord> queryWrapper = buildQueryWrapper(queryParams, tenantId);
        return baseMapper.selectPage(page, queryWrapper);
    }

    /**
     * 多条件查询关联关系列表（租户隔离）
     *
     * @param queryParams 查询参数Map，支持动态条件组合
     * @param tenantId 租户ID，用于数据隔离
     * @return List<HouseLandlord> 符合条件的关联关系列表
     * @throws IllegalArgumentException 当租户ID为空时抛出
     *
     * 说明：
     * 1. 此方法与分页查询使用相同的查询逻辑，但不进行分页处理
     * 2. 适用于需要获取所有匹配记录的场景
     * 3. 可扩展支持更多查询条件（如关联时间范围等）
     */
    @Override
    public List<HouseLandlord> listByConditions(Map<String, Object> queryParams, Long tenantId) {
        // 租户ID必填校验
        Assert.notNull(tenantId, "租户ID不能为空");

        // 构建查询条件
        LambdaQueryWrapper<HouseLandlord> queryWrapper = buildQueryWrapper(queryParams, tenantId);
        return baseMapper.selectList(queryWrapper);
    }

    /**
     * 按房源ID查询关联的房东列表
     *
     * @param houseId 房源ID
     * @param tenantId 租户ID，用于数据隔离
     * @return List<HouseLandlord> 该房源关联的所有房东列表
     * @throws IllegalArgumentException 当房源ID或租户ID为空时抛出
     *
     * 业务用途：
     * 1. 查看房源的所有房东信息
     * 2. 房源详情页展示关联房东
     * 3. 批量处理房源相关的房东操作
     *
     * 说明：
     * 1. 返回列表包含该房源的所有关联记录
     * 2. 每个记录包含房东ID和其他关联信息
     * 3. 按创建时间自然排序（如需特定排序可扩展）
     */
    @Override
    public List<HouseLandlord> listByHouseId(Long houseId, Long tenantId) {
        // 参数校验
        Assert.notNull(houseId, "房源ID不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");

        return baseMapper.selectList(new LambdaQueryWrapper<HouseLandlord>()
                .eq(HouseLandlord::getHouseId, houseId)
                .eq(HouseLandlord::getTenantId, tenantId));
    }

    /**
     * 按房东ID查询关联的房源列表
     *
     * @param landlordId 房东ID
     * @param tenantId 租户ID，用于数据隔离
     * @return List<HouseLandlord> 该房东关联的所有房源列表
     * @throws IllegalArgumentException 当房东ID或租户ID为空时抛出
     *
     * 业务用途：
     * 1. 查看房东拥有的所有房源
     * 2. 房东详情页展示关联房源
     * 3. 批量处理房东相关的房源操作
     *
     * 说明：
     * 1. 返回列表包含该房东的所有关联记录
     * 2. 每个记录包含房源ID和其他关联信息
     * 3. 按创建时间自然排序（如需特定排序可扩展）
     */
    @Override
    public List<HouseLandlord> listByLandlordId(Long landlordId, Long tenantId) {
        // 参数校验
        Assert.notNull(landlordId, "房东ID不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");

        return baseMapper.selectList(new LambdaQueryWrapper<HouseLandlord>()
                .eq(HouseLandlord::getLandlordId, landlordId)
                .eq(HouseLandlord::getTenantId, tenantId));
    }

    // ==================== 批量操作方法 ====================

    /**
     * 批量新增房源与房东关联关系
     *
     * @param houseLandlordList 关联关系列表
     * @return boolean 批量新增成功返回true，失败返回false（事务回滚）
     * @throws IllegalArgumentException 当列表为空或租户ID不一致时抛出
     *
     * 执行流程：
     * 1. 列表非空校验
     * 2. 租户一致性校验（批量记录必须属于同一租户）
     * 3. 逐条记录必填字段校验
     * 4. 逐条记录关联唯一性校验
     * 5. 批量保存到数据库（事务保障）
     *
     * 使用场景：
     * 1. 房源批量关联多个房东
     * 2. 房东批量关联多个房源
     * 3. 数据迁移时的批量关联
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchSaveHouseLandlords(List<HouseLandlord> houseLandlordList) {
        // 1. 列表非空校验
        Assert.isTrue(!CollectionUtils.isEmpty(houseLandlordList), "关联列表不能为空");

        // 2. 校验所有记录租户ID一致
        Long tenantId = houseLandlordList.get(0).getTenantId();
        Assert.notNull(tenantId, "租户ID不能为空");
        boolean hasDifferentTenant = houseLandlordList.stream()
                .anyMatch(item -> !Objects.equals(item.getTenantId(), tenantId));
        Assert.isTrue(!hasDifferentTenant, "批量新增的关联关系必须属于同一租户");

        // 3. 逐条校验每条记录的必填字段和唯一性
        for (HouseLandlord item : houseLandlordList) {
            validateRequiredFields(item);
            validateRelationUniqueness(item, null);
        }

        // 4. 执行批量保存
        return saveBatch(houseLandlordList);
    }

    /**
     * 批量删除关联关系
     *
     * @param ids 关联关系ID列表
     * @param tenantId 租户ID，用于权限校验
     * @return boolean 批量删除成功返回true，失败返回false（事务回滚）
     * @throws IllegalArgumentException 当参数为空或存在跨租户记录时抛出
     *
     * 执行流程：
     * 1. 参数非空校验
     * 2. 跨租户记录校验（防止越权删除）
     * 3. 执行批量删除操作（事务保障）
     *
     * 安全机制：
     * 1. 强制租户ID校验，确保只能删除自己租户的数据
     * 2. 批量操作前验证所有记录归属，防止部分成功部分失败
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchRemoveHouseLandlords(List<Long> ids, Long tenantId) {
        // 1. 参数非空校验
        Assert.isTrue(!CollectionUtils.isEmpty(ids), "关联ID列表不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");

        // 2. 跨租户记录校验
        long count = baseMapper.selectCount(new LambdaQueryWrapper<HouseLandlord>()
                .in(HouseLandlord::getId, ids)
                .ne(HouseLandlord::getTenantId, tenantId));
        Assert.isTrue(count == 0, "存在跨租户的关联数据，无法批量删除");

        // 3. 执行批量删除
        return removeByIds(ids);
    }

    // ==================== 内部工具方法 ====================

    /**
     * 校验关联关系的必填字段
     *
     * @param houseLandlord 关联关系实体对象
     * @throws IllegalArgumentException 当必填字段为空时抛出
     *
     * 校验规则：
     * 1. 租户ID不能为空（确保数据归属明确）
     * 2. 房源ID不能为空（确保关联对象有效）
     * 3. 房东ID不能为空（确保关联对象有效）
     *
     * 说明：此方法在保存和更新操作前必须调用，确保数据完整性
     */
    private void validateRequiredFields(HouseLandlord houseLandlord) {
        Assert.notNull(houseLandlord.getTenantId(), "租户ID不能为空");
        Assert.notNull(houseLandlord.getHouseId(), "房源ID不能为空");
        Assert.notNull(houseLandlord.getLandlordId(), "房东ID不能为空");
    }

    /**
     * 校验房源与房东的关联关系唯一性
     *
     * @param houseLandlord 关联关系实体对象
     * @param excludeId 需要排除的关联ID（更新时使用，避免与自身冲突）
     * @throws IllegalArgumentException 当关联关系已存在时抛出
     *
     * 校验逻辑：
     * 1. 同一租户下，同一房源与同一房东只能建立一次关联
     * 2. 支持排除自身ID，用于更新操作时避免误判
     *
     * 业务意义：
     * 1. 防止重复关联造成数据冗余
     * 2. 确保关联关系的唯一性和准确性
     * 3. 避免业务逻辑混淆和数据不一致
     */
    private void validateRelationUniqueness(HouseLandlord houseLandlord, Long excludeId) {
        // 构建查询条件：相同租户、相同房源、相同房东
        LambdaQueryWrapper<HouseLandlord> queryWrapper = new LambdaQueryWrapper<HouseLandlord>()
                .eq(HouseLandlord::getTenantId, houseLandlord.getTenantId())
                .eq(HouseLandlord::getHouseId, houseLandlord.getHouseId())
                .eq(HouseLandlord::getLandlordId, houseLandlord.getLandlordId());

        // 排除自身ID（更新时使用）
        if (excludeId != null) {
            queryWrapper.ne(HouseLandlord::getId, excludeId);
        }

        // 执行查询计数
        long count = baseMapper.selectCount(queryWrapper);
        Assert.isTrue(count == 0, "该房源与房东已存在关联关系，不可重复关联");
    }

    /**
     * 构建多条件查询封装器
     *
     * @param queryParams 查询参数Map，支持动态条件组合
     * @param tenantId 租户ID，用于数据隔离
     * @return LambdaQueryWrapper<HouseLandlord> 查询条件封装器
     *
     * 支持的查询条件：
     * 1. houseId: 房源ID（精确匹配）
     * 2. landlordId: 房东ID（精确匹配）
     *
     * 扩展说明：
     * 1. 可根据业务需求添加更多查询条件
     * 2. 支持时间范围查询（如关联时间范围）
     * 3. 支持状态查询（如果关联关系有状态字段）
     */
    private LambdaQueryWrapper<HouseLandlord> buildQueryWrapper(Map<String, Object> queryParams, Long tenantId) {
        // 强制添加租户隔离条件
        LambdaQueryWrapper<HouseLandlord> queryWrapper = new LambdaQueryWrapper<HouseLandlord>()
                .eq(HouseLandlord::getTenantId, tenantId);

        // 如果查询参数为空，直接返回基本查询条件
        if (CollectionUtils.isEmpty(queryParams)) {
            return queryWrapper;
        }

        // 动态拼接查询条件
        if (queryParams.containsKey("houseId") && queryParams.get("houseId") != null) {
            queryWrapper.eq(HouseLandlord::getHouseId, queryParams.get("houseId"));
        }
        if (queryParams.containsKey("landlordId") && queryParams.get("landlordId") != null) {
            queryWrapper.eq(HouseLandlord::getLandlordId, queryParams.get("landlordId"));
        }
        // 可扩展其他条件（如关联时间范围等）

        return queryWrapper;
    }
}
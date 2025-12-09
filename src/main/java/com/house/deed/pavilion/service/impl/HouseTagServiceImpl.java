package com.house.deed.pavilion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.entity.HouseTag;
import com.house.deed.pavilion.mapper.HouseTagMapper;
import com.house.deed.pavilion.service.HouseTagService;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * 房源与标签关联表（租户级数据） 服务实现类
 * </p>
 * <p>
 * 负责房源与标签关联关系的全生命周期管理，包括关联关系的创建、查询、更新、删除等核心操作。
 * 房源标签关联是多对多关系，支持一个房源添加多个标签，一个标签可以关联到多个房源。
 * 所有操作均强制进行租户数据隔离校验，确保数据安全性和业务完整性。
 * </p>
 * <p>
 * 核心特性：
 * 1. 租户数据隔离：所有操作必须验证租户ID，确保跨租户数据不可见
 * 2. 关联唯一性保障：同一租户下房源与标签组合必须唯一，防止重复关联
 * 3. 双向查询支持：支持按房源查询标签，也支持按标签查询房源
 * 4. 批量操作优化：提供批量增删功能，支持事务一致性保障
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Service
public class HouseTagServiceImpl extends ServiceImpl<HouseTagMapper, HouseTag> implements HouseTagService {

    // ==================== 基础CRUD实现 ====================

    /**
     * 新增房源标签关联关系
     *
     * @param houseTag 关联关系实体对象，必须包含租户ID、房源ID和标签ID
     * @return boolean 新增成功返回true，失败返回false
     * @throws IllegalArgumentException 当参数校验失败或关联已存在时抛出
     *
     * 执行流程：
     * 1. 实体类非空字段校验（租户ID、房源ID、标签ID不能为空）
     * 2. 唯一性校验：同一租户下{房源ID, 标签ID}组合必须唯一
     * 3. 调用MyBatis-Plus保存方法持久化数据
     *
     * 业务约束：
     * 1. 同一房源在同一租户下不能重复添加相同的标签
     * 2. 房源ID和标签ID必须在当前租户下存在有效数据
     * 3. 关联关系是租户级别的，不同租户可以有相同的房源-标签关联
     */
    @Override
    public boolean saveHouseTag(HouseTag houseTag) {
        // 1. 实体类非空字段校验（与@NotNull注解匹配）
        Assert.notNull(houseTag.getTenantId(), "租户ID不能为空");
        Assert.notNull(houseTag.getHouseId(), "房源ID不能为空");
        Assert.notNull(houseTag.getTagId(), "标签ID不能为空");

        // 2. 唯一性校验：防止同一房源重复添加同一标签
        long count = baseMapper.selectCount(new LambdaQueryWrapper<HouseTag>()
                .eq(HouseTag::getTenantId, houseTag.getTenantId())
                .eq(HouseTag::getHouseId, houseTag.getHouseId())
                .eq(HouseTag::getTagId, houseTag.getTagId()));
        Assert.isTrue(count == 0, "当前房源已添加该标签，无需重复关联");

        // 3. 保存数据
        return save(houseTag);
    }

    /**
     * 更新房源标签关联记录
     *
     * @param houseTag 更新后的关联关系实体对象，必须包含ID和租户ID
     * @return boolean 更新成功返回true，失败返回false
     * @throws IllegalArgumentException 当参数校验失败时抛出
     * @throws IllegalStateException 当记录不存在或权限不足时抛出
     *
     * 执行流程：
     * 1. 基础参数校验（关联ID和租户ID不能为空）
     * 2. 记录存在性及租户归属校验
     * 3. 锁定核心关联字段，确保关联关系的稳定性
     * 4. 执行数据库更新操作
     *
     * 更新限制：
     * 1. 核心关联字段（房源ID、标签ID、租户ID）不允许修改
     * 2. 创建时间等自动填充字段不允许修改
     * 3. 仅允许更新非核心字段，如关联备注等
     *
     * 设计原则：关联关系一旦建立，核心关联信息不可更改
     */
    @Override
    public boolean updateHouseTagById(HouseTag houseTag) {
        // 1. 基础参数校验
        Assert.notNull(houseTag.getId(), "关联ID不能为空");
        Assert.notNull(houseTag.getTenantId(), "租户ID不能为空");

        // 2. 校验记录存在且归属当前租户
        HouseTag exist = getById(houseTag.getId());
        Assert.notNull(exist, "房源标签关联记录不存在");
        Assert.isTrue(Objects.equals(exist.getTenantId(), houseTag.getTenantId()),
                "无权限操作其他租户的关联记录");

        // 3. 锁定核心关联字段（不允许修改）
        houseTag.setHouseId(exist.getHouseId());       // 房源ID不可更改
        houseTag.setTagId(exist.getTagId());           // 标签ID不可更改
        houseTag.setTenantId(exist.getTenantId());     // 租户ID不可更改
        houseTag.setCreateTime(exist.getCreateTime()); // 创建时间不可更改

        return updateById(houseTag);
    }

    /**
     * 删除房源标签关联记录
     *
     * @param id 关联记录的唯一标识
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
     * 1. 删除关联记录不会影响房源和标签的主表数据
     * 2. 删除操作不可逆，建议先确认业务影响
     * 3. 支持事务回滚，确保操作原子性
     */
    @Override
    public boolean removeHouseTagById(Long id, Long tenantId) {
        // 1. 参数校验
        Assert.notNull(id, "关联ID不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");

        // 2. 校验租户归属
        HouseTag exist = getById(id);
        Assert.notNull(exist, "房源标签关联记录不存在");
        Assert.isTrue(Objects.equals(exist.getTenantId(), tenantId),
                "无权限操作其他租户的关联记录");

        // 3. 执行删除操作
        return removeById(id);
    }

    /**
     * 按ID查询关联记录（租户隔离）
     *
     * @param id 关联记录的唯一标识
     * @param tenantId 租户ID，用于数据隔离
     * @return HouseTag 关联关系实体对象，不存在时返回null
     * @throws IllegalArgumentException 当参数为空时抛出
     *
     * 说明：
     * 1. 强制添加租户ID查询条件，确保租户数据隔离
     * 2. 返回包含关联记录所有字段的完整信息
     * 3. 主要用于关联详情查看和编辑前数据加载
     */
    @Override
    public HouseTag getHouseTagById(Long id, Long tenantId) {
        // 参数校验
        Assert.notNull(id, "关联ID不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");

        return getOne(new LambdaQueryWrapper<HouseTag>()
                .eq(HouseTag::getId, id)
                .eq(HouseTag::getTenantId, tenantId));
    }

    // ==================== 多条件查询实现 ====================

    /**
     * 分页查询房源标签关联记录
     *
     * @param page 分页参数对象，包含页码、每页大小等信息
     * @param queryParams 查询参数Map，支持动态条件组合
     * @param tenantId 租户ID，用于数据隔离
     * @return IPage<HouseTag> 分页结果对象，包含数据列表和分页信息
     * @throws IllegalArgumentException 当租户ID为空时抛出
     *
     * 支持的查询条件：
     * 1. houseId: 房源ID（精确匹配）
     * 2. tagId: 标签ID（精确匹配）
     *
     * 默认排序：按创建时间倒序排列（最新关联记录在前）
     */
    @Override
    public IPage<HouseTag> pageQuery(Page<HouseTag> page, Map<String, Object> queryParams, Long tenantId) {
        // 租户ID必填校验
        Assert.notNull(tenantId, "租户ID不能为空");

        // 构建查询条件
        LambdaQueryWrapper<HouseTag> wrapper = buildQueryWrapper(queryParams, tenantId);
        return baseMapper.selectPage(page, wrapper);
    }

    /**
     * 多条件查询关联记录列表（租户隔离）
     *
     * @param queryParams 查询参数Map，支持动态条件组合
     * @param tenantId 租户ID，用于数据隔离
     * @return List<HouseTag> 符合条件的关联记录列表
     * @throws IllegalArgumentException 当租户ID为空时抛出
     *
     * 说明：
     * 1. 此方法与分页查询使用相同的查询逻辑，但不进行分页处理
     * 2. 适用于需要获取所有匹配记录的场景
     * 3. 按创建时间倒序排列，最新记录在前
     */
    @Override
    public List<HouseTag> listByConditions(Map<String, Object> queryParams, Long tenantId) {
        // 租户ID必填校验
        Assert.notNull(tenantId, "租户ID不能为空");

        // 构建查询条件
        LambdaQueryWrapper<HouseTag> wrapper = buildQueryWrapper(queryParams, tenantId);
        return baseMapper.selectList(wrapper);
    }

    /**
     * 按房源ID查询关联的标签
     *
     * @param houseId 房源ID
     * @param tenantId 租户ID，用于数据隔离
     * @return List<HouseTag> 该房源的所有标签关联列表，按创建时间倒序排列
     * @throws IllegalArgumentException 当房源ID或租户ID为空时抛出
     *
     * 业务用途：
     * 1. 查看房源的所有标签信息
     * 2. 房源详情页展示关联标签
     * 3. 批量处理房源相关的标签操作
     *
     * 返回说明：
     * 1. 返回列表包含该房源的所有标签关联记录
     * 2. 每个记录包含标签ID和其他关联信息
     * 3. 按创建时间倒序排列，最新添加的标签在前
     */
    @Override
    public List<HouseTag> listByHouseId(Long houseId, Long tenantId) {
        // 参数校验
        Assert.notNull(houseId, "房源ID不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");

        return baseMapper.selectList(new LambdaQueryWrapper<HouseTag>()
                .eq(HouseTag::getHouseId, houseId)
                .eq(HouseTag::getTenantId, tenantId)
                .orderByDesc(HouseTag::getCreateTime)); // 按创建时间倒序
    }

    /**
     * 按标签ID查询关联的房源
     *
     * @param tagId 标签ID
     * @param tenantId 租户ID，用于数据隔离
     * @return List<HouseTag> 该标签关联的所有房源列表，按创建时间倒序排列
     * @throws IllegalArgumentException 当标签ID或租户ID为空时抛出
     *
     * 业务用途：
     * 1. 查看标签关联的所有房源
     * 2. 标签详情页展示关联房源
     * 3. 批量处理标签相关的房源操作
     *
     * 返回说明：
     * 1. 返回列表包含该标签的所有房源关联记录
     * 2. 每个记录包含房源ID和其他关联信息
     * 3. 按创建时间倒序排列，最新关联的房源在前
     */
    @Override
    public List<HouseTag> listByTagId(Long tagId, Long tenantId) {
        // 参数校验
        Assert.notNull(tagId, "标签ID不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");

        return baseMapper.selectList(new LambdaQueryWrapper<HouseTag>()
                .eq(HouseTag::getTagId, tagId)
                .eq(HouseTag::getTenantId, tenantId)
                .orderByDesc(HouseTag::getCreateTime)); // 按创建时间倒序
    }

    // ==================== 批量操作实现 ====================

    /**
     * 批量新增房源标签关联记录
     *
     * @param houseTagList 关联记录列表
     * @return boolean 批量新增成功返回true，失败返回false
     * @throws IllegalArgumentException 当列表为空或记录格式无效时抛出
     *
     * 执行流程：
     * 1. 列表非空校验
     * 2. 租户一致性校验（批量记录必须属于同一租户）
     * 3. 逐条记录基础参数校验
     * 4. 逐条记录唯一性预校验
     * 5. 批量保存到数据库
     *
     * 使用场景：
     * 1. 房源批量添加多个标签
     * 2. 标签批量关联到多个房源
     * 3. 数据迁移时的批量关联
     */
    @Override
    public boolean batchSaveHouseTags(List<HouseTag> houseTagList) {
        // 列表非空校验
        Assert.isTrue(!CollectionUtils.isEmpty(houseTagList), "批量新增的关联列表不能为空");

        // 1. 校验租户一致性
        Long tenantId = houseTagList.get(0).getTenantId();
        Assert.notNull(tenantId, "租户ID不能为空");

        // 逐条校验
        for (HouseTag tag : houseTagList) {
            // 2. 租户一致性校验
            Assert.isTrue(Objects.equals(tag.getTenantId(), tenantId),
                    "批量操作的关联记录必须属于同一租户");
            // 3. 字段非空校验
            Assert.notNull(tag.getHouseId(), "房源ID不能为空");
            Assert.notNull(tag.getTagId(), "标签ID不能为空");
            // 4. 唯一性预校验
            long count = baseMapper.selectCount(new LambdaQueryWrapper<HouseTag>()
                    .eq(HouseTag::getTenantId, tenantId)
                    .eq(HouseTag::getHouseId, tag.getHouseId())
                    .eq(HouseTag::getTagId, tag.getTagId()));
            Assert.isTrue(count == 0,
                    "房源ID=" + tag.getHouseId() + "已关联标签ID=" + tag.getTagId() + "，无法重复添加");
        }

        // 5. 执行批量保存
        return saveBatch(houseTagList);
    }

    /**
     * 批量删除关联记录
     *
     * @param ids 关联记录ID列表
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
    public boolean batchRemoveHouseTags(List<Long> ids, Long tenantId) {
        // 参数校验
        Assert.notNull(tenantId, "租户ID不能为空");
        Assert.isTrue(!CollectionUtils.isEmpty(ids), "批量删除的ID列表不能为空");

        // 校验所有记录的租户归属
        long invalidCount = baseMapper.selectCount(new LambdaQueryWrapper<HouseTag>()
                .in(HouseTag::getId, ids)
                .ne(HouseTag::getTenantId, tenantId));
        Assert.isTrue(invalidCount == 0, "存在不属于当前租户的关联记录，无法批量删除");

        // 执行批量删除
        return removeByIds(ids);
    }

    /**
     * 批量删除房源的所有标签关联
     *
     * @param houseId 房源ID
     * @param tenantId 租户ID，用于数据隔离
     * @return boolean 删除成功返回true，失败返回false
     * @throws IllegalArgumentException 当房源ID或租户ID为空时抛出
     *
     * 使用场景：
     * 1. 清理房源的所有标签关联
     * 2. 房源重新打标签前清除历史标签
     * 3. 房源删除时的关联清理
     *
     * 注意事项：
     * 1. 此操作会删除该房源所有的标签关联记录
     * 2. 删除操作不可逆，建议先确认业务影响
     */
    @Override
    public boolean batchRemoveByHouseId(Long houseId, Long tenantId) {
        // 参数校验
        Assert.notNull(houseId, "房源ID不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");

        // 删除指定房源的所有标签关联
        return baseMapper.delete(new LambdaQueryWrapper<HouseTag>()
                .eq(HouseTag::getHouseId, houseId)
                .eq(HouseTag::getTenantId, tenantId)) > 0;
    }

    // ==================== 内部工具方法 ====================

    /**
     * 构建多条件查询封装器
     *
     * @param queryParams 查询参数Map，支持动态条件组合
     * @param tenantId 租户ID，用于数据隔离
     * @return LambdaQueryWrapper<HouseTag> 查询条件封装器
     *
     * 支持的查询条件：
     * 1. houseId: 房源ID（精确匹配，对应实体类house_id字段）
     * 2. tagId: 标签ID（精确匹配，对应实体类tag_id字段）
     *
     * 默认排序：按创建时间倒序排列（最新关联记录在前）
     *
     * 说明：查询条件与实体类字段一一对应，确保查询的准确性和一致性
     */
    private LambdaQueryWrapper<HouseTag> buildQueryWrapper(Map<String, Object> queryParams, Long tenantId) {
        // 强制添加租户隔离条件
        LambdaQueryWrapper<HouseTag> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HouseTag::getTenantId, tenantId);

        // 如果查询参数为空，直接返回基本查询条件
        if (ObjectUtils.isEmpty(queryParams)) {
            wrapper.orderByDesc(HouseTag::getCreateTime);
            return wrapper;
        }

        // 房源ID筛选（对应实体类house_id字段）
        if (queryParams.containsKey("houseId") && queryParams.get("houseId") != null) {
            wrapper.eq(HouseTag::getHouseId, queryParams.get("houseId"));
        }

        // 标签ID筛选（对应实体类tag_id字段）
        if (queryParams.containsKey("tagId") && queryParams.get("tagId") != null) {
            wrapper.eq(HouseTag::getTagId, queryParams.get("tagId"));
        }

        // 按创建时间倒序（最新关联在前）
        wrapper.orderByDesc(HouseTag::getCreateTime);

        return wrapper;
    }
}
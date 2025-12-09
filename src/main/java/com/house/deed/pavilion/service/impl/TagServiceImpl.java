package com.house.deed.pavilion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.entity.Tag;
import com.house.deed.pavilion.mapper.TagMapper;
import com.house.deed.pavilion.service.TagService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 标签表服务实现类
 * <p>
 * 负责标签数据（租户级隔离）的完整生命周期管理，包括标签的创建、查询、更新、删除及批量操作。
 * 核心特性：
 * 1. 租户数据隔离：所有操作均强制校验租户权限，确保数据安全
 * 2. 业务完整性：同一租户内标签名称+类型组合唯一，防止重复标签
 * 3. 批量操作支持：提供高效的批量增删接口，支持事务一致性
 * 4. 查询优化：支持多条件组合查询、分页查询和按类型查询
 * </p>
 * <p>
 * 适用场景：
 * - 房源标签管理：标记房源特征（如"学区房"、"地铁房"等）
 * - 客户标签管理：标记客户属性（如"VIP客户"、"意向强烈"等）
 * - 标签化分类：为业务实体提供灵活的标签化分类能力
 * </p>
 *
 * @author yuquanxi
 * @version 1.0.0
 * @since 2025-11-26
 */
@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag> implements TagService {

    // ==================== 基础CRUD操作（增强租户校验） ====================

    /**
     * 新增标签（强制租户隔离和唯一性校验）
     * <p>
     * 执行流程：
     * 1. 基础参数校验：确保租户ID、标签名称、标签类型等必填字段有效
     * 2. 默认值设置：自动填充创建时间、更新时间
     * 3. 唯一性校验：检查同一租户下相同名称+类型的标签是否已存在
     * 4. 数据持久化：保存标签到数据库
     * </p>
     *
     * @param tag 标签实体对象，必须包含租户ID、标签名称、标签类型等必要字段
     * @return true-保存成功，false-保存失败
     * @throws IllegalArgumentException 当参数校验失败（如租户ID为空、标签名称重复等）
     * @throws org.springframework.dao.DataAccessException 当数据库操作异常
     *
     * @example
     * <pre>
     * Tag tag = new Tag();
     * tag.setTenantId(1001L);
     * tag.setTagName("学区房");
     * tag.setTagType("HOUSE");
     * tag.setDescription("附近有优质学校");
     * boolean success = tagService.saveTag(tag); // 返回true表示保存成功
     * </pre>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveTag(Tag tag) {
        // 1. 基础参数校验
        validateTagParams(tag);

        // 2. 设置默认值：创建时间和更新时间
        tag.setCreateTime(LocalDateTime.now());
        tag.setUpdateTime(LocalDateTime.now());

        // 3. 租户内标签名称+类型组合唯一性校验
        // 确保同一租户下不存在相同名称和类型的标签
        checkTagNameUnique(tag.getTenantId(), tag.getTagName(), tag.getTagType(), null);

        // 4. 保存标签到数据库
        return save(tag);
    }

    /**
     * 更新标签信息（强租户隔离，仅允许更新非核心字段）
     * <p>
     * 更新策略：
     * 1. 数据存在性验证：确保要更新的标签存在且属于当前租户
     * 2. 字段权限控制：租户ID、创建时间等核心字段不可修改
     * 3. 唯一性校验：如果标签名称或类型有变更，重新校验唯一性
     * 4. 自动更新：更新时间字段自动设置为当前时间
     * </p>
     *
     * @param tag 标签实体对象，必须包含有效的ID和所有需要更新的字段
     * @return true-更新成功，false-更新失败
     * @throws IllegalArgumentException 当标签ID为空、标签不存在或租户权限不匹配
     * @throws org.springframework.dao.DataAccessException 当数据库操作异常
     *
     * @note 安全限制：
     * - 租户ID不可修改：防止数据越权
     * - 创建时间不可修改：保证数据审计完整性
     * - 仅当前租户管理员可操作：通过租户ID校验实现
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateTagById(Tag tag) {
        // 1. 基础参数校验：ID必须存在
        Assert.notNull(tag.getId(), "标签ID不能为空");
        validateTagParams(tag);

        // 2. 租户归属校验：确保只能操作本租户的数据
        Tag existingTag = getById(tag.getId());
        Assert.notNull(existingTag, "标签不存在");
        Assert.isTrue(Objects.equals(existingTag.getTenantId(), tag.getTenantId()),
                "无权限操作其他租户的标签");

        // 3. 标签名称+类型组合唯一性校验（仅在名称或类型有修改时执行）
        // 避免不必要的数据库查询，提升性能
        if (!Objects.equals(existingTag.getTagName(), tag.getTagName())
                || !Objects.equals(existingTag.getTagType(), tag.getTagType())) {
            checkTagNameUnique(tag.getTenantId(), tag.getTagName(), tag.getTagType(), tag.getId());
        }

        // 4. 锁定不可修改字段并设置更新时间
        tag.setTenantId(existingTag.getTenantId()); // 租户ID不可修改
        tag.setCreateTime(existingTag.getCreateTime()); // 创建时间不可修改
        tag.setUpdateTime(LocalDateTime.now()); // 自动设置更新时间

        return updateById(tag);
    }

    /**
     * 删除指定标签（强制租户隔离）
     * <p>
     * 安全删除机制：
     * 1. 参数校验：确保标签ID和租户ID有效
     * 2. 权限校验：验证标签属于当前租户，防止越权删除
     * 3. 关联数据检查：业务层应确保标签无使用关联（如房源已使用的标签）
     * 4. 执行删除：执行物理删除或逻辑删除（根据业务配置）
     * </p>
     *
     * @param id       标签主键ID，不能为空
     * @param tenantId 租户ID，用于数据隔离验证
     * @return true-删除成功，false-删除失败
     * @throws IllegalArgumentException 当参数无效或无操作权限时抛出
     *
     * @warning 删除操作不可逆，请确保业务允许删除（如标签未被使用）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeTagById(Long id, Long tenantId) {
        Assert.notNull(id, "标签ID不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");

        // 校验租户归属：确保删除的是本租户的数据
        Tag tag = getById(id);
        Assert.notNull(tag, "标签不存在");
        Assert.isTrue(Objects.equals(tag.getTenantId(), tenantId),
                "无权限操作其他租户的标签");

        return removeById(id);
    }

    /**
     * 查询标签详情（强制租户隔离）
     * <p>
     * 查询机制：
     * 1. 双重验证：同时验证标签ID存在性和租户权限
     * 2. 数据隔离：通过租户ID条件确保只返回本租户数据
     * 3. 性能优化：使用QueryWrapper直接查询，避免二次查询
     * </p>
     *
     * @param id       标签主键ID，不能为空
     * @param tenantId 租户ID，用于数据隔离验证
     * @return 标签实体对象，如果不存在或权限不足则返回null
     * @throws IllegalArgumentException 当参数为空时抛出
     *
     * @performance 注意：此方法使用selectOne，如果数据库中存在重复数据（理论上不应出现），可能抛出异常
     * @see #getTagByIdV2(Long, Long) 更安全的替代方案
     */
    public Tag getTagById(Long id, Long tenantId) {
        Assert.notNull(id, "标签ID不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");

        // 构建查询条件：同时匹配ID和租户ID
        QueryWrapper<Tag> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", id)
                .eq("tenant_id", tenantId);

        return baseMapper.selectOne(queryWrapper);
    }

    /**
     * 安全查询标签详情（强制租户隔离，推荐使用）
     * <p>
     * 相比getTagById方法的改进：
     * 1. 使用LIMIT 1防止返回多条数据
     * 2. 通过selectList查询避免selectOne的潜在异常
     * 3. 更清晰的空值处理逻辑
     * </p>
     *
     * @param id       标签主键ID，不能为空
     * @param tenantId 租户ID，用于数据隔离验证
     * @return 标签实体对象，如果不存在或权限不足则返回null
     */
    public Tag getTagByIdV2(Long id, Long tenantId) {
        Assert.notNull(id, "标签ID不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");

        QueryWrapper<Tag> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", id)
                .eq("tenant_id", tenantId)
                .last("LIMIT 1"); // 明确限制返回一条数据

        List<Tag> tags = baseMapper.selectList(queryWrapper);
        return CollectionUtils.isEmpty(tags) ? null : tags.get(0);
    }

    /**
     * 查询标签详情并验证存在性（强制租户隔离）
     * <p>
     * 业务场景：在需要确保标签存在且可用的场景下使用
     * 例如：为房源添加标签前，需要验证标签有效性
     * </p>
     *
     * @param id       标签主键ID，不能为空
     * @param tenantId 租户ID，用于数据隔离验证
     * @return 标签实体对象，保证非空
     * @throws IllegalArgumentException 当标签不存在或无权访问时抛出
     *
     * @example
     * <pre>
     * // 在业务逻辑中使用
     * Tag tag = tagService.getTagByIdAndValidate(tagId, currentTenantId);
     * // 后续可直接使用tag，无需空值检查
     * </pre>
     */
    public Tag getTagByIdAndValidate(Long id, Long tenantId) {
        Assert.notNull(id, "标签ID不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");

        Tag tag = getTagById(id, tenantId);
        Assert.notNull(tag, "标签不存在或无权访问");
        return tag;
    }

    // ==================== 多条件查询 ====================

    /**
     * 分页查询标签列表（多条件筛选 + 租户隔离）
     * <p>
     * 支持的查询条件：
     * - tagType: 标签类型（精确匹配，如"HOUSE"、"CUSTOMER"）
     * - tagName: 标签名称（模糊查询，支持部分匹配）
     * - description: 标签描述（模糊查询，可选）
     * </p>
     * <p>
     * 排序规则：
     * - 默认按创建时间降序排列（最新创建的在前）
     * - 可根据业务需求扩展其他排序字段
     * </p>
     *
     * @param page        分页参数对象，包含页码、页大小等信息
     * @param queryParams 查询条件映射表，键为字段名，值为查询条件
     * @param tenantId    租户ID，用于数据隔离
     * @return 分页结果对象，包含数据列表和分页信息
     * @throws IllegalArgumentException 当租户ID为空时抛出
     *
     * @example
     * <pre>
     * Page<Tag> page = new Page<>(1, 10);
     * Map<String, Object> params = new HashMap<>();
     * params.put("tagType", "HOUSE");
     * params.put("tagName", "学区");
     * IPage<Tag> result = tagService.pageQuery(page, params, tenantId);
     * </pre>
     */
    @Override
    public IPage<Tag> pageQuery(Page<Tag> page, Map<String, Object> queryParams, Long tenantId) {
        Assert.notNull(tenantId, "租户ID不能为空");

        QueryWrapper<Tag> wrapper = buildQueryWrapper(queryParams, tenantId);
        return baseMapper.selectPage(page, wrapper);
    }

    /**
     * 多条件查询标签列表（不分页，租户隔离）
     * <p>
     * 适用场景：
     * - 数据导出：需要获取所有符合条件的标签
     * - 下拉选择：为前端提供标签选项列表
     * - 批量处理：获取需要批量操作的标签集合
     * </p>
     * <p>
     * ⚠️ 性能警告：当数据量较大时，建议使用分页查询
     * </p>
     *
     * @param queryParams 查询条件映射表
     * @param tenantId    租户ID，用于数据隔离
     * @return 符合条件的标签列表，如果没有匹配项则返回空列表
     */
    @Override
    public List<Tag> listByConditions(Map<String, Object> queryParams, Long tenantId) {
        Assert.notNull(tenantId, "租户ID不能为空");

        QueryWrapper<Tag> wrapper = buildQueryWrapper(queryParams, tenantId);
        return baseMapper.selectList(wrapper);
    }

    /**
     * 按标签类型查询标签列表（租户隔离）
     * <p>
     * 业务场景：
     * - 房源标签管理：查询所有房源标签
     * - 客户标签管理：查询所有客户标签
     * - 标签分类展示：按类型分组展示标签
     * </p>
     *
     * @param tagType  标签类型，支持值："HOUSE"（房源标签）、"CUSTOMER"（客户标签）
     * @param tenantId 租户ID，用于数据隔离
     * @return 指定类型的标签列表，按创建时间降序排列
     * @throws IllegalArgumentException 当参数为空或标签类型无效时抛出
     */
    @Override
    public List<Tag> listByTagType(String tagType, Long tenantId) {
        Assert.notNull(tagType, "标签类型不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");

        QueryWrapper<Tag> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("tag_type", tagType)
                .eq("tenant_id", tenantId)
                .orderByDesc("create_time");
        return baseMapper.selectList(queryWrapper);
    }

    // ==================== 批量操作 ====================

    /**
     * 批量保存标签（同一租户，事务保证）
     * <p>
     * 执行流程：
     * 1. 租户一致性校验：确保所有标签属于同一租户
     * 2. 参数有效性校验：逐个验证标签参数
     * 3. 唯一性校验：检查每个标签在租户内是否唯一
     * 4. 批量保存：使用MyBatis-Plus的saveBatch方法
     * </p>
     * <p>
     * 事务特性：
     * - 原子性：所有标签要么全部保存成功，要么全部失败
     * - 一致性：批量操作期间数据状态保持一致
     * </p>
     *
     * @param tagList 标签实体列表，不能为空或包含null元素
     * @return true-批量保存成功，false-批量保存失败
     * @throws IllegalArgumentException 当参数校验失败时抛出
     *
     * @performance 批量保存性能优于逐条保存，建议一次性批量操作不超过1000条
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchSaveTags(List<Tag> tagList) {
        Assert.isTrue(!CollectionUtils.isEmpty(tagList), "标签列表不能为空");

        // 统一租户校验：批量操作必须针对同一租户
        Long tenantId = tagList.get(0).getTenantId();
        Assert.notNull(tenantId, "租户ID不能为空");

        for (Tag tag : tagList) {
            // 租户一致性校验
            Assert.isTrue(Objects.equals(tag.getTenantId(), tenantId), "批量操作仅支持同一租户");
            // 参数有效性校验
            validateTagParams(tag);
            // 唯一性校验
            checkTagNameUnique(tenantId, tag.getTagName(), tag.getTagType(), null);

            // 设置默认值
            tag.setCreateTime(LocalDateTime.now());
            tag.setUpdateTime(LocalDateTime.now());
        }

        return saveBatch(tagList);
    }

    /**
     * 批量删除标签（租户隔离，事务保证）
     * <p>
     * 安全机制：
     * 1. 存在性校验：验证所有ID对应的标签都存在
     * 2. 租户归属校验：确保所有标签都属于当前租户
     * 3. 批量删除：一次性删除所有符合条件的标签
     * </p>
     *
     * @param ids      标签ID列表，不能为空
     * @param tenantId 租户ID，用于数据隔离验证
     * @return true-批量删除成功，false-批量删除失败
     * @throws IllegalArgumentException 当参数无效或无操作权限时抛出
     *
     * @note 建议在删除前检查标签是否被使用，避免产生数据不一致
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchRemoveTags(List<Long> ids, Long tenantId) {
        Assert.isTrue(!CollectionUtils.isEmpty(ids), "标签ID列表不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");

        // 校验所有标签的租户归属
        List<Tag> tags = listByIds(ids);
        Assert.isTrue(tags.size() == ids.size(), "存在无效的标签ID");
        tags.forEach(tag -> Assert.isTrue(Objects.equals(tag.getTenantId(), tenantId),
                "无权限操作其他租户的标签"));

        return removeByIds(ids);
    }

    // ==================== 内部工具方法 ====================

    /**
     * 构建动态查询条件封装器
     * <p>
     * 根据传入的查询参数动态构建SQL查询条件
     * 支持的条件类型：
     * - 等值查询：tagType（精确匹配）
     * - 模糊查询：tagName、description（LIKE查询）
     * - 固定条件：tenant_id（租户隔离）
     * </p>
     *
     * @param queryParams 查询条件映射表，可包含tagType、tagName、description等键
     * @param tenantId    租户ID，用于添加租户隔离条件
     * @return 构建好的QueryWrapper对象，可直接用于数据库查询
     *
     * @performance 使用QueryWrapper而非LambdaQueryWrapper，兼容性更好但类型安全稍差
     */
    private QueryWrapper<Tag> buildQueryWrapper(Map<String, Object> queryParams, Long tenantId) {
        QueryWrapper<Tag> wrapper = new QueryWrapper<>();
        // 固定条件：租户隔离
        wrapper.eq("tenant_id", tenantId)
                .orderByDesc("create_time"); // 默认排序

        if (queryParams == null) {
            return wrapper;
        }

        // 标签类型筛选（精确匹配）
        if (queryParams.containsKey("tagType") && queryParams.get("tagType") != null) {
            wrapper.eq("tag_type", queryParams.get("tagType"));
        }

        // 标签名称模糊查询（支持部分匹配）
        if (queryParams.containsKey("tagName") && queryParams.get("tagName") != null) {
            wrapper.like("tag_name", queryParams.get("tagName"));
        }

        // 标签描述模糊查询（可选条件）
        if (queryParams.containsKey("description") && queryParams.get("description") != null) {
            wrapper.like("description", queryParams.get("description"));
        }

        return wrapper;
    }

    /**
     * 标签参数基础校验
     * <p>
     * 校验规则：
     * 1. 租户ID：必填，不能为空
     * 2. 标签名称：必填，长度不超过50字符
     * 3. 标签类型：必填，只能是"HOUSE"或"CUSTOMER"
     * 4. 标签描述：可选，如果提供则长度不超过200字符
     * </p>
     *
     * @param tag 待校验的标签实体对象
     * @throws IllegalArgumentException 当任何校验规则不满足时抛出
     */
    private void validateTagParams(Tag tag) {
        // 必填字段校验
        Assert.notNull(tag.getTenantId(), "租户ID不能为空");
        Assert.hasText(tag.getTagName(), "标签名称不能为空");
        Assert.hasText(tag.getTagType(), "标签类型不能为空");

        // 长度限制校验
        Assert.isTrue(tag.getTagName().length() <= 50, "标签名称长度不能超过50字符");

        // 枚举值校验：只允许预定义的标签类型
        Assert.isTrue("HOUSE".equals(tag.getTagType()) || "CUSTOMER".equals(tag.getTagType()),
                "标签类型只能是HOUSE或CUSTOMER");

        // 可选字段校验：描述字段长度限制
        if (tag.getDescription() != null) {
            Assert.isTrue(tag.getDescription().length() <= 200, "标签描述长度不能超过200字符");
        }
    }

    /**
     * 标签名称+类型组合唯一性校验
     * <p>
     * 业务规则：同一租户下，标签名称和类型的组合必须唯一
     * 例如：租户A可以同时拥有"学区房"(HOUSE)和"学区房"(CUSTOMER)两个标签，
     * 但不能有两个"学区房"(HOUSE)标签。
     * </p>
     *
     * @param tenantId  租户ID，用于限定查询范围
     * @param tagName   标签名称，用于唯一性检查
     * @param tagType   标签类型，与名称共同构成唯一键
     * @param excludeId 排除的标签ID（更新场景下需要排除自身）
     * @throws IllegalArgumentException 当已存在相同名称+类型的标签时抛出
     */
    private void checkTagNameUnique(Long tenantId, String tagName, String tagType, Long excludeId) {
        QueryWrapper<Tag> wrapper = new QueryWrapper<>();
        wrapper.eq("tenant_id", tenantId)
                .eq("tag_name", tagName)
                .eq("tag_type", tagType);

        // 更新场景：需要排除当前标签自身
        if (excludeId != null) {
            wrapper.ne("id", excludeId);
        }

        long count = count(wrapper);
        Assert.isTrue(count == 0,
                String.format("当前租户下标签名称和类型组合已存在：%s(%s)", tagName, tagType));
    }

    /**
     * 批量校验标签ID的有效性（租户隔离）
     * <p>
     * 主要用途：
     * 1. 批量操作前的数据验证
     * 2. 外部传入ID列表的有效性检查
     * 3. 租户权限批量验证
     * </p>
     *
     * @param ids      待校验的标签ID列表
     * @param tenantId 租户ID，用于数据隔离验证
     * @return 有效的标签实体列表
     * @throws IllegalArgumentException 当存在无效的标签ID时抛出，异常信息包含无效ID列表
     */
    private List<Tag> validateTagIds(List<Long> ids, Long tenantId) {
        if (CollectionUtils.isEmpty(ids) || tenantId == null) {
            return Collections.emptyList();
        }

        QueryWrapper<Tag> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("id", ids)
                .eq("tenant_id", tenantId);

        List<Tag> validTags = baseMapper.selectList(queryWrapper);

        // 验证所有ID都有效
        if (validTags.size() != ids.size()) {
            // 找出无效的ID，提供明确的错误信息
            Set<Long> validIds = validTags.stream()
                    .map(Tag::getId)
                    .collect(Collectors.toSet());
            List<Long> invalidIds = ids.stream()
                    .filter(id -> !validIds.contains(id))
                    .collect(Collectors.toList());
            throw new IllegalArgumentException("存在无效的标签ID：" + invalidIds);
        }

        return validTags;
    }

    // ==================== 扩展方法 ====================

    /**
     * 统计指定租户的标签数量
     * <p>
     * 业务场景：
     * - 租户管理面板：展示标签使用情况
     * - 系统监控：监控标签数据增长
     * - 配额检查：检查标签数量是否超限
     * </p>
     *
     * @param tenantId 租户ID
     * @return 标签数量，当租户ID为空时返回0
     */
    public long countByTenant(Long tenantId) {
        if (tenantId == null) {
            return 0;
        }

        QueryWrapper<Tag> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("tenant_id", tenantId);
        return count(queryWrapper);
    }

    /**
     * 检查标签是否存在且属于指定租户
     *
     * @param id       标签ID
     * @param tenantId 租户ID
     * @return true-标签存在且属于该租户，false-标签不存在或权限不足
     */
    public boolean existsByIdAndTenant(Long id, Long tenantId) {
        if (id == null || tenantId == null) {
            return false;
        }

        QueryWrapper<Tag> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", id)
                .eq("tenant_id", tenantId);
        return count(queryWrapper) > 0;
    }

    /**
     * 根据标签名称列表查询标签（租户隔离）
     * <p>
     * 适用场景：
     * - 批量创建实体时关联现有标签
     * - 根据名称列表获取标签详情
     * - 标签导入时的重复检查
     * </p>
     *
     * @param tagNames 标签名称集合，不能为空
     * @param tagType  标签类型，不能为空
     * @param tenantId 租户ID，不能为空
     * @return 匹配的标签列表，如果没有匹配项则返回空列表
     */
    public List<Tag> listByTagNames(Collection<String> tagNames, String tagType, Long tenantId) {
        if (CollectionUtils.isEmpty(tagNames) || !StringUtils.hasText(tagType) || tenantId == null) {
            return Collections.emptyList();
        }

        QueryWrapper<Tag> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("tag_name", tagNames)
                .eq("tag_type", tagType)
                .eq("tenant_id", tenantId);
        return baseMapper.selectList(queryWrapper);
    }

    /**
     * 获取标签名称到ID的映射表（租户隔离）
     * <p>
     * 性能优化：通过一次查询获取所有匹配标签的映射关系，
     * 避免在循环中多次查询数据库。
     * </p>
     *
     * @param tagNames 标签名称集合
     * @param tagType  标签类型
     * @param tenantId 租户ID
     * @return 标签名称到ID的映射表，键为标签名称，值为标签ID
     *
     * @example
     * <pre>
     * // 批量创建房源时，需要将标签名称转换为标签ID
     * Map<String, Long> nameToIdMap = tagService.getTagNameToIdMap(
     *     Arrays.asList("学区房", "地铁房"), "HOUSE", tenantId);
     * // 结果示例：{"学区房": 1, "地铁房": 2}
     * </pre>
     */
    public Map<String, Long> getTagNameToIdMap(Collection<String> tagNames, String tagType, Long tenantId) {
        List<Tag> tags = listByTagNames(tagNames, tagType, tenantId);
        return tags.stream()
                .collect(Collectors.toMap(Tag::getTagName, Tag::getId));
    }
}
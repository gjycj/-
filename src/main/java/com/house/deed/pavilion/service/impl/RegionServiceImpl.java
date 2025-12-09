package com.house.deed.pavilion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.entity.Region;
import com.house.deed.pavilion.mapper.RegionMapper;
import com.house.deed.pavilion.service.RegionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 区域管理服务实现类
 *
 * <p>实现区域信息的增删改查及批量操作，支持四级行政区划（省/市/区/街道）管理</p>
 * <p>业务特点：</p>
 * <ul>
 *   <li>系统默认区域（tenantId=0）为公共区域，所有租户共享且不可修改/删除</li>
 *   <li>租户自定义区域支持与系统区域混合查询（按区域层级统一展示）</li>
 *   <li>四级区域层级严格校验（省→市→区→街道）</li>
 *   <li>同一租户下，同父级区域名称必须唯一</li>
 *   <li>支持区域树形结构构建，便于前端展示</li>
 * </ul>
 * <p>技术实现：</p>
 * <ul>
 *   <li>使用QueryWrapper构建动态查询条件</li>
 *   <li>所有字段约束完全匹配实体类注解配置</li>
 *   <li>批量操作包含事务保证，确保数据一致性</li>
 *   <li>使用Spring Assert进行参数校验</li>
 * </ul>
 */
@Service
public class RegionServiceImpl extends ServiceImpl<RegionMapper, Region> implements RegionService {

    // ==================== 常量定义 ====================

    /**
     * 合法区域层级集合
     * <p>四级行政区划：</p>
     * <ul>
     *   <li>1 - 省级</li>
     *   <li>2 - 市级</li>
     *   <li>3 - 区级</li>
     *   <li>4 - 街道/乡镇级</li>
     * </ul>
     */
    private static final Set<Byte> VALID_REGION_LEVELS = Set.of((byte) 1, (byte) 2, (byte) 3, (byte) 4);

    /**
     * 区域名称最大长度（匹配实体类@Size注解）
     */
    private static final int REGION_NAME_MAX_LENGTH = 50;

    /**
     * 行政编码长度（6位数字，匹配实体类@Pattern注解）
     */
    private static final int REGION_CODE_LENGTH = 6;

    /**
     * 系统租户ID（公共区域，所有租户共享）
     */
    private static final Long SYSTEM_TENANT_ID = 0L;

    // ==================== 数据库字段常量 ====================

    private static final String COL_ID = "id";
    private static final String COL_TENANT_ID = "tenant_id";
    private static final String COL_REGION_LEVEL = "region_level";
    private static final String COL_REGION_NAME = "region_name";
    private static final String COL_PARENT_ID = "parent_id";
    private static final String COL_SORT = "sort";
    private static final String COL_REGION_CODE = "region_code";
    private static final String COL_CREATE_TIME = "create_time";

    // ==================== 按区域层级查询 ====================

    /**
     * 根据区域层级查询区域列表
     *
     * <p>查询规则：</p>
     * <ul>
     *   <li>普通租户：可查询系统公共区域（tenantId=0）和本租户自定义区域</li>
     *   <li>系统租户：仅查询系统公共区域</li>
     *   <li>结果按排序号升序排列</li>
     * </ul>
     *
     * @param regionLevel 区域层级（1-4）
     * @param tenantId 租户ID
     * @return 符合条件的区域实体对象列表
     * @throws IllegalArgumentException 当区域层级无效时抛出
     */
    @Override
    public List<Region> listByRegionLevel(Byte regionLevel, Long tenantId) {
        Assert.notNull(regionLevel, "区域层级不能为空");
        Assert.isTrue(VALID_REGION_LEVELS.contains(regionLevel), "无效区域层级：" + regionLevel);
        Assert.notNull(tenantId, "租户ID不能为空");

        QueryWrapper<Region> wrapper = new QueryWrapper<>();
        wrapper.eq(COL_REGION_LEVEL, regionLevel);

        // 租户隔离规则
        if (!SYSTEM_TENANT_ID.equals(tenantId)) {
            wrapper.and(w -> w.eq(COL_TENANT_ID, SYSTEM_TENANT_ID)
                    .or()
                    .eq(COL_TENANT_ID, tenantId));
        } else {
            wrapper.eq(COL_TENANT_ID, SYSTEM_TENANT_ID);
        }

        wrapper.orderByAsc(COL_SORT);
        return baseMapper.selectList(wrapper);
    }

    // ==================== 子区域查询 ====================

    /**
     * 根据父级区域ID查询子区域列表
     *
     * <p>父级区域必须存在且属于当前租户（或为系统公共区域）</p>
     * <p>结果按排序号升序排列</p>
     *
     * @param parentId 父级区域ID（0表示顶级区域）
     * @param tenantId 租户ID
     * @return 符合条件的子区域实体对象列表
     */
    @Override
    public List<Region> listChildrenByParentId(Long parentId, Long tenantId) {
        Assert.notNull(parentId, "父级区域ID不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");

        // 父级ID默认0（顶级），校验父级存在
        if (!SYSTEM_TENANT_ID.equals(parentId)) {
            Region parentRegion = getRegionById(parentId, tenantId);
            Assert.notNull(parentRegion, "父级区域不存在");
        }

        QueryWrapper<Region> wrapper = new QueryWrapper<>();
        wrapper.eq(COL_PARENT_ID, parentId);
        wrapper.eq(COL_TENANT_ID, tenantId);
        wrapper.orderByAsc(COL_SORT);

        return baseMapper.selectList(wrapper);
    }

    // ==================== 基础CRUD ====================

    /**
     * 新增区域信息
     *
     * <p>业务校验：</p>
     * <ul>
     *   <li>基础字段非空校验（区域名称、区域层级）</li>
     *   <li>字段长度校验（区域名称）</li>
     *   <li>数值范围校验（层级、排序号、父级ID）</li>
     *   <li>父子层级关系校验（四级行政层级约束）</li>
     *   <li>行政编码格式校验（6位数字）</li>
     *   <li>同一租户下同父级区域名称唯一性校验</li>
     * </ul>
     * <p>默认值设置：</p>
     * <ul>
     *   <li>租户ID未传时设为0（系统公共区域）</li>
     *   <li>父级ID未传时设为0（顶级区域）</li>
     *   <li>排序号未传时设为0</li>
     * </ul>
     *
     * @param region 区域实体对象，需包含必填信息
     * @return 新增成功返回true，否则返回false
     * @throws IllegalArgumentException 当违反任何业务规则时抛出
     */
    @Override
    public boolean saveRegion(Region region) {
        completeDefaultValues(region);
        validateEntityBaseConstraints(region);
        validateParentLevelRelation(region);
        validateRegionCode(region);
        validateRegionNameUnique(region.getTenantId(), region.getRegionName(), region.getParentId(), null);
        return baseMapper.insert(region) > 0;
    }

    /**
     * 根据ID更新区域信息
     *
     * <p>业务限制：</p>
     * <ul>
     *   <li>系统默认区域（tenantId=0）禁止修改</li>
     *   <li>核心字段（租户ID、父级ID、区域层级、行政编码）禁止修改</li>
     *   <li>仅允许修改区域名称和排序号</li>
     * </ul>
     * <p>业务校验：</p>
     * <ul>
     *   <li>数据必须存在且属于当前租户</li>
     *   <li>若修改区域名称，需校验同一租户下同父级区域名称唯一性</li>
     *   <li>修改字段的约束校验（长度、数值范围）</li>
     * </ul>
     *
     * @param region 区域实体对象，需包含主键ID和需要更新的字段
     * @return 更新成功返回true，否则返回false
     * @throws IllegalArgumentException 当违反任何业务规则时抛出
     */
    @Override
    public boolean updateRegionById(Region region) {
        Assert.notNull(region.getId(), "区域ID不能为空");
        Long tenantId = region.getTenantId() == null ? SYSTEM_TENANT_ID : region.getTenantId();

        Region existRegion = getRegionById(region.getId(), tenantId);
        Assert.notNull(existRegion, "区域不存在或无权限操作");

        if (SYSTEM_TENANT_ID.equals(existRegion.getTenantId())) {
            throw new IllegalArgumentException("系统默认区域（tenantId=0）禁止修改");
        }

        validateImmutableFields(region);
        validateUpdateFieldConstraints(region);

        if (region.getRegionName() != null && !region.getRegionName().equals(existRegion.getRegionName())) {
            validateRegionNameUnique(tenantId, region.getRegionName(), existRegion.getParentId(), region.getId());
        }

        Region updateEntity = new Region();
        updateEntity.setId(region.getId());
        if (region.getRegionName() != null) {
            updateEntity.setRegionName(region.getRegionName());
        }
        if (region.getSort() != null) {
            updateEntity.setSort(region.getSort());
        }

        return baseMapper.updateById(updateEntity) > 0;
    }

    /**
     * 根据ID物理删除区域信息
     *
     * <p>业务限制：</p>
     * <ul>
     *   <li>系统默认区域（tenantId=0）禁止删除</li>
     *   <li>存在下级子区域的区域禁止删除</li>
     * </ul>
     * <p>业务校验：数据必须存在且属于当前租户</p>
     *
     * @param id 区域主键ID
     * @param tenantId 租户ID，用于数据隔离验证
     * @return 删除成功返回true，否则返回false
     * @throws IllegalArgumentException 当违反删除限制时抛出
     */
    @Override
    public boolean removeRegionById(Long id, Long tenantId) {
        Assert.notNull(id, "区域ID不能为空");
        Long queryTenantId = tenantId == null ? SYSTEM_TENANT_ID : tenantId;

        Region existRegion = getRegionById(id, queryTenantId);
        Assert.notNull(existRegion, "区域不存在或无权限操作");

        if (SYSTEM_TENANT_ID.equals(existRegion.getTenantId())) {
            throw new IllegalArgumentException("系统默认区域（tenantId=0）禁止删除");
        }

        QueryWrapper<Region> childWrapper = new QueryWrapper<>();
        childWrapper.eq(COL_PARENT_ID, id);
        childWrapper.eq(COL_TENANT_ID, existRegion.getTenantId());
        long childCount = baseMapper.selectCount(childWrapper);

        if (childCount > 0) {
            throw new IllegalArgumentException("当前区域存在下级子区域，禁止删除");
        }

        return baseMapper.deleteById(id) > 0;
    }

    /**
     * 根据ID查询区域详细信息（租户隔离）
     *
     * <p>查询规则：</p>
     * <ul>
     *   <li>普通租户：可查询系统公共区域（tenantId=0）和本租户自定义区域</li>
     *   <li>系统租户：仅查询系统公共区域</li>
     * </ul>
     *
     * @param id 区域主键ID
     * @param tenantId 租户ID，用于数据隔离
     * @return 符合条件的区域实体对象，未找到返回null
     */
    @Override
    public Region getRegionById(Long id, Long tenantId) {
        Assert.notNull(id, "区域ID不能为空");
        Long queryTenantId = tenantId == null ? SYSTEM_TENANT_ID : tenantId;

        QueryWrapper<Region> wrapper = new QueryWrapper<>();
        wrapper.eq(COL_ID, id);

        if (!SYSTEM_TENANT_ID.equals(queryTenantId)) {
            wrapper.and(w -> w.eq(COL_TENANT_ID, SYSTEM_TENANT_ID)
                    .or()
                    .eq(COL_TENANT_ID, queryTenantId));
        } else {
            wrapper.eq(COL_TENANT_ID, SYSTEM_TENANT_ID);
        }

        return baseMapper.selectOne(wrapper);
    }

    // ==================== 多条件查询 ====================

    /**
     * 多条件分页查询区域信息
     *
     * <p>支持以下查询条件：</p>
     * <ul>
     *   <li>区域名称模糊查询</li>
     *   <li>区域层级精确查询</li>
     *   <li>父级区域ID精确查询</li>
     *   <li>行政编码精确查询</li>
     *   <li>排序号范围查询</li>
     * </ul>
     * <p>排序规则：按排序号升序 → 创建时间降序</p>
     *
     * @param page 分页参数对象，包含页码和每页大小
     * @param queryParams 查询参数映射表，key为字段名，value为查询值
     * @param tenantId 租户ID，用于数据隔离
     * @return 分页查询结果，包含区域列表和分页信息
     */
    @Override
    public IPage<Region> pageQuery(Page<Region> page, Map<String, Object> queryParams, Long tenantId) {
        Assert.notNull(tenantId, "租户ID不能为空");
        QueryWrapper<Region> wrapper = buildQueryWrapper(queryParams, tenantId);

        wrapper.orderByAsc(COL_SORT)
                .orderByDesc(COL_CREATE_TIME);

        return baseMapper.selectPage(page, wrapper);
    }

    /**
     * 多条件查询区域列表（不分页）
     *
     * <p>查询条件与分页查询方法保持一致，但不进行分页处理</p>
     *
     * @param queryParams 查询参数映射表，key为字段名，value为查询值
     * @param tenantId 租户ID，用于数据隔离
     * @return 符合条件的区域实体对象列表
     */
    @Override
    public List<Region> listByConditions(Map<String, Object> queryParams, Long tenantId) {
        Assert.notNull(tenantId, "租户ID不能为空");
        return baseMapper.selectList(buildQueryWrapper(queryParams, tenantId));
    }

    /**
     * 查询区域树形结构
     *
     * <p>构建完整的区域树，包含所有层级关系</p>
     * <p>树形结构规则：</p>
     * <ul>
     *   <li>顶级节点为parentId=0的区域</li>
     *   <li>每个节点包含children属性，存放子区域列表</li>
     *   <li>按排序号升序排列</li>
     * </ul>
     *
     * @param tenantId 租户ID，用于数据隔离
     * @return 区域树形结构列表
     */
    @Override
    public List<Region> listRegionTree(Long tenantId) {
        Assert.notNull(tenantId, "租户ID不能为空");
        List<Region> allRegions = listByConditions(new HashMap<>(), tenantId);
        return buildRegionTree(allRegions, SYSTEM_TENANT_ID);
    }

    // ==================== 批量操作 ====================

    /**
     * 批量新增区域信息（事务保证）
     *
     * <p>在单个事务中执行批量新增，任一记录校验失败或保存失败将导致整个操作回滚</p>
     * <p>批量校验：</p>
     * <ul>
     *   <li>租户一致性校验（所有区域必须属于同一租户）</li>
     *   <li>禁止批量新增系统默认区域（tenantId=0）</li>
     *   <li>实体约束校验（复用单条新增的校验逻辑）</li>
     *   <li>父子层级关系校验</li>
     *   <li>行政编码格式校验</li>
     *   <li>区域名称唯一性校验</li>
     * </ul>
     *
     * @param regionList 区域实体对象列表
     * @return 批量新增成功返回true，否则返回false
     * @throws IllegalArgumentException 当违反任何业务规则时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchSaveRegions(List<Region> regionList) {
        if (CollectionUtils.isEmpty(regionList)) {
            return false;
        }

        Long tenantId = regionList.get(0).getTenantId() == null ? SYSTEM_TENANT_ID : regionList.get(0).getTenantId();
        boolean hasInvalidTenant = regionList.stream()
                .anyMatch(r -> {
                    Long rTenantId = r.getTenantId() == null ? SYSTEM_TENANT_ID : r.getTenantId();
                    return !Objects.equals(rTenantId, tenantId);
                });
        Assert.isTrue(!hasInvalidTenant, "批量新增的区域必须属于同一租户");

        if (SYSTEM_TENANT_ID.equals(tenantId)) {
            throw new IllegalArgumentException("系统默认区域（tenantId=0）禁止批量新增");
        }

        for (Region region : regionList) {
            completeDefaultValues(region);
            validateEntityBaseConstraints(region);
            validateParentLevelRelation(region);
            validateRegionCode(region);
            validateRegionNameUnique(tenantId, region.getRegionName(), region.getParentId(), null);
        }

        return saveBatch(regionList);
    }

    /**
     * 批量更新区域排序号（事务保证）
     *
     * <p>在单个事务中执行批量排序更新，任一记录校验失败或更新失败将导致整个操作回滚</p>
     * <p>业务限制：系统默认区域（tenantId=0）禁止批量更新排序</p>
     * <p>批量校验：</p>
     * <ul>
     *   <li>租户一致性校验（所有区域必须属于同一租户）</li>
     *   <li>区域ID非空校验</li>
     *   <li>排序号非空且非负校验</li>
     *   <li>所有区域必须属于当前租户</li>
     * </ul>
     *
     * @param regionList 区域实体对象列表（需包含ID和sort字段）
     * @return 批量更新成功返回true，否则返回false
     * @throws IllegalArgumentException 当违反任何业务规则时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchUpdateRegionSort(List<Region> regionList) {
        if (CollectionUtils.isEmpty(regionList)) {
            return false;
        }

        Long tenantId = regionList.get(0).getTenantId() == null ? SYSTEM_TENANT_ID : regionList.get(0).getTenantId();
        boolean hasInvalidTenant = regionList.stream()
                .anyMatch(r -> {
                    Long rTenantId = r.getTenantId() == null ? SYSTEM_TENANT_ID : r.getTenantId();
                    return !Objects.equals(rTenantId, tenantId);
                });
        Assert.isTrue(!hasInvalidTenant, "批量更新的区域必须属于同一租户");

        if (SYSTEM_TENANT_ID.equals(tenantId)) {
            throw new IllegalArgumentException("系统默认区域（tenantId=0）禁止批量更新排序");
        }

        List<Long> regionIds = regionList.stream().map(Region::getId).collect(Collectors.toList());
        validateRegionIdsBelongToTenant(tenantId, regionIds);

        for (Region region : regionList) {
            Assert.notNull(region.getId(), "区域ID不能为空");
            Assert.notNull(region.getSort(), "排序序号不能为空");
            Assert.isTrue(region.getSort() >= 0, "排序序号不能为负数");
        }

        return updateBatchById(regionList.stream().map(r -> {
            Region updateEntity = new Region();
            updateEntity.setId(r.getId());
            updateEntity.setSort(r.getSort());
            return updateEntity;
        }).collect(Collectors.toList()));
    }

    /**
     * 批量删除区域信息（事务保证）
     *
     * <p>在单个事务中执行批量删除，任一记录校验失败或删除失败将导致整个操作回滚</p>
     * <p>业务限制：存在下级子区域的区域禁止删除</p>
     * <p>批量校验：所有区域必须属于当前租户</p>
     *
     * @param ids 待删除的区域ID列表
     * @param tenantId 租户ID，用于数据隔离验证
     * @return 批量删除成功返回true，否则返回false
     * @throws IllegalArgumentException 当违反删除限制时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchRemoveRegions(List<Long> ids, Long tenantId) {
        if (CollectionUtils.isEmpty(ids)) {
            return false;
        }
        Assert.notNull(tenantId, "租户ID不能为空");

        validateRegionIdsBelongToTenant(tenantId, ids);

        QueryWrapper<Region> childWrapper = new QueryWrapper<>();
        childWrapper.in(COL_PARENT_ID, ids);
        childWrapper.eq(COL_TENANT_ID, tenantId);
        long hasChildCount = baseMapper.selectCount(childWrapper);

        if (hasChildCount > 0) {
            throw new IllegalArgumentException("部分区域存在下级子区域，禁止批量删除");
        }

        return baseMapper.deleteBatchIds(ids) > 0;
    }

    /**
     * 验证区域ID列表是否全部属于当前租户
     *
     * <p>校验逻辑：</p>
     * <ul>
     *   <li>检查ID是否存在（是否存在未查询到的ID）</li>
     *   <li>检查存在的ID是否属于当前租户</li>
     * </ul>
     * <p>权限规则：普通租户仅能操作自身区域，系统租户可操作所有区域</p>
     *
     * @param tenantId 租户ID
     * @param regionIds 待验证的区域ID列表
     * @throws IllegalArgumentException 当存在不存在的ID或无权限操作的ID时抛出
     */
    @Override
    public void validateRegionIdsBelongToTenant(Long tenantId, List<Long> regionIds) {
        if (CollectionUtils.isEmpty(regionIds)) {
            return;
        }

        QueryWrapper<Region> wrapper = new QueryWrapper<>();
        wrapper.select(COL_ID, COL_TENANT_ID);
        wrapper.in(COL_ID, regionIds);
        List<Region> regions = baseMapper.selectList(wrapper);

        Set<Long> existingIds = regions.stream().map(Region::getId).collect(Collectors.toSet());
        List<Long> nonExistentIds = regionIds.stream()
                .filter(id -> !existingIds.contains(id))
                .toList();
        Assert.isTrue(nonExistentIds.isEmpty(), "以下区域ID不存在：" + nonExistentIds);

        if (!SYSTEM_TENANT_ID.equals(tenantId)) {
            List<Long> invalidIds = regions.stream()
                    .filter(r -> !SYSTEM_TENANT_ID.equals(r.getTenantId()) && !Objects.equals(r.getTenantId(), tenantId))
                    .map(Region::getId)
                    .toList();
            Assert.isTrue(invalidIds.isEmpty(), "无权限操作以下区域ID：" + invalidIds);
        }
    }

    // ==================== 私有工具方法 ====================

    /**
     * 补全默认值
     *
     * <p>为区域对象的缺失字段设置默认值</p>
     * <ul>
     *   <li>租户ID未设置 → 设为0（系统公共区域）</li>
     *   <li>父级ID未设置 → 设为0（顶级区域）</li>
     *   <li>排序号未设置 → 设为0</li>
     * </ul>
     *
     * @param region 待补全的区域对象
     */
    private void completeDefaultValues(Region region) {
        if (region.getTenantId() == null) {
            region.setTenantId(SYSTEM_TENANT_ID);
        }
        if (region.getParentId() == null) {
            region.setParentId(SYSTEM_TENANT_ID);
        }
        if (region.getSort() == null) {
            region.setSort(0);
        }
    }

    /**
     * 构建查询条件（严格匹配实体类字段）
     *
     * <p>根据查询参数动态构建查询条件，支持以下参数：</p>
     * <ul>
     *   <li>regionName - 区域名称模糊查询</li>
     *   <li>regionLevel - 区域层级精确查询</li>
     *   <li>parentId - 父级区域ID精确查询</li>
     *   <li>regionCode - 行政编码精确查询（6位数字）</li>
     *   <li>minSort/maxSort - 排序号范围查询</li>
     * </ul>
     * <p>租户隔离规则：普通租户可查询系统区域+本租户区域，系统租户仅查询系统区域</p>
     *
     * @param queryParams 查询参数映射表
     * @param tenantId 租户ID，用于数据隔离
     * @return 构建完成的QueryWrapper对象
     */
    private QueryWrapper<Region> buildQueryWrapper(Map<String, Object> queryParams, Long tenantId) {
        QueryWrapper<Region> wrapper = new QueryWrapper<>();

        if (!SYSTEM_TENANT_ID.equals(tenantId)) {
            wrapper.and(w -> w.eq(COL_TENANT_ID, SYSTEM_TENANT_ID)
                    .or()
                    .eq(COL_TENANT_ID, tenantId));
        } else {
            wrapper.eq(COL_TENANT_ID, SYSTEM_TENANT_ID);
        }

        if (ObjectUtils.isEmpty(queryParams)) {
            return wrapper;
        }

        // 区域名称模糊查询
        if (queryParams.containsKey("regionName") && StringUtils.hasText(queryParams.get("regionName").toString())) {
            wrapper.like(COL_REGION_NAME, queryParams.get("regionName"));
        }

        // 区域层级精确查询
        if (queryParams.containsKey("regionLevel") && queryParams.get("regionLevel") != null) {
            Byte level = Byte.parseByte(queryParams.get("regionLevel").toString());
            Assert.isTrue(VALID_REGION_LEVELS.contains(level), "无效区域层级：" + level);
            wrapper.eq(COL_REGION_LEVEL, level);
        }

        // 父级区域ID精确查询
        if (queryParams.containsKey("parentId") && queryParams.get("parentId") != null) {
            wrapper.eq(COL_PARENT_ID, queryParams.get("parentId"));
        }

        // 行政编码精确查询
        if (queryParams.containsKey("regionCode") && StringUtils.hasText(queryParams.get("regionCode").toString())) {
            String code = queryParams.get("regionCode").toString();
            Assert.isTrue(code.length() == REGION_CODE_LENGTH && code.matches("^\\d{6}$"), "行政编码必须为6位数字");
            wrapper.eq(COL_REGION_CODE, code);
        }

        // 排序号范围查询
        if (queryParams.containsKey("minSort") && queryParams.get("minSort") != null) {
            int minSort = Integer.parseInt(queryParams.get("minSort").toString());
            Assert.isTrue(minSort >= 0, "最小排序序号不能为负数");
            wrapper.ge(COL_SORT, minSort);
        }
        if (queryParams.containsKey("maxSort") && queryParams.get("maxSort") != null) {
            int maxSort = Integer.parseInt(queryParams.get("maxSort").toString());
            Assert.isTrue(maxSort >= 0, "最大排序序号不能为负数");
            wrapper.le(COL_SORT, maxSort);
        }

        return wrapper;
    }

    /**
     * 校验实体类基础约束（非空、长度、数值范围）
     *
     * <p>完全匹配实体类注解配置，确保数据完整性</p>
     * <p>校验内容包括：</p>
     * <ul>
     *   <li>非空字段校验（区域名称、区域层级）</li>
     *   <li>字段长度校验（区域名称）</li>
     *   <li>数值范围校验（层级、排序号、父级ID、租户ID）</li>
     * </ul>
     *
     * @param region 待校验的区域实体对象
     * @throws IllegalArgumentException 当任何校验失败时抛出，包含具体的错误信息
     */
    private void validateEntityBaseConstraints(Region region) {
        Assert.hasLength(region.getRegionName(), "区域名称不能为空");
        Assert.notNull(region.getRegionLevel(), "区域层级不能为空");

        Assert.isTrue(region.getRegionName().length() <= REGION_NAME_MAX_LENGTH,
                "区域名称长度不能超过" + REGION_NAME_MAX_LENGTH + "字符");

        Assert.isTrue(VALID_REGION_LEVELS.contains(region.getRegionLevel()),
                "区域层级仅支持1（省）、2（市）、3（区）、4（街道）");
        Assert.isTrue(region.getParentId() >= 0, "父级区域ID不能为负数");
        Assert.isTrue(region.getSort() >= 0, "排序序号不能为负数");

        if (region.getTenantId() != null) {
            Assert.isTrue(region.getTenantId() >= 0, "租户ID不能为负数");
        }
    }

    /**
     * 校验父子层级关系
     *
     * <p>业务规则：</p>
     * <ul>
     *   <li>省级区域（层级1）的父级ID必须为0</li>
     *   <li>非省级区域（层级2-4）的父级ID不能为0</li>
     *   <li>父级区域必须存在</li>
     *   <li>父级区域的层级必须比当前区域层级小1</li>
     * </ul>
     *
     * @param region 待校验的区域实体对象
     * @throws IllegalArgumentException 当父子层级关系不合法时抛出
     */
    private void validateParentLevelRelation(Region region) {
        Long parentId = region.getParentId();
        Byte level = region.getRegionLevel();

        if (level == 1 && !SYSTEM_TENANT_ID.equals(parentId)) {
            throw new IllegalArgumentException("省级区域（层级1）的父级ID必须为0");
        }

        if (level > 1) {
            if (SYSTEM_TENANT_ID.equals(parentId)) {
                throw new IllegalArgumentException("非省级区域（层级2-4）的父级ID不能为0");
            }

            Region parentRegion = getRegionById(parentId, region.getTenantId());
            Assert.notNull(parentRegion, "父级区域不存在");
            if (parentRegion.getRegionLevel() != level - 1) {
                throw new IllegalArgumentException(
                        "当前区域层级为" + level + "，父级区域层级必须为" + (level - 1) + "，实际为" + parentRegion.getRegionLevel()
                );
            }
        }
    }

    /**
     * 校验行政编码
     *
     * <p>业务规则：</p>
     * <ul>
     *   <li>系统默认区域（tenantId=0）：行政编码必须非空且为6位数字</li>
     *   <li>租户自定义区域：行政编码可为空，若不为空则必须为6位数字</li>
     * </ul>
     *
     * @param region 待校验的区域实体对象
     * @throws IllegalArgumentException 当行政编码格式不合法时抛出
     */
    private void validateRegionCode(Region region) {
        String code = region.getRegionCode();
        Long tenantId = region.getTenantId();

        if (SYSTEM_TENANT_ID.equals(tenantId)) {
            Assert.hasLength(code, "系统默认区域的行政编码不能为空");
            Assert.isTrue(code.length() == REGION_CODE_LENGTH && code.matches("^\\d{6}$"),
                    "行政编码必须为6位数字，当前值：" + code);
        }

        if (!SYSTEM_TENANT_ID.equals(tenantId) && StringUtils.hasText(code)) {
            Assert.isTrue(code.length() == REGION_CODE_LENGTH && code.matches("^\\d{6}$"),
                    "行政编码必须为6位数字，当前值：" + code);
        }
    }

    /**
     * 校验同一租户下同父级区域名称唯一性
     *
     * @param tenantId 租户ID
     * @param regionName 区域名称
     * @param parentId 父级区域ID
     * @param excludeId 排除的ID（更新场景下排除自身）
     * @throws IllegalArgumentException 当区域名称已存在时抛出
     */
    private void validateRegionNameUnique(Long tenantId, String regionName, Long parentId, Long excludeId) {
        QueryWrapper<Region> wrapper = new QueryWrapper<>();
        wrapper.eq(COL_TENANT_ID, tenantId);
        wrapper.eq(COL_PARENT_ID, parentId);
        wrapper.eq(COL_REGION_NAME, regionName);

        if (excludeId != null) {
            wrapper.ne(COL_ID, excludeId);
        }

        long count = baseMapper.selectCount(wrapper);
        if (count > 0) {
            throw new IllegalArgumentException("同一租户+同父级下区域名称已存在：" + regionName);
        }
    }

    /**
     * 校验不可修改的核心字段
     *
     * <p>以下字段在创建后不允许修改：</p>
     * <ul>
     *   <li>租户ID（tenantId）</li>
     *   <li>父级区域ID（parentId）</li>
     *   <li>区域层级（regionLevel）</li>
     *   <li>行政编码（regionCode）</li>
     *   <li>创建时间（createTime）</li>
     * </ul>
     *
     * @param region 待校验的区域对象
     * @throws IllegalArgumentException 当尝试修改不可变字段时抛出
     */
    private void validateImmutableFields(Region region) {
        if (region.getTenantId() != null) {
            throw new IllegalArgumentException("租户ID禁止修改");
        }
        if (region.getParentId() != null) {
            throw new IllegalArgumentException("父级区域ID禁止修改");
        }
        if (region.getRegionLevel() != null) {
            throw new IllegalArgumentException("区域层级禁止修改");
        }
        if (region.getRegionCode() != null) {
            throw new IllegalArgumentException("行政编码禁止修改");
        }
        if (region.getCreateTime() != null) {
            throw new IllegalArgumentException("创建时间禁止修改");
        }
    }

    /**
     * 校验更新字段的约束（仅校验非空的修改字段）
     *
     * <p>用于更新操作，仅对传入的非空字段进行约束校验</p>
     *
     * @param region 待校验的区域对象
     * @throws IllegalArgumentException 当修改字段违反约束时抛出
     */
    private void validateUpdateFieldConstraints(Region region) {
        if (region.getRegionName() != null) {
            Assert.isTrue(region.getRegionName().length() <= REGION_NAME_MAX_LENGTH,
                    "区域名称长度不能超过" + REGION_NAME_MAX_LENGTH + "字符");
        }
        if (region.getSort() != null) {
            Assert.isTrue(region.getSort() >= 0, "排序序号不能为负数");
        }
    }

    /**
     * 构建区域树形结构
     *
     * <p>递归构建区域树，将扁平的区域列表转换为树形结构</p>
     * <p>构建逻辑：</p>
     * <ul>
     *   <li>查找所有parentId等于指定父级ID的区域</li>
     *   <li>递归构建每个区域的子区域树</li>
     *   <li>按排序号升序排列</li>
     * </ul>
     *
     * @param allRegions 所有区域列表
     * @param parentId 父级区域ID
     * @return 树形结构区域列表
     */
    private List<Region> buildRegionTree(List<Region> allRegions, Long parentId) {
        List<Region> treeNodes = new ArrayList<>();
        for (Region region : allRegions) {
            if (Objects.equals(region.getParentId(), parentId)) {
                List<Region> children = buildRegionTree(allRegions, region.getId());
                region.setChildren(children);
                treeNodes.add(region);
            }
        }
        return treeNodes.stream().sorted(Comparator.comparingInt(Region::getSort)).collect(Collectors.toList());
    }
}
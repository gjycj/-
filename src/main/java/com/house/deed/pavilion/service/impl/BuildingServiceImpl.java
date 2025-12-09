package com.house.deed.pavilion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.entity.Building;
import com.house.deed.pavilion.mapper.BuildingMapper;
import com.house.deed.pavilion.service.BuildingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 楼栋信息表（租户级数据）服务实现类
 * </p>
 *
 * 核心业务说明：
 * 1. 负责楼栋信息的CRUD操作及多条件查询、批量处理，是房源管理的基础数据支撑；
 * 2. 严格租户隔离：所有操作均强制校验tenantId，确保数据仅归属租户可见可控；
 * 3. 业务约束：维护同一楼盘下楼栋号的唯一性，支撑房源与楼栋的关联关系；
 * 4. 与其他实体关联：关联Property（楼盘）、House（房源），需保证关联数据的一致性。
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Service
public class BuildingServiceImpl extends ServiceImpl<BuildingMapper, Building> implements BuildingService {

    /**
     * 新增楼栋信息（带租户校验）
     * <p>
     * 业务逻辑：
     * 1. 校验租户ID非空；
     * 2. 校验同一楼盘+租户内楼栋号唯一，避免重复；
     * 3. 调用底层层插入数据。
     *
     * @param building 楼栋实体（必须包含tenantId、propertyId、buildingNo等核心字段）
     * @return true-新增成功，false-新增失败
     * @throws IllegalArgumentException 当租户ID为空或楼栋号重复时抛出
     */
    @Override
    public boolean saveBuilding(Building building) {
        // 校验租户ID存在
        if (building.getTenantId() == null) {
            throw new IllegalArgumentException("租户ID不能为空");
        }

        // 校验楼栋号在同一楼盘+租户内唯一
        LambdaQueryWrapper<Building> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Building::getTenantId, building.getTenantId())
                .eq(Building::getPropertyId, building.getPropertyId())
                .eq(Building::getBuildingNo, building.getBuildingNo());
        long count = baseMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new IllegalArgumentException("当前楼盘下已存在相同楼栋号：" + building.getBuildingNo());
        }

        return baseMapper.insert(building) > 0;
    }

    /**
     * 根据ID更新楼栋信息（带租户校验）
     * <p>
     * 业务逻辑：
     * 1. 校验租户ID和主键ID非空；
     * 2. 校验楼栋归属当前租户，防止越权操作；
     * 3. 若更新楼栋号，需再次校验同一楼盘内唯一性（排除自身）；
     * 4. 调用Mapper层更新数据。
     *
     * @param building 楼栋实体（必须包含id和tenantId）
     * @return true-更新成功，false-更新失败
     * @throws IllegalArgumentException 当租户ID/主键为空、数据归属异常或楼栋号重复时抛出
     */
    @Override
    public boolean updateBuildingById(Building building) {
        // 校验租户ID和主键存在
        if (building.getTenantId() == null || building.getId() == null) {
            throw new IllegalArgumentException("租户ID和楼栋ID不能为空");
        }

        // 校验数据归属当前租户
        Building existBuilding = baseMapper.selectById(building.getId());
        if (existBuilding == null || !existBuilding.getTenantId().equals(building.getTenantId())) {
            throw new IllegalArgumentException("楼栋不存在或无权限操作");
        }

        // 若更新楼栋号，需再次校验唯一性（排除自身）
        if (building.getBuildingNo() != null) {
            LambdaQueryWrapper<Building> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Building::getTenantId, building.getTenantId())
                    .eq(Building::getPropertyId, building.getPropertyId())
                    .eq(Building::getBuildingNo, building.getBuildingNo())
                    .ne(Building::getId, building.getId());
            long count = baseMapper.selectCount(queryWrapper);
            if (count > 0) {
                throw new IllegalArgumentException("当前楼盘下已存在相同楼栋号：" + building.getBuildingNo());
            }
        }

        return baseMapper.updateById(building) > 0;
    }

    /**
     * 根据ID删除楼栋（带租户校验）
     * <p>
     * 业务逻辑：
     * 1. 校验楼栋ID和租户ID非空；
     * 2. 校验楼栋归属校验，确保待删除楼栋属于当前租户所有；
     * 3. 调用底层删除方法（实际业务中可能需先校验楼栋下是否有房源，防止误删）。
     *
     * @param id        楼栋ID
     * @param tenantId  租户ID（用于数据隔离校验）
     * @return true-删除成功，false-删除失败
     * @throws IllegalArgumentException 当ID/租户ID为空或数据归属异常时抛出
     */
    @Override
    public boolean removeBuildingById(Long id, Long tenantId) {
        if (id == null || tenantId == null) {
            throw new IllegalArgumentException("楼栋ID和租户ID不能为空");
        }

        // 校验数据归属当前租户
        Building existBuilding = baseMapper.selectById(id);
        if (existBuilding == null || !existBuilding.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("楼栋不存在或无权限操作");
        }

        return baseMapper.deleteById(id) > 0;
    }

    /**
     * 根据ID查询楼栋（带租户隔离）
     * <p>
     * 业务逻辑：
     * 1. 校验ID和租户ID非空；
     * 2. 联合ID和租户ID查询，确保返回数据归属当前租户。
     *
     * @param id       楼栋ID
     * @param tenantId 租户ID
     * @return 楼栋实体（null表示不存在或无权限）
     */
    @Override
    public Building getBuildingById(Long id, Long tenantId) {
        if (id == null || tenantId == null) {
            return null;
        }

        LambdaQueryWrapper<Building> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Building::getId, id)
                .eq(Building::getTenantId, tenantId);
        return baseMapper.selectOne(queryWrapper);
    }

    /**
     * 分页查询楼栋（支持多条件+租户隔离）
     * <p>
     * 业务逻辑：
     * 1. 基于传入的查询参数构建动态条件（如楼盘ID、楼栋号模糊匹配等）；
     * 2. 强制附加租户ID条件，确保数据隔离；
     * 3. 调用分页查询方法，返回分页结果。
     *
     * @param page        分页参数（页码、每页条数）
     * @param queryParams 查询条件（支持：propertyId、buildingNo、buildingType等）
     * @param tenantId    租户ID（强制隔离）
     * @return 分页结果（含数据列表和分页信息）
     */
    @Override
    public IPage<Building> pageQuery(Page<Building> page, Map<String, Object> queryParams, Long tenantId) {
        LambdaQueryWrapper<Building> queryWrapper = buildQueryWrapper(queryParams, tenantId);
        return baseMapper.selectPage(page, queryWrapper);
    }

    /**
     * 多条件查询楼栋列表（带租户隔离）
     * <p>
     * 业务逻辑：
     * 1. 复用条件构建方法，生成查询条件；
     * 2. 强制租户隔离，返回符合条件的楼栋列表。
     *
     * @param queryParams 查询条件（支持：propertyId、unitCount、totalFloor等）
     * @param tenantId    租户ID
     * @return 符合条件的楼栋列表
     */
    @Override
    public List<Building> listByConditions(Map<String, Object> queryParams, Long tenantId) {
        LambdaQueryWrapper<Building> queryWrapper = buildQueryWrapper(queryParams, tenantId);
        return baseMapper.selectList(queryWrapper);
    }

    /**
     * 根据楼盘ID查询楼栋列表（租户隔离）
     * <p>
     * 业务逻辑：
     * 1. 校验楼盘ID和租户ID非空；
     * 2. 按楼盘ID+租户ID查询，结果按楼栋号升序排列，符合用户认知习惯。
     *
     * @param propertyId 楼盘ID
     * @param tenantId   租户ID
     * @return 该楼盘下的所有楼栋
     */
    @Override
    public List<Building> listByPropertyId(Long propertyId, Long tenantId) {
        if (propertyId == null || tenantId == null) {
            return List.of();
        }

        LambdaQueryWrapper<Building> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Building::getTenantId, tenantId)
                .eq(Building::getPropertyId, propertyId)
                .orderByAsc(Building::getBuildingNo); // 按楼栋号升序排列
        return baseMapper.selectList(queryWrapper);
    }

    /**
     * 构建查询条件（复用逻辑）
     * <p>
     * 内部工具方法，统一处理动态条件拼接，确保租户隔离和条件正确性。
     *
     * @param queryParams 动态查询参数
     * @param tenantId    租户ID（强制加入条件）
     * @return 构建完成的查询条件包装器
     */
    private LambdaQueryWrapper<Building> buildQueryWrapper(Map<String, Object> queryParams, Long tenantId) {
        LambdaQueryWrapper<Building> queryWrapper = new LambdaQueryWrapper<>();
        // 强制租户隔离
        queryWrapper.eq(Building::getTenantId, tenantId);

        if (queryParams == null) {
            return queryWrapper;
        }

        // 动态拼接条件（根据实体类字段扩展）
        if (queryParams.containsKey("propertyId") && queryParams.get("propertyId") != null) {
            queryWrapper.eq(Building::getPropertyId, queryParams.get("propertyId"));
        }
        if (queryParams.containsKey("buildingNo") && queryParams.get("buildingNo") != null) {
            queryWrapper.like(Building::getBuildingNo, queryParams.get("buildingNo"));
        }
        if (queryParams.containsKey("buildingType") && queryParams.get("buildingType") != null) {
            queryWrapper.eq(Building::getBuildingType, queryParams.get("buildingType"));
        }
        if (queryParams.containsKey("totalFloor") && queryParams.get("totalFloor") != null) {
            queryWrapper.eq(Building::getTotalFloor, queryParams.get("totalFloor"));
        }
        if (queryParams.containsKey("unitCount") && queryParams.get("unitCount") != null) {
            queryWrapper.eq(Building::getUnitCount, queryParams.get("unitCount"));
        }

        // 默认按创建时间降序
        queryWrapper.orderByDesc(Building::getCreateTime);
        return queryWrapper;
    }

    /**
     * 批量新增楼栋（事务保证，统一租户）
     * <p>
     * 业务逻辑：
     * 1. 校验列表非空，且所有楼栋属于同一租户；
     * 2. 批量校验楼栋号唯一性（同一楼盘+租户内）；
     * 3. 开启事务，批量插入数据，确保原子性。
     *
     * @param buildingList 楼栋列表（必须包含相同tenantId）
     * @return true-全部新增成功，false-新增失败（事务回滚）
     * @throws IllegalArgumentException 当列表为空、租户ID不一致或楼栋号重复时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchSaveBuildings(List<Building> buildingList) {
        if (CollectionUtils.isEmpty(buildingList)) {
            return false;
        }

        // 校验所有楼栋属于同一租户
        Long tenantId = buildingList.get(0).getTenantId();
        if (tenantId == null) {
            throw new IllegalArgumentException("租户ID不能为空");
        }
        boolean hasInvalidTenant = buildingList.stream()
                .anyMatch(building -> !tenantId.equals(building.getTenantId()));
        if (hasInvalidTenant) {
            throw new IllegalArgumentException("批量操作的楼栋必须属于同一租户");
        }

        // 批量校验楼栋号唯一性
        for (Building building : buildingList) {
            LambdaQueryWrapper<Building> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Building::getTenantId, tenantId)
                    .eq(Building::getPropertyId, building.getPropertyId())
                    .eq(Building::getBuildingNo, building.getBuildingNo());
            if (baseMapper.selectCount(queryWrapper) > 0) {
                throw new IllegalArgumentException("楼盘[" + building.getPropertyId() + "]下已存在楼栋号：" + building.getBuildingNo());
            }
        }

        return saveBatch(buildingList);
    }

    /**
     * 批量删除楼栋（带租户校验）
     * <p>
     * 业务逻辑：
     * 1. 校验ID列表和租户ID非空；
     * 2. 校验所有ID归属当前租户（调用校验方法）；
     * 3. 开启事务，批量删除数据。
     *
     * @param ids       楼栋ID列表
     * @param tenantId  租户ID
     * @return true-全部删除成功，false-删除失败
     * @throws IllegalArgumentException 当列表为空、租户ID为空或存在非当前租户ID时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchRemoveBuildings(List<Long> ids, Long tenantId) {
        if (CollectionUtils.isEmpty(ids) || tenantId == null) {
            return false;
        }

        // 校验所有ID属于当前租户
        validateBuildingIdsBelongToTenant(tenantId, ids);

        return baseMapper.deleteBatchIds(ids) > 0;
    }

    /**
     * 校验楼栋ID列表是否均属于当前租户
     * <p>
     * 业务逻辑：
     * 1. 统计租户下存在的ID数量，与传入列表长度对比；
     * 2. 数量不一致则说明存在非当前租户的ID，抛出异常。
     *
     * @param tenantId    租户ID
     * @param buildingIds 楼栋ID列表
     * @throws IllegalArgumentException 当存在不属于当前租户的ID时抛出
     */
    @Override
    public void validateBuildingIdsBelongToTenant(Long tenantId, List<Long> buildingIds) {
        if (CollectionUtils.isEmpty(buildingIds) || tenantId == null) {
            return;
        }

        // 查询租户下存在的楼栋ID数量
        LambdaQueryWrapper<Building> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Building::getTenantId, tenantId)
                .in(Building::getId, buildingIds);
        long count = baseMapper.selectCount(queryWrapper);

        if (count != buildingIds.size()) {
            throw new IllegalArgumentException("存在不属于当前租户的楼栋ID");
        }
    }
}
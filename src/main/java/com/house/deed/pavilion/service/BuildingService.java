package com.house.deed.pavilion.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.house.deed.pavilion.entity.Building;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 楼栋信息表（租户级数据） 服务类
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
public interface BuildingService extends IService<Building> {

    // ==================== 基础CRUD（增强租户隔离） ====================
    /**
     * 新增楼栋信息（带租户校验）
     * @param building 楼栋实体（必须包含tenantId）
     * @return 是否新增成功
     */
    boolean saveBuilding(Building building);

    /**
     * 根据ID更新楼栋信息（带租户校验）
     * @param building 楼栋实体（必须包含id和tenantId）
     * @return 是否更新成功
     */
    boolean updateBuildingById(Building building);

    /**
     * 根据ID删除楼栋（带租户校验）
     * @param id 楼栋ID
     * @param tenantId 租户ID（用于数据隔离校验）
     * @return 是否删除成功
     */
    boolean removeBuildingById(Long id, Long tenantId);

    /**
     * 根据ID查询楼栋（带租户隔离）
     * @param id 楼栋ID
     * @param tenantId 租户ID
     * @return 楼栋实体（null表示不存在或无权限）
     */
    Building getBuildingById(Long id, Long tenantId);


    // ==================== 多条件查询 ====================
    /**
     * 分页查询楼栋（支持多条件+租户隔离）
     * @param page 分页参数（页码、每页条数）
     * @param queryParams 查询条件（支持：propertyId、buildingNo、buildingType等）
     * @param tenantId 租户ID（强制隔离）
     * @return 分页结果（含数据列表和分页信息）
     */
    IPage<Building> pageQuery(Page<Building> page, Map<String, Object> queryParams, Long tenantId);

    /**
     * 多条件查询楼栋列表（带租户隔离）
     * @param queryParams 查询条件（支持：propertyId、unitCount、totalFloor等）
     * @param tenantId 租户ID
     * @return 符合条件的楼栋列表
     */
    List<Building> listByConditions(Map<String, Object> queryParams, Long tenantId);

    /**
     * 根据楼盘ID查询楼栋列表（租户隔离）
     * @param propertyId 楼盘ID
     * @param tenantId 租户ID
     * @return 该楼盘下的所有楼栋
     */
    List<Building> listByPropertyId(Long propertyId, Long tenantId);


    // ==================== 批量操作 ====================
    /**
     * 批量新增楼栋（事务保证，统一租户）
     * @param buildingList 楼栋列表（必须包含相同tenantId）
     * @return 是否全部新增成功
     */
    boolean batchSaveBuildings(List<Building> buildingList);

    /**
     * 批量删除楼栋（带租户校验）
     * @param ids 楼栋ID列表
     * @param tenantId 租户ID
     * @return 是否全部删除成功
     */
    boolean batchRemoveBuildings(List<Long> ids, Long tenantId);

    /**
     * 校验楼栋ID列表是否均属于当前租户
     * @param tenantId 租户ID
     * @param buildingIds 楼栋ID列表
     * @throws IllegalArgumentException 当存在不属于当前租户的ID时抛出
     */
    void validateBuildingIdsBelongToTenant(Long tenantId, List<Long> buildingIds);



}
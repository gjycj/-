package com.house.deed.pavilion.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.house.deed.pavilion.entity.Region;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 区域管理表（租户级数据隔离 + 树形层级结构） 服务类
 * </p>
 * 核心业务：区域CRUD、树形结构查询、多条件筛选、批量操作（严格遵循层级约束+租户隔离）
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
public interface RegionService extends IService<Region> {

    // ==================== 基础CRUD（层级约束+租户隔离） ====================
    /**
     * 新增区域（校验层级/父级关联/行政编码/租户规则）
     * @param region 区域实体
     * @return 是否新增成功
     */
    boolean saveRegion(Region region);

    /**
     * 更新区域（系统默认区域禁止修改，租户自定义仅允许修改名称/排序）
     * @param region 区域实体
     * @return 是否更新成功
     */
    boolean updateRegionById(Region region);

    /**
     * 删除区域（系统默认区域禁止删除，租户自定义区域需无下级子区域）
     * @param id 区域ID
     * @param tenantId 租户ID
     * @return 是否删除成功
     */
    boolean removeRegionById(Long id, Long tenantId);

    /**
     * 查询区域详情（租户隔离：系统默认区域所有人可见，租户自定义仅自身可见）
     * @param id 区域ID
     * @param tenantId 租户ID（0表示查询系统默认区域）
     * @return 区域实体
     */
    Region getRegionById(Long id, Long tenantId);

    // ==================== 多条件+树形查询（贴合业务场景） ====================
    /**
     * 分页查询区域（多条件+租户隔离）
     * @param page 分页参数
     * @param queryParams 查询条件（regionName/regionLevel/parentId/regionCode等）
     * @param tenantId 租户ID
     * @return 分页结果
     */
    IPage<Region> pageQuery(Page<Region> page, Map<String, Object> queryParams, Long tenantId);

    /**
     * 多条件查询区域列表（租户隔离）
     * @param queryParams 查询条件
     * @param tenantId 租户ID
     * @return 区域列表
     */
    List<Region> listByConditions(Map<String, Object> queryParams, Long tenantId);

    /**
     * 查询指定父级下的子区域（租户隔离+层级约束）
     * @param parentId 父级区域ID
     * @param tenantId 租户ID
     * @return 子区域列表（按sort升序）
     */
    List<Region> listChildrenByParentId(Long parentId, Long tenantId);

    /**
     * 查询指定层级的区域（租户隔离）
     * @param regionLevel 区域层级（1=省，2=市，3=区，4=街道）
     * @param tenantId 租户ID
     * @return 区域列表
     */
    List<Region> listByRegionLevel(Byte regionLevel, Long tenantId);

    /**
     * 构建区域树形结构（租户隔离，递归查询子级）
     * @param tenantId 租户ID
     * @return 树形结构区域列表
     */
    List<Region> listRegionTree(Long tenantId);

    // ==================== 批量操作（事务保障+约束校验） ====================
    /**
     * 批量新增租户自定义区域（同一租户，事务保证）
     * @param regionList 区域列表
     * @return 是否批量新增成功
     */
    boolean batchSaveRegions(List<Region> regionList);

    /**
     * 批量更新区域排序（仅租户自定义区域，事务保证）
     * @param regionList 仅含id/sort/tenantId的区域列表
     * @return 是否批量更新成功
     */
    boolean batchUpdateRegionSort(List<Region> regionList);

    /**
     * 批量删除租户自定义区域（需无下级子区域，事务保证）
     * @param ids 区域ID列表
     * @param tenantId 租户ID
     * @return 是否批量删除成功
     */
    boolean batchRemoveRegions(List<Long> ids, Long tenantId);

    /**
     * 校验区域ID列表是否属于当前租户（系统默认区域无需校验）
     * @param tenantId 租户ID
     * @param regionIds 区域ID列表
     */
    void validateRegionIdsBelongToTenant(Long tenantId, List<Long> regionIds);
}
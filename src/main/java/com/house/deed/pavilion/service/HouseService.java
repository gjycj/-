package com.house.deed.pavilion.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.house.deed.pavilion.entity.House;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 房源信息表（租户核心数据） 服务类
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
public interface HouseService extends IService<House> {

    // ==================== 基础CRUD（增强租户隔离） ====================
    /**
     * 新增房源（强制租户绑定）
     * @param house 房源实体
     * @param tenantId 租户ID
     * @return 是否新增成功
     */
    boolean saveHouse(House house, Long tenantId);

    /**
     * 更新房源（校验租户归属）
     * @param house 房源实体
     * @param tenantId 租户ID
     * @return 是否更新成功
     */
    boolean updateHouseById(House house, Long tenantId);

    /**
     * 删除房源（校验租户归属）
     * @param id 房源ID
     * @param tenantId 租户ID
     * @return 是否删除成功
     */
    boolean removeHouseById(Long id, Long tenantId);

    /**
     * 按ID查询房源（租户隔离）
     * @param id 房源ID
     * @param tenantId 租户ID
     * @return 房源实体
     */
    House getHouseById(Long id, Long tenantId);

    // ==================== 多条件查询 ====================
    /**
     * 分页查询房源（多条件+租户隔离）
     * @param page 分页参数（页码/每页条数）
     * @param queryParams 查询条件（房源编号/状态/户型/面积/价格等）
     * @param tenantId 租户ID
     * @return 分页结果
     */
    IPage<House> pageQuery(Page<House> page, Map<String, Object> queryParams, Long tenantId);

    /**
     * 多条件查询房源列表（租户隔离）
     * @param queryParams 查询条件
     * @param tenantId 租户ID
     * @return 房源列表
     */
    List<House> listByConditions(Map<String, Object> queryParams, Long tenantId);

    /**
     * 按楼盘ID查询房源（租户隔离）
     * @param propertyId 楼盘ID
     * @param tenantId 租户ID
     * @return 房源列表
     */
    List<House> listByPropertyId(Long propertyId, Long tenantId);

    // ==================== 批量操作 ====================
    /**
     * 批量新增房源（同一租户）
     * @param houseList 房源列表
     * @param tenantId 租户ID
     * @return 是否批量新增成功
     */
    boolean batchSaveHouses(List<House> houseList, Long tenantId);

    /**
     * 批量更新房源状态（租户隔离）
     * @param ids 房源ID列表
     * @param status 目标状态（如：1=在售，2=已租，3=已售）
     * @param tenantId 租户ID
     * @return 是否批量更新成功
     */
    boolean batchUpdateStatus(List<Long> ids, String status, Long tenantId);

    /**
     * 批量删除房源（租户隔离）
     * @param ids 房源ID列表
     * @param tenantId 租户ID
     * @return 是否批量删除成功
     */
    boolean batchRemoveHouses(List<Long> ids, Long tenantId);
}
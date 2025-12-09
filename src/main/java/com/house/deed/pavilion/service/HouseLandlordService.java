package com.house.deed.pavilion.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.house.deed.pavilion.entity.HouseLandlord;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 房源与房东关联表（租户级数据） 服务类
 * </p>
 * <p>
 * 负责房源与房东关联关系的管理，提供CRUD、多条件查询、批量操作等功能，
 * 所有操作需遵守租户数据隔离规则，确保数据安全性。
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
public interface HouseLandlordService extends IService<HouseLandlord> {

    // ==================== 基础CRUD操作 ====================

    /**
     * 新增房源与房东关联关系（带租户校验）
     *
     * @param houseLandlord 关联关系实体（需包含houseId、landlordId、tenantId）
     * @return 是否新增成功
     */
    boolean saveHouseLandlord(HouseLandlord houseLandlord);

    /**
     * 更新关联关系（带租户校验）
     *
     * @param houseLandlord 关联关系实体（需包含id和tenantId）
     * @return 是否更新成功
     */
    boolean updateHouseLandlordById(HouseLandlord houseLandlord);

    /**
     * 删除关联关系（带租户校验）
     *
     * @param id       关联ID
     * @param tenantId 租户ID
     * @return 是否删除成功
     */
    boolean removeHouseLandlordById(Long id, Long tenantId);

    /**
     * 按ID查询关联关系（租户隔离）
     *
     * @param id       关联ID
     * @param tenantId 租户ID
     * @return 关联关系实体
     */
    HouseLandlord getHouseLandlordById(Long id, Long tenantId);

    // ==================== 多条件查询 ====================

    /**
     * 分页查询关联关系（多条件+租户隔离）
     *
     * @param page        分页参数
     * @param queryParams 查询条件（支持houseId、landlordId等）
     * @param tenantId    租户ID
     * @return 分页结果
     */
    IPage<HouseLandlord> pageQuery(Page<HouseLandlord> page, Map<String, Object> queryParams, Long tenantId);

    /**
     * 多条件查询查询关联关系列表（租户隔离）
     *
     * @param queryParams 查询条件
     * @param tenantId    租户ID
     * @return 关联关系列表
     */
    List<HouseLandlord> listByConditions(Map<String, Object> queryParams, Long tenantId);

    /**
     * 按房源ID查询关联的房东（租户隔离）
     *
     * @param houseId   房源ID
     * @param tenantId  租户ID
     * @return 关联的房东列表
     */
    List<HouseLandlord> listByHouseId(Long houseId, Long tenantId);

    /**
     * 按房东ID查询关联的房源（租户隔离）
     *
     * @param landlordId 房东ID
     * @param tenantId   租户ID
     * @return 关联的房源列表
     */
    List<HouseLandlord> listByLandlordId(Long landlordId, Long tenantId);

    // ==================== 批量操作 ====================

    /**
     * 批量新增关联关系（同一租户）
     *
     * @param houseLandlordList 关联关系列表
     * @return 是否批量新增成功
     */
    boolean batchSaveHouseLandlords(List<HouseLandlord> houseLandlordList);

    /**
     * 批量删除关联关系（租户隔离）
     *
     * @param ids      关联ID列表
     * @param tenantId 租户ID
     * @return 是否批量删除成功
     */
    boolean batchRemoveHouseLandlords(List<Long> ids, Long tenantId);
}
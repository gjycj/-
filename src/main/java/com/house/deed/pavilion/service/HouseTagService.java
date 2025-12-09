package com.house.deed.pavilion.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.house.deed.pavilion.entity.HouseTag;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 房源与标签关联表（租户级数据） 服务类
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
public interface HouseTagService extends IService<HouseTag> {

    // ==================== 基础CRUD（增强租户校验） ====================
    /**
     * 新增房源标签关联（带唯一性校验）
     * @param houseTag 关联实体
     * @return 是否新增成功
     */
    boolean saveHouseTag(HouseTag houseTag);

    /**
     * 更新房源标签关联（仅允许更新非核心字段）
     * @param houseTag 关联实体
     * @return 是否更新成功
     */
    boolean updateHouseTagById(HouseTag houseTag);

    /**
     * 删除房源标签关联（租户隔离）
     * @param id 关联ID
     * @param tenantId 租户ID
     * @return 是否删除成功
     */
    boolean removeHouseTagById(Long id, Long tenantId);

    /**
     * 按ID查询关联（租户隔离）
     * @param id 关联ID
     * @param tenantId 租户ID
     * @return 关联实体
     */
    HouseTag getHouseTagById(Long id, Long tenantId);


    // ==================== 多条件查询 ====================
    /**
     * 分页查询关联记录（多条件+租户隔离）
     * @param page 分页参数
     * @param queryParams 查询条件（支持houseId、tagId等）
     * @param tenantId 租户ID
     * @return 分页结果
     */
    IPage<HouseTag> pageQuery(Page<HouseTag> page, Map<String, Object> queryParams, Long tenantId);

    /**
     * 多条件查询关联列表（租户隔离）
     * @param queryParams 查询条件
     * @param tenantId 租户ID
     * @return 关联列表
     */
    List<HouseTag> listByConditions(Map<String, Object> queryParams, Long tenantId);

    /**
     * 按房源ID查询关联标签（租户隔离）
     * @param houseId 房源ID
     * @param tenantId 租户ID
     * @return 关联列表
     */
    List<HouseTag> listByHouseId(Long houseId, Long tenantId);

    /**
     * 按标签ID查询关联房源（租户隔离）
     * @param tagId 标签ID
     * @param tenantId 租户ID
     * @return 关联列表
     */
    List<HouseTag> listByTagId(Long tagId, Long tenantId);


    // ==================== 批量操作 ====================
    /**
     * 批量新增房源标签关联（同一租户）
     * @param houseTagList 关联列表
     * @return 是否批量新增成功
     */
    boolean batchSaveHouseTags(List<HouseTag> houseTagList);

    /**
     * 批量删除房源标签关联（租户隔离）
     * @param ids 关联ID列表
     * @param tenantId 租户ID
     * @return 是否批量删除成功
     */
    boolean batchRemoveHouseTags(List<Long> ids, Long tenantId);

    /**
     * 批量删除房源的所有标签关联（租户隔离）
     * @param houseId 房源ID
     * @param tenantId 租户ID
     * @return 是否删除成功
     */
    boolean batchRemoveByHouseId(Long houseId, Long tenantId);
}
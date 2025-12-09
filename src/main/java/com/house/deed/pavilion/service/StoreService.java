package com.house.deed.pavilion.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.house.deed.pavilion.entity.Store;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 门店信息表（租户级数据） 服务类
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
public interface StoreService extends IService<Store> {

    // ==================== 基础CRUD（增强租户隔离） ====================
    /**
     * 新增门店（强制租户绑定+编码唯一校验）
     * @param store 门店实体
     * @return 是否新增成功
     */
    boolean saveStore(Store store);

    /**
     * 更新门店（校验租户归属+编码唯一校验）
     * @param store 门店实体
     * @return 是否更新成功
     */
    boolean updateStoreById(Store store);

    /**
     * 删除门店（校验租户归属）
     * @param id 门店ID
     * @param tenantId 租户ID
     * @return 是否删除成功
     */
    boolean removeStoreById(Long id, Long tenantId);

    /**
     * 按ID查询门店（租户隔离）
     * @param id 门店ID
     * @param tenantId 租户ID
     * @return 门店实体
     */
    Store getStoreById(Long id, Long tenantId);

    // ==================== 多条件查询 ====================
    /**
     * 分页查询门店（多条件+租户隔离）
     * @param page 分页参数
     * @param queryParams 查询条件（区域ID/状态/门店名称等）
     * @param tenantId 租户ID
     * @return 分页结果
     */
    IPage<Store> pageQuery(Page<Store> page, Map<String, Object> queryParams, Long tenantId);

    /**
     * 多条件查询门店列表（租户隔离）
     * @param queryParams 查询条件
     * @param tenantId 租户ID
     * @return 门店列表
     */
    List<Store> listByConditions(Map<String, Object> queryParams, Long tenantId);

    /**
     * 按区域ID查询门店（租户隔离）
     * @param regionId 区域ID
     * @param tenantId 租户ID
     * @return 门店列表
     */
    List<Store> listByRegionId(Long regionId, Long tenantId);

    // ==================== 批量操作 ====================
    /**
     * 批量新增门店（同一租户）
     * @param storeList 门店列表
     * @return 是否批量新增成功
     */
    boolean batchSaveStores(List<Store> storeList);

    /**
     * 批量更新门店状态（租户隔离）
     * @param ids 门店ID列表
     * @param status 目标状态（0=停业，1=营业）
     * @param tenantId 租户ID
     * @return 是否批量更新成功
     */
    boolean batchUpdateStatus(List<Long> ids, Byte status, Long tenantId);
}
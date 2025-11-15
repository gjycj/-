package com.house.deed.pavilion.module.store.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.house.deed.pavilion.common.exception.BusinessException;
import com.house.deed.pavilion.module.store.entity.Store;

/**
 * <p>
 * 门店信息表（租户级数据）服务接口
 * 负责门店的CRUD、唯一性校验、关联数据校验等核心业务逻辑
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
public interface IStoreService extends IService<Store> {

    /**
     * 校验门店编码在租户内的唯一性
     * <p>
     * 用于门店新增和编辑场景，确保同一租户下门店编码不重复
     * 编辑时通过excludeId排除当前门店自身
     * </p>
     *
     * @param tenantId   租户ID（数据隔离标识）
     * @param storeCode  门店编码（待校验的编码）
     * @param excludeId  排除的门店ID（编辑时传入，新增时为null）
     * @return boolean  true-编码唯一，false-编码已存在
     */
    boolean checkStoreCodeUnique(Long tenantId, String storeCode, Long excludeId);

    /**
     * 校验店长是否为当前租户的在职经纪人
     * <p>
     * 确保关联的店长属于当前租户且状态为在职（status=1）
     * 若店长ID为null则不校验（允许未分配店长）
     * </p>
     *
     * @param tenantId  租户ID（数据隔离标识）
     * @param managerId 店长ID（关联agent表的主键）
     * @throws BusinessException 当店长不存在、不属于当前租户或已离职时抛出
     */
    void validateManager(Long tenantId, Long managerId);

    /**
     * 校验区域ID合法性（必须属于当前租户或系统默认区域）
     * @param tenantId 租户ID
     * @param regionId 区域ID
     */
    void validateRegion(Long tenantId, Long regionId);

}
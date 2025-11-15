package com.house.deed.pavilion.module.region.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.house.deed.pavilion.module.region.entity.Region;

import java.util.List;

/**
 * <p>
 * 区域管理表（租户级数据隔离） 服务类
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
public interface IRegionService extends IService<Region> {

    List<Region> getRegionList(Long parentId);

    /**
     * 校验区域ID合法性（必须属于当前租户或系统默认区域）
     * @param tenantId 当前租户ID
     * @param regionId 待校验的区域ID
     */
    void validateRegion(Long tenantId, Long regionId);

}

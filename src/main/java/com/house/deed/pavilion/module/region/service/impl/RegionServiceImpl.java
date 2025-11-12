package com.house.deed.pavilion.module.region.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.common.util.TenantContext;
import com.house.deed.pavilion.module.region.entity.Region;
import com.house.deed.pavilion.module.region.mapper.RegionMapper;
import com.house.deed.pavilion.module.region.service.IRegionService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 区域管理表（租户级数据隔离） 服务实现类
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
@Service
public class RegionServiceImpl extends ServiceImpl<RegionMapper, Region> implements IRegionService {

    @Override
    public List<Region> getRegionList(Long parentId) {
        Long tenantId = TenantContext.getTenantId();
        // 查询条件：当前租户的区域 + 系统默认区域（tenant_id=0）
        return lambdaQuery()
                .eq(Region::getParentId, parentId)
                .and(q -> q.eq(Region::getTenantId, tenantId).or().eq(Region::getTenantId, 0))
                .list();
    }

}

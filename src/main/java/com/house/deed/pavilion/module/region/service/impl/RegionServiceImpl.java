package com.house.deed.pavilion.module.region.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.common.exception.BusinessException;
import com.house.deed.pavilion.common.util.TenantContext;
import com.house.deed.pavilion.common.util.ValidateUtil;
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

    /**
     * 校验区域ID合法性：
     * 1. 区域必须存在
     * 2. 区域必须属于当前租户（tenant_id匹配）或系统默认区域（tenant_id=0）
     */
    @Override
    public void validateRegion(Long tenantId, Long regionId) {
        // 非空校验
        ValidateUtil.notNull(regionId, "区域ID不能为空");

        // 查询区域是否存在
        Region region = getById(regionId);
        if (region == null) {
            throw new BusinessException(400, "区域不存在");
        }

        // 校验区域归属（当前租户或系统默认区域）
        if (!region.getTenantId().equals(tenantId) && region.getTenantId() != 0) {
            throw new BusinessException(400, "区域不属于当前租户，无权访问");
        }
    }

}

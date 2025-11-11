package com.house.deed.pavilion.module.region.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.module.region.entity.Region;
import com.house.deed.pavilion.module.region.mapper.RegionMapper;
import com.house.deed.pavilion.module.region.service.IRegionService;
import org.springframework.stereotype.Service;

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

}

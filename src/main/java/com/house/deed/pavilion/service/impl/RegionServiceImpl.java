package com.house.deed.pavilion.service.impl;

import com.house.deed.pavilion.entity.Region;
import com.house.deed.pavilion.mapper.RegionMapper;
import com.house.deed.pavilion.service.RegionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 区域管理表（租户级数据隔离） 服务实现类
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Service
public class RegionServiceImpl extends ServiceImpl<RegionMapper, Region> implements RegionService {

}

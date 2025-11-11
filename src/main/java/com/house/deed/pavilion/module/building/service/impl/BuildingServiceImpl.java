package com.house.deed.pavilion.module.building.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.module.building.entity.Building;
import com.house.deed.pavilion.module.building.mapper.BuildingMapper;
import com.house.deed.pavilion.module.building.service.IBuildingService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 楼栋信息表（租户级数据） 服务实现类
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
@Service
public class BuildingServiceImpl extends ServiceImpl<BuildingMapper, Building> implements IBuildingService {

}

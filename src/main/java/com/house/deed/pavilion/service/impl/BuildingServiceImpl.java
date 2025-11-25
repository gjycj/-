package com.house.deed.pavilion.service.impl;

import com.house.deed.pavilion.entity.Building;
import com.house.deed.pavilion.mapper.BuildingMapper;
import com.house.deed.pavilion.service.BuildingService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 楼栋信息表（租户级数据） 服务实现类
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Service
public class BuildingServiceImpl extends ServiceImpl<BuildingMapper, Building> implements BuildingService {

}

package com.house.deed.pavilion.module.house.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.module.house.entity.House;
import com.house.deed.pavilion.module.house.mapper.HouseMapper;
import com.house.deed.pavilion.module.house.service.IHouseService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 房源信息表（租户核心数据） 服务实现类
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
@Service
public class HouseServiceImpl extends ServiceImpl<HouseMapper, House> implements IHouseService {

}

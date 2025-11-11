package com.house.deed.pavilion.module.landlord.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.module.landlord.entity.Landlord;
import com.house.deed.pavilion.module.landlord.mapper.LandlordMapper;
import com.house.deed.pavilion.module.landlord.service.ILandlordService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 房东信息表（租户级数据隔离） 服务实现类
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
@Service
public class LandlordServiceImpl extends ServiceImpl<LandlordMapper, Landlord> implements ILandlordService {

}

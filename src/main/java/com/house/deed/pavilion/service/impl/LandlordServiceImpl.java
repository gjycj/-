package com.house.deed.pavilion.service.impl;

import com.house.deed.pavilion.entity.Landlord;
import com.house.deed.pavilion.mapper.LandlordMapper;
import com.house.deed.pavilion.service.LandlordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 房东信息表（租户级数据隔离） 服务实现类
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Service
public class LandlordServiceImpl extends ServiceImpl<LandlordMapper, Landlord> implements LandlordService {

}

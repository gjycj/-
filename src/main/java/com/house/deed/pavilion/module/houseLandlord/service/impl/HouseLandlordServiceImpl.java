package com.house.deed.pavilion.module.houseLandlord.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.module.houseLandlord.entity.HouseLandlord;
import com.house.deed.pavilion.module.houseLandlord.mapper.HouseLandlordMapper;
import com.house.deed.pavilion.module.houseLandlord.service.IHouseLandlordService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 房源与房东关联表（租户级数据） 服务实现类
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
@Service
public class HouseLandlordServiceImpl extends ServiceImpl<HouseLandlordMapper, HouseLandlord> implements IHouseLandlordService {

    @Override
    public List<HouseLandlord> getByHouseId(Long houseId) {
        return lambdaQuery().eq(HouseLandlord::getHouseId, houseId).list();
    }

    @Override
    public boolean removeByHouseAndLandlord(Long houseId, Long landlordId) {
        return lambdaUpdate()
                .eq(HouseLandlord::getHouseId, houseId)
                .eq(HouseLandlord::getLandlordId, landlordId)
                .remove();
    }

    @Override
    public boolean existsById(Long id) {
        return this.exists(Wrappers.<HouseLandlord>lambdaQuery().eq(HouseLandlord::getId, id));
    }
}

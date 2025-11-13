package com.house.deed.pavilion.module.houseLandlord.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.house.deed.pavilion.module.houseLandlord.entity.HouseLandlord;

import java.util.List;

/**
 * <p>
 * 房源与房东关联表（租户级数据） 服务类
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
public interface IHouseLandlordService extends IService<HouseLandlord> {

    // 根据房源ID查询关联的房东
    List<HouseLandlord> getByHouseId(Long houseId);

    // 解除房源与房东的关联
    boolean removeByHouseAndLandlord(Long houseId, Long landlordId);

    boolean existsById(Long id);

}

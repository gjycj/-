package com.house.deed.pavilion.module.houseHandover.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.house.deed.pavilion.module.houseHandover.entity.HouseHandover;
import com.house.deed.pavilion.module.houseHandover.repository.HouseHandoverDTO;

/**
 * <p>
 * 房屋交接记录表（租户级数据） 服务类
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
public interface IHouseHandoverService extends IService<HouseHandover> {
    /**
     * 创建房屋交接记录
     * @param dto 交接信息DTO
     * @return 交接记录ID
     */
    Long createHandover(HouseHandoverDTO dto);

    /**
     * 分页查询房源的交接记录
     * @param page 分页参数
     * @param houseId 房源ID
     * @return 分页结果
     */
    Page<HouseHandover> getHandoverPageByHouse(Page<HouseHandover> page, Long houseId);
}

package com.house.deed.pavilion.module.house.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.house.deed.pavilion.module.house.entity.House;

import java.util.List;

/**
 * <p>
 * 房源信息表（租户核心数据） 服务类
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
public interface IHouseService extends IService<House> {

    Page<House> getHousePage(Page<House> page, String houseNo, String status);

    boolean addHouse(House house, List<Long> landlordIds);

}

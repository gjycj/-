package com.house.deed.pavilion.module.house.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.house.deed.pavilion.module.house.dto.HouseAddDTO;
import com.house.deed.pavilion.module.house.entity.House;


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

    boolean existsById(Long id);

    /**
     * 房源录入
     * @param dto 录入请求DTO
     * @param currentAgentId 当前登录经纪人ID
     * @return 录入成功的房源ID
     */
    Long addHouse(HouseAddDTO dto, Long currentAgentId);
}

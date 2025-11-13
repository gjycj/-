package com.house.deed.pavilion.module.house.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.module.house.entity.House;
import com.house.deed.pavilion.module.house.mapper.HouseMapper;
import com.house.deed.pavilion.module.house.service.IHouseService;
import com.house.deed.pavilion.module.houseLandlord.entity.HouseLandlord;
import com.house.deed.pavilion.module.houseLandlord.service.IHouseLandlordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

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

    @Autowired
    private IHouseLandlordService houseLandlordService;

    // 新增房源（自动关联当前租户）
    @Override
    public boolean addHouse(House house, List<Long> landlordIds) {
        // 1. 保存房源（tenant_id由自动填充注入）
        boolean save = save(house);
        if (!save) return false;

        // 2. 关联房东（房源与房东必须同租户）
        List<HouseLandlord> relations = landlordIds.stream().map(landlordId -> {
            HouseLandlord hl = new HouseLandlord();
            hl.setHouseId(house.getId());
            hl.setLandlordId(landlordId);
            // tenant_id自动填充（与当前租户一致）
            return hl;
        }).collect(Collectors.toList());
        return houseLandlordService.saveBatch(relations);
    }

    // 查询租户的房源列表（插件自动添加tenant_id条件）
    @Override
    public Page<House> getHousePage(Page<House> page, String houseNo, String status) {
        return lambdaQuery()
                .like(StrUtil.isNotBlank(houseNo), House::getHouseNo, houseNo)
                .eq(StrUtil.isNotBlank(status), House::getStatus, status)
                .page(page);
    }

    @Override
    public boolean existsById(Long id) {
        return this.exists(Wrappers.<House>lambdaQuery().eq(House::getId, id));
    }

}

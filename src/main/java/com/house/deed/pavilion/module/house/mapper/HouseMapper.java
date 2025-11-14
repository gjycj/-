package com.house.deed.pavilion.module.house.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.house.deed.pavilion.module.house.entity.House;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * 房源信息表（租户核心数据） Mapper 接口
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
public interface HouseMapper extends BaseMapper<House> {
    /**
     * 校验楼栋内房号是否重复
     * @param buildingId 楼栋ID
     * @param houseNo 房号
     * @return 存在返回1，不存在返回0
     */
    Integer checkHouseNoUnique(@Param("buildingId") Long buildingId, @Param("houseNo") String houseNo);

}

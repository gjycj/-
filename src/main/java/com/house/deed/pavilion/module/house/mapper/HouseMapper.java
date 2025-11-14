package com.house.deed.pavilion.module.house.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.house.deed.pavilion.module.house.entity.House;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * 房源信息表（租户核心数据） Mapper 接口
 * 负责房源信息的数据库操作，继承BaseMapper提供基础CRUD能力
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
public interface HouseMapper extends BaseMapper<House> {

    /**
     * 校验同一租户下同一楼栋内房号是否重复
     * 用于房源新增/编辑时的唯一性校验，确保数据完整性
     *
     * @param buildingId 楼栋ID，关联building表的主键
     * @param houseNo    房号（如1单元301）
     * @param tenantId   租户ID，用于数据隔离，确保租户间数据独立
     * @return Integer 存在重复返回1，不存在返回0
     */
    Integer checkHouseNoUnique(
            @Param("buildingId") Long buildingId,
            @Param("houseNo") String houseNo,
            @Param("tenantId") Long tenantId
    );

}
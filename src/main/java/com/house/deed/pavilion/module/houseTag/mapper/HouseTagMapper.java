package com.house.deed.pavilion.module.houseTag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.house.deed.pavilion.module.houseTag.entity.HouseTag;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 房源与标签关联表（租户级数据） Mapper 接口
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
public interface HouseTagMapper extends BaseMapper<HouseTag> {

    // 无需tenantId参数，插件自动从上下文获取并添加条件
    List<Long> selectTagIdsByHouseId(@Param("houseId") Long houseId);

    // 无需tenantId参数，插件自动处理租户隔离
    int deleteByHouseIdAndTagIds(
            @Param("houseId") Long houseId,
            @Param("tagIds") List<Long> tagIds
    );
}

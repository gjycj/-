package com.house.deed.pavilion.module.houseTag.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.house.deed.pavilion.module.houseTag.entity.HouseTag;

import java.util.List;

/**
 * <p>
 * 房源与标签关联表（租户级数据） 服务类
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
public interface IHouseTagService extends IService<HouseTag> {
    List<Long> getTagIdsByHouseId(Long houseId);
    boolean batchAddTags(Long houseId, List<Long> tagIds);
    boolean removeTags(Long houseId, List<Long> tagIds);
    boolean replaceTags(Long houseId, List<Long> tagIds);
}

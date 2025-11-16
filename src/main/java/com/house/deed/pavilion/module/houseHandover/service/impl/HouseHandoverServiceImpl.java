package com.house.deed.pavilion.module.houseHandover.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.common.exception.BusinessException;
import com.house.deed.pavilion.common.util.BeanConvertUtil;
import com.house.deed.pavilion.common.util.TenantContext;
import com.house.deed.pavilion.module.house.service.IHouseService;
import com.house.deed.pavilion.module.houseHandover.entity.HouseHandover;
import com.house.deed.pavilion.module.houseHandover.mapper.HouseHandoverMapper;
import com.house.deed.pavilion.module.houseHandover.repository.HouseHandoverDTO;
import com.house.deed.pavilion.module.houseHandover.service.IHouseHandoverService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * <p>
 * 房屋交接记录表（租户级数据） 服务实现类
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
@Service
public class HouseHandoverServiceImpl extends ServiceImpl<HouseHandoverMapper, HouseHandover> implements IHouseHandoverService {

    @Resource
    private IHouseService houseService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createHandover(HouseHandoverDTO dto) {
        Long tenantId = TenantContext.getTenantId();

        // 校验房源存在性（当前租户）
        if (!houseService.existsById(dto.getHouseId())) {
            throw new BusinessException(400, "房源不存在或无权访问");
        }

        // DTO转实体
        HouseHandover handover = BeanConvertUtil.convert(dto, HouseHandover.class);
        handover.setTenantId(tenantId);
        handover.setCreateTime(LocalDateTime.now());

        // 保存交接记录
        this.save(handover);
        return handover.getId();
    }

    @Override
    public Page<HouseHandover> getHandoverPageByHouse(Page<HouseHandover> page, Long houseId) {
        Long tenantId = TenantContext.getTenantId();
        QueryWrapper<HouseHandover> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("tenant_id", tenantId)
                .eq("house_id", houseId)
                .orderByDesc("handover_time");
        return baseMapper.selectPage(page, queryWrapper);
    }
}

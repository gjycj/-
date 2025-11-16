package com.house.deed.pavilion.module.houseBackup.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.common.exception.BusinessException;
import com.house.deed.pavilion.common.util.TenantContext;
import com.house.deed.pavilion.module.houseBackup.entity.HouseBackup;
import com.house.deed.pavilion.module.houseBackup.mapper.HouseBackupMapper;
import com.house.deed.pavilion.module.houseBackup.service.IHouseBackupService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 房源删除备份表（租户级存档） 服务实现类
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
@Service
public class HouseBackupServiceImpl extends ServiceImpl<HouseBackupMapper, HouseBackup> implements IHouseBackupService {

    @Override
    public Page<HouseBackup> getBackupPage(Page<HouseBackup> page, Long originalHouseId) {
        Long tenantId = TenantContext.getTenantId();
        QueryWrapper<HouseBackup> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("tenant_id", tenantId)
                .eq(originalHouseId != null, "original_id", originalHouseId)
                .orderByDesc("delete_time"); // 按删除时间倒序
        return baseMapper.selectPage(page, queryWrapper);
    }

    @Override
    public HouseBackup getBackupById(Long backupId) {
        Long tenantId = TenantContext.getTenantId();
        HouseBackup backup = baseMapper.selectById(backupId);
        if (backup == null || !backup.getTenantId().equals(tenantId)) {
            throw new BusinessException(404, "备份记录不存在或无权访问");
        }
        return backup;
    }
}
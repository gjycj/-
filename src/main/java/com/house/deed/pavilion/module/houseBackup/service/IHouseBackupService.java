package com.house.deed.pavilion.module.houseBackup.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.house.deed.pavilion.module.houseBackup.entity.HouseBackup;

/**
 * <p>
 * 房源删除备份表（租户级存档） 服务类
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
public interface IHouseBackupService extends IService<HouseBackup> {

    /**
     * 分页查询当前租户的房源备份记录
     * @param page 分页参数
     * @param originalHouseId 原房源ID（可选，为空则查询全部）
     * @return 分页结果
     */
    Page<HouseBackup> getBackupPage(Page<HouseBackup> page, Long originalHouseId);

    /**
     * 根据备份ID查询详情（带租户权限校验）
     * @param backupId 备份记录ID
     * @return 备份详情
     */
    HouseBackup getBackupById(Long backupId);
}

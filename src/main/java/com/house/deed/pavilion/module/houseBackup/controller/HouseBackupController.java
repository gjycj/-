package com.house.deed.pavilion.module.houseBackup.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.house.deed.pavilion.common.dto.ResultDTO;
import com.house.deed.pavilion.module.houseBackup.entity.HouseBackup;
import com.house.deed.pavilion.module.houseBackup.service.IHouseBackupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 房源删除备份表（租户级存档） 前端控制器
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
@RestController
@RequestMapping("/module/houseBackup")
@Tag(name = "房源备份管理", description = "房源删除备份的查询与恢复接口")
public class HouseBackupController {

    @Resource
    private IHouseBackupService houseBackupService;

    @GetMapping("/page")
    @Operation(summary = "分页查询备份记录", description = "支持按原房源ID筛选")
    public ResultDTO<Page<HouseBackup>> getBackupPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long originalHouseId) {

        Page<HouseBackup> page = new Page<>(pageNum, pageSize);
        Page<HouseBackup> resultPage = houseBackupService.getBackupPage(page, originalHouseId);
        return ResultDTO.success(resultPage);
    }

    @GetMapping("/{backupId}")
    @Operation(summary = "查询备份详情", description = "根据备份ID获取完整备份信息")
    public ResultDTO<HouseBackup> getBackupDetail(@PathVariable Long backupId) {
        HouseBackup backup = houseBackupService.getBackupById(backupId);
        return ResultDTO.success(backup);
    }
}
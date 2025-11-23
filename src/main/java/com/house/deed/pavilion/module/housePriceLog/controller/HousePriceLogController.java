package com.house.deed.pavilion.module.housePriceLog.controller;

import com.house.deed.pavilion.common.dto.ResultDTO;
import com.house.deed.pavilion.module.housePriceLog.entity.HousePriceLog;
import com.house.deed.pavilion.module.housePriceLog.service.IHousePriceLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * 房源价格变动记录表（租户级数据） 前端控制器
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
@Tag(name = "房源价格变动记录", description = "房源价格变动记录查询及管理接口")
@RestController
@RequestMapping("/module/housePriceLog")
public class HousePriceLogController {

    @Resource
    private IHousePriceLogService housePriceLogService;

    @GetMapping("/by-house/{houseId}")
    @Operation(summary = "查询房源价格变动记录", description = "根据房源ID查询价格变动历史")
    public ResultDTO<List<HousePriceLog>> getByHouseId(
            @Parameter(description = "房源ID", required = true) @PathVariable Long houseId) {
        return ResultDTO.success(housePriceLogService.getByHouseId(houseId));
    }
}
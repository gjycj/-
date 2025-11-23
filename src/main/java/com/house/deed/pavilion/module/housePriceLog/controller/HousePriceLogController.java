package com.house.deed.pavilion.module.housePriceLog.controller;

import com.house.deed.pavilion.common.dto.ResultDTO;
import com.house.deed.pavilion.module.housePriceLog.dto.HousePriceLogDTO;
import com.house.deed.pavilion.module.housePriceLog.entity.HousePriceLog;
import com.house.deed.pavilion.module.housePriceLog.service.IHousePriceLogService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/module/housePriceLog")
public class HousePriceLogController {

    @Resource
    private IHousePriceLogService housePriceLogService;

    /**
     * 记录房源价格变动（权限：仅房源创建人或店长可操作）
     */
    @PostMapping("/record")
    @Operation(
            summary = "创建价格变动记录",
            description = "【权限说明】仅房源创建人或所属店长可创建记录，创建后不可修改/删除。"
    )
    public ResultDTO<Boolean> recordPriceChange(@RequestBody HousePriceLogDTO dto) {
        boolean logId = housePriceLogService.recordPriceChange(dto);
        return ResultDTO.success(logId);
    }

    /**
     * 按房源ID查询价格变动记录（权限：仅记录创建人、店长或管理员可查看）
     */
    @GetMapping("/house/{houseId}")
    @Operation(
            summary = "查询房源价格变动记录",
            description = "【权限说明】仅记录创建人、所属店长或管理员可查看对应房源的价格变动记录。"
    )
    public ResultDTO<List<HousePriceLog>> getByHouseId(
            @PathVariable Long houseId
    ) {
        List<HousePriceLog> logs = housePriceLogService.getByHouseId(houseId);
        return ResultDTO.success(logs);
    }
}
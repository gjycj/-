package com.house.deed.pavilion.module.houseHandover.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.house.deed.pavilion.common.dto.ResultDTO;
import com.house.deed.pavilion.module.houseHandover.entity.HouseHandover;
import com.house.deed.pavilion.module.houseHandover.repository.HouseHandoverDTO;
import com.house.deed.pavilion.module.houseHandover.service.IHouseHandoverService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 房屋交接记录表（租户级数据） 前端控制器
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
@RestController
@RequestMapping("/module/houseHandover")
@Tag(name = "房屋交接管理", description = "入住/退租交接记录接口")
public class HouseHandoverController {

    @Resource
    private IHouseHandoverService houseHandoverService;

    @PostMapping("/add")
    @Operation(summary = "新增交接记录", description = "创建入住或退租交接记录")
    public ResultDTO<Long> addHandover(@Valid @RequestBody HouseHandoverDTO dto) {
        Long handoverId = houseHandoverService.createHandover(dto);
        return ResultDTO.success(handoverId);
    }

    @GetMapping("/house/{houseId}")
    @Operation(summary = "查询房源交接记录", description = "分页获取指定房源的所有交接记录")
    public ResultDTO<Page<HouseHandover>> getHouseHandovers(
            @PathVariable Long houseId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        Page<HouseHandover> page = new Page<>(pageNum, pageSize);
        Page<HouseHandover> resultPage = houseHandoverService.getHandoverPageByHouse(page, houseId);
        return ResultDTO.success(resultPage);
    }
}
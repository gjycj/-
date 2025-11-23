package com.house.deed.pavilion.module.houseTag.controller;

import com.house.deed.pavilion.common.dto.ResultDTO;
import com.house.deed.pavilion.module.houseTag.service.IHouseTagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 房源与标签关联表（租户级数据） 前端控制器
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
@Tag(name = "房源标签关联", description = "房源与标签的关联管理接口")
@RestController
@RequestMapping("/module/houseTag")
public class HouseTagController {

    @Resource
    private IHouseTagService houseTagService;

    @GetMapping("/by-house/{houseId}")
    @Operation(summary = "查询房源标签", description = "根据房源ID查询关联的标签ID列表")
    public ResultDTO<List<Long>> getTagIdsByHouseId(
            @Parameter(description = "房源ID", required = true) @PathVariable Long houseId) {
        return ResultDTO.success(houseTagService.getTagIdsByHouseId(houseId));
    }

    @PostMapping("/add")
    @Operation(summary = "添加房源标签", description = "为房源添加标签关联")
    public ResultDTO<Boolean> addHouseTags(
            @Parameter(description = "房源ID", required = true) @RequestParam Long houseId,
            @Parameter(description = "标签ID列表", required = true) @RequestBody List<Long> tagIds) {
        return ResultDTO.success(houseTagService.batchAddTags(houseId, tagIds));
    }

    @PostMapping("/remove")
    @Operation(summary = "移除房源标签", description = "移除房源与指定标签的关联")
    public ResultDTO<Boolean> removeHouseTags(
            @Parameter(description = "房源ID", required = true) @RequestParam Long houseId,
            @Parameter(description = "标签ID列表", required = true) @RequestBody List<Long> tagIds) {
        return ResultDTO.success(houseTagService.removeTags(houseId, tagIds));
    }

    @PostMapping("/replace")
    @Operation(summary = "替换房源标签", description = "替换房源的所有标签（先删除后添加）")
    public ResultDTO<Boolean> replaceHouseTags(
            @Parameter(description = "房源ID", required = true) @RequestParam Long houseId,
            @Parameter(description = "新标签ID列表", required = true) @RequestBody List<Long> tagIds) {
        return ResultDTO.success(houseTagService.replaceTags(houseId, tagIds));
    }
}
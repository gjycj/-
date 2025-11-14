package com.house.deed.pavilion.module.house.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.house.deed.pavilion.common.dto.ResultDTO;
import com.house.deed.pavilion.common.exception.BusinessException;
import com.house.deed.pavilion.module.house.dto.HouseAddDTO;
import com.house.deed.pavilion.module.house.entity.House;
import com.house.deed.pavilion.module.house.repository.TransactionType;
import com.house.deed.pavilion.module.house.service.IHouseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;



/**
 * <p>
 * 房源信息表（租户核心数据） 前端控制器
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
@RestController
@RequestMapping("/module/house")
public class HouseController {

    @Resource
    private IHouseService houseService;

    /**
     * 房源录入接口
     */
    @PostMapping("/add")
    @Operation(summary = "新增房源（含标签和图片）", description = "创建房源信息，同时关联房东、标签和房源图片")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "录入成功",
                    content = @Content(schema = @Schema(implementation = ResultDTO.class))),
            @ApiResponse(responseCode = "400", description = "参数错误（如必填字段为空）"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public ResultDTO<String> addHouse(
            @Parameter(description = "房源录入参数", required = true) @Valid @RequestBody HouseAddDTO dto,
            @Parameter(description = "当前登录经纪人ID", required = true) Long currentAgentId) {
        Long houseId = houseService.addHouse(dto, currentAgentId);
        return ResultDTO.success("房源" + houseId + "录入成功");
    }

    /**
     * 根据ID查询房源
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询房源详情", description = "根据房源ID获取房源完整信息")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功", content = @Content(schema = @Schema(implementation = House.class))),
            @ApiResponse(responseCode = "404", description = "房源不存在")
    })
    public ResultDTO<House> getHouseById(
            @Parameter(description = "房源ID", required = true) @PathVariable Long id) {
        House house = houseService.getById(id);
        if (house == null) {
            throw new BusinessException(404, "房源不存在");
        }
        TransactionType transactionType = TransactionType.getByCode(String.valueOf(house.getTransactionType()));
        house.setTransactionType(transactionType);
        return ResultDTO.success(house);
    }

    /**
     * 分页查询房源列表（支持按房号和状态筛选）
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询房源", description = "支持按房号模糊查询和状态精确筛选")
    @ApiResponse(responseCode = "200", description = "查询成功", content = @Content(schema = @Schema(implementation = Page.class)))
    public ResultDTO<Page<House>> getHousePage(
            @Parameter(description = "页码，默认1")
            @RequestParam(defaultValue = "1") Integer pageNum,

            @Parameter(description = "每页条数，默认10")
            @RequestParam(defaultValue = "10") Integer pageSize,

            @Parameter(description = "房号（模糊查询）")
            @RequestParam() String houseNo,

            @Parameter(description = "房源状态（精确匹配，可选值：ON_SALE/RESERVED/SOLD/OFF_SHELF）")
            @RequestParam() String status) {

        Page<House> page = new Page<>(pageNum, pageSize);
        Page<House> resultPage = houseService.getHousePage(page, houseNo, status);
        return ResultDTO.success(resultPage);
    }

    /**
     * 更新房源信息
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新房源信息", description = "根据ID更新房源信息，ID必须在路径和请求体中保持一致")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "400", description = "ID不匹配"),
            @ApiResponse(responseCode = "404", description = "房源不存在")
    })
    public ResultDTO<Boolean> updateHouse(
            @Parameter(description = "房源ID", required = true) @PathVariable Long id,
            @Parameter(description = "更新后的房源信息", required = true) @RequestBody House house) {
        if (!id.equals(house.getId())) {
            throw new BusinessException(400, "ID不匹配");
        }
        if (!houseService.existsById(id)) {
            throw new BusinessException(404, "房源不存在");
        }
        boolean success = houseService.updateById(house);
        return ResultDTO.success(success);
    }

    /**
     * 删除房源（自动备份到house_backup表）
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除房源", description = "删除房源前会自动备份数据到房源备份表，支持恢复操作")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "404", description = "房源不存在")
    })
    public ResultDTO<Boolean> deleteHouse(
            @Parameter(description = "房源ID", required = true) @PathVariable Long id,
            @Parameter(description = "删除操作人", required = true) @RequestParam String operator) {
        if (!houseService.existsById(id)) {
            throw new BusinessException(404, "房源不存在");
        }
        boolean success = houseService.deleteAndBackup(id, operator);
        return ResultDTO.success(success);
    }
}
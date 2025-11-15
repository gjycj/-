package com.house.deed.pavilion.module.maintenanceOrder.controller;

import com.house.deed.pavilion.common.dto.ResultDTO;
import com.house.deed.pavilion.common.exception.BusinessException;
import com.house.deed.pavilion.module.maintenanceOrder.entity.MaintenanceOrder;
import com.house.deed.pavilion.module.maintenanceOrder.service.IMaintenanceOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 房源维修工单表（租户级数据） 前端控制器
 * 负责维修工单的创建、查询、更新、删除等接口管理
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
@RestController
@RequestMapping("/module/maintenanceOrder")
@Tag(name = "维修工单", description = "维修工单CRUD及状态管理接口")
public class MaintenanceOrderController {

    @Autowired
    private IMaintenanceOrderService maintenanceOrderService;

    /**
     * 创建维修工单
     * 自动校验房源归属当前租户，生成租户内唯一工单编号
     */
    @PostMapping
    @Operation(summary = "创建维修工单", description = "新增维修工单，需传入房源ID、报修人信息等必填字段")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "创建成功"),
            @ApiResponse(responseCode = "400", description = "参数无效或房源不存在"),
            @ApiResponse(responseCode = "500", description = "服务器异常")
    })
    public ResultDTO<Long> createOrder(
            @Parameter(description = "维修工单信息", required = true) @RequestBody MaintenanceOrder order) {
        Long orderId = maintenanceOrderService.createOrder(order);
        return ResultDTO.success(orderId);
    }

    /**
     * 查询工单详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询工单详情", description = "根据工单ID查询详情（仅能查询当前租户数据）")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "404", description = "工单不存在或无权访问")
    })
    public ResultDTO<MaintenanceOrder> getOrderById(
            @Parameter(description = "工单ID", required = true) @PathVariable Long id) {
        MaintenanceOrder order = maintenanceOrderService.getOrderById(id);
        if (order == null) {
            throw new BusinessException(404, "工单不存在或无权访问");
        }
        return ResultDTO.success(order);
    }

    /**
     * 更新工单状态（如分配维修师傅、标记完成等）
     */
    @PutMapping("/{id}/status")
    @Operation(summary = "更新工单状态", description = "仅允许更新状态、维修师傅、费用等字段")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "400", description = "状态流转无效"),
            @ApiResponse(responseCode = "404", description = "工单不存在")
    })
    public ResultDTO<Boolean> updateOrderStatus(
            @Parameter(description = "工单ID", required = true) @PathVariable Long id,
            @Parameter(description = "更新信息（含状态、维修师傅ID等）", required = true) @RequestBody MaintenanceOrder updateInfo) {
        if (!id.equals(updateInfo.getId())) {
            throw new BusinessException(400, "ID不匹配");
        }
        boolean success = maintenanceOrderService.updateOrderStatus(updateInfo);
        return ResultDTO.success(success);
    }
}
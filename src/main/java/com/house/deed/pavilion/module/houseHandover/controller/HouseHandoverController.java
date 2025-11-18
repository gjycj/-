package com.house.deed.pavilion.module.houseHandover.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.house.deed.pavilion.common.dto.ResultDTO;
import com.house.deed.pavilion.common.exception.BusinessException;
import com.house.deed.pavilion.common.util.TenantContext;
import com.house.deed.pavilion.module.houseHandover.entity.HouseHandover;
import com.house.deed.pavilion.module.houseHandover.repository.HouseHandoverDTO;
import com.house.deed.pavilion.module.houseHandover.service.IHouseHandoverService;
import com.house.deed.pavilion.module.houseHandover.vo.HouseHandoverDetailVO;
import com.house.deed.pavilion.module.maintenanceOrder.entity.MaintenanceOrder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping("/detail/{id}")
    @Operation(summary = "查询交接记录详情（含关联维修工单）")
    public ResultDTO<HouseHandoverDetailVO> getHandoverDetail(@PathVariable Long id) {
        HouseHandover handover = houseHandoverService.getById(id);
        if (handover == null || !handover.getTenantId().equals(TenantContext.getTenantId())) {
            throw new BusinessException(404, "交接记录不存在或无权访问");
        }
        // 查询关联的维修工单
        List<MaintenanceOrder> orders = houseHandoverService.getRelatedMaintenanceOrders(id);
        // 封装VO返回（需新建HouseHandoverDetailVO，包含handover和orders字段）
        HouseHandoverDetailVO vo = new HouseHandoverDetailVO();
        BeanUtils.copyProperties(handover, vo);
        vo.setMaintenanceOrders(orders);
        return ResultDTO.success(vo);
    }

    @PostMapping("/add")
    @Operation(summary = "新增交接记录", description = "创建入住（CHECK_IN）或退租（CHECK_OUT）交接记录")
    public ResultDTO<Long> addHandover(
            @Parameter(description = "交接记录信息", required = true)
            @Valid @RequestBody HouseHandoverDTO dto) {
        Long handoverId = houseHandoverService.createHandover(dto);
        return ResultDTO.success(handoverId);
    }

    /**
     * 按ID查询单个交接记录
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询交接记录详情", description = "根据交接ID获取完整交接信息")
    public ResultDTO<HouseHandover> getHandoverById(
            @Parameter(description = "交接记录ID", required = true)
            @PathVariable Long id) {
        HouseHandover handover = houseHandoverService.getById(id);
        return ResultDTO.success(handover);
    }

    /**
     * 更新交接记录
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新交接记录", description = "根据ID更新交接信息（房源ID不可修改）")
    public ResultDTO<Boolean> updateHandover(
            @Parameter(description = "交接记录ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "更新后的交接信息", required = true)
            @Valid @RequestBody HouseHandoverDTO dto) {
        boolean success = houseHandoverService.updateHandover(id, dto);
        return ResultDTO.success(success);
    }

    /**
     * 删除交接记录
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除交接记录", description = "删除指定ID的交接记录（需有权限）")
    public ResultDTO<Boolean> deleteHandover(
            @Parameter(description = "交接记录ID", required = true)
            @PathVariable Long id) {
        boolean success = houseHandoverService.deleteHandover(id);
        return ResultDTO.success(success);
    }

    /**
     * 按房源ID分页查询交接记录
     */
    @GetMapping("/page/house")
    @Operation(summary = "按房源查询交接记录", description = "分页查询指定房源的所有交接记录（入住/退租）")
    public ResultDTO<Page<HouseHandover>> getHandoverPageByHouse(
            @Parameter(description = "页码，默认1")
            @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数，默认10")
            @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "房源ID", required = true)
            @RequestParam Long houseId) {
        Page<HouseHandover> page = new Page<>(pageNum, pageSize);
        Page<HouseHandover> resultPage = houseHandoverService.getHandoverPageByHouse(page, houseId);
        return ResultDTO.success(resultPage);
    }

    /**
     * 按合同ID查询交接记录
     */
    @GetMapping("/by-contract")
    @Operation(summary = "按合同查询交接记录", description = "查询指定合同关联的所有交接记录（如入住+退租）")
    public ResultDTO<List<HouseHandover>> getByContractId(
            @Parameter(description = "合同ID", required = true)
            @RequestParam Long contractId) {
        List<HouseHandover> handovers = houseHandoverService.getByContractId(contractId);
        return ResultDTO.success(handovers);
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
package com.house.deed.pavilion.module.contract.controller;

import com.house.deed.pavilion.common.dto.ResultDTO;
import com.house.deed.pavilion.common.exception.BusinessException;
import com.house.deed.pavilion.module.contract.entity.Contract;
import com.house.deed.pavilion.module.contract.service.IContractService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 合同控制器
 */
@RestController
@RequestMapping("/module/contract")
public class ContractController {

    @Resource
    private IContractService contractService;

    /**
     * 创建交易合同（自动校验房源和客户归属）
     */
    @PostMapping("/createContract")
    public ResultDTO<Boolean> createContract(@RequestBody Contract contract) {
        // 校验必填字段
        if (contract.getHouseId() == null || contract.getCustomerId() == null) {
            throw new BusinessException(400, "房源ID和客户ID不能为空");
        }
        boolean success = contractService.createContract(contract);
        return ResultDTO.success(success);
    }

    /**
     * 查询合同详情
     */
    @GetMapping("/{id}")
    public ResultDTO<Contract> getContractById(@PathVariable Long id) {
        Contract contract = contractService.getById(id);
        if (contract == null) {
            throw new BusinessException(404, "合同不存在");
        }
        return ResultDTO.success(contract);
    }

    /**
     * 更新合同状态
     */
    @PatchMapping("/{id}/status")
    public ResultDTO<Boolean> updateContractStatus(
            @PathVariable Long id,
            @RequestParam String targetStatus) {

        // 校验状态参数合法性
        List<String> validStatus = List.of("SIGNED", "EXECUTING", "COMPLETED", "TERMINATED");
        if (!validStatus.contains(targetStatus)) {
            throw new BusinessException(400, "状态只能是SIGNED/EXECUTING/COMPLETED/TERMINATED");
        }

        boolean success = contractService.updateContractStatus(id, targetStatus);
        return ResultDTO.success(success);
    }
}
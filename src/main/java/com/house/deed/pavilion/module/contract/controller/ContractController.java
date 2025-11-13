package com.house.deed.pavilion.module.contract.controller;

import com.house.deed.pavilion.common.dto.ResultDTO;
import com.house.deed.pavilion.common.exception.BusinessException;
import com.house.deed.pavilion.module.contract.entity.Contract;
import com.house.deed.pavilion.module.contract.service.IContractService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 合同控制器
 */
@RestController
@RequestMapping("/module/contract")
public class ContractController {

    @Autowired
    private IContractService contractService;

    /**
     * 创建交易合同（自动校验房源和客户归属）
     */
    @PostMapping
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
}
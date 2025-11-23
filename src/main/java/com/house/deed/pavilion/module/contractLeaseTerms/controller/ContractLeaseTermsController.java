package com.house.deed.pavilion.module.contractLeaseTerms.controller;

import com.house.deed.pavilion.common.dto.ResultDTO;
import com.house.deed.pavilion.module.contractLeaseTerms.entity.ContractLeaseTerms;
import com.house.deed.pavilion.module.contractLeaseTerms.service.IContractLeaseTermsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 租赁合同附加条款表（租户级数据） 前端控制器
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
@RestController
@RequestMapping("/module/contractLeaseTerms")
public class ContractLeaseTermsController {

    @Resource
    private IContractLeaseTermsService leaseTermsService;

    // 新增/更新（已有）
    @PostMapping("/maintainOrUpdate")
    public ResultDTO<Boolean> saveOrUpdate(@Valid @RequestBody ContractLeaseTerms terms) {
        return ResultDTO.success(leaseTermsService.saveOrUpdateLeaseTerms(terms));
    }

    // 查询（已有）
    @GetMapping("/contract/{contractId}")
    public ResultDTO<ContractLeaseTerms> getByContractId(@PathVariable Long contractId) {
        return ResultDTO.success(leaseTermsService.getByContractId(contractId));
    }

    // 新增：删除合同附加条款
    @DeleteMapping("/contract/{contractId}")
    public ResultDTO<Boolean> deleteByContractId(@PathVariable Long contractId) {
        return ResultDTO.success(leaseTermsService.removeByContractId(contractId));
    }

    /**
     * 批量查询多个合同的附加条款
     * @param contractIds 合同ID列表（不能为空，且需属于当前租户）
     * @return 合同ID与条款的映射（key:合同ID，value:条款信息）
     */
    @PostMapping("/batch/contracts")
    @Operation(summary = "批量查询合同附加条款", description = "根据合同ID列表批量查询对应的附加条款，仅返回有权访问的合同数据")
    public ResultDTO<Map<Long, ContractLeaseTerms>> batchGetByContractIds(
            @Parameter(in = ParameterIn.DEFAULT, description = "合同ID列表", required = true)
            @RequestBody @NotEmpty(message = "合同ID列表不能为空") List<Long> contractIds) {
        return ResultDTO.success(leaseTermsService.getBatchByContractIds(contractIds));
    }

}

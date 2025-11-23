package com.house.deed.pavilion.module.contractLeaseTerms.controller;

import com.house.deed.pavilion.common.dto.ResultDTO;
import com.house.deed.pavilion.module.contractLeaseTerms.entity.ContractLeaseTerms;
import com.house.deed.pavilion.module.contractLeaseTerms.service.IContractLeaseTermsService;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

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
    public ResultDTO<Boolean> saveOrUpdate(@RequestBody ContractLeaseTerms terms) {
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

}

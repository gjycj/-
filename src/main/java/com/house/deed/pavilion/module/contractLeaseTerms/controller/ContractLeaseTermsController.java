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

    /**
     * 新增/更新租赁合同附加条款
     */
    @PostMapping("/maintainOrUpdate")
    public ResultDTO<Boolean> saveOrUpdate(@RequestBody ContractLeaseTerms terms) {
        boolean success = leaseTermsService.saveOrUpdateLeaseTerms(terms);
        return ResultDTO.success(success);
    }

    /**
     * 查询合同附加条款
     */
    @GetMapping("/contract/{contractId}")
    public ResultDTO<ContractLeaseTerms> getByContractId(@PathVariable Long contractId) {
        ContractLeaseTerms terms = leaseTermsService.getByContractId(contractId);
        return ResultDTO.success(terms);
    }

}

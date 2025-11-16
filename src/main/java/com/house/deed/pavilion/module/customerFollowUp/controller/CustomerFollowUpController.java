package com.house.deed.pavilion.module.customerFollowUp.controller;

import com.house.deed.pavilion.common.dto.ResultDTO;
import com.house.deed.pavilion.common.util.TenantContext;
import com.house.deed.pavilion.module.customerFollowUp.entity.CustomerFollowUp;
import com.house.deed.pavilion.module.customerFollowUp.service.ICustomerFollowUpService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * 客户跟进记录表（租户级数据） 前端控制器
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
@RestController
@RequestMapping("/module/customerFollowUp")
public class CustomerFollowUpController {


    @Resource
    private ICustomerFollowUpService customerFollowUpService;

    // 新增：通过合同ID查询带看记录
    @GetMapping("/by-contract")
    public ResultDTO<List<CustomerFollowUp>> getByContractId(@RequestParam Long contractId) {
        Long tenantId = TenantContext.getTenantId();
        List<CustomerFollowUp> followUps = customerFollowUpService.getByContractId(contractId, tenantId);
        return ResultDTO.success(followUps);
    }

}

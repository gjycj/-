package com.house.deed.pavilion.common.util;

import com.house.deed.pavilion.common.exception.BusinessException;
import com.house.deed.pavilion.module.contract.entity.Contract;
import com.house.deed.pavilion.module.contract.service.IContractService;
import com.house.deed.pavilion.module.contractLeaseTerms.entity.ContractLeaseTerms;
import com.house.deed.pavilion.module.contractLeaseTerms.service.IContractLeaseTermsService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * 合同校验工具类（解耦服务间直接依赖）
 */
@Component
public class TempValidationUtil {

//    @Resource
//    private IContractService contractService;

    @Resource
    private IContractLeaseTermsService contractLeaseTermsService;

    public ContractLeaseTerms validateContractLeaseTerms(Long contractId, Long tenantId) {
        ContractLeaseTerms contractLeaseTerms = contractLeaseTermsService.getByContractId(contractId);
        if (contractLeaseTerms != null && contractLeaseTerms.getTenantId().equals(tenantId)) {
            throw new BusinessException(400, "租赁合同存在附加条款，无法删除");
        }
        return null;
    }

}

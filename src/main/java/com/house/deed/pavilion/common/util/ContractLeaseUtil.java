package com.house.deed.pavilion.common.util;

import com.house.deed.pavilion.common.exception.BusinessException;
import com.house.deed.pavilion.module.contract.entity.Contract;
import com.house.deed.pavilion.module.contract.service.IContractService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class ContractLeaseUtil {

    @Resource
    private IContractService contractService;

    /**
     * 校验合同存在性及租户归属
     *
     * @param contractId 合同ID
     * @param tenantId   租户ID
     * @return 合法的合同实体
     */
    public Contract validateContract(Long contractId, Long tenantId) {
        Contract contract = contractService.getById(contractId);
        if (contract == null || !contract.getTenantId().equals(tenantId)) {
            throw new BusinessException(404, "合同不存在或无权访问");
        }
        return contract;
    }

}

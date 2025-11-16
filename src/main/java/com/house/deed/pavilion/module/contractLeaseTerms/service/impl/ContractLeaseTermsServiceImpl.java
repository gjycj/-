package com.house.deed.pavilion.module.contractLeaseTerms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.common.exception.BusinessException;
import com.house.deed.pavilion.common.util.TenantContext;
import com.house.deed.pavilion.module.contract.entity.Contract;
import com.house.deed.pavilion.module.contract.service.IContractService;
import com.house.deed.pavilion.module.contractLeaseTerms.entity.ContractLeaseTerms;
import com.house.deed.pavilion.module.contractLeaseTerms.mapper.ContractLeaseTermsMapper;
import com.house.deed.pavilion.module.contractLeaseTerms.service.IContractLeaseTermsService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 租赁合同附加条款表（租户级数据） 服务实现类
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
@Service
public class ContractLeaseTermsServiceImpl extends ServiceImpl<ContractLeaseTermsMapper, ContractLeaseTerms> implements IContractLeaseTermsService {

    @Resource
    private IContractService contractService;

    @Override
    public boolean saveOrUpdateLeaseTerms(ContractLeaseTerms terms) {
        Long tenantId = TenantContext.getTenantId();
        terms.setTenantId(tenantId);

        // 1. 校验合同存在性及类型（必须是租赁合同）
        Contract contract = contractService.getById(terms.getContractId());
        if (contract == null || !contract.getTenantId().equals(tenantId)) {
            throw new BusinessException(404, "合同不存在或无权访问");
        }
        if (!"RENT".equals(contract.getContractType())) {
            throw new BusinessException(400, "仅租赁合同可添加附加条款");
        }

        // 2. 保存或更新（按合同ID唯一约束）
        return saveOrUpdate(terms);
    }

    @Override
    public ContractLeaseTerms getByContractId(Long contractId) {
        Long tenantId = TenantContext.getTenantId();
        return baseMapper.selectOne(new LambdaQueryWrapper<ContractLeaseTerms>()
                .eq(ContractLeaseTerms::getTenantId, tenantId)
                .eq(ContractLeaseTerms::getContractId, contractId));
    }

}

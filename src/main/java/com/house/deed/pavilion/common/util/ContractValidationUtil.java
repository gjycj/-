package com.house.deed.pavilion.common.util;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.house.deed.pavilion.common.exception.BusinessException;
import com.house.deed.pavilion.module.contract.entity.Contract;
import com.house.deed.pavilion.module.contract.mapper.ContractMapper;
import com.house.deed.pavilion.module.contractAttachment.entity.ContractAttachment;
import com.house.deed.pavilion.module.contractAttachment.mapper.ContractAttachmentMapper;
import com.house.deed.pavilion.module.contractLeaseTerms.entity.ContractLeaseTerms;
import com.house.deed.pavilion.module.contractLeaseTerms.mapper.ContractLeaseTermsMapper;
import com.house.deed.pavilion.module.customerFollowUp.entity.CustomerFollowUp;
import com.house.deed.pavilion.module.customerFollowUp.mapper.CustomerFollowUpMapper;
import com.house.deed.pavilion.module.visitRecord.entity.VisitRecord;
import com.house.deed.pavilion.module.visitRecord.mapper.VisitRecordMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;


/**
 * 合同校验工具类（基于Mapper解耦，消除服务间依赖）
 */
@Component
public class ContractValidationUtil {

    @Resource
    private ContractLeaseTermsMapper contractLeaseTermsMapper;

    @Resource
    private ContractAttachmentMapper attachmentMapper;

    @Resource
    private VisitRecordMapper visitRecordMapper;

    @Resource
    private CustomerFollowUpMapper followUpMapper;

    @Resource
    private ContractMapper contractMapper;

    /**
     * 校验租赁合同附加条款存在性
     */
    public ContractLeaseTerms validateContractLeaseTerms(Long contractId, Long tenantId) {
        ContractLeaseTerms terms = contractLeaseTermsMapper.selectOne(
                new LambdaQueryWrapper<ContractLeaseTerms>()
                        .eq(ContractLeaseTerms::getContractId, contractId)
                        .eq(ContractLeaseTerms::getTenantId, tenantId)
        );
        if (terms != null) {
            throw new BusinessException(400, "租赁合同存在附加条款，无法删除");
        }
        return null;
    }

    /**
     * 校验合同存在性及租户归属（直接操作Mapper）
     */
    public Contract validateContract(Long contractId, Long tenantId) {
        Contract contract = contractMapper.selectById(contractId);
        if (contract == null || !contract.getTenantId().equals(tenantId)) {
            throw new BusinessException(404, "合同不存在或无权访问");
        }
        return contract;
    }

    /**
     * 检查合同是否有关联附件（基于Mapper查询）
     */
    public boolean hasRelatedAttachments(Long contractId, Long tenantId) {
        int count = Math.toIntExact(attachmentMapper.selectCount(
                new LambdaQueryWrapper<ContractAttachment>()
                        .eq(ContractAttachment::getContractId, contractId)
                        .eq(ContractAttachment::getTenantId, tenantId)
        ));
        return count > 0;
    }

    /**
     * 检查合同是否有关联带看记录
     */
    public boolean hasRelatedVisitRecords(Long contractId, Long tenantId) {
        int count = Math.toIntExact(visitRecordMapper.selectCount(
                new LambdaQueryWrapper<VisitRecord>()
                        .eq(VisitRecord::getContractId, contractId)
                        .eq(VisitRecord::getTenantId, tenantId)
        ));
        return count > 0;
    }

    /**
     * 检查合同是否有关联跟进记录
     */
    public boolean hasRelatedFollowUps(Long contractId, Long tenantId) {
        int count = Math.toIntExact(followUpMapper.selectCount(
                new LambdaQueryWrapper<CustomerFollowUp>()
                        .eq(CustomerFollowUp::getContractId, contractId)
                        .eq(CustomerFollowUp::getTenantId, tenantId)
        ));
        return count > 0;
    }

    /**
     * 统一校验：删除合同前检查关联数据（全Mapper实现）
     */
    public void validateNoDependenciesBeforeDelete(Long contractId) {
        Long tenantId = TenantContext.getTenantId();
        // 1. 先校验合同本身存在性
        validateContract(contractId, tenantId);

        // 2. 检查各类关联数据
        if (hasRelatedAttachments(contractId, tenantId)) {
            throw new BusinessException(400, "合同存在关联附件，无法删除");
        }
        if (hasRelatedVisitRecords(contractId, tenantId)) {
            throw new BusinessException(400, "合同存在关联带看记录，无法删除");
        }
        if (hasRelatedFollowUps(contractId, tenantId)) {
            throw new BusinessException(400, "合同存在关联跟进记录，无法删除");
        }
        // 3. 检查租赁附加条款（仅租赁合同需要）
        Contract contract = contractMapper.selectById(contractId);
        if ("RENT".equals(contract.getContractType())) {
            validateContractLeaseTerms(contractId, tenantId);
        }
    }
}
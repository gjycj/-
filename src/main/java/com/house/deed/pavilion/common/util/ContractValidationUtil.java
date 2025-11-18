package com.house.deed.pavilion.common.util;

import com.house.deed.pavilion.common.exception.BusinessException;
import com.house.deed.pavilion.module.contractAttachment.entity.ContractAttachment;
import com.house.deed.pavilion.module.contractAttachment.service.IContractAttachmentService;
import com.house.deed.pavilion.module.contractLeaseTerms.entity.ContractLeaseTerms;
import com.house.deed.pavilion.module.contractLeaseTerms.service.IContractLeaseTermsService;
import com.house.deed.pavilion.module.customerFollowUp.service.ICustomerFollowUpService;
import com.house.deed.pavilion.module.visitRecord.service.IVisitRecordService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;


/**
 * 合同校验工具类（解耦服务间直接依赖）
 */
@Component
public class ContractValidationUtil {

//    @Resource
//    private IContractLeaseTermsService contractLeaseTermsService;

    @Resource
    private IContractAttachmentService attachmentService;

    @Resource
    private IVisitRecordService visitRecordService;

    @Resource
    private ICustomerFollowUpService followUpService;

//    public ContractLeaseTerms validateContractLeaseTerms(Long contractId, Long tenantId) {
//        ContractLeaseTerms contractLeaseTerms = contractLeaseTermsService.getByContractId(contractId);
//        if (contractLeaseTerms != null && contractLeaseTerms.getTenantId().equals(tenantId)) {
//            throw new BusinessException(400, "租赁合同存在附加条款，无法删除");
//        }
//        return null;
//    }

    /**
     * 检查合同是否有关联附件
     * @param contractId 合同ID
     * @param tenantId 租户ID（多租户隔离）
     * @return 存在关联附件返回true，否则返回false
     */
    public boolean hasRelatedAttachments(Long contractId, Long tenantId) {
        List<ContractAttachment> attachments = attachmentService.getByContractId(contractId);
        // 注意：需确保attachmentService.getByContractId方法已实现租户隔离（参考之前的代码，该方法已通过TenantContext获取租户ID）
        return !attachments.isEmpty();
    }

    /**
     * 检查合同是否有关联带看记录
     */
    public boolean hasRelatedVisitRecords(Long contractId, Long tenantId) {
        List<?> visitRecords = visitRecordService.getByContractId(contractId, tenantId);
        return !visitRecords.isEmpty();
    }

    /**
     * 检查合同是否有关联跟进记录
     */
    public boolean hasRelatedFollowUps(Long contractId, Long tenantId) {
        List<?> followUps = followUpService.getByContractId(contractId, tenantId);
        return !followUps.isEmpty();
    }

    /**
     * 统一校验：删除合同前检查是否存在关联数据（防止误删）
     * 若存在关联数据则抛出异常
     */
    public void validateNoDependenciesBeforeDelete(Long contractId, Long tenantId) {
        // 检查附件
        if (hasRelatedAttachments(contractId, tenantId)) {
            throw new BusinessException(400, "合同存在关联附件，无法删除");
        }
        // 检查带看记录
        if (hasRelatedVisitRecords(contractId, tenantId)) {
            throw new BusinessException(400, "合同存在关联带看记录，无法删除");
        }
        // 检查跟进记录
        if (hasRelatedFollowUps(contractId, tenantId)) {
            throw new BusinessException(400, "合同存在关联跟进记录，无法删除");
        }
    }
}
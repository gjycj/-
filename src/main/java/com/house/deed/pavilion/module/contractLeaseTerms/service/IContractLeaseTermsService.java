package com.house.deed.pavilion.module.contractLeaseTerms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.house.deed.pavilion.module.contractLeaseTerms.entity.ContractLeaseTerms;

/**
 * <p>
 * 租赁合同附加条款表（租户级数据） 服务类
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
public interface IContractLeaseTermsService extends IService<ContractLeaseTerms> {

    /**
     * 新增/更新租赁合同附加条款（一个合同仅一条）
     * @param terms 条款信息
     * @return 是否成功
     */
    boolean saveOrUpdateLeaseTerms(ContractLeaseTerms terms);

    /**
     * 根据合同ID查询条款
     * @param contractId 合同ID
     * @return 条款信息
     */
    ContractLeaseTerms getByContractId(Long contractId);

}

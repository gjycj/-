package com.house.deed.pavilion.module.contractLeaseTerms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.house.deed.pavilion.module.contractLeaseTerms.entity.ContractLeaseTerms;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 租赁合同附加条款表（租户级数据） 服务类
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
public interface IContractLeaseTermsService extends IService<ContractLeaseTerms> {

    // 已有方法：新增/更新、按合同ID查询
    boolean saveOrUpdateLeaseTerms(ContractLeaseTerms terms);
    ContractLeaseTerms getByContractId(Long contractId);

    // 新增：删除合同附加条款
    boolean removeByContractId(Long contractId);

    // 新增：批量查询多个合同的附加条款
    Map<Long, ContractLeaseTerms> getBatchByContractIds(List<Long> contractIds);

}

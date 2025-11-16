package com.house.deed.pavilion.module.contract.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.house.deed.pavilion.module.contract.entity.Contract;

/**
 * <p>
 * 交易合同表（租户核心业务数据） 服务类
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
public interface IContractService extends IService<Contract> {

    boolean createContract(Contract contract);

    /**
     * 更新合同状态（带流转校验）
     * @param contractId 合同ID
     * @param targetStatus 目标状态（SIGNED/EXECUTING/COMPLETED/TERMINATED）
     * @return 是否更新成功
     */
    boolean updateContractStatus(Long contractId, String targetStatus);

}

package com.house.deed.pavilion.module.contract.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.house.deed.pavilion.module.contract.entity.Contract;

import java.util.List;

/**
 * <p>
 * 交易合同表（租户核心业务数据） 服务类
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
public interface IContractService extends IService<Contract> {

    List<Contract> getByCustomerId(Long customerId, Long tenantId);

    boolean createContract(Contract contract);

    /**
     * 更新合同状态（带流转校验）
     * @param contractId 合同ID
     * @param targetStatus 目标状态（SIGNED/EXECUTING/COMPLETED/TERMINATED）
     * @return 是否更新成功
     */
    boolean updateContractStatus(Long contractId, String targetStatus);

    /**
     * 更新合同非状态字段（金额、付款方式等）
     * @param contract 包含更新信息的合同实体（不含状态字段）
     * @return 是否更新成功
     */
    boolean updateContract(Contract contract);

    /**
     * 删除合同（带权限校验和关联检查）
     * @param contractId 合同ID
     * @return 是否删除成功
     */
    boolean removeContract(Long contractId);

    /**
     * 根据ID查询合同（带租户隔离）
     * @param contractId 合同ID
     * @return 合同详情
     */
    Contract getByIdWithTenant(Long contractId);

    /**
     * 通过房源ID查询合同（带租户隔离）
     * @param houseId 房源ID
     * @return 合同列表
     */
    List<Contract> getByHouseId(Long houseId);

}

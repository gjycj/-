package com.house.deed.pavilion.module.contract.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.module.contract.entity.Contract;
import com.house.deed.pavilion.module.contract.mapper.ContractMapper;
import com.house.deed.pavilion.module.contract.service.IContractService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 交易合同表（租户核心业务数据） 服务实现类
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
@Service
public class ContractServiceImpl extends ServiceImpl<ContractMapper, Contract> implements IContractService {

}

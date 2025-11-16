package com.house.deed.pavilion.module.customerFollowUp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.module.customerFollowUp.entity.CustomerFollowUp;
import com.house.deed.pavilion.module.customerFollowUp.mapper.CustomerFollowUpMapper;
import com.house.deed.pavilion.module.customerFollowUp.service.ICustomerFollowUpService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 客户跟进记录表（租户级数据） 服务实现类
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
@Service
public class CustomerFollowUpServiceImpl extends ServiceImpl<CustomerFollowUpMapper, CustomerFollowUp> implements ICustomerFollowUpService {

    @Override
    public List<CustomerFollowUp> getByContractId(Long contractId, Long tenantId) {
        return lambdaQuery()
                .eq(CustomerFollowUp::getTenantId, tenantId)
                .eq(CustomerFollowUp::getContractId, contractId)
                .list();
    }

}

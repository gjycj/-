package com.house.deed.pavilion.module.customer.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.module.customer.entity.Customer;
import com.house.deed.pavilion.module.customer.mapper.CustomerMapper;
import com.house.deed.pavilion.module.customer.service.ICustomerService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 客户信息表（租户级数据隔离） 服务实现类
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
@Service
public class CustomerServiceImpl extends ServiceImpl<CustomerMapper, Customer> implements ICustomerService {
    @Override
    public boolean existsById(Long id) {
        return this.exists(Wrappers.<Customer>lambdaQuery().eq(Customer::getId, id));
    }
}

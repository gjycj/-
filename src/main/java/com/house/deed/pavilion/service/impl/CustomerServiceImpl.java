package com.house.deed.pavilion.service.impl;

import com.house.deed.pavilion.entity.Customer;
import com.house.deed.pavilion.mapper.CustomerMapper;
import com.house.deed.pavilion.service.CustomerService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 客户信息表（租户级数据隔离） 服务实现类
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Service
public class CustomerServiceImpl extends ServiceImpl<CustomerMapper, Customer> implements CustomerService {

}

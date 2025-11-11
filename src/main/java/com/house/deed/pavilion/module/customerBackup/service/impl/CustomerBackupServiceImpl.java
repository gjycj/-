package com.house.deed.pavilion.module.customerBackup.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.module.customerBackup.entity.CustomerBackup;
import com.house.deed.pavilion.module.customerBackup.mapper.CustomerBackupMapper;
import com.house.deed.pavilion.module.customerBackup.service.ICustomerBackupService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 客户删除备份表（租户级存档） 服务实现类
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
@Service
public class CustomerBackupServiceImpl extends ServiceImpl<CustomerBackupMapper, CustomerBackup> implements ICustomerBackupService {

}

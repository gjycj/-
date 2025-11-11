package com.house.deed.pavilion.module.tenant.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.module.tenant.entity.Tenant;
import com.house.deed.pavilion.module.tenant.mapper.TenantMapper;
import com.house.deed.pavilion.module.tenant.service.ITenantService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 多租户核心信息表（租户隔离根表） 服务实现类
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
@Service
public class TenantServiceImpl extends ServiceImpl<TenantMapper, Tenant> implements ITenantService {

}

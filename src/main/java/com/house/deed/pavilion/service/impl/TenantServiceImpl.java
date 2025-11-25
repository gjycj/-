package com.house.deed.pavilion.service.impl;

import com.house.deed.pavilion.entity.Tenant;
import com.house.deed.pavilion.mapper.TenantMapper;
import com.house.deed.pavilion.service.TenantService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 多租户核心信息表（租户隔离根表） 服务实现类
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Service
public class TenantServiceImpl extends ServiceImpl<TenantMapper, Tenant> implements TenantService {

}

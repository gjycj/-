package com.house.deed.pavilion.common.aspect.interceptor;

import cn.hutool.core.util.StrUtil;
import com.house.deed.pavilion.common.exception.BusinessException;
import com.house.deed.pavilion.common.util.TenantContext;
import com.house.deed.pavilion.module.tenant.entity.Tenant;
import com.house.deed.pavilion.module.tenant.service.ITenantService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class TenantInterceptor implements HandlerInterceptor {
    @Autowired
    private ITenantService tenantService;


    // 修改 TenantInterceptor 的 preHandle 方法
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String tenantIdStr = request.getHeader("tenant-id");
        if (StrUtil.isBlank(tenantIdStr)) {
            // 抛出租户ID缺失异常
            throw new BusinessException(401, "缺少租户ID（tenant-id请求头）");
        }
        // 根据租户编码查询租户ID
        Tenant tenant = tenantService.lambdaQuery()
                .eq(Tenant::getId, tenantIdStr)
                .one();
        System.out.println("zh:"+tenant);
        if (tenant == null || tenant.getStatus() != 1) {
            // 抛出租户不存在或禁用异常
            throw new BusinessException(403, "租户不存在或已禁用");
        }
        TenantContext.setTenantId(tenant.getId());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 清除上下文，避免线程复用导致租户ID混乱
        TenantContext.clear();
    }
}
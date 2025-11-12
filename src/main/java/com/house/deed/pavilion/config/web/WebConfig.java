package com.house.deed.pavilion.config.web;

import com.house.deed.pavilion.common.aspect.interceptor.TenantInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private TenantInterceptor tenantInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 对所有业务接口生效（排除租户管理本身的接口，如租户注册/登录）
        registry.addInterceptor(tenantInterceptor)
                .addPathPatterns("/module/**")
                .excludePathPatterns("/module/tenant/login", "/module/tenant/register");
    }
}
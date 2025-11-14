package com.house.deed.pavilion.config.mybatis;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.house.deed.pavilion.common.exception.BusinessException;
import com.house.deed.pavilion.common.util.TenantContext;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MyBatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 添加租户插件
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler() {
            // 修改 MyBatisPlusConfig 的 getTenantId 方法
            public Expression getTenantId() {
                System.out.println(2);
                Long tenantId = TenantContext.getTenantId();
                if (tenantId == null) {
                    // 抛出自定义异常，而非通用RuntimeException
                    throw new BusinessException(401, "租户ID不存在（上下文未获取到租户信息）");
                }
                return new LongValue(tenantId);
            }

            @Override
            public String getTenantIdColumn() {
                return "tenant_id"; // 数据库租户字段名
            }

            // 忽略租户过滤的表（如系统级表）
            @Override
            public boolean ignoreTable(String tableName) {
                return "sys_user".equals(tableName) || "tenant".equals(tableName) || "tenant_config".equals(tableName); // 示例：系统用户表无需租户过滤
            }
        }));
        return interceptor;
    }
}
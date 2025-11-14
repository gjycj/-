package com.house.deed.pavilion.common.util;


import org.springframework.stereotype.Component;

/**
 *  “租户上下文” 来存储当前操作的租户 ID，确保在一次请求链路中所有业务操作都能获取到正确的租户 ID（如从登录信息、请求头、域名中解析）。
 * */
@Component
public class TenantContext {
    // 线程局部变量存储当前租户ID
    private static final ThreadLocal<Long> CURRENT_TENANT = new ThreadLocal<>();

    // 设置当前租户ID（如登录后、请求拦截时调用）
    public static void setTenantId(Long tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    // 获取当前租户ID（业务逻辑中调用）
    public static Long getTenantId() {
        return CURRENT_TENANT.get();
    }

    // 清除租户ID（请求结束时调用，避免内存泄漏）
    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
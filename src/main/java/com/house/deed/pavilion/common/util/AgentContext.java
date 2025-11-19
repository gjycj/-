package com.house.deed.pavilion.common.util;

import com.house.deed.pavilion.common.exception.BusinessException;

public class AgentContext {
    private static final ThreadLocal<Long> AGENT_ID_HOLDER = new ThreadLocal<>();

    // 设置当前经纪人ID（登录时调用）
    public static void setAgentId(Long agentId) {
        AGENT_ID_HOLDER.set(agentId);
    }

    // 获取当前经纪人ID（权限校验时调用）
    public static Long getAgentId() {
        Long agentId = AGENT_ID_HOLDER.get();
        if (agentId == null) {
            throw new BusinessException(401, "未获取到经纪人信息，请重新登录");
        }
        return agentId;
    }

    // 清除上下文（请求结束时调用）
    public static void clear() {
        AGENT_ID_HOLDER.remove();
    }
}
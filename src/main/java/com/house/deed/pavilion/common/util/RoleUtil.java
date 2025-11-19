package com.house.deed.pavilion.common.util;

import com.house.deed.pavilion.common.exception.BusinessException;
import com.house.deed.pavilion.module.agent.entity.Agent;
import com.house.deed.pavilion.module.agent.service.IAgentService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * 角色判断工具类
 * 用于判断当前经纪人是否具备管理员、店长等特殊角色权限
 */
@Component
public class RoleUtil {

    private static IAgentService agentService;

    // 静态注入（解决工具类中使用Spring服务的问题）
    @Resource
    public void setAgentService(IAgentService agentService) {
        RoleUtil.agentService = agentService;
    }

    /**
     * 判断当前登录经纪人是否为系统管理员
     * （假设管理员角色通过agent表的position字段标识，值为"ADMIN"）
     */
    public static boolean isAdmin() {
        Long currentAgentId = AgentContext.getAgentId();
        return checkRole(currentAgentId, "ADMIN");
    }

    /**
     * 判断当前登录经纪人是否为店长
     * （对应agent表中position为"店长"的角色，参考数据库agent表记录）
     */
    public static boolean isStoreManager() {
        Long currentAgentId = AgentContext.getAgentId();
        return checkRole(currentAgentId, "店长");
    }

    /**
     * 通用角色检查逻辑
     * @param agentId 经纪人ID
     * @param targetRole 目标角色（匹配agent表的position字段）
     */
    private static boolean checkRole(Long agentId, String targetRole) {
        if (agentId == null) {
            throw new BusinessException(401, "未获取到经纪人信息，请重新登录");
        }

        // 查询经纪人信息（从缓存或数据库）
        Agent agent = agentService.getById(agentId);
        if (agent == null) {
            throw new BusinessException(404, "经纪人信息不存在");
        }

        // 角色匹配（position字段存储角色信息）
        return targetRole.equals(agent.getPosition());
    }
}
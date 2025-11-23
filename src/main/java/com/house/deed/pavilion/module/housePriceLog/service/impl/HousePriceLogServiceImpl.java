package com.house.deed.pavilion.module.housePriceLog.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.common.aspect.annotation.AgentDataPermission;
import com.house.deed.pavilion.common.exception.BusinessException;
import com.house.deed.pavilion.common.util.AgentContext;
import com.house.deed.pavilion.common.util.TenantContext;
import com.house.deed.pavilion.module.agent.entity.Agent;
import com.house.deed.pavilion.module.agent.service.IAgentService;
import com.house.deed.pavilion.module.house.entity.House;
import com.house.deed.pavilion.module.house.service.IHouseService;
import com.house.deed.pavilion.module.housePriceLog.entity.HousePriceLog;
import com.house.deed.pavilion.module.housePriceLog.mapper.HousePriceLogMapper;
import com.house.deed.pavilion.module.housePriceLog.service.IHousePriceLogService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 房源价格变动记录表（租户级数据） 服务实现类
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
@Service
public class HousePriceLogServiceImpl extends ServiceImpl<HousePriceLogMapper, HousePriceLog> implements IHousePriceLogService {

    @Resource
    private IHouseService houseService;

    @Resource
    private IAgentService agentService;

    @Override
    @AgentDataPermission(
            operation = AgentDataPermission.OperationType.QUERY,
            entityClass = HousePriceLog.class
    )
    public List<HousePriceLog> getByHouseId(Long houseId) {
        // 校验房源是否存在且属于当前经纪人
        House house = houseService.getById(houseId);
        if (house == null) {
            throw new BusinessException(404, "房源不存在");
        }

        return lambdaQuery()
                .eq(HousePriceLog::getHouseId, houseId)
                .orderByDesc(HousePriceLog::getCreateTime)
                .list();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @AgentDataPermission(
            operation = AgentDataPermission.OperationType.CREATE,
            entityClass = HousePriceLog.class
    )
    public boolean recordPriceChange(Long houseId, BigDecimal oldPrice, BigDecimal newPrice, String reason) {
        Long currentAgentId = AgentContext.getAgentId();
        Long tenantId = TenantContext.getTenantId();

        // 校验房源归属
        House house = houseService.getById(houseId);
        if (house == null || !house.getTenantId().equals(tenantId)) {
            throw new BusinessException(404, "房源不存在或无权访问");
        }

        // 校验是否为创建人
        if (!house.getCreateAgentId().equals(currentAgentId)) {
            throw new BusinessException(403, "无权操作：仅创建人可修改价格");
        }

        // 获取操作人信息
        Agent agent = agentService.getById(currentAgentId);

        // 记录价格变动日志
        HousePriceLog log = new HousePriceLog();
        log.setTenantId(tenantId);
        log.setHouseId(houseId);
        log.setPriceBefore(oldPrice);
        log.setPriceAfter(newPrice);
        log.setChangeReason(reason);
        log.setOperatorId(currentAgentId);
        log.setOperatorName(agent != null ? agent.getName() : "未知");
        log.setCreateTime(LocalDateTime.now());

        return save(log);
    }
}

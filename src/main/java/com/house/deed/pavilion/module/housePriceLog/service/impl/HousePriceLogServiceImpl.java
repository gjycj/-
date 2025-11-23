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
import com.house.deed.pavilion.module.housePriceLog.dto.HousePriceLogDTO;
import com.house.deed.pavilion.module.housePriceLog.entity.HousePriceLog;
import com.house.deed.pavilion.module.housePriceLog.mapper.HousePriceLogMapper;
import com.house.deed.pavilion.module.housePriceLog.service.IHousePriceLogService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
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
@Slf4j
public class HousePriceLogServiceImpl extends ServiceImpl<HousePriceLogMapper, HousePriceLog> implements IHousePriceLogService {

    @Resource
    private IHouseService houseService;

    @Resource
    private IAgentService agentService;

    /**
     * 按房源ID查询价格变动记录（查询权限控制）
     */
    @Override
    @AgentDataPermission(
            operation = AgentDataPermission.OperationType.QUERY,
            entityClass = HousePriceLog.class,
            dataIdParam = "houseId", // 入参中的房源ID
            creatorField = "createAgentId" // 日志创建人字段
    )
    public List<HousePriceLog> getByHouseId(Long houseId) {
        return lambdaQuery()
                .eq(HousePriceLog::getHouseId, houseId)
                .orderByDesc(HousePriceLog::getCreateTime)
                .list();
    }

    /**
     * 创建价格变动记录（创建权限控制）
     * 注：需校验操作人是否有权限修改对应房源的价格（关联房源创建人）
     */
    @Override
    @AgentDataPermission(
            operation = AgentDataPermission.OperationType.CREATE,
            entityClass = HousePriceLog.class,
            dataIdParam = "dto.houseId", // DTO中的房源ID
            creatorField = "createAgentId" // 日志创建人字段（自动绑定当前经纪人）
    )
    @Transactional(rollbackFor = Exception.class)
    public boolean recordPriceChange(HousePriceLogDTO housePriceLogDTO) {
        Long currentAgentId = AgentContext.getAgentId();
        Long tenantId = TenantContext.getTenantId();

        // 校验房源归属
        House house = houseService.getById(housePriceLogDTO.getHouseId());
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
        log.setHouseId(housePriceLogDTO.getHouseId());
        log.setPriceBefore(housePriceLogDTO.getOldPrice());
        log.setPriceAfter(housePriceLogDTO.getNewPrice());
        log.setChangeReason(housePriceLogDTO.getChangeReason());
        log.setOperatorId(currentAgentId);
        log.setOperatorName(agent != null ? agent.getName() : "未知");
        log.setCreateTime(LocalDateTime.now());

        return save(log);
    }

    @Override
    public boolean updateById(HousePriceLog entity) {
        throw new BusinessException(403, "价格变动记录为核心日志，不支持修改");
    }

    @Override
    public boolean removeById(Serializable id) {
        throw new BusinessException(403, "价格变动记录为核心日志，不支持删除");
    }
}

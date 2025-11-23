package com.house.deed.pavilion.module.visitRecord.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.common.aspect.annotation.AgentDataPermission;
import com.house.deed.pavilion.common.util.TenantContext;
import com.house.deed.pavilion.common.util.ValidateUtil;
import com.house.deed.pavilion.module.visitRecord.entity.VisitRecord;
import com.house.deed.pavilion.module.visitRecord.mapper.VisitRecordMapper;
import com.house.deed.pavilion.module.visitRecord.service.IVisitRecordService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 带看记录表（租户级数据） 服务实现类
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
@Service
public class VisitRecordServiceImpl extends ServiceImpl<VisitRecordMapper, VisitRecord> implements IVisitRecordService {

    /**
     * 批量查询带看记录（按ID列表）
     * 权限逻辑：仅能查询自己创建的带看记录（管理员/店长无限制）
     */
    @Override
    @AgentDataPermission(
            operation = AgentDataPermission.OperationType.QUERY,
            entityClass = VisitRecord.class,
            creatorField = "agentId" // 带看记录的创建人字段为agent_id
    )
    public List<VisitRecord> getBatchByIds(List<Long> ids) {
        Long tenantId = TenantContext.getTenantId();
        return baseMapper.selectList(new LambdaQueryWrapper<VisitRecord>()
                .eq(VisitRecord::getTenantId, tenantId)
                .in(VisitRecord::getId, ids)
        );
    }

    /**
     * 按客户ID查询带看记录
     * 权限逻辑：自动过滤当前经纪人创建的记录（管理员/店长无限制）
     */
    @Override
    @AgentDataPermission(
            operation = AgentDataPermission.OperationType.QUERY,
            entityClass = VisitRecord.class,
            creatorField = "agentId" // 带看记录的创建人字段
    )
    public List<VisitRecord> getByCustomerId(Long customerId) {
        Long tenantId = TenantContext.getTenantId();
        // 基础查询条件（权限过滤由注解自动追加）
        return baseMapper.selectList(
                new LambdaQueryWrapper<VisitRecord>()
                        .eq(VisitRecord::getTenantId, tenantId)
                        .eq(VisitRecord::getCustomerId, customerId)
        );
    }

    /**
     * 按房源ID查询带看记录
     * 权限逻辑：自动过滤当前经纪人创建的记录（管理员/店长无限制）
     */
    @Override
    @AgentDataPermission(
            operation = AgentDataPermission.OperationType.QUERY,
            entityClass = VisitRecord.class,
            creatorField = "agentId"
    )
    public List<VisitRecord> getByHouseId(Long houseId, Long tenantId) {
        ValidateUtil.notNull(houseId, "房源ID不能为空");
        ValidateUtil.notNull(tenantId, "租户ID不能为空");
        // 基础查询条件（权限过滤由注解自动追加）
        return baseMapper.selectList(new LambdaQueryWrapper<VisitRecord>()
                .eq(VisitRecord::getHouseId, houseId)
                .eq(VisitRecord::getTenantId, tenantId)
        );
    }

    /**
     * 按合同ID查询带看记录
     * 权限逻辑：自动过滤当前经纪人创建的记录（管理员/店长无限制）
     */
    @Override
    @AgentDataPermission(
            operation = AgentDataPermission.OperationType.QUERY,
            entityClass = VisitRecord.class,
            creatorField = "agentId"
    )
    public List<VisitRecord> getByContractId(Long contractId, Long tenantId) {
        // 基础查询条件（权限过滤由注解自动追加）
        return lambdaQuery()
                .eq(VisitRecord::getTenantId, tenantId)
                .eq(VisitRecord::getContractId, contractId)
                .list();
    }
}
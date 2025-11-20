package com.house.deed.pavilion.module.visitRecord.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.common.util.AgentContext;
import com.house.deed.pavilion.common.util.RoleUtil;
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

    @Override
    public List<VisitRecord> getByCustomerId(Long customerId) {
        Long currentAgentId = AgentContext.getAgentId();
        Long tenantId = TenantContext.getTenantId();
        boolean isPrivileged = RoleUtil.isAdmin() || RoleUtil.isStoreManager();
        // 非管理员/店长仅能查询自己的带看记录
        if (!isPrivileged) {
            return baseMapper.selectList(
                    new LambdaQueryWrapper<VisitRecord>()
                            .eq(VisitRecord::getTenantId, tenantId)
                            .eq(VisitRecord::getCustomerId, customerId)
                            .eq(VisitRecord::getAgentId, currentAgentId)
            );
        }
        return baseMapper.selectList(
                new LambdaQueryWrapper<VisitRecord>()
                        .eq(VisitRecord::getTenantId, tenantId)
                        .eq(VisitRecord::getCustomerId, customerId)
        );
    }

    @Override
    public List<VisitRecord> getByHouseId(Long houseId, Long tenantId) {
        ValidateUtil.notNull(houseId, "房源ID不能为空");
        ValidateUtil.notNull(tenantId, "租户ID不能为空");
        Long currentAgentId = AgentContext.getAgentId();
        boolean isPrivileged = RoleUtil.isAdmin() || RoleUtil.isStoreManager();
        // 非管理员/店长仅能查询自己的带看记录
        if (!isPrivileged) {
            return baseMapper.selectList(new LambdaQueryWrapper<VisitRecord>()
                    .eq(VisitRecord::getHouseId, houseId)
                    .eq(VisitRecord::getAgentId, currentAgentId)
                    .eq(VisitRecord::getTenantId, tenantId));
        }
        return baseMapper.selectList(new LambdaQueryWrapper<VisitRecord>()
                .eq(VisitRecord::getHouseId, houseId)
                .eq(VisitRecord::getTenantId, tenantId));
    }

    @Override
    public List<VisitRecord> getByContractId(Long contractId, Long tenantId) {
        Long currentAgentId = AgentContext.getAgentId();
        boolean isPrivileged = RoleUtil.isAdmin() || RoleUtil.isStoreManager();
        if (!isPrivileged) {
            return lambdaQuery()
                    .eq(VisitRecord::getTenantId, tenantId)
                    .eq(VisitRecord::getContractId, contractId)
                    .eq(VisitRecord::getAgentId, currentAgentId)
                    .list();
        }
        return lambdaQuery()
                .eq(VisitRecord::getTenantId, tenantId)
                .eq(VisitRecord::getContractId, contractId)
                .list();
    }

}

package com.house.deed.pavilion.module.contractLeaseTerms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.common.aspect.annotation.AgentDataPermission;
import com.house.deed.pavilion.common.exception.BusinessException;
import com.house.deed.pavilion.common.util.ContractValidationUtil;
import com.house.deed.pavilion.common.util.RoleUtil;
import com.house.deed.pavilion.common.util.TenantContext;
import com.house.deed.pavilion.module.contract.entity.Contract;
import com.house.deed.pavilion.module.contractLeaseTerms.entity.ContractLeaseTerms;
import com.house.deed.pavilion.module.contractLeaseTerms.mapper.ContractLeaseTermsMapper;
import com.house.deed.pavilion.module.contractLeaseTerms.service.IContractLeaseTermsService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * 租赁合同附加条款表（租户级数据） 服务实现类
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
@Service
public class ContractLeaseTermsServiceImpl extends ServiceImpl<ContractLeaseTermsMapper, ContractLeaseTerms> implements IContractLeaseTermsService {

    @Resource
    private ContractValidationUtil contractValidationUtil;

    // 新增/更新：权限关联合同创建人
    @Override
    @AgentDataPermission(
            operation = AgentDataPermission.OperationType.UPDATE,
            entityClass = Contract.class, // 关联合同实体
            dataIdParam = "terms.contractId", // 合同ID参数
            creatorField = "agentId" // 合同创建人字段（agent_id）
    )
    public boolean saveOrUpdateLeaseTerms(ContractLeaseTerms terms) {
        Long tenantId = TenantContext.getTenantId();
        terms.setTenantId(tenantId);

        // 校验合同存在性及类型（必须是租赁合同）
        Contract contract = contractValidationUtil.validateContract(terms.getContractId(), tenantId);
        if (!"RENT".equals(contract.getContractType())) {
            throw new BusinessException(400, "仅租赁合同可添加附加条款");
        }

        return saveOrUpdate(terms);
    }

    // 查询：自动继承合同的权限过滤
    @Override
    @AgentDataPermission(
            operation = AgentDataPermission.OperationType.QUERY,
            entityClass = Contract.class,
            dataIdParam = "contractId",
            creatorField = "agentId"
    )
    public ContractLeaseTerms getByContractId(Long contractId) {
        Long tenantId = TenantContext.getTenantId();
        return baseMapper.selectOne(new LambdaQueryWrapper<ContractLeaseTerms>()
                .eq(ContractLeaseTerms::getTenantId, tenantId)
                .eq(ContractLeaseTerms::getContractId, contractId)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @AgentDataPermission(
            operation = AgentDataPermission.OperationType.DELETE,
            entityClass = Contract.class,
            dataIdParam = "contractId",
            creatorField = "agentId"
    )
    public boolean removeByContractId(Long contractId) {
        Long tenantId = TenantContext.getTenantId();
        // 1. 校验合同存在性及租户归属
        Contract contract = contractValidationUtil.validateContract(contractId, tenantId);

        // 2. 校验合同类型（仅租赁合同可操作）
        if (!"RENT".equals(contract.getContractType())) {
            throw new BusinessException(400, "仅租赁合同的附加条款可删除");
        }

        // 3. 新增：校验前校验合同状态，已终止合同不允许删除条款
        if ("TERMINATED".equals(contract.getStatus())) {
            throw new BusinessException(400, "已终止的合同不允许删除附加条款");
        }

        // 4. 校验权限校验（仅管理员/店长可操作）
        if (!RoleUtil.isAdmin() && !RoleUtil.isStoreManager()) {
            throw new BusinessException(403, "无权删除：仅管理员或店长可操作");
        }

        // 5. 校验条款存在性校验
        ContractLeaseTerms terms = getByContractId(contractId);
        if (terms == null) {
            throw new BusinessException(404, "该合同无附加条款，无需删除");
        }

        // 6. 执行删除
        int deleteCount = baseMapper.delete(new LambdaQueryWrapper<ContractLeaseTerms>()
                .eq(ContractLeaseTerms::getTenantId, tenantId)
                .eq(ContractLeaseTerms::getContractId, contractId)
        );

        return deleteCount > 0;
    }

    // 批量查询：用于列表接口优化
    @Override
    @AgentDataPermission(
            operation = AgentDataPermission.OperationType.QUERY,
            entityClass = Contract.class,
            dataIdParam = "contractIds", // 批量参数
            creatorField = "agentId"
    )
    public Map<Long, ContractLeaseTerms> getBatchByContractIds(List<Long> contractIds) {
        Long tenantId = TenantContext.getTenantId();
        List<ContractLeaseTerms> termsList = baseMapper.selectList(new LambdaQueryWrapper<ContractLeaseTerms>()
                .eq(ContractLeaseTerms::getTenantId, tenantId)
                .in(ContractLeaseTerms::getContractId, contractIds)
        );
        // 转为合同ID->条款的映射
        return termsList.stream()
                .collect(Collectors.toMap(ContractLeaseTerms::getContractId, Function.identity()));
    }

}

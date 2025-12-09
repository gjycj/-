package com.house.deed.pavilion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.entity.ContractLeaseTerms;
import com.house.deed.pavilion.mapper.ContractLeaseTermsMapper;
import com.house.deed.pavilion.service.ContractLeaseTermsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

/**
 * 租赁合同附加条款服务实现类
 * 提供租赁合同附加条款的CRUD、批量操作、多条件查询等功能
 * 所有操作均强制租户数据隔离，确保数据安全
 *
 * @author 系统生成
 * @version 1.0
 * @since 2024
 */
@Service
public class ContractLeaseTermsServiceImpl extends ServiceImpl<ContractLeaseTermsMapper, ContractLeaseTerms> implements ContractLeaseTermsService {

    // ==================== 私有工具方法（复用逻辑） ====================

    /**
     * 校验指定条款ID是否属于当前租户
     * 用于单条数据的权限校验，确保用户只能操作自己租户的数据
     *
     * @param id 租赁合同附加条款ID
     * @param tenantId 当前租户ID
     * @return boolean 校验通过返回true
     * @throws IllegalArgumentException 当条款不存在或租户ID不匹配时抛出
     */
    private boolean validateTermsBelongToTenant(Long id, Long tenantId) {
        // 根据ID查询条款信息
        ContractLeaseTerms terms = baseMapper.selectById(id);

        // 检查条款是否存在
        if (terms == null) {
            throw new IllegalArgumentException("租赁合同附加条款不存在，条款ID：" + id);
        }

        // 检查租户权限：当前操作租户必须与条款所属租户一致
        if (!terms.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("无权限操作其他租户的条款，条款ID：" + id);
        }
        return true;
    }

    /**
     * 批量校验多个条款ID是否属于当前租户
     * 用于批量操作前的权限校验，确保批量操作的数据都在当前租户权限范围内
     * 使用QueryWrapper避免Lambda表达式可能引起的缓存问题
     *
     * @param ids 租赁合同附加条款ID列表
     * @param tenantId 当前租户ID
     * @throws IllegalArgumentException 当条款不存在或租户ID不匹配时抛出
     */
    private void validateTermsIdsBelongToTenant(List<Long> ids, Long tenantId) {
        // 空列表直接返回，不做数据库查询
        if (ids == null || ids.isEmpty()) {
            return;
        }

        // 构建查询条件：只查询ID和租户ID字段，提高查询效率
        QueryWrapper<ContractLeaseTerms> wrapper = new QueryWrapper<>();
        wrapper.select("id", "tenant_id")
                .in("id", ids);

        // 执行查询，获取存在的条款列表
        List<ContractLeaseTerms> termsList = baseMapper.selectList(wrapper);

        // 提取数据库中实际存在的条款ID
        List<Long> existingIds = termsList.stream()
                .map(ContractLeaseTerms::getId)
                .toList();

        // 检查请求的ID列表中哪些在数据库中不存在
        List<Long> nonExistentIds = ids.stream()
                .filter(id -> !existingIds.contains(id))
                .toList();

        // 如果存在不存在的ID，抛出异常
        if (!nonExistentIds.isEmpty()) {
            throw new IllegalArgumentException("以下条款ID不存在：" + nonExistentIds);
        }

        // 检查租户权限：找出不属于当前租户的条款ID
        List<Long> invalidIds = termsList.stream()
                .filter(terms -> !terms.getTenantId().equals(tenantId))
                .map(ContractLeaseTerms::getId)
                .toList();

        // 如果存在无权限操作的条款，抛出异常
        if (!invalidIds.isEmpty()) {
            throw new IllegalArgumentException("无权限操作其他租户的条款，无效条款ID：" + invalidIds);
        }
    }

    /**
     * 构建动态查询条件包装器
     * 根据查询参数动态构建查询条件，并强制添加租户隔离条件
     * 支持合同ID、是否允许养宠物、是否允许转租等条件的动态拼接
     *
     * @param queryParams 查询参数Map，支持contractId、allowPet、allowSublet等键
     * @param tenantId 当前租户ID，用于数据隔离
     * @return QueryWrapper<ContractLeaseTerms> 构建好的查询条件包装器
     */
    private QueryWrapper<ContractLeaseTerms> buildQueryWrapper(Map<String, Object> queryParams, Long tenantId) {
        QueryWrapper<ContractLeaseTerms> queryWrapper = new QueryWrapper<>();

        // 强制租户数据隔离：所有查询都必须包含租户ID条件
        queryWrapper.eq("tenant_id", tenantId);

        // 动态拼接查询条件：只有参数非空时才添加对应条件
        if (queryParams != null) {
            // 按合同ID查询
            if (queryParams.containsKey("contractId") && queryParams.get("contractId") != null) {
                queryWrapper.eq("contract_id", queryParams.get("contractId"));
            }

            // 按是否允许养宠物查询（0-不允许，1-允许）
            if (queryParams.containsKey("allowPet") && queryParams.get("allowPet") != null) {
                queryWrapper.eq("allow_pet", queryParams.get("allowPet"));
            }

            // 按是否允许转租查询（0-不允许，1-允许）
            if (queryParams.containsKey("allowSublet") && queryParams.get("allowSublet") != null) {
                queryWrapper.eq("allow_sublet", queryParams.get("allowSublet"));
            }

            // 可在此扩展其他查询条件，如创建时间范围、更新时-间范围等
        }

        return queryWrapper;
    }

    // ==================== 基础CRUD操作实现 ====================

    /**
     * 新增租赁合同附加条款
     * 在保存前会校验必填字段，确保数据的完整性
     *
     * @param terms 租赁合同附加条款实体对象
     * @return boolean 新增成功返回true，失败返回false
     * @throws IllegalArgumentException 当租户ID或合同ID为空时抛出
     */
    @Override
    public boolean saveTerms(ContractLeaseTerms terms) {
        // 校验租户ID：确保数据有明确的租户归属
        if (terms.getTenantId() == null) {
            throw new IllegalArgumentException("新增条款失败：租户ID不能为空");
        }

        // 校验合同ID：确保条款有明确的合同关联
        if (terms.getContractId() == null) {
            throw new IllegalArgumentException("新增条款失败：合同ID不能为空");
        }

        // 执行插入操作，返回影响行数大于0表示成功
        return baseMapper.insert(terms) > 0;
    }

    /**
     * 根据ID更新租赁合同附加条款
     * 更新前会校验该条款是否属于当前租户，确保数据安全
     *
     * @param terms 租赁合同附加条款实体对象，必须包含ID和tenantId
     * @return boolean 更新成功返回true，失败返回false
     * @throws IllegalArgumentException 当条款不存在或租户ID不匹配时抛出
     */
    @Override
    public boolean updateTermsById(ContractLeaseTerms terms) {
        // 前置权限校验：确保当前租户有权限更新该条款
        validateTermsBelongToTenant(terms.getId(), terms.getTenantId());

        // 执行更新操作，返回影响行数大于0表示成功
        return baseMapper.updateById(terms) > 0;
    }

    /**
     * 根据ID删除租赁合同附加条款
     * 删除前会校验该条款是否属于当前租户，防止越权删除
     *
     * @param id 要删除的条款ID
     * @param tenantId 当前租户ID
     * @return boolean 删除成功返回true，失败返回false
     * @throws IllegalArgumentException 当条款不存在或租户ID不匹配时抛出
     */
    @Override
    public boolean removeTermsById(Long id, Long tenantId) {
        // 前置权限校验：确保当前租户有权限删除该条款
        validateTermsBelongToTenant(id, tenantId);

        // 执行删除操作，返回影响行数大于0表示成功
        return baseMapper.deleteById(id) > 0;
    }

    /**
     * 根据ID查询租赁合同附加条款详情
     * 查询时会强制添加租户隔离条件，确保只能查询到本租户的数据
     *
     * @param id 要查询的条款ID
     * @param tenantId 当前租户ID
     * @return ContractLeaseTerms 租赁合同附加条款实体，未找到时返回null
     */
    @Override
    public ContractLeaseTerms getTermsById(Long id, Long tenantId) {
        // 构建查询条件：同时匹配ID和租户ID
        QueryWrapper<ContractLeaseTerms> wrapper = new QueryWrapper<>();
        wrapper.eq("id", id)
                .eq("tenant_id", tenantId);

        // 执行查询，返回单个结果
        return baseMapper.selectOne(wrapper);
    }

    // ==================== 多条件查询操作实现 ====================

    /**
     * 分页查询租赁合同附加条款
     * 支持多条件动态查询，结果按创建时间倒序排列（最新的在前）
     *
     * @param page 分页参数对象，包含页码、每页大小等信息
     * @param queryParams 查询参数Map，支持contractId、allowPet、allowSublet等条件
     * @param tenantId 当前租户ID
     * @return IPage<ContractLeaseTerms> 分页结果对象，包含数据列表和分页信息
     */
    @Override
    public IPage<ContractLeaseTerms> pageQuery(Page<ContractLeaseTerms> page, Map<String, Object> queryParams, Long tenantId) {
        // 构建动态查询条件
        QueryWrapper<ContractLeaseTerms> queryWrapper = buildQueryWrapper(queryParams, tenantId);

        // 按创建时间倒序排列，确保最新的数据在前
        queryWrapper.orderByDesc("create_time");

        // 执行分页查询
        return baseMapper.selectPage(page, queryWrapper);
    }

    /**
     * 多条件查询租赁合同附加条款列表
     * 支持多条件动态查询，结果按创建时间倒序排列，不分页
     *
     * @param queryParams 查询参数Map，支持contractId、allowPet、allowSublet等条件
     * @param tenantId 当前租户ID
     * @return List<ContractLeaseTerms> 条款实体列表
     */
    @Override
    public List<ContractLeaseTerms> listByConditions(Map<String, Object> queryParams, Long tenantId) {
        // 构建动态查询条件
        QueryWrapper<ContractLeaseTerms> queryWrapper = buildQueryWrapper(queryParams, tenantId);

        // 按创建时间倒序排列
        queryWrapper.orderByDesc("create_time");

        // 执行查询，返回列表
        return baseMapper.selectList(queryWrapper);
    }

    /**
     * 根据合同ID查询相关的所有附加条款
     * 用于获取指定合同下的所有条款信息
     *
     * @param contractId 合同ID
     * @param tenantId 当前租户ID
     * @return List<ContractLeaseTerms> 该合同下的条款列表，按创建时间倒序排列
     */
    @Override
    public List<ContractLeaseTerms> listByContractId(Long contractId, Long tenantId) {
        // 构建查询条件：匹配合同ID和租户ID
        QueryWrapper<ContractLeaseTerms> wrapper = new QueryWrapper<>();
        wrapper.eq("contract_id", contractId)
                .eq("tenant_id", tenantId)
                .orderByDesc("create_time"); // 按创建时间倒序

        // 执行查询，返回列表
        return baseMapper.selectList(wrapper);
    }

    // ==================== 批量操作实现 ====================

    /**
     * 批量新增租赁合同附加条款
     * 使用事务保证批量操作的原子性，任一失败则全部回滚
     *
     * @param termsList 租赁合同附加条款实体列表
     * @return boolean 批量新增成功返回true，失败返回false
     * @throws IllegalArgumentException 当列表中存在租户ID或合同ID为空的条款时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchSaveTerms(List<ContractLeaseTerms> termsList) {
        // 空列表检查
        if (termsList == null || termsList.isEmpty()) {
            return false;
        }

        // 批量校验：检查每个条款的必填字段
        for (ContractLeaseTerms terms : termsList) {
            if (terms.getTenantId() == null) {
                throw new IllegalArgumentException("批量新增失败：条款列表中存在租户ID为空的记录");
            }
            if (terms.getContractId() == null) {
                throw new IllegalArgumentException("批量新增失败：条款列表中存在合同ID为空的记录");
            }
        }

        // 执行批量保存
        return saveBatch(termsList);
    }

    /**
     * 批量更新租赁合同附加条款的指定字段
     * 使用事务保证批量操作的原子性，支持部分字段更新
     *
     * @param ids 要更新的条款ID列表
     * @param allowPet 是否允许养宠物（0-不允许，1-允许），为null时不更新该字段
     * @param allowSublet 是否允许转租（0-不允许，1-允许），为null时不更新该字段
     * @param tenantId 当前租户ID
     * @return boolean 批量更新成功返回true，失败返回false
     * @throws IllegalArgumentException 当条款不存在或租户ID不匹配时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchUpdateTerms(List<Long> ids, Byte allowPet, Byte allowSublet, Long tenantId) {
        // 空列表检查
        if (ids == null || ids.isEmpty()) {
            return false;
        }

        // 前置权限校验：确保所有要更新的条款都属于当前租户
        validateTermsIdsBelongToTenant(ids, tenantId);

        // 构建更新实体对象：只设置需要更新的字段
        ContractLeaseTerms updateEntity = new ContractLeaseTerms();
        if (allowPet != null) {
            updateEntity.setAllowPet(allowPet);
        }
        if (allowSublet != null) {
            updateEntity.setAllowSublet(allowSublet);
        }

        // 构建更新条件：匹配ID列表和租户ID
        QueryWrapper<ContractLeaseTerms> updateWrapper = new QueryWrapper<>();
        updateWrapper.in("id", ids)
                .eq("tenant_id", tenantId);

        // 执行批量更新，返回影响行数大于0表示成功
        return baseMapper.update(updateEntity, updateWrapper) > 0;
    }

    /**
     * 批量删除租赁合同附加条款
     * 使用事务保证批量操作的原子性，任一失败则全部回滚
     *
     * @param ids 要删除的条款ID列表
     * @param tenantId 当前租户ID
     * @return boolean 批量删除成功返回true，失败返回false
     * @throws IllegalArgumentException 当条款不存在或租户ID不匹配时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchRemoveTerms(List<Long> ids, Long tenantId) {
        // 空列表检查
        if (ids == null || ids.isEmpty()) {
            return false;
        }

        // 前置权限校验：确保所有要删除的条款都属于当前租户
        validateTermsIdsBelongToTenant(ids, tenantId);

        // 执行批量删除，返回影响行数大于0表示成功
        return baseMapper.deleteBatchIds(ids) > 0;
    }
}
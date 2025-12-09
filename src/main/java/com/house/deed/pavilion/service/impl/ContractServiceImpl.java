package com.house.deed.pavilion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.entity.Contract;
import com.house.deed.pavilion.mapper.ContractMapper;
import com.house.deed.pavilion.service.ContractService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import java.util.List;
import java.util.Map;

/**
 * 交易合同表服务实现类
 * 负责处理租户合同数据的核心业务逻辑，包括CRUD操作、批量处理和多条件查询
 * 所有操作均强制租户数据隔离，确保不同租户间的数据安全隔离
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Service
public class ContractServiceImpl extends ServiceImpl<ContractMapper, Contract> implements ContractService {

    // ==================== 公共条件构建方法 ====================

    /**
     * 构建统一查询条件包装器
     * 统一处理租户数据隔离和动态查询参数，确保所有查询都具备租户隔离特性
     *
     * @param queryParams 动态查询参数Map，支持以下查询条件：
     *                   - contractNo: 合同编号（精确匹配）
     *                   - status: 合同状态（精确匹配）
     *                   - startTime: 签订时间范围开始时间
     *                   - endTime: 签订时间范围结束时间
     *                   - partyAName: 甲方名称（模糊匹配）
     * @param tenantId 当前租户ID，用于强制数据隔离
     * @return QueryWrapper<Contract> 构建完成的查询条件包装器
     */
    private QueryWrapper<Contract> buildQueryWrapper(Map<String, Object> queryParams, Long tenantId) {
        QueryWrapper<Contract> queryWrapper = new QueryWrapper<>();

        // 强制租户数据隔离：所有查询必须包含租户ID条件
        queryWrapper.eq("tenant_id", tenantId);

        // 动态拼接查询条件：仅处理非空参数，避免无效查询条件
        if (queryParams != null) {
            // 合同编号精确匹配查询
            if (!ObjectUtils.isEmpty(queryParams.get("contractNo"))) {
                queryWrapper.eq("contract_no", queryParams.get("contractNo"));
            }

            // 合同状态精确匹配查询（如：1=生效中，2=已终止，3=已解约等）
            if (!ObjectUtils.isEmpty(queryParams.get("status"))) {
                queryWrapper.eq("status", queryParams.get("status"));
            }

            // 签订时间范围查询：大于等于开始时间
            if (!ObjectUtils.isEmpty(queryParams.get("startTime"))) {
                queryWrapper.ge("sign_time", queryParams.get("startTime"));
            }

            // 签订时间范围查询：小于等于结束时间
            if (!ObjectUtils.isEmpty(queryParams.get("endTime"))) {
                queryWrapper.le("sign_time", queryParams.get("endTime"));
            }

            // 甲方名称模糊查询（支持部分匹配）
            if (!ObjectUtils.isEmpty(queryParams.get("partyAName"))) {
                queryWrapper.like("party_a_name", queryParams.get("partyAName"));
            }

            // 可扩展其他查询条件区域
        }

        // 默认排序规则：按签订时间倒序排列，最新的合同显示在前面
        queryWrapper.orderByDesc("sign_time");
        return queryWrapper;
    }


    // ==================== 多条件分页查询 ====================

    /**
     * 多条件分页查询合同数据
     * 支持动态查询条件，结果按签订时间倒序排列，适用于前端表格展示
     *
     * @param page 分页参数对象，包含当前页码、每页大小等分页信息
     * @param queryParams 动态查询参数，支持多种查询条件组合
     * @param tenantId 当前租户ID，用于数据隔离
     * @return IPage<Contract> 分页结果对象，包含数据列表和完整的分页信息
     */
    @Override
    public IPage<Contract> pageQuery(Page<Contract> page, Map<String, Object> queryParams, Long tenantId) {
        QueryWrapper<Contract> queryWrapper = buildQueryWrapper(queryParams, tenantId);
        return baseMapper.selectPage(page, queryWrapper);
    }


    // ==================== 多条件查询列表 ====================

    /**
     * 多条件查询合同列表（不分页）
     * 适用于数据导出、下拉选择等不需要分页的业务场景
     *
     * @param queryParams 动态查询参数，支持多种查询条件组合
     * @param tenantId 当前租户ID，用于数据隔离
     * @return List<Contract> 符合条件的合同实体列表，按签订时间倒序排列
     */
    @Override
    public List<Contract> listByConditions(Map<String, Object> queryParams, Long tenantId) {
        QueryWrapper<Contract> queryWrapper = buildQueryWrapper(queryParams, tenantId);
        return baseMapper.selectList(queryWrapper);
    }


    // ==================== 批量新增操作 ====================

    /**
     * 批量新增合同数据
     * 使用事务保证数据一致性，任一合同保存失败则全部回滚
     * 会校验所有合同必须属于同一租户，避免数据混乱
     *
     * @param contracts 待新增的合同实体列表
     * @return boolean 批量新增成功返回true，失败返回false
     * @throws IllegalArgumentException 当合同列表为空或租户不一致时抛出业务异常
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchSaveContracts(List<Contract> contracts) {
        // 参数空值校验
        if (contracts == null || contracts.isEmpty()) {
            return false;
        }

        // 获取基准租户ID（以第一个合同的租户ID为准）
        Long tenantId = contracts.get(0).getTenantId();

        // 批量租户一致性校验：确保所有合同都属于同一租户
        for (Contract contract : contracts) {
            if (!tenantId.equals(contract.getTenantId())) {
                throw new IllegalArgumentException("批量新增失败：合同所属租户不一致");
            }
            // 可扩展其他业务校验逻辑区域
        }

        // 执行批量保存操作
        return saveBatch(contracts);
    }


    // ==================== 批量更新状态 ====================

    /**
     * 批量更新合同状态
     * 适用于批量生效、批量终止等业务场景，使用事务保证操作原子性
     *
     * @param ids 待更新状态的合同ID列表
     * @param status 目标状态值（如："生效"、"终止"、"作废"等）
     * @param tenantId 当前租户ID，用于权限校验
     * @return boolean 更新成功返回true，失败返回false
     * @throws IllegalArgumentException 当ID列表为空或权限校验失败时抛出业务异常
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchUpdateStatus(List<Long> ids, String status, Long tenantId) {
        // 参数空值校验
        if (ids == null || ids.isEmpty()) {
            return false;
        }

        // 前置权限校验：确保所有要更新的合同都属于当前租户
        validateContractIdsBelongToTenant(tenantId, ids);

        // 构建更新实体，只设置需要更新的状态字段
        Contract updateContract = new Contract();
        updateContract.setStatus(status);

        // 构建更新条件：匹配ID列表和租户ID
        QueryWrapper<Contract> updateWrapper = new QueryWrapper<>();
        updateWrapper.in("id", ids)
                .eq("tenant_id", tenantId);

        // 执行批量更新操作
        return baseMapper.update(updateContract, updateWrapper) > 0;
    }


    // ==================== 批量删除操作 ====================

    /**
     * 批量删除合同数据
     * 使用事务保证操作原子性，删除前会进行严格的权限校验
     * 注意：此操作会物理删除数据，请谨慎使用
     *
     * @param ids 待删除的合同ID列表
     * @param tenantId 当前租户ID，用于权限校验
     * @return boolean 删除成功返回true，失败返回false
     * @throws IllegalArgumentException 当ID列表为空或权限校验失败时抛出业务异常
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchRemoveContracts(List<Long> ids, Long tenantId) {
        // 参数空值校验
        if (ids == null || ids.isEmpty()) {
            return false;
        }

        // 前置权限校验：确保所有要删除的合同都属于当前租户
        validateContractIdsBelongToTenant(tenantId, ids);

        // 执行批量删除操作
        return baseMapper.deleteBatchIds(ids) > 0;
    }


    // ==================== 私有校验方法 ====================

    /**
     * 验证合同ID列表是否全部属于当前租户
     * 用于批量操作前的权限校验，确保数据安全性和操作权限
     *
     * @param tenantId 当前操作租户ID
     * @param contractIds 待校验的合同ID列表
     * @throws IllegalArgumentException 当存在不存在的ID或跨租户操作的ID时抛出详细异常信息
     */
    private void validateContractIdsBelongToTenant(Long tenantId, List<Long> contractIds) {
        // 查询数据库中存在的合同ID及其租户信息（仅查询必要字段）
        List<Contract> contracts = baseMapper.selectList(
                new QueryWrapper<Contract>()
                        .select("id", "tenant_id")
                        .in("id", contractIds)
        );

        // 提取数据库中实际存在的合同ID
        List<Long> existingIds = contracts.stream()
                .map(Contract::getId)
                .toList();

        // 检查请求的ID列表中哪些在数据库中不存在
        List<Long> nonExistentIds = contractIds.stream()
                .filter(id -> !existingIds.contains(id))
                .toList();

        // 如果存在不存在的ID，抛出详细异常信息
        if (!nonExistentIds.isEmpty()) {
            throw new IllegalArgumentException("合同ID不存在: " + nonExistentIds);
        }

        // 检查租户权限：找出不属于当前租户的合同ID
        List<Long> invalidIds = contracts.stream()
                .filter(contract -> !contract.getTenantId().equals(tenantId))
                .map(Contract::getId)
                .toList();

        // 如果存在跨租户操作的ID，抛出权限异常
        if (!invalidIds.isEmpty()) {
            throw new IllegalArgumentException("无权限操作其他租户的合同ID: " + invalidIds);
        }
    }

}
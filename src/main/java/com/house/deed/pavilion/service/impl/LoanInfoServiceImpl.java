package com.house.deed.pavilion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.entity.LoanInfo;
import com.house.deed.pavilion.mapper.LoanInfoMapper;
import com.house.deed.pavilion.service.LoanInfoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 贷款信息服务实现类
 *
 * <p>实现贷款信息的增删改查及批量操作，所有方法均包含租户级数据隔离和严格的业务校验</p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Service
public class LoanInfoServiceImpl extends ServiceImpl<LoanInfoMapper, LoanInfo> implements LoanInfoService {

    /**
     * 合法的贷款类型枚举集合
     * <p>仅允许以下三种贷款类型：</p>
     * <ul>
     *   <li>COMMERCIAL - 商业贷款</li>
     *   <li>FUND - 公积金贷款</li>
     *   <li>COMBINED - 组合贷款</li>
     * </ul>
     */
    private static final Set<String> VALID_LOAN_TYPES = Set.of("COMMERCIAL", "FUND", "COMBINED");

    /**
     * 合法的贷款状态枚举集合
     * <p>仅允许以下三种贷款状态：</p>
     * <ul>
     *   <li>APPLYING - 申请中</li>
     *   <li>APPROVED - 已审批通过</li>
     *   <li>REJECTED - 已拒绝</li>
     * </ul>
     */
    private static final Set<String> VALID_LOAN_STATUSES = Set.of("APPLYING", "APPROVED", "REJECTED");

    /**
     * 新增贷款信息
     *
     * <p>业务校验：</p>
     * <ul>
     *   <li>同一租户下合同ID+贷款类型组合必须唯一（避免同一合同重复申请同类型贷款）</li>
     *   <li>贷款类型必须在合法枚举范围内</li>
     *   <li>贷款状态必须在合法枚举范围内</li>
     *   <li>审批状态与审批时间必须联动（仅APPROVED状态可填写审批时间）</li>
     * </ul>
     * <p>技术实现：自动填充createTime（通过实体类的@TableField(fill = FieldFill.INSERT)配置）</p>
     *
     * @param loanInfo 贷款信息实体对象，需包含租户ID、合同ID、贷款类型、贷款状态等必填信息
     * @return 新增成功返回true，否则返回false
     * @throws IllegalArgumentException 当违反任何业务规则时抛出，包含具体的错误信息
     */
    @Override
    public boolean saveLoanInfo(LoanInfo loanInfo) {
        // 校验贷款类型合法性
        if (!VALID_LOAN_TYPES.contains(loanInfo.getLoanType())) {
            throw new IllegalArgumentException("无效贷款类型：" + loanInfo.getLoanType() +
                    "，允许值：COMMERCIAL/FUND/COMBINED");
        }

        // 校验贷款状态合法性
        if (!VALID_LOAN_STATUSES.contains(loanInfo.getLoanStatus())) {
            throw new IllegalArgumentException("无效贷款状态：" + loanInfo.getLoanStatus() +
                    "，允许值：APPLYING/APPROVED/REJECTED");
        }

        // 校验状态与审批时间联动关系
        validateStatusAndApproveTime(loanInfo.getLoanStatus(), loanInfo.getApproveTime());

        // 校验租户内合同+贷款类型唯一性
        QueryWrapper<LoanInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("tenant_id", loanInfo.getTenantId())
                .eq("contract_id", loanInfo.getContractId())
                .eq("loan_type", loanInfo.getLoanType());
        long count = baseMapper.selectCount(wrapper);
        if (count > 0) {
            throw new IllegalArgumentException("当前租户下合同ID=" + loanInfo.getContractId() +
                    "已存在" + loanInfo.getLoanType() + "类型贷款");
        }

        // 执行新增操作（createTime自动填充）
        return baseMapper.insert(loanInfo) > 0;
    }

    /**
     * 根据ID更新贷款信息
     *
     * <p>业务校验：</p>
     * <ul>
     *   <li>数据必须存在且属于当前租户</li>
     *   <li>贷款类型不可变更（业务约束：贷款类型一旦确定不允许修改）</li>
     *   <li>状态变更时需校验审批时间联动规则</li>
     *   <li>贷款状态必须在合法枚举范围内</li>
     * </ul>
     * <p>技术实现：自动填充updateTime（通过实体类的@TableField(fill = FieldFill.INSERT_UPDATE)配置）</p>
     *
     * @param loanInfo 贷款信息实体对象，需包含主键ID、租户ID及需要更新的字段
     * @return 更新成功返回true，否则返回false
     * @throws IllegalArgumentException 当数据不存在、无权限操作、贷款类型被修改或违反其他业务规则时抛出
     */
    @Override
    public boolean updateLoanInfoById(LoanInfo loanInfo) {
        // 校验数据存在且属于当前租户
        LoanInfo existLoan = baseMapper.selectById(loanInfo.getId());
        if (existLoan == null || !existLoan.getTenantId().equals(loanInfo.getTenantId())) {
            throw new IllegalArgumentException("贷款记录不存在或无权限操作");
        }

        // 禁止修改贷款类型（业务约束）
        if (loanInfo.getLoanType() != null && !loanInfo.getLoanType().equals(existLoan.getLoanType())) {
            throw new IllegalArgumentException("贷款类型不可修改");
        }

        // 若修改状态，校验合法性及与审批时间的联动
        if (loanInfo.getLoanStatus() != null) {
            if (!VALID_LOAN_STATUSES.contains(loanInfo.getLoanStatus())) {
                throw new IllegalArgumentException("无效贷款状态：" + loanInfo.getLoanStatus());
            }
            validateStatusAndApproveTime(loanInfo.getLoanStatus(), loanInfo.getApproveTime());
        }

        // 执行更新操作（updateTime自动填充）
        return baseMapper.updateById(loanInfo) > 0;
    }

    /**
     * 根据ID物理删除贷款信息
     *
     * <p>业务校验：数据必须存在且属于当前租户</p>
     *
     * @param id 贷款信息主键ID
     * @param tenantId 租户ID，用于数据隔离验证
     * @return 删除成功返回true，否则返回false
     * @throws IllegalArgumentException 当数据不存在或无权限操作时抛出
     */
    @Override
    public boolean removeLoanInfoById(Long id, Long tenantId) {
        // 校验数据存在且属于当前租户
        LoanInfo existLoan = baseMapper.selectById(id);
        if (existLoan == null || !existLoan.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("贷款记录不存在或无权限操作");
        }
        return baseMapper.deleteById(id) > 0;
    }

    /**
     * 根据ID查询贷款详细信息（租户隔离）
     *
     * <p>查询时自动应用租户隔离条件，确保只能查询到当前租户的数据</p>
     *
     * @param id 贷款信息主键ID
     * @param tenantId 租户ID，用于数据隔离
     * @return 符合条件的贷款信息实体对象，未找到返回null
     */
    @Override
    public LoanInfo getLoanInfoById(Long id, Long tenantId) {
        QueryWrapper<LoanInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", id)
                .eq("tenant_id", tenantId);
        return baseMapper.selectOne(queryWrapper);
    }

    /**
     * 多条件分页查询贷款信息
     *
     * <p>支持以下查询条件：</p>
     * <ul>
     *   <li>银行名称模糊查询</li>
     *   <li>贷款类型精确查询</li>
     *   <li>贷款状态精确查询</li>
     *   <li>合同ID精确查询</li>
     *   <li>贷款金额范围查询（此处简化处理为精确查询）</li>
     *   <li>申请时间范围查询（示例：大于等于开始时间）</li>
     * </ul>
     * <p>默认按申请时间降序排列（最新的申请在前）</p>
     *
     * @param page 分页参数对象，包含页码和每页大小
     * @param loanInfo 查询条件实体对象，非空字段将作为查询条件
     * @param tenantId 租户ID，用于数据隔离
     * @return 分页查询结果，包含贷款信息列表和分页信息
     */
    @Override
    public IPage<LoanInfo> pageQuery(Page<LoanInfo> page, LoanInfo loanInfo, Long tenantId) {
        QueryWrapper<LoanInfo> queryWrapper = new QueryWrapper<>();
        // 强制租户隔离
        queryWrapper.eq("tenant_id", tenantId);

        // 动态拼接查询条件（非空字段才参与筛选）
        if (loanInfo.getBankName() != null) {
            queryWrapper.like("bank_name", loanInfo.getBankName());
        }
        if (loanInfo.getLoanType() != null) {
            queryWrapper.eq("loan_type", loanInfo.getLoanType());
        }
        if (loanInfo.getLoanStatus() != null) {
            queryWrapper.eq("loan_status", loanInfo.getLoanStatus());
        }
        if (loanInfo.getContractId() != null) {
            queryWrapper.eq("contract_id", loanInfo.getContractId());
        }
        // 金额范围查询（实际业务中可拆分为min和max两个参数）
        if (loanInfo.getLoanAmount() != null) {
            queryWrapper.eq("loan_amount", loanInfo.getLoanAmount());
        }
        // 申请时间范围查询（示例：大于等于开始时间）
        if (loanInfo.getApplyTime() != null) {
            queryWrapper.ge("apply_time", loanInfo.getApplyTime());
        }

        // 按申请时间降序排列
        queryWrapper.orderByDesc("apply_time");

        // 执行分页查询
        return baseMapper.selectPage(page, queryWrapper);
    }

    /**
     * 批量新增贷款信息（事务保证）
     *
     * <p>在单个事务中执行批量新增，任一记录校验失败或保存失败将导致整个操作回滚</p>
     * <p>批量校验包括：</p>
     * <ul>
     *   <li>基础规则校验（贷款类型、状态、状态与审批时间联动）</li>
     *   <li>合同+贷款类型组合唯一性校验</li>
     * </ul>
     * <p>注意：此方法仅校验批量数据内部的唯一性，未校验与数据库中现有数据的唯一性</p>
     *
     * @param loanInfos 贷款信息实体对象列表
     * @return 批量新增成功返回true，否则返回false
     * @throws IllegalArgumentException 当任意记录违反业务规则或存在重复的合同+贷款类型组合时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchSaveLoanInfos(List<LoanInfo> loanInfos) {
        if (loanInfos.isEmpty()) {
            return false;
        }

        // 批量校验基础规则
        for (LoanInfo loan : loanInfos) {
            // 校验贷款类型
            if (!VALID_LOAN_TYPES.contains(loan.getLoanType())) {
                throw new IllegalArgumentException("无效贷款类型：" + loan.getLoanType());
            }
            // 校验贷款状态
            if (!VALID_LOAN_STATUSES.contains(loan.getLoanStatus())) {
                throw new IllegalArgumentException("无效贷款状态：" + loan.getLoanStatus());
            }
            // 校验状态与审批时间联动
            validateStatusAndApproveTime(loan.getLoanStatus(), loan.getApproveTime());
        }

        // 批量校验合同+贷款类型唯一性（在批量数据内部）
        List<String> uniqueKeys = loanInfos.stream()
                .map(loan -> loan.getTenantId() + "_" + loan.getContractId() + "_" + loan.getLoanType())
                .toList();
        if (uniqueKeys.size() != uniqueKeys.stream().distinct().count()) {
            throw new IllegalArgumentException("批量数据中存在重复的合同+贷款类型组合");
        }

        // 执行批量保存（事务保证）
        return saveBatch(loanInfos);
    }

    /**
     * 批量删除贷款信息（事务保证）
     *
     * <p>在单个事务中执行批量删除，任一记录校验失败或删除失败将导致整个操作回滚</p>
     * <p>先验证所有ID都属于当前租户，然后执行批量删除</p>
     *
     * @param ids 待删除的贷款信息ID列表
     * @param tenantId 租户ID，用于数据隔离验证
     * @return 批量删除成功返回true，否则返回false
     * @throws IllegalArgumentException 当存在不属于当前租户的贷款ID时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchRemoveLoanInfos(List<Long> ids, Long tenantId) {
        if (ids.isEmpty()) {
            return false;
        }

        // 校验所有ID都属于当前租户
        validateLoanIdsBelongToTenant(tenantId, ids);

        // 批量删除（事务保证）
        return baseMapper.deleteBatchIds(ids) > 0;
    }

    /**
     * 验证贷款ID列表是否全部属于当前租户
     *
     * <p>两步验证：</p>
     * <ol>
     *   <li>检查ID是否存在（是否存在未查询到的ID）</li>
     *   <li>检查存在的ID是否属于当前租户</li>
     * </ol>
     * <p>验证失败时抛出具体的异常信息，便于定位问题</p>
     *
     * @param tenantId 租户ID
     * @param loanIds 待验证的贷款信息ID列表
     * @throws IllegalArgumentException 当存在不存在的ID或不属于当前租户的ID时抛出
     */
    @Override
    public void validateLoanIdsBelongToTenant(Long tenantId, List<Long> loanIds) {
        if (loanIds.isEmpty()) {
            return;
        }

        // 查询数据库中存在的贷款ID及其租户ID
        QueryWrapper<LoanInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id", "tenant_id");
        queryWrapper.in("id", loanIds);
        List<LoanInfo> loans = baseMapper.selectList(queryWrapper);

        // 检查是否存在未查询到的ID（不存在的ID）
        List<Long> existingIds = loans.stream().map(LoanInfo::getId).toList();
        List<Long> nonExistentIds = loanIds.stream()
                .filter(id -> !existingIds.contains(id))
                .toList();
        if (!nonExistentIds.isEmpty()) {
            throw new IllegalArgumentException("贷款ID不存在: " + nonExistentIds);
        }

        // 检查存在的ID是否属于当前租户
        List<Long> invalidIds = loans.stream()
                .filter(loan -> !loan.getTenantId().equals(tenantId))
                .map(LoanInfo::getId)
                .toList();
        if (!invalidIds.isEmpty()) {
            throw new IllegalArgumentException("无权限操作贷款ID: " + invalidIds);
        }
    }

    /**
     * 校验贷款状态与审批时间的联动关系
     *
     * <p>业务规则：</p>
     * <ul>
     *   <li>APPROVED状态：审批时间必须非空（表示已审批通过且有明确的审批时间）</li>
     *   <li>其他状态（APPLYING/REJECTED）：审批时间必须为空（表示尚未审批或已拒绝，无审批时间）</li>
     * </ul>
     *
     * @param loanStatus 贷款状态
     * @param approveTime 审批时间
     * @throws IllegalArgumentException 当状态与审批时间不匹配时抛出
     */
    private void validateStatusAndApproveTime(String loanStatus, LocalDateTime approveTime) {
        if ("APPROVED".equals(loanStatus)) {
            if (approveTime == null) {
                throw new IllegalArgumentException("审批通过状态必须填写审批时间");
            }
        } else {
            if (approveTime != null) {
                throw new IllegalArgumentException(loanStatus + "状态不允许填写审批时间");
            }
        }
    }
}
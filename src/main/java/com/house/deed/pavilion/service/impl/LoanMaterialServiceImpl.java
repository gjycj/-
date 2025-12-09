package com.house.deed.pavilion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.entity.LoanMaterial;
import com.house.deed.pavilion.mapper.LoanMaterialMapper;
import com.house.deed.pavilion.service.LoanMaterialService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 贷款材料提交记录服务实现类
 *
 * <p>实现贷款材料信息的增删改查及批量操作，所有方法均包含租户级数据隔离和严格的业务校验</p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Service
public class LoanMaterialServiceImpl extends ServiceImpl<LoanMaterialMapper, LoanMaterial> implements LoanMaterialService {

    /**
     * 合法的材料类型枚举集合
     * <p>与实体类allowableValues配置保持一致，包括：</p>
     * <ul>
     *   <li>INCOME_PROOF - 收入证明</li>
     *   <li>ID_CARD - 身份证</li>
     *   <li>HOUSE_PROPERTY - 房产证</li>
     *   <li>MARRIAGE_CERT - 结婚证</li>
     *   <li>BANK_FLOW - 银行流水</li>
     *   <li>OTHER - 其他材料</li>
     * </ul>
     */
    private static final Set<String> VALID_MATERIAL_TYPES = Set.of(
            "INCOME_PROOF", "ID_CARD", "HOUSE_PROPERTY", "MARRIAGE_CERT", "BANK_FLOW", "OTHER"
    );

    /**
     * 合法的材料状态枚举集合
     * <p>与实体类allowableValues配置保持一致，包括：</p>
     * <ul>
     *   <li>UNSUBMITTED - 未提交</li>
     *   <li>SUBMITTED - 已提交</li>
     *   <li>APPROVED - 已审批通过</li>
     *   <li>REJECTED - 已拒绝</li>
     * </ul>
     */
    private static final Set<String> VALID_STATUSES = Set.of(
            "UNSUBMITTED", "SUBMITTED", "APPROVED", "REJECTED"
    );

    /**
     * 新增贷款材料信息（带业务校验）
     *
     * <p>业务校验：</p>
     * <ul>
     *   <li>基础参数非空校验（租户ID、贷款ID）</li>
     *   <li>材料类型必须在合法枚举范围内</li>
     *   <li>材料状态必须在合法枚举范围内</li>
     *   <li>状态与相关字段联动校验（提交时间、材料URL、驳回原因）</li>
     *   <li>贷款ID合法性校验（需确保同租户下存在该贷款）</li>
     * </ul>
     * <p>技术实现：自动填充createTime（通过实体类的@TableField(fill = FieldFill.INSERT)配置）</p>
     *
     * @param loanMaterial 贷款材料实体对象，需包含租户ID、贷款ID、材料类型、状态等必填信息
     * @return 新增成功返回true，否则返回false
     * @throws IllegalArgumentException 当违反任何业务规则时抛出，包含具体的错误信息
     */
    @Override
    public boolean saveLoanMaterial(LoanMaterial loanMaterial) {
        // 1. 基础参数校验
        validateLoanMaterialBase(loanMaterial);

        // 2. 状态联动校验（提交状态需关联提交时间和材料URL）
        validateStatusRelatedFields(loanMaterial.getStatus(), loanMaterial.getSubmitTime(),
                loanMaterial.getMaterialUrl(), loanMaterial.getRejectReason());

        // 3. 校验贷款ID合法性（需确保同租户下存在该贷款）
        validateLoanIdExists(loanMaterial.getTenantId(), loanMaterial.getLoanId());

        // 4. 执行新增操作（createTime自动填充）
        return baseMapper.insert(loanMaterial) > 0;
    }

    /**
     * 更新贷款材料信息（带权限校验）
     *
     * <p>业务校验：</p>
     * <ul>
     *   <li>数据必须存在且属于当前租户</li>
     *   <li>禁止修改贷款ID和租户ID（核心关联字段不可变）</li>
     *   <li>若修改材料类型，需校验新值在合法枚举范围内</li>
     *   <li>若修改状态，需校验状态联动字段的合法性</li>
     * </ul>
     *
     * @param loanMaterial 贷款材料实体对象，需包含主键ID、租户ID及需要更新的字段
     * @return 更新成功返回true，否则返回false
     * @throws IllegalArgumentException 当数据不存在、无权限操作、违反业务规则时抛出
     */
    @Override
    public boolean updateLoanMaterialById(LoanMaterial loanMaterial) {
        // 1. 校验数据存在且属于当前租户
        LoanMaterial existMaterial = getLoanMaterialById(loanMaterial.getId(), loanMaterial.getTenantId());
        if (existMaterial == null) {
            throw new IllegalArgumentException("贷款材料不存在或无权限操作");
        }

        // 2. 禁止修改贷款ID和租户ID（核心关联字段不可变）
        if (!existMaterial.getLoanId().equals(loanMaterial.getLoanId())) {
            throw new IllegalArgumentException("贷款ID不可修改");
        }
        if (!existMaterial.getTenantId().equals(loanMaterial.getTenantId())) {
            throw new IllegalArgumentException("租户ID不可修改");
        }

        // 3. 若修改材料类型，需校验合法性
        if (loanMaterial.getMaterialType() != null &&
                !loanMaterial.getMaterialType().equals(existMaterial.getMaterialType())) {
            if (!VALID_MATERIAL_TYPES.contains(loanMaterial.getMaterialType())) {
                throw new IllegalArgumentException("无效材料类型：" + loanMaterial.getMaterialType());
            }
        }

        // 4. 若修改状态，需校验状态联动字段
        if (loanMaterial.getStatus() != null) {
            validateStatusRelatedFields(loanMaterial.getStatus(), loanMaterial.getSubmitTime(),
                    loanMaterial.getMaterialUrl(), loanMaterial.getRejectReason());
        }

        // 执行更新操作
        return baseMapper.updateById(loanMaterial) > 0;
    }

    /**
     * 根据ID物理删除贷款材料信息（带权限校验）
     *
     * <p>业务校验：数据必须存在且属于当前租户</p>
     *
     * @param id 贷款材料主键ID
     * @param tenantId 租户ID，用于数据隔离验证
     * @return 删除成功返回true，否则返回false
     * @throws IllegalArgumentException 当数据不存在或无权限操作时抛出
     */
    @Override
    public boolean removeLoanMaterialById(Long id, Long tenantId) {
        // 校验数据存在且属于当前租户
        LoanMaterial existMaterial = getLoanMaterialById(id, tenantId);
        if (existMaterial == null) {
            throw new IllegalArgumentException("贷款材料不存在或无权限操作");
        }
        return baseMapper.deleteById(id) > 0;
    }

    /**
     * 根据ID查询贷款材料详细信息（租户隔离）
     *
     * <p>查询时自动应用租户隔离条件，确保只能查询到当前租户的数据</p>
     *
     * @param id 贷款材料主键ID
     * @param tenantId 租户ID，用于数据隔离
     * @return 符合条件的贷款材料实体对象，未找到返回null
     */
    @Override
    public LoanMaterial getLoanMaterialById(Long id, Long tenantId) {
        return baseMapper.selectOne(new LambdaQueryWrapper<LoanMaterial>()
                .eq(LoanMaterial::getId, id)
                .eq(LoanMaterial::getTenantId, tenantId));
    }

    /**
     * 多条件分页查询贷款材料信息
     *
     * <p>支持以下查询条件：</p>
     * <ul>
     *   <li>贷款ID精确查询</li>
     *   <li>材料类型精确查询</li>
     *   <li>材料状态精确查询</li>
     *   <li>提交时间范围查询（示例：大于等于开始时间）</li>
     * </ul>
     * <p>排序规则：按提交时间降序（最新提交在前），未提交的排在最后（通过状态升序实现）</p>
     *
     * @param page 分页参数对象，包含页码和每页大小
     * @param queryParams 查询参数映射表，key为字段名，value为查询值
     * @param tenantId 租户ID，用于数据隔离
     * @return 分页查询结果，包含贷款材料列表和分页信息
     */
    @Override
    public IPage<LoanMaterial> pageQuery(Page<LoanMaterial> page, Map<String, Object> queryParams, Long tenantId) {
        LambdaQueryWrapper<LoanMaterial> queryWrapper = buildLambdaQueryWrapper(queryParams, tenantId);
        // 按提交时间降序（最新提交在前），未提交的排在最后（UNSUBMITTED状态值排最后）
        queryWrapper.orderByDesc(LoanMaterial::getSubmitTime)
                .orderByAsc(LoanMaterial::getStatus);
        return baseMapper.selectPage(page, queryWrapper);
    }

    /**
     * 多条件查询贷款材料列表（不分页）
     *
     * <p>查询条件与分页查询方法保持一致，但不进行分页处理</p>
     *
     * @param queryParams 查询参数映射表，key为字段名，value为查询值
     * @param tenantId 租户ID，用于数据隔离
     * @return 符合条件的贷款材料实体对象列表
     */
    @Override
    public List<LoanMaterial> listByConditions(Map<String, Object> queryParams, Long tenantId) {
        return baseMapper.selectList(buildLambdaQueryWrapper(queryParams, tenantId));
    }

    /**
     * 根据贷款ID查询材料列表
     *
     * <p>按材料类型升序排列，便于用户查看和整理</p>
     *
     * @param loanId 贷款信息主键ID
     * @param tenantId 租户ID，用于数据隔离
     * @return 符合条件的贷款材料实体对象列表，按材料类型排序
     */
    @Override
    public List<LoanMaterial> listByLoanId(Long loanId, Long tenantId) {
        return baseMapper.selectList(new LambdaQueryWrapper<LoanMaterial>()
                .eq(LoanMaterial::getLoanId, loanId)
                .eq(LoanMaterial::getTenantId, tenantId)
                .orderByAsc(LoanMaterial::getMaterialType));
    }

    /**
     * 批量新增贷款材料信息（事务保证）
     *
     * <p>在单个事务中执行批量新增，任一记录校验失败或保存失败将导致整个操作回滚</p>
     * <p>批量校验包括：</p>
     * <ul>
     *   <li>统一租户ID校验（批量操作必须属于同一租户）</li>
     *   <li>基础参数校验（非空、枚举值等）</li>
     *   <li>状态联动字段校验</li>
     *   <li>贷款ID合法性校验</li>
     * </ul>
     *
     * @param loanMaterials 贷款材料实体对象列表
     * @return 批量新增成功返回true，否则返回false
     * @throws IllegalArgumentException 当违反任何业务规则时抛出，包含具体的错误信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchSaveLoanMaterials(List<LoanMaterial> loanMaterials) {
        if (CollectionUtils.isEmpty(loanMaterials)) {
            return false;
        }

        // 1. 统一校验租户ID（批量操作必须属于同一租户）
        Long tenantId = loanMaterials.get(0).getTenantId();
        if (loanMaterials.stream().anyMatch(m -> !m.getTenantId().equals(tenantId))) {
            throw new IllegalArgumentException("批量操作的材料必须属于同一租户");
        }

        // 2. 逐条校验（复用单条新增的校验逻辑）
        for (LoanMaterial material : loanMaterials) {
            validateLoanMaterialBase(material);
            validateStatusRelatedFields(material.getStatus(), material.getSubmitTime(),
                    material.getMaterialUrl(), material.getRejectReason());
            validateLoanIdExists(tenantId, material.getLoanId());
        }

        // 执行批量保存（事务保证）
        return saveBatch(loanMaterials);
    }

    /**
     * 批量更新材料状态（事务保证）
     *
     * <p>在单个事务中执行批量状态更新，主要用于批量审批或驳回操作</p>
     * <p>状态联动处理：</p>
     * <ul>
     *   <li>SUBMITTED状态：自动填充提交时间为当前时间</li>
     *   <li>REJECTED状态：要求必须填写驳回原因（由调用方在参数中设置）</li>
     * </ul>
     *
     * @param ids 待更新的贷款材料ID列表
     * @param status 目标状态值，必须在合法枚举范围内
     * @param tenantId 租户ID，用于数据隔离验证
     * @return 批量更新成功返回true，否则返回false
     * @throws IllegalArgumentException 当状态值非法、无权限操作或REJECTED状态未填写驳回原因时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchUpdateStatus(List<Long> ids, String status, Long tenantId) {
        if (CollectionUtils.isEmpty(ids) || !VALID_STATUSES.contains(status)) {
            return false;
        }

        // 1. 校验所有ID属于当前租户
        validateMaterialIdsBelongToTenant(tenantId, ids);

        // 2. 状态联动字段处理
        LoanMaterial updateEntity = new LoanMaterial();
        updateEntity.setStatus(status);
        if ("SUBMITTED".equals(status)) {
            updateEntity.setSubmitTime(LocalDateTime.now()); // 提交状态自动填充当前时间
        } else if ("REJECTED".equals(status) && updateEntity.getRejectReason() == null) {
            throw new IllegalArgumentException("驳回状态必须填写驳回原因");
        }

        // 3. 执行批量更新（仅更新指定字段）
        return baseMapper.update(updateEntity, new LambdaQueryWrapper<LoanMaterial>()
                .in(LoanMaterial::getId, ids)
                .eq(LoanMaterial::getTenantId, tenantId)) > 0;
    }

    /**
     * 批量删除贷款材料信息（事务保证）
     *
     * <p>在单个事务中执行批量删除，任一记录校验失败或删除失败将导致整个操作回滚</p>
     * <p>先验证所有ID都属于当前租户，然后执行批量删除</p>
     *
     * @param ids 待删除的贷款材料ID列表
     * @param tenantId 租户ID，用于数据隔离验证
     * @return 批量删除成功返回true，否则返回false
     * @throws IllegalArgumentException 当存在不属于当前租户的材料ID时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchRemoveLoanMaterials(List<Long> ids, Long tenantId) {
        if (CollectionUtils.isEmpty(ids)) {
            return false;
        }

        // 1. 校验所有ID属于当前租户
        validateMaterialIdsBelongToTenant(tenantId, ids);

        // 2. 批量删除（事务保证）
        return baseMapper.deleteBatchIds(ids) > 0;
    }

    /**
     * 验证材料ID列表是否全部属于当前租户
     *
     * <p>两步验证：</p>
     * <ol>
     *   <li>检查ID是否存在（是否存在未查询到的ID）</li>
     *   <li>检查存在的ID是否属于当前租户</li>
     * </ol>
     * <p>验证失败时抛出具体的异常信息，便于定位问题</p>
     *
     * @param tenantId 租户ID
     * @param materialIds 待验证的贷款材料ID列表
     * @throws IllegalArgumentException 当存在不存在的ID或不属于当前租户的ID时抛出
     */
    @Override
    public void validateMaterialIdsBelongToTenant(Long tenantId, List<Long> materialIds) {
        if (CollectionUtils.isEmpty(materialIds)) {
            return;
        }

        // 查询数据库中存在的材料ID及其租户ID
        List<LoanMaterial> materials = baseMapper.selectList(new LambdaQueryWrapper<LoanMaterial>()
                .select(LoanMaterial::getId, LoanMaterial::getTenantId)
                .in(LoanMaterial::getId, materialIds));

        // 检查是否存在未查询到的ID（不存在的ID）
        Set<Long> existingIds = materials.stream().map(LoanMaterial::getId).collect(Collectors.toSet());
        List<Long> nonExistentIds = materialIds.stream()
                .filter(id -> !existingIds.contains(id))
                .toList();
        if (!nonExistentIds.isEmpty()) {
            throw new IllegalArgumentException("材料ID不存在: " + nonExistentIds);
        }

        // 检查存在的ID是否属于当前租户
        List<Long> invalidIds = materials.stream()
                .filter(m -> !m.getTenantId().equals(tenantId))
                .map(LoanMaterial::getId)
                .toList();
        if (!invalidIds.isEmpty()) {
            throw new IllegalArgumentException("无权限操作材料ID: " + invalidIds);
        }
    }


    // ==================== 私有工具方法 ====================
    /**
     * 构建Lambda查询条件（类型安全）
     *
     * <p>根据查询参数动态构建查询条件，支持以下参数：</p>
     * <ul>
     *   <li>loanId - 贷款ID精确查询</li>
     *   <li>materialType - 材料类型精确查询</li>
     *   <li>status - 材料状态精确查询</li>
     *   <li>submitTimeStart - 提交时间起始范围</li>
     * </ul>
     * <p>所有查询均自动添加租户隔离条件</p>
     *
     * @param queryParams 查询参数映射表
     * @param tenantId 租户ID，用于数据隔离
     * @return 构建完成的LambdaQueryWrapper对象
     */
    private LambdaQueryWrapper<LoanMaterial> buildLambdaQueryWrapper(Map<String, Object> queryParams, Long tenantId) {
        LambdaQueryWrapper<LoanMaterial> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(LoanMaterial::getTenantId, tenantId);

        if (queryParams != null) {
            // 贷款ID精确查询
            if (queryParams.containsKey("loanId") && queryParams.get("loanId") != null) {
                queryWrapper.eq(LoanMaterial::getLoanId, queryParams.get("loanId"));
            }
            // 材料类型精确查询
            if (queryParams.containsKey("materialType") && queryParams.get("materialType") != null) {
                queryWrapper.eq(LoanMaterial::getMaterialType, queryParams.get("materialType"));
            }
            // 材料状态精确查询
            if (queryParams.containsKey("status") && queryParams.get("status") != null) {
                queryWrapper.eq(LoanMaterial::getStatus, queryParams.get("status"));
            }
            // 提交时间范围查询（大于等于开始时间）
            if (queryParams.containsKey("submitTimeStart") && queryParams.get("submitTimeStart") != null) {
                queryWrapper.ge(LoanMaterial::getSubmitTime, queryParams.get("submitTimeStart"));
            }
        }
        return queryWrapper;
    }

    /**
     * 基础参数校验（非空、枚举值等）
     *
     * <p>校验内容：</p>
     * <ul>
     *   <li>租户ID和贷款ID非空校验</li>
     *   <li>材料类型合法性校验（必须在VALID_MATERIAL_TYPES集合中）</li>
     *   <li>材料状态合法性校验（必须在VALID_STATUSES集合中）</li>
     * </ul>
     *
     * @param material 待校验的贷款材料实体对象
     * @throws IllegalArgumentException 当任何校验失败时抛出，包含具体的错误信息
     */
    private void validateLoanMaterialBase(LoanMaterial material) {
        // 租户ID和贷款ID非空校验
        if (material.getTenantId() == null) {
            throw new IllegalArgumentException("租户ID不能为空");
        }
        if (material.getLoanId() == null) {
            throw new IllegalArgumentException("贷款ID不能为空");
        }

        // 材料类型校验
        if (material.getMaterialType() == null || !VALID_MATERIAL_TYPES.contains(material.getMaterialType())) {
            throw new IllegalArgumentException("无效材料类型：" + material.getMaterialType() +
                    "，允许值：INCOME_PROOF/ID_CARD/HOUSE_PROPERTY/MARRIAGE_CERT/BANK_FLOW/OTHER");
        }

        // 状态校验
        if (material.getStatus() == null || !VALID_STATUSES.contains(material.getStatus())) {
            throw new IllegalArgumentException("无效材料状态：" + material.getStatus() +
                    "，允许值：UNSUBMITTED/SUBMITTED/APPROVED/REJECTED");
        }
    }

    /**
     * 状态关联字段校验
     *
     * <p>业务规则：</p>
     * <ul>
     *   <li>UNSUBMITTED状态：提交时间、材料URL、驳回原因必须为空</li>
     *   <li>SUBMITTED/APPROVED状态：提交时间和材料URL必须非空，驳回原因必须为空</li>
     *   <li>REJECTED状态：提交时间、材料URL、驳回原因必须非空</li>
     * </ul>
     *
     * @param status 材料状态
     * @param submitTime 提交时间
     * @param materialUrl 材料URL
     * @param rejectReason 驳回原因
     * @throws IllegalArgumentException 当状态与相关字段不匹配时抛出
     */
    private void validateStatusRelatedFields(String status, LocalDateTime submitTime,
                                             String materialUrl, String rejectReason) {
        switch (status) {
            case "UNSUBMITTED":
                // 未提交状态：提交时间、材料URL、驳回原因必须为空
                if (submitTime != null) {
                    throw new IllegalArgumentException("未提交状态不允许设置提交时间");
                }
                if (materialUrl != null) {
                    throw new IllegalArgumentException("未提交状态不允许上传材料");
                }
                if (rejectReason != null) {
                    throw new IllegalArgumentException("未提交状态不允许设置驳回原因");
                }
                break;
            case "SUBMITTED":
            case "APPROVED":
                // 已提交/审核通过：提交时间和材料URL必须非空，驳回原因必须为空
                if (submitTime == null) {
                    throw new IllegalArgumentException(status + "状态必须设置提交时间");
                }
                if (materialUrl == null || materialUrl.trim().isEmpty()) {
                    throw new IllegalArgumentException(status + "状态必须上传材料URL");
                }
                if (rejectReason != null) {
                    throw new IllegalArgumentException(status + "状态不允许设置驳回原因");
                }
                break;
            case "REJECTED":
                // 审核驳回：提交时间、材料URL、驳回原因必须非空
                if (submitTime == null) {
                    throw new IllegalArgumentException("驳回状态必须设置提交时间");
                }
                if (materialUrl == null || materialUrl.trim().isEmpty()) {
                    throw new IllegalArgumentException("驳回状态必须上传材料URL");
                }
                if (rejectReason == null || rejectReason.trim().isEmpty()) {
                    throw new IllegalArgumentException("驳回状态必须填写驳回原因");
                }
                break;
            default:
                throw new IllegalArgumentException("未知材料状态：" + status);
        }
    }

    /**
     * 校验贷款ID是否存在于当前租户
     *
     * <p>示例方法，实际业务中需注入LoanInfoService进行关联查询</p>
     * <p>实现逻辑：调用LoanInfoService的getLoanInfoById方法，验证贷款是否存在且属于当前租户</p>
     *
     * @param tenantId 租户ID
     * @param loanId 贷款ID
     * @throws IllegalArgumentException 当贷款ID不存在或不属于当前租户时抛出
     * @apiNote 此方法当前为示例实现，需根据实际业务进行完善
     */
    private void validateLoanIdExists(Long tenantId, Long loanId) {
        // 实际实现需注入LoanInfoService，示例逻辑：
        // if (loanInfoService.getLoanInfoById(loanId, tenantId) == null) {
        //     throw new IllegalArgumentException("贷款ID不存在或不属于当前租户：" + loanId);
        // }
    }
}
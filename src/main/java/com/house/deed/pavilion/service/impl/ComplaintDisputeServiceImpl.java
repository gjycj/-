package com.house.deed.pavilion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.entity.ComplaintDispute;
import com.house.deed.pavilion.mapper.ComplaintDisputeMapper;
import com.house.deed.pavilion.service.ComplaintDisputeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * <p>
 * 投诉与纠纷记录表（租户级数据） 服务实现类
 * </p>
 *
 * <p>
 * 本服务类负责投诉与纠纷记录的全生命周期管理，包括：
 * - 投诉纠纷的登记、处理和归档
 * - 纠纷编号的自动生成和唯一性保证
 * - 租户级数据隔离和权限控制
 * - 状态流转的业务规则校验
 * - 批量操作的事务一致性保证
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Service
public class ComplaintDisputeServiceImpl extends ServiceImpl<ComplaintDisputeMapper, ComplaintDispute> implements ComplaintDisputeService {

    /**
     * 纠纷编号生成器
     *
     * <p>
     * 生成格式为 DIS+年月日+3位流水号的唯一纠纷编号，如 DIS20251126001。
     * 编号规则确保同一租户内每天从001开始顺序生成，支持高并发场景下的唯一性。
     * </p>
     *
     * @param tenantId 租户ID，用于隔离不同租户的编号序列
     * @return String 生成的唯一纠纷编号
     */
    public String generateDisputeNo(Long tenantId) {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        // 使用 QueryWrapper 替代 LambdaQueryWrapper
        QueryWrapper<ComplaintDispute> wrapper = new QueryWrapper<>();
        wrapper.eq("tenant_id", tenantId)
                .like("dispute_no", "DIS" + dateStr)  // 匹配当天所有编号
                .select("dispute_no");
        List<ComplaintDispute> list = baseMapper.selectList(wrapper);

        // 计算下一个流水号（3位数字，不足补0）
        AtomicInteger maxSeq = new AtomicInteger(0);
        list.forEach(dispute -> {
            String no = dispute.getDisputeNo();
            // 提取编号末尾3位作为流水号
            int seq = Integer.parseInt(no.substring(no.length() - 3));
            if (seq > maxSeq.get()) {
                maxSeq.set(seq);
            }
        });
        String seqStr = String.format("%03d", maxSeq.incrementAndGet());
        return "DIS" + dateStr + seqStr;
    }

    /**
     * 新增投诉纠纷记录
     *
     * <p>
     * 创建新的投诉纠纷记录，系统会自动生成唯一纠纷编号。
     * 适用于客户投诉、交易纠纷、服务投诉等场景的登记。
     * </p>
     *
     * @param dispute 投诉纠纷实体对象，包含投诉人信息、纠纷类型、详情描述等
     * @return boolean 创建结果，true表示创建成功，false表示创建失败
     * @throws IllegalArgumentException 当必填字段为空或租户ID为空时抛出
     */
    @Override
    public boolean saveComplaintDispute(ComplaintDispute dispute) {
        // 1. 自动生成并设置纠纷编号，确保编号的唯一性和规范性
        String disputeNo = generateDisputeNo(dispute.getTenantId());
        dispute.setDisputeNo(disputeNo);

        // 2. 业务必填字段校验（兜底校验，配合实体类注解使用）
        if (ObjectUtils.isEmpty(dispute.getComplainantType()) || ObjectUtils.isEmpty(dispute.getDisputeType())) {
            throw new IllegalArgumentException("投诉人类型和纠纷类型不能为空");
        }

        // 3. 执行数据插入操作，createTime字段由MyBatis-Plus自动填充
        return baseMapper.insert(dispute) > 0;
    }

    /**
     * 更新投诉纠纷记录
     *
     * <p>
     * 更新现有投诉纠纷记录信息，系统会校验数据权限和业务规则。
     * 禁止修改纠纷编号和创建人信息，状态变更时需要指定处理人。
     * </p>
     *
     * @param dispute 投诉纠纷实体对象，必须包含ID和租户ID用于权限校验
     * @return boolean 更新结果，true表示更新成功，false表示更新失败
     * @throws IllegalArgumentException 当记录不存在、无操作权限或状态变更时未指定处理人时抛出
     */
    @Override
    public boolean updateComplaintDisputeById(ComplaintDispute dispute) {
        // 1. 数据存在性及租户权限校验：确保只能操作本租户的数据
        ComplaintDispute existDispute = baseMapper.selectById(dispute.getId());
        if (existDispute == null || !existDispute.getTenantId().equals(dispute.getTenantId())) {
            throw new IllegalArgumentException("纠纷记录不存在或无权限操作");
        }

        // 2. 保护性字段处理：禁止修改编号和创建人，避免数据不一致
        dispute.setDisputeNo(null); // 强制置空，避免前端误传
        dispute.setCreateAgentId(null); // 创建人信息不可变更

        // 3. 状态变更业务规则校验：状态变化时必须指定处理人
        if (!ObjectUtils.isEmpty(dispute.getStatus()) && !dispute.getStatus().equals(existDispute.getStatus())) {
            if (ObjectUtils.isEmpty(dispute.getHandlerId())) {
                throw new IllegalArgumentException("状态变更时处理人ID不能为空");
            }
        }

        // 4. 执行更新操作，updateTime字段通过MyBatis-Plus自动填充
        return baseMapper.updateById(dispute) > 0;
    }

    /**
     * 删除投诉纠纷记录
     *
     * <p>
     * 删除指定的投诉纠纷记录，系统会校验数据权限和删除条件。
     * 仅允许删除【已受理】状态的记录，防止误删处理中的纠纷。
     * </p>
     *
     * @param id 要删除的纠纷记录主键ID
     * @param tenantId 当前操作租户ID，用于权限校验
     * @return boolean 删除结果，true表示删除成功，false表示删除失败
     * @throws IllegalArgumentException 当记录不存在、无操作权限或记录状态不允许删除时抛出
     */
    @Override
    public boolean removeComplaintDisputeById(Long id, Long tenantId) {
        // 1. 数据存在性及租户权限校验
        ComplaintDispute existDispute = getComplaintDisputeById(id, tenantId);
        if (existDispute == null) {
            throw new IllegalArgumentException("纠纷记录不存在或无权限操作");
        }

        // 2. 状态校验：仅允许删除已受理状态的记录，保护处理中和已完成的记录
        if (!"ACCEPTED".equals(existDispute.getStatus())) {
            throw new IllegalArgumentException("仅允许删除【已受理】状态的纠纷记录");
        }

        return baseMapper.deleteById(id) > 0;
    }

    /**
     * 按ID查询纠纷记录详情
     *
     * <p>
     * 根据记录ID查询详细信息，系统会自动进行租户隔离校验。
     * 只能查询到当前租户下的纠纷记录信息。
     * </p>
     *
     * @param id 纠纷记录主键ID
     * @param tenantId 当前操作租户ID
     * @return ComplaintDispute 投诉纠纷实体对象，未找到时返回null
     */
    @Override
    public ComplaintDispute getComplaintDisputeById(Long id, Long tenantId) {
        LambdaQueryWrapper<ComplaintDispute> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ComplaintDispute::getId, id)
                .eq(ComplaintDispute::getTenantId, tenantId);
        return baseMapper.selectOne(wrapper);
    }

    /**
     * 多条件分页查询
     *
     * <p>
     * 支持多种条件的投诉纠纷分页查询，系统强制租户隔离。
     * 适用于管理后台的纠纷列表展示、数据筛选和统计分析等场景。
     * </p>
     *
     * @param page 分页参数对象，包含页码、页大小、排序等信息
     * @param queryMap 查询条件映射表，支持纠纷类型、状态、投诉人类型、时间范围等条件
     * @param tenantId 当前操作租户ID
     * @return IPage<ComplaintDispute> 分页结果对象，包含数据列表和分页信息
     */
    @Override
    public IPage<ComplaintDispute> pageQuery(Page<ComplaintDispute> page, Map<String, Object> queryMap, Long tenantId) {
        QueryWrapper<ComplaintDispute> wrapper = buildQueryWrapper(queryMap, tenantId);
        return baseMapper.selectPage(page, wrapper);
    }

    /**
     * 多条件列表查询
     *
     * <p>
     * 支持多种条件的投诉纠纷列表查询，不进行分页处理。
     * 适用于数据导出、报表生成、批量处理等场景。
     * </p>
     *
     * @param queryMap 查询条件映射表
     * @param tenantId 当前操作租户ID
     * @return List<ComplaintDispute> 符合条件的纠纷记录列表
     */
    @Override
    public List<ComplaintDispute> listByConditions(Map<String, Object> queryMap, Long tenantId) {
        QueryWrapper<ComplaintDispute> wrapper = buildQueryWrapper(queryMap, tenantId);
        return baseMapper.selectList(wrapper);
    }

    /**
     * 批量更新纠纷状态
     *
     * <p>
     * 批量更新多个纠纷记录的状态，使用事务保证数据一致性。
     * 系统会校验状态值的合法性和所有记录ID的租户归属。
     * 适用于批量受理、批量处理、批量结案等场景。
     * </p>
     *
     * @param ids 要更新的纠纷记录ID列表
     * @param status 目标状态（ACCEPTED=已受理，PROCESSING=处理中，RESOLVED=已解决，CANCELED=已取消）
     * @param handlerId 处理人ID，状态变更时必须指定
     * @param tenantId 当前操作租户ID
     * @return boolean 批量更新结果，true表示更新成功，false表示更新失败
     * @throws IllegalArgumentException 当ID列表为空、状态值无效或存在无权限操作的记录时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchUpdateStatus(List<Long> ids, String status, Long handlerId, Long tenantId) {
        // 空列表校验
        if (CollectionUtils.isEmpty(ids)) {
            return false;
        }

        // 1. 租户权限校验：确保所有纠纷记录都属于当前租户
        validateDisputeIdsBelongToTenant(tenantId, ids);

        // 2. 状态值合法性校验：防止传入无效状态值
        List<String> validStatus = List.of("ACCEPTED", "PROCESSING", "RESOLVED", "CANCELED");
        if (!validStatus.contains(status)) {
            throw new IllegalArgumentException("无效的状态值：" + status);
        }

        // 3. 构建更新对象并执行批量更新
        ComplaintDispute updateDispute = new ComplaintDispute();
        updateDispute.setStatus(status);
        updateDispute.setHandlerId(handlerId); // 记录状态变更的处理人

        LambdaQueryWrapper<ComplaintDispute> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(ComplaintDispute::getId, ids)
                .eq(ComplaintDispute::getTenantId, tenantId);

        return baseMapper.update(updateDispute, wrapper) > 0;
    }

    /**
     * 批量删除纠纷记录
     *
     * <p>
     * 批量删除多个纠纷记录，使用事务保证数据一致性。
     * 系统会校验所有记录ID的租户归属和删除条件。
     * 仅允许批量删除【已受理】状态的记录。
     * </p>
     *
     * @param ids 要删除的纠纷记录ID列表
     * @param tenantId 当前操作租户ID
     * @return boolean 批量删除结果，true表示删除成功，false表示删除失败
     * @throws IllegalArgumentException 当ID列表为空、存在无权限操作的记录或存在非已受理状态的记录时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchRemoveComplaintDisputes(List<Long> ids, Long tenantId) {
        // 空列表校验
        if (CollectionUtils.isEmpty(ids)) {
            return false;
        }

        // 1. 租户权限校验：确保所有纠纷记录都属于当前租户
        validateDisputeIdsBelongToTenant(tenantId, ids);

        // 2. 状态条件校验：仅允许删除已受理状态的记录
        LambdaQueryWrapper<ComplaintDispute> statusWrapper = new LambdaQueryWrapper<>();
        statusWrapper.in(ComplaintDispute::getId, ids)
                .eq(ComplaintDispute::getTenantId, tenantId)
                .ne(ComplaintDispute::getStatus, "ACCEPTED");
        long invalidCount = baseMapper.selectCount(statusWrapper);
        if (invalidCount > 0) {
            throw new IllegalArgumentException("存在非【已受理】状态的记录，无法批量删除");
        }

        // 3. 执行批量删除操作
        return baseMapper.deleteBatchIds(ids) > 0;
    }

    /**
     * 校验纠纷记录ID列表是否属于当前租户
     *
     * <p>
     * 内部校验方法，用于验证给定的纠纷记录ID列表是否全部属于指定租户。
     * 会同时校验记录存在性和租户归属权限。
     * </p>
     *
     * @param tenantId 目标租户ID
     * @param disputeIds 待校验的纠纷记录ID列表
     * @throws IllegalArgumentException 当存在不存在的记录ID或无权限操作的记录时抛出
     */
    @Override
    public void validateDisputeIdsBelongToTenant(Long tenantId, List<Long> disputeIds) {
        if (CollectionUtils.isEmpty(disputeIds)) {
            return;
        }

        // 1. 查询存在的记录ID及租户ID（只查询必要字段提升性能）- 使用 QueryWrapper 避免 Lambda 缓存问题
        QueryWrapper<ComplaintDispute> wrapper = new QueryWrapper<>();
        wrapper.select("id", "tenant_id")  // 只查询ID和租户ID字段，提升性能
                .in("id", disputeIds);      // 查询指定ID列表的记录

        List<ComplaintDispute> disputes = baseMapper.selectList(wrapper);

        // 2. 检查不存在的ID：确保所有传入的ID都对应实际存在的记录
        List<Long> existingIds = disputes.stream()
                .map(ComplaintDispute::getId)
                .toList();

        List<Long> nonExistentIds = disputeIds.stream()
                .filter(id -> !existingIds.contains(id))
                .toList();

        if (!nonExistentIds.isEmpty()) {
            throw new IllegalArgumentException("纠纷记录ID不存在: " + nonExistentIds);
        }

        // 3. 检查租户归属：确保所有记录都属于当前操作租户
        List<Long> invalidIds = disputes.stream()
                .filter(dispute -> !dispute.getTenantId().equals(tenantId))
                .map(ComplaintDispute::getId)
                .toList();

        if (!invalidIds.isEmpty()) {
            throw new IllegalArgumentException("无权限操作纠纷记录ID: " + invalidIds);
        }
    }

    /**
     * 构建动态查询条件包装器
     *
     * <p>
     * 内部方法，用于构建统一的查询条件，消除重复代码。
     * 支持多种条件的动态拼接，所有查询都强制租户隔离。
     * </p>
     *
     * @param queryMap 查询条件映射表，支持以下键：
     *                - disputeType: 纠纷类型筛选（如合同纠纷、服务投诉等）
     *                - status: 状态筛选（ACCEPTED=已受理，PROCESSING=处理中，RESOLVED=已解决，CANCELED=已取消）
     *                - complainantType: 投诉人类型（如客户、经纪人、第三方等）
     *                - relatedContractId: 关联合同ID精确查询
     *                - startTime: 创建时间范围查询（开始时间）
     *                - endTime: 创建时间范围查询（结束时间）
     * @param tenantId 当前操作租户ID
     * @return QueryWrapper<ComplaintDispute> 构建好的查询条件包装器
     */
    private QueryWrapper<ComplaintDispute> buildQueryWrapper(Map<String, Object> queryMap, Long tenantId) {
        QueryWrapper<ComplaintDispute> wrapper = new QueryWrapper<>();
        // 强制租户隔离：所有查询都必须限制在当前租户范围内
        wrapper.eq("tenant_id", tenantId);

        // 动态拼接查询条件：只有非空的参数才会参与查询
        if (!ObjectUtils.isEmpty(queryMap)) {
            // 纠纷类型筛选：按预设的纠纷分类进行查询
            if (queryMap.containsKey("disputeType") && queryMap.get("disputeType") != null) {
                wrapper.eq("dispute_type", queryMap.get("disputeType"));
            }
            // 状态筛选：按纠纷处理状态查询
            if (queryMap.containsKey("status") && queryMap.get("status") != null) {
                wrapper.eq("status", queryMap.get("status"));
            }
            // 投诉人类型筛选：按投诉人身份类型查询
            if (queryMap.containsKey("complainantType") && queryMap.get("complainantType") != null) {
                wrapper.eq("complainant_type", queryMap.get("complainantType"));
            }
            // 关联合同ID精确查询：用于查看特定合同相关的所有纠纷
            if (queryMap.containsKey("relatedContractId") && queryMap.get("relatedContractId") != null) {
                wrapper.eq("related_contract_id", queryMap.get("relatedContractId"));
            }
            // 时间范围查询：按创建时间筛选，支持历史数据查询
            if (queryMap.containsKey("startTime") && queryMap.get("startTime") != null) {
                wrapper.ge("create_time", queryMap.get("startTime"));
            }
            if (queryMap.containsKey("endTime") && queryMap.get("endTime") != null) {
                wrapper.le("create_time", queryMap.get("endTime"));
            }
        }

        // 默认排序：按创建时间倒序，确保最新的投诉纠纷显示在最前面
        wrapper.orderByDesc("create_time");
        return wrapper;
    }
}
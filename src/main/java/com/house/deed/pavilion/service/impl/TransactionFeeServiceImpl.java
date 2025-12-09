package com.house.deed.pavilion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.entity.TransactionFee;
import com.house.deed.pavilion.mapper.TransactionFeeMapper;
import com.house.deed.pavilion.service.TransactionFeeService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 交易费用明细表（租户级数据）服务实现类
 *
 * <p>管理房产交易过程中产生的各类费用明细，包括中介费、税费、定金等服务费。
 * 支持多维度查询、批量操作和状态管理，为财务管理和交易结算提供支持。</p>
 *
 * <p><b>核心业务特性：</b></p>
 * <ul>
 *   <li><b>租户数据隔离</b>：所有操作均需指定租户ID，确保数据安全隔离</li>
 *   <li><b>费用类型管理</b>：支持中介费、税费、定金等多种费用类型</li>
 *   <li><b>支付状态跟踪</b>：提供完整的支付状态流转管理</li>
 *   <li><b>批量操作优化</b>：支持批量增删改查，提高处理效率</li>
 *   <li><b>数据完整性校验</b>：严格的业务规则和字段校验</li>
 * </ul>
 *
 * <p><b>支付状态流转：</b></p>
 * <pre>
 * UNPAID → PARTIALLY_PAID → PAID
 *      ↘ REFUNDED
 * </pre>
 *
 * @author yuquanxi
 * @since 2025-11-26
 * @version 1.0.0
 */
@Slf4j
@Service
public class TransactionFeeServiceImpl extends ServiceImpl<TransactionFeeMapper, TransactionFee> implements TransactionFeeService {

    /**
     * 有效的支付状态枚举值
     * <p>用于支付状态的合法性校验，确保状态值的规范性和一致性</p>
     * <ul>
     *   <li><b>UNPAID</b>：未支付，初始状态</li>
     *   <li><b>PARTIALLY_PAID</b>：部分支付，已支付部分金额</li>
     *   <li><b>PAID</b>：已支付，费用已全额支付</li>
     *   <li><b>REFUNDED</b>：已退款，费用已全额或部分退款</li>
     * </ul>
     */
    private static final List<String> VALID_PAYMENT_STATUSES = List.of("UNPAID", "PARTIALLY_PAID", "PAID", "REFUNDED");

    /**
     * 有效的费用类型枚举值
     * <p>定义系统中支持的所有费用类型，用于费用分类管理</p>
     * <ul>
     *   <li><b>AGENCY_FEE</b>：中介服务费</li>
     *   <li><b>TAX</b>：相关税费</li>
     *   <li><b>DEPOSIT</b>：定金/保证金</li>
     *   <li><b>SERVICE_FEE</b>：服务费</li>
     *   <li><b>OTHER</b>：其他费用</li>
     * </ul>
     */
    private static final List<String> VALID_FEE_TYPES = List.of("AGENCY_FEE", "TAX", "DEPOSIT", "SERVICE_FEE", "OTHER");

    /**
     * 有效的支付方枚举值
     * <p>定义费用支付的责任方，用于费用分摊和结算</p>
     * <ul>
     *   <li><b>CUSTOMER</b>：客户/买方承担</li>
     *   <li><b>LANDLORD</b>：房东/卖方承担</li>
     *   <li><b>SHARED</b>：双方共同承担</li>
     * </ul>
     */
    private static final List<String> VALID_PAYERS = List.of("CUSTOMER", "LANDLORD", "SHARED");

    /**
     * 分页查询交易费用明细
     * <p>支持多条件组合查询并返回分页结果，适用于管理后台列表展示和财务对账</p>
     *
     * @param page 分页参数对象，包含页码、每页大小等分页信息
     * @param queryParams 查询条件参数映射，支持合同ID、费用类型、支付方等多维度筛选
     * @param tenantId 租户ID，用于数据隔离，不能为null
     * @return IPage<TransactionFee> 分页查询结果，包含数据列表和分页统计信息
     * @throws IllegalArgumentException 当租户ID为空时抛出异常
     *
     * <p><b>支持的条件参数：</b></p>
     * <table border="1">
     *   <tr><th>参数键</th><th>类型</th><th>说明</th></tr>
     *   <tr><td>contractId</td><td>Long</td><td>合同ID，精确匹配</td></tr>
     *   <tr><td>feeType</td><td>String</td><td>费用类型，精确匹配（见VALID_FEE_TYPES）</td></tr>
     *   <tr><td>payer</td><td>String</td><td>支付方，精确匹配（见VALID_PAYERS）</td></tr>
     *   <tr><td>paymentStatus</td><td>String</td><td>支付状态，精确匹配（见VALID_PAYMENT_STATUSES）</td></tr>
     *   <tr><td>startTime</td><td>LocalDateTime</td><td>创建时间起始范围</td></tr>
     *   <tr><td>endTime</td><td>LocalDateTime</td><td>创建时间结束范围</td></tr>
     *   <tr><td>minAmount</td><td>BigDecimal</td><td>费用金额最小值</td></tr>
     *   <tr><td>maxAmount</td><td>BigDecimal</td><td>费用金额最大值</td></tr>
     * </table>
     *
     * <p><b>排序规则：</b>默认按创建时间倒序排列（最新记录在前）</p>
     */
    @Override
    public IPage<TransactionFee> pageQuery(Page<TransactionFee> page, Map<String, Object> queryParams, Long tenantId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("租户ID不能为空");
        }

        LambdaQueryWrapper<TransactionFee> queryWrapper = buildQueryWrapper(queryParams, tenantId);
        return baseMapper.selectPage(page, queryWrapper);
    }

    /**
     * 条件查询交易费用明细列表
     * <p>支持多条件组合查询返回完整列表，适用于数据导出和批量处理</p>
     *
     * @param queryParams 查询条件参数映射，支持合同ID、费用类型、支付方等多维度筛选
     * @param tenantId 租户ID，用于数据隔离，不能为null
     * @return List<TransactionFee> 满足条件的费用明细列表
     * @throws IllegalArgumentException 当租户ID为空时抛出异常
     *
     * <p><b>使用场景：</b></p>
     * <ul>
     *   <li>数据导出和报表生成</li>
     *   <li>批量处理和计算</li>
     *   <li>数据统计和分析</li>
     * </ul>
     */
    @Override
    public List<TransactionFee> listByConditions(Map<String, Object> queryParams, Long tenantId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("租户ID不能为空");
        }

        LambdaQueryWrapper<TransactionFee> queryWrapper = buildQueryWrapper(queryParams, tenantId);
        return baseMapper.selectList(queryWrapper);
    }

    /**
     * 批量保存交易费用明细
     * <p>批量创建交易费用记录，操作前会进行严格的数据校验和租户一致性检查</p>
     *
     * @param transactionFees 待保存的交易费用实体列表，不能为null或空
     * @return boolean 批量操作结果，true-成功 false-失败
     * @throws IllegalArgumentException 当参数非法、数据校验失败或租户不一致时抛出异常
     *
     * <p><b>批量操作要求：</b></p>
     * <ul>
     *   <li>列表不能为空</li>
     *   <li>所有记录必须属于同一租户</li>
     *   <li>每条记录必须通过数据有效性校验</li>
     * </ul>
     *
     * <p><b>事务保证：</b>采用原子操作，任一记录保存失败则全部回滚</p>
     *
     * <p><b>执行流程：</b></p>
     * <ol>
     *   <li>参数非空检查</li>
     *   <li>租户一致性校验</li>
     *   <li>逐条数据有效性验证</li>
     *   <li>批量保存操作</li>
     * </ol>
     */
    @Override
    public boolean batchSave(List<TransactionFee> transactionFees) {
        if (CollectionUtils.isEmpty(transactionFees)) {
            log.warn("批量保存交易费用失败：费用列表为空");
            return false;
        }

        // 校验租户一致性：批量操作的所有记录必须属于同一租户
        Long tenantId = transactionFees.get(0).getTenantId();
        if (tenantId == null) {
            throw new IllegalArgumentException("租户ID不能为空");
        }

        for (TransactionFee fee : transactionFees) {
            if (!Objects.equals(tenantId, fee.getTenantId())) {
                throw new IllegalArgumentException("批量操作的费用记录必须属于同一租户");
            }

            // 逐条验证数据有效性，包括必填字段、业务规则等
            validateTransactionFee(fee);
        }

        // 执行批量保存操作
        return saveBatch(transactionFees);
    }

    /**
     * 批量更新交易费用支付状态
     * <p>批量修改指定交易费用的支付状态，并自动处理支付时间等关联字段</p>
     *
     * @param ids 待更新的费用记录ID列表，不能为null或空
     * @param paymentStatus 目标支付状态，必须在有效状态范围内
     * @param tenantId 租户ID，用于数据隔离和权限校验，不能为null
     * @return boolean 操作结果，true-成功 false-失败
     * @throws IllegalArgumentException 当参数非法、状态值无效或租户ID为空时抛出异常
     *
     * <p><b>支付时间自动处理逻辑：</b></p>
     * <table border="1">
     *   <tr><th>目标状态</th><th>支付时间处理</th><th>说明</th></tr>
     *   <tr><td>PAID</td><td>设置为当前时间</td><td>表示费用已全额支付完成</td></tr>
     *   <tr><td>REFUNDED</td><td>设置为当前时间</td><td>表示费用已全额或部分退款完成</td></tr>
     *   <tr><td>UNPAID</td><td>清空为null</td><td>状态回退，支付时间应清空</td></tr>
     *   <tr><td>PARTIALLY_PAID</td><td>清空为null</td><td>部分支付状态下支付时间无效</td></tr>
     * </table>
     *
     * <p><b>注意事项：</b>即使更新0行也返回true，表示SQL执行成功</p>
     */
    @Override
    public boolean batchUpdateStatus(List<Long> ids, String paymentStatus, Long tenantId) {
        if (CollectionUtils.isEmpty(ids)) {
            log.warn("批量更新支付状态失败：ID列表为空");
            return false;
        }

        if (tenantId == null) {
            throw new IllegalArgumentException("租户ID不能为空");
        }

        // 支付状态合法性校验
        if (!VALID_PAYMENT_STATUSES.contains(paymentStatus)) {
            throw new IllegalArgumentException("支付状态值无效，仅支持：" + VALID_PAYMENT_STATUSES);
        }

        // 构建更新条件：指定ID列表且属于指定租户
        LambdaUpdateWrapper<TransactionFee> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.in(TransactionFee::getId, ids)
                .eq(TransactionFee::getTenantId, tenantId)
                .set(TransactionFee::getPaymentStatus, paymentStatus);

        // 根据支付状态自动处理支付时间
        if ("PAID".equals(paymentStatus) || "REFUNDED".equals(paymentStatus)) {
            // 已支付或已退款状态：自动记录当前时间为支付时间
            updateWrapper.set(TransactionFee::getPaymentTime, LocalDateTime.now());
        } else {
            // 未支付或部分支付状态：清空支付时间
            updateWrapper.set(TransactionFee::getPaymentTime, null);
        }

        // 执行批量更新操作
        update(null, updateWrapper);
        return true;
    }

    /**
     * 批量删除交易费用明细
     * <p>批量删除指定租户下的交易费用记录，操作不可逆，需谨慎使用</p>
     *
     * @param ids 待删除的费用记录ID列表，不能为null或空
     * @param tenantId 租户ID，用于权限校验和数据隔离，不能为null
     * @return boolean 操作结果，true-成功 false-失败
     * @throws IllegalArgumentException 当参数非法或租户ID为空时抛出异常
     *
     * <p><b>安全机制：</b></p>
     * <ul>
     *   <li>租户隔离：只删除属于指定租户的记录</li>
     *   <li>条件删除：使用QueryWrapper确保条件匹配</li>
     *   <li>权限控制：防止越权删除其他租户数据</li>
     * </ul>
     *
     * <p><b>风险提示：</b>操作不可逆，建议先进行数据备份或确认</p>
     */
    @Override
    public boolean batchRemoveByIds(List<Long> ids, Long tenantId) {
        if (CollectionUtils.isEmpty(ids)) {
            log.warn("批量删除交易费用失败：ID列表为空");
            return false;
        }

        if (tenantId == null) {
            throw new IllegalArgumentException("租户ID不能为空");
        }

        // 构建删除条件：指定ID列表且属于指定租户
        LambdaQueryWrapper<TransactionFee> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(TransactionFee::getId, ids)
                .eq(TransactionFee::getTenantId, tenantId);

        // 执行批量删除操作
        remove(queryWrapper);
        return true;
    }

    /**
     * 批量更新费用金额
     * <p>批量修改指定交易费用的金额，适用于费用调整或修正场景</p>
     *
     * @param ids 待更新的费用记录ID列表，不能为null或空
     * @param amount 新的费用金额，不能为null且必须大于等于0
     * @param tenantId 租户ID，用于数据隔离和权限校验，不能为null
     * @return boolean 操作结果，true-成功 false-失败
     * @throws IllegalArgumentException 当参数非法、金额无效或租户ID为空时抛出异常
     *
     * <p><b>金额校验规则：</b></p>
     * <ul>
     *   <li>金额不能为null</li>
     *   <li>金额不能为负数（即必须大于等于0）</li>
     *   <li>金额为0表示免费（某些特殊场景）</li>
     * </ul>
     *
     * <p><b>使用场景：</b></p>
     * <ul>
     *   <li>费用金额调整</li>
     *   <li>批量费用修正</li>
     *   <li>促销或折扣处理</li>
     * </ul>
     */
    public boolean batchUpdateAmount(List<Long> ids, BigDecimal amount, Long tenantId) {
        if (CollectionUtils.isEmpty(ids)) {
            log.warn("批量更新费用金额失败：ID列表为空");
            return false;
        }

        if (tenantId == null) {
            throw new IllegalArgumentException("租户ID不能为空");
        }

        // 金额有效性校验
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("费用金额不能为空且不能为负数");
        }

        // 构建更新实体和条件
        TransactionFee updateEntity = new TransactionFee();
        updateEntity.setAmount(amount);

        LambdaQueryWrapper<TransactionFee> updateWrapper = new LambdaQueryWrapper<>();
        updateWrapper.in(TransactionFee::getId, ids)
                .eq(TransactionFee::getTenantId, tenantId);

        // 执行批量更新操作
        update(updateEntity, updateWrapper);
        return true;
    }

    /**
     * 根据合同ID查询交易费用明细
     * <p>查询指定合同关联的所有费用明细，按创建时间倒序排列</p>
     *
     * @param contractId 合同ID，不能为null
     * @param tenantId 租户ID，用于数据隔离，不能为null
     * @return List<TransactionFee> 指定合同关联的费用明细列表，按创建时间倒序排列
     * @throws IllegalArgumentException 当合同ID或租户ID为空时抛出异常
     *
     * <p><b>使用场景：</b></p>
     * <ul>
     *   <li>合同详情页面展示关联费用</li>
     *   <li>合同结算和财务对账</li>
     *   <li>费用统计和汇总计算</li>
     * </ul>
     *
     * <p><b>排序规则：</b>按创建时间倒序排列（最新创建的费用在前）</p>
     */
    public List<TransactionFee> listByContractId(Long contractId, Long tenantId) {
        if (contractId == null) {
            throw new IllegalArgumentException("合同ID不能为空");
        }

        if (tenantId == null) {
            throw new IllegalArgumentException("租户ID不能为空");
        }

        // 构建查询条件：指定合同ID和租户ID，按创建时间倒序排列
        LambdaQueryWrapper<TransactionFee> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TransactionFee::getContractId, contractId)
                .eq(TransactionFee::getTenantId, tenantId)
                .orderByDesc(TransactionFee::getCreateTime);

        return list(queryWrapper);
    }

    /**
     * 构建查询条件（通用方法）
     * <p>根据查询参数和租户ID构建MyBatis Plus查询条件对象，支持多条件组合查询</p>
     *
     * @param queryParams 查询参数映射，支持多种条件的组合
     * @param tenantId 租户ID，用于数据隔离，必填
     * @return LambdaQueryWrapper<TransactionFee> 构建好的查询条件对象
     *
     * <p><b>查询条件优先级：</b></p>
     * <ol>
     *   <li>租户ID条件（强制添加）</li>
     *   <li>合同ID条件（精确匹配）</li>
     *   <li>费用类型条件（精确匹配，需验证有效性）</li>
     *   <li>支付方条件（精确匹配，需验证有效性）</li>
     *   <li>支付状态条件（精确匹配，需验证有效性）</li>
     *   <li>时间范围条件（创建时间范围查询）</li>
     *   <li>金额范围条件（费用金额区间查询）</li>
     * </ol>
     *
     * <p><b>参数类型校验：</b>所有参数都进行类型安全检查，避免类型转换异常</p>
     */
    private LambdaQueryWrapper<TransactionFee> buildQueryWrapper(Map<String, Object> queryParams, Long tenantId) {
        LambdaQueryWrapper<TransactionFee> queryWrapper = new LambdaQueryWrapper<>();

        // 强制添加租户隔离条件，确保数据安全
        queryWrapper.eq(TransactionFee::getTenantId, tenantId);

        // 空参数检查：查询参数为空时直接返回基本条件
        if (MapUtils.isEmpty(queryParams)) {
            return queryWrapper;
        }

        // 合同ID查询：精确匹配
        if (queryParams.containsKey("contractId")) {
            Object contractId = queryParams.get("contractId");
            if (contractId instanceof Long) {
                queryWrapper.eq(TransactionFee::getContractId, (Long) contractId);
            }
        }

        // 费用类型查询：精确匹配，需验证类型有效性
        if (queryParams.containsKey("feeType")) {
            Object feeType = queryParams.get("feeType");
            if (feeType instanceof String && VALID_FEE_TYPES.contains((String) feeType)) {
                queryWrapper.eq(TransactionFee::getFeeType, feeType);
            }
        }

        // 支付方查询：精确匹配，需验证支付方有效性
        if (queryParams.containsKey("payer")) {
            Object payer = queryParams.get("payer");
            if (payer instanceof String && VALID_PAYERS.contains((String) payer)) {
                queryWrapper.eq(TransactionFee::getPayer, payer);
            }
        }

        // 支付状态查询：精确匹配，需验证状态有效性
        if (queryParams.containsKey("paymentStatus")) {
            Object paymentStatus = queryParams.get("paymentStatus");
            if (paymentStatus instanceof String && VALID_PAYMENT_STATUSES.contains((String) paymentStatus)) {
                queryWrapper.eq(TransactionFee::getPaymentStatus, paymentStatus);
            }
        }

        // 时间范围查询：创建时间区间查询（必须同时提供起始和结束时间）
        if (queryParams.containsKey("startTime") && queryParams.get("startTime") instanceof LocalDateTime &&
                queryParams.containsKey("endTime") && queryParams.get("endTime") instanceof LocalDateTime) {
            queryWrapper.between(TransactionFee::getCreateTime,
                    queryParams.get("startTime"),
                    queryParams.get("endTime"));
        }

        // 金额范围查询：费用金额区间查询
        if (queryParams.containsKey("minAmount") && queryParams.get("minAmount") instanceof BigDecimal) {
            queryWrapper.ge(TransactionFee::getAmount, queryParams.get("minAmount"));
        }
        if (queryParams.containsKey("maxAmount") && queryParams.get("maxAmount") instanceof BigDecimal) {
            queryWrapper.le(TransactionFee::getAmount, queryParams.get("maxAmount"));
        }

        // 默认排序规则：按创建时间倒序排列
        queryWrapper.orderByDesc(TransactionFee::getCreateTime);

        return queryWrapper;
    }

    /**
     * 验证交易费用数据的有效性
     * <p>对交易费用实体进行全面的业务规则校验，确保数据完整性和一致性</p>
     *
     * @param fee 待验证的交易费用实体
     * @throws IllegalArgumentException 当任何校验规则不满足时抛出异常
     *
     * <p><b>校验规则：</b></p>
     * <table border="1">
     *   <tr><th>校验项</th><th>规则说明</th><th>异常消息</th></tr>
     *   <tr><td>实体非空</td><td>费用实体不能为null</td><td>"交易费用不能为空"</td></tr>
     *   <tr><td>租户ID</td><td>租户ID不能为null</td><td>"租户ID不能为空"</td></tr>
     *   <tr><td>合同ID</td><td>合同ID不能为null</td><td>"合同ID不能为空"</td></tr>
     *   <tr><td>费用类型</td><td>必须在有效类型范围内</td><td>"费用类型无效，仅支持：[...]"</td></tr>
     *   <tr><td>费用金额</td><td>不能为null且必须≥0</td><td>"费用金额不能为空且不能为负数"</td></tr>
     *   <tr><td>支付方</td><td>必须在有效支付方范围内</td><td>"支付方无效，仅支持：[...]"</td></tr>
     *   <tr><td>支付状态</td><td>必须在有效状态范围内</td><td>"支付状态无效，仅支持：[...]"</td></tr>
     *   <tr><td>支付时间逻辑</td><td>已支付/已退款状态必须有支付时间</td><td>"已支付或已退款状态必须设置支付时间"</td></tr>
     *   <tr><td>支付时间逻辑</td><td>未支付/部分支付状态不应有支付时间</td><td>（警告日志）</td></tr>
     * </table>
     */
    private void validateTransactionFee(TransactionFee fee) {
        if (fee == null) {
            throw new IllegalArgumentException("交易费用不能为空");
        }

        if (fee.getTenantId() == null) {
            throw new IllegalArgumentException("租户ID不能为空");
        }

        if (fee.getContractId() == null) {
            throw new IllegalArgumentException("合同ID不能为空");
        }

        // 费用类型有效性校验
        if (fee.getFeeType() == null || !VALID_FEE_TYPES.contains(fee.getFeeType())) {
            throw new IllegalArgumentException("费用类型无效，仅支持：" + VALID_FEE_TYPES);
        }

        // 费用金额有效性校验：不能为null且不能为负数
        if (fee.getAmount() == null || fee.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("费用金额不能为空且不能为负数");
        }

        // 支付方有效性校验
        if (fee.getPayer() == null || !VALID_PAYERS.contains(fee.getPayer())) {
            throw new IllegalArgumentException("支付方无效，仅支持：" + VALID_PAYERS);
        }

        // 支付状态有效性校验
        if (fee.getPaymentStatus() == null || !VALID_PAYMENT_STATUSES.contains(fee.getPaymentStatus())) {
            throw new IllegalArgumentException("支付状态无效，仅支持：" + VALID_PAYMENT_STATUSES);
        }

        // 支付时间与支付状态的逻辑一致性校验
        // 规则1：已支付或已退款状态必须设置支付时间
        if (("PAID".equals(fee.getPaymentStatus()) || "REFUNDED".equals(fee.getPaymentStatus()))
                && fee.getPaymentTime() == null) {
            throw new IllegalArgumentException("已支付或已退款状态必须设置支付时间");
        }

        // 规则2：未支付或部分支付状态不应有支付时间（如有则自动清空并记录警告）
        if (("UNPAID".equals(fee.getPaymentStatus()) || "PARTIALLY_PAID".equals(fee.getPaymentStatus()))
                && fee.getPaymentTime() != null) {
            log.warn("未支付或部分支付状态不应设置支付时间，已忽略");
            fee.setPaymentTime(null);
        }
    }
}
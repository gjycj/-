package com.house.deed.pavilion.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.house.deed.pavilion.entity.TransactionFee;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 交易费用明细表（租户级数据） 服务类
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
public interface TransactionFeeService extends IService<TransactionFee> {

    // ==================== 多条件分页查询（带租户隔离） ====================
    /**
     * 分页查询交易费用记录（支持多条件+租户隔离）
     * @param page 分页参数
     * @param queryParams 查询条件（支持：contractId、feeType、paymentStatus、startTime、endTime等）
     * @param tenantId 租户ID（强制隔离）
     * @return 分页结果
     */
    IPage<TransactionFee> pageQuery(Page<TransactionFee> page, Map<String, Object> queryParams, Long tenantId);

    // ==================== 多条件查询列表（带租户隔离） ====================
    /**
     * 多条件查询交易费用列表（租户隔离）
     * @param queryParams 查询条件
     * @param tenantId 租户ID
     * @return 符合条件的费用列表
     */
    List<TransactionFee> listByConditions(Map<String, Object> queryParams, Long tenantId);

    // ==================== 批量操作 ====================
    /**
     * 批量新增交易费用（带租户校验）
     * @param transactionFees 费用列表
     * @return 操作结果
     */
    boolean batchSave(List<TransactionFee> transactionFees);

    /**
     * 批量更新支付状态（带租户校验）
     * @param ids 费用ID列表
     * @param paymentStatus 目标支付状态
     * @param tenantId 租户ID
     * @return 操作结果
     */
    boolean batchUpdateStatus(List<Long> ids, String paymentStatus, Long tenantId);

    /**
     * 批量删除交易费用（带租户校验）
     * @param ids 费用ID列表
     * @param tenantId 租户ID
     * @return 操作结果
     */
    boolean batchRemoveByIds(List<Long> ids, Long tenantId);

}
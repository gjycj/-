package com.house.deed.pavilion.module.customer.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.house.deed.pavilion.module.customer.entity.Customer;

import java.math.BigDecimal;

/**
 * <p>
 * 客户信息表（租户级数据隔离） 服务类
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
public interface ICustomerService extends IService<Customer> {
    boolean existsById(Long id);

    /**
     * 带条件的客户分页查询
     * @param page 分页参数
     * @param intendedRegionId 意向区域ID（可选）
     * @param priceMin 意向价格下限（可选）
     * @param priceMax 意向价格上限（可选）
     * @param customerType 客户类型（可选）
     * @param status 客户状态（可选）
     * @return 分页结果
     */
    Page<Customer> getCustomerPageByCondition(
            Page<Customer> page,
            Long intendedRegionId,
            BigDecimal priceMin,
            BigDecimal priceMax,
            String customerType,
            String status
    );

    /**
     * 更新客户状态（带流转校验）
     * @param customerId 客户ID
     * @param targetStatus 目标状态（ACTIVE/DEALED/DORMANT）
     * @param operatorId 操作人ID（当前经纪人）
     * @return 是否更新成功
     */
    boolean updateStatus(Long customerId, String targetStatus, Long operatorId);
}

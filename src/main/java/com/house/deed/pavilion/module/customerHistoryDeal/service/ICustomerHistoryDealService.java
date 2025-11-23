package com.house.deed.pavilion.module.customerHistoryDeal.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.house.deed.pavilion.module.contract.entity.Contract;
import com.house.deed.pavilion.module.customerHistoryDeal.entity.CustomerHistoryDeal;

/**
 * <p>
 * 客户历史成交记录表（租户级数据） 服务类
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
public interface ICustomerHistoryDealService extends IService<CustomerHistoryDeal> {
    // 从合同生成成交记录
    void createFromContract(Long contractId);

    // 按客户ID查询历史成交记录（带权限控制）
    Page<CustomerHistoryDeal> getByCustomerId(Page<CustomerHistoryDeal> page, Long customerId);
}

package com.house.deed.pavilion.module.customerFollowUp.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.house.deed.pavilion.common.aspect.annotation.AgentDataPermission;
import com.house.deed.pavilion.module.customerFollowUp.entity.CustomerFollowUp;

import java.util.List;

/**
 * <p>
 * 客户跟进记录表（租户级数据） 服务类
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
public interface ICustomerFollowUpService extends IService<CustomerFollowUp> {
    // 新增：通过合同ID查询带看记录
    List<CustomerFollowUp> getByContractId(Long contractId, Long tenantId);

    boolean saveWithTimeCheck(CustomerFollowUp followUp);

    // 分页查询客户的跟进记录（已声明）
    @AgentDataPermission(
            operation = AgentDataPermission.OperationType.QUERY,
            entityClass = CustomerFollowUp.class,
            creatorField = "agentId" // 跟进记录的创建人字段
    )
    Page<CustomerFollowUp> getByCustomerId(Page<CustomerFollowUp> page, Long customerId, Long tenantId);

    boolean updateFollowUp(CustomerFollowUp followUp);

    @AgentDataPermission(
            operation = AgentDataPermission.OperationType.DELETE,
            entityClass = CustomerFollowUp.class,
            dataIdParam = "id" // 跟进记录ID参数
    )
    boolean deleteFollowUp(Long id);

    CustomerFollowUp getByIdWithPermission(Long id);
}

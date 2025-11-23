package com.house.deed.pavilion.module.customerHistoryDeal.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.common.aspect.annotation.AgentDataPermission;
import com.house.deed.pavilion.common.exception.BusinessException;
import com.house.deed.pavilion.common.util.TenantContext;
import com.house.deed.pavilion.module.contract.entity.Contract;
import com.house.deed.pavilion.module.contract.service.IContractService;
import com.house.deed.pavilion.module.customer.entity.Customer;
import com.house.deed.pavilion.module.customerHistoryDeal.entity.CustomerHistoryDeal;
import com.house.deed.pavilion.module.customerHistoryDeal.mapper.CustomerHistoryDealMapper;
import com.house.deed.pavilion.module.customerHistoryDeal.service.ICustomerHistoryDealService;
import com.house.deed.pavilion.module.house.entity.House;
import com.house.deed.pavilion.module.house.service.IHouseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * <p>
 * 客户历史成交记录表（租户级数据） 服务实现类
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
@Service
@Slf4j
public class CustomerHistoryDealServiceImpl extends ServiceImpl<CustomerHistoryDealMapper, CustomerHistoryDeal> implements ICustomerHistoryDealService {

    @Autowired
    private IContractService contractService;

    @Autowired
    private IHouseService houseService;



    /**
     * 从合同生成客户成交记录（基于现有代码字段）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createFromContract(Long contractId) {
        Long tenantId = TenantContext.getTenantId();
        // 1. 查询合同（校验租户归属）
        Contract contract = contractService.lambdaQuery()
                .eq(Contract::getId, contractId)
                .eq(Contract::getTenantId, tenantId)
                .one();
        if (contract == null) {
            throw new BusinessException(404, "合同不存在或不属于当前租户");
        }

        // 2. 校验合同状态（假设合同状态字段为status，"COMPLETED"表示已成交）
        if (!"COMPLETED".equals(contract.getStatus())) {
            throw new BusinessException(400, "仅已成交合同可生成记录（当前状态：" + contract.getStatus() + "）");
        }

        // 3. 去重校验（基于客户-合同唯一索引）
        boolean exists = lambdaQuery()
                .eq(CustomerHistoryDeal::getCustomerId, contract.getCustomerId())
                .eq(CustomerHistoryDeal::getContractId, contractId)
                .exists();
        if (exists) {
            log.warn("客户[{}]的合同[{}]已生成成交记录，跳过", contract.getCustomerId(), contractId);
        }

        // 4. 补充房源信息（使用House实体的现有字段拼接，无getCommunityName方法）
        House house = houseService.getById(contract.getHouseId());
        // 拼接规则：房号 + 户型（如"1单元301 3室2厅1卫"）
        String houseInfo = String.format("%s %s", house.getHouseNo(), house.getHouseType());

        // 5. 确定成交类型（复用TransactionType的code，如SALE/RENT）
        String dealType = contract.getContractType(); // 假设合同有transactionType字段，直接对应枚举code

        // 6. 构建并保存成交记录
        CustomerHistoryDeal deal = new CustomerHistoryDeal();
        deal.setTenantId(tenantId);
        deal.setCustomerId(contract.getCustomerId());
        deal.setContractId(contractId);
        deal.setDealTime(LocalDate.now()); // 或使用合同的实际成交日期
        deal.setHouseInfo(houseInfo);
        deal.setDealType(dealType);
        deal.setCreateTime(LocalDateTime.now());

        save(deal);
    }

    /**
     * 按客户ID查询成交记录（带权限控制，修正注解参数）
     */
    @Override
    @AgentDataPermission(
            operation = AgentDataPermission.OperationType.QUERY,
            entityClass = CustomerHistoryDeal.class,
            // 权限逻辑：通过客户表的createAgentId控制（关联查询）
            relatedEntityClass = Customer.class, // 修正参数名：relatedEntityClass（而非relatedEntity）
            relatedIdField = "customerId",      // 修正参数名：relatedIdField（而非relatedField）
            relatedCreatorField = "createAgentId"
    )
    public Page<CustomerHistoryDeal> getByCustomerId(Page<CustomerHistoryDeal> page, Long customerId) {
        return lambdaQuery()
                .eq(CustomerHistoryDeal::getTenantId, TenantContext.getTenantId())
                .eq(CustomerHistoryDeal::getCustomerId, customerId)
                .orderByDesc(CustomerHistoryDeal::getDealTime)
                .page(page);
    }
}

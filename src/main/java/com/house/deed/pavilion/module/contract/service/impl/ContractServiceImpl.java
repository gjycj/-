package com.house.deed.pavilion.module.contract.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.common.exception.BusinessException;
import com.house.deed.pavilion.common.util.TenantContext;
import com.house.deed.pavilion.module.contract.entity.Contract;
import com.house.deed.pavilion.module.contract.mapper.ContractMapper;
import com.house.deed.pavilion.module.contract.service.IContractService;
import com.house.deed.pavilion.module.customer.entity.Customer;
import com.house.deed.pavilion.module.customer.service.ICustomerService;
import com.house.deed.pavilion.module.house.entity.House;
import com.house.deed.pavilion.module.house.service.IHouseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * <p>
 * 交易合同表（租户核心业务数据） 服务实现类
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
@Service
public class ContractServiceImpl extends ServiceImpl<ContractMapper, Contract> implements IContractService {

    @Autowired
    private IHouseService houseService;
    @Autowired
    private ICustomerService customerService;

    @Override
    @Transactional
    public boolean createContract(Contract contract) {
        Long tenantId = TenantContext.getTenantId();

        // 1. 校验房源是否属于当前租户
        House house = houseService.getById(contract.getHouseId());
        if (house == null || !house.getTenantId().equals(tenantId)) {
            throw new RuntimeException("房源不存在或不属于当前租户");
        }

        // 2. 校验客户是否属于当前租户
        Customer customer = customerService.getById(contract.getCustomerId());
        if (customer == null || !customer.getTenantId().equals(tenantId)) {
            throw new RuntimeException("客户不存在或不属于当前租户");
        }

        // 3. 生成租户内唯一的合同编号（如 TENANT123_CONTRACT20240520001）
        String contractNo = generateContractNo(tenantId);
        contract.setContractNo(contractNo);

        // 4. 保存合同（tenant_id自动填充）
        return save(contract);
    }

    /**
     * 合同状态流转（如：签约→执行中→完成）
     */
    @Transactional
    public boolean updateContractStatus(Long contractId, String targetStatus) {
        Contract contract = getById(contractId);
        if (contract == null) {
            throw new BusinessException(404, "合同不存在");
        }
        // 校验状态流转合法性（例如：SIGNED→EXECUTING→COMPLETED）
        validateStatusTransition(contract.getStatus(), targetStatus);
        contract.setStatus(targetStatus);
        return updateById(contract);
    }

    // 校验状态流转是否合法
    private void validateStatusTransition(String currentStatus, String targetStatus) {
        List<String> validTransitions = switch (currentStatus) {
            case "SIGNED" -> List.of("EXECUTING", "TERMINATED");
            case "EXECUTING" -> List.of("COMPLETED", "TERMINATED");
            default -> List.of(); // 其他状态不允许流转
        };
        if (!validTransitions.contains(targetStatus)) {
            throw new BusinessException(400, "不允许从[" + currentStatus + "]流转至[" + targetStatus + "]");
        }
    }

    // 生成租户内唯一的合同编号
    private String generateContractNo(Long tenantId) {
        // 实现逻辑：结合租户ID、日期、自增序号
        return "T" + tenantId + "_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + RandomUtil.randomNumbers(3);
    }

}

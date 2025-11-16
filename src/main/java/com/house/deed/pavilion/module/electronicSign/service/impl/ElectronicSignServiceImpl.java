package com.house.deed.pavilion.module.electronicSign.service.impl;

import cn.hutool.core.lang.UUID;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.common.exception.BusinessException;
import com.house.deed.pavilion.common.util.TenantContext;
import com.house.deed.pavilion.module.contract.entity.Contract;
import com.house.deed.pavilion.module.contract.service.IContractService;
import com.house.deed.pavilion.module.electronicSign.entity.ElectronicSign;
import com.house.deed.pavilion.module.electronicSign.mapper.ElectronicSignMapper;
import com.house.deed.pavilion.module.electronicSign.service.IElectronicSignService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * <p>
 * 电子签约信息表（租户级数据） 服务实现类
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
@Service
public class ElectronicSignServiceImpl extends ServiceImpl<ElectronicSignMapper, ElectronicSign> implements IElectronicSignService {

    @Resource
    private IContractService contractService;

    @Override
    public ElectronicSign createElectronicSign(Long contractId, String signPlatform) {
        Long tenantId = TenantContext.getTenantId();

        // 1. 校验合同存在性
        Contract contract = contractService.getById(contractId);
        if (contract == null || !contract.getTenantId().equals(tenantId)) {
            throw new BusinessException(404, "合同不存在或无权访问");
        }

        // 2. 生成签约链接（实际应调用电子签平台API）
        String signUrl = "https://sign-platform.example.com/" + UUID.randomUUID();

        // 3. 保存电子签记录
        ElectronicSign electronicSign = new ElectronicSign();
        electronicSign.setTenantId(tenantId);
        electronicSign.setContractId(contractId);
        electronicSign.setSignPlatform(signPlatform);
        electronicSign.setSignUrl(signUrl);
        electronicSign.setSignStatus("PENDING"); // 初始状态：待签
        electronicSign.setCreateTime(LocalDateTime.now());
        save(electronicSign);

        return electronicSign;
    }

    @Override
    public String updateSignStatus(Long signId, boolean customerSign, boolean landlordSign) {
        Long tenantId = TenantContext.getTenantId();
        ElectronicSign electronicSign = getById(signId);
        if (electronicSign == null || !electronicSign.getTenantId().equals(tenantId)) {
            throw new BusinessException(404, "电子签记录不存在");
        }

        // 更新签约时间
        if (customerSign) {
            electronicSign.setCustomerSignTime(LocalDateTime.now());
        }
        if (landlordSign) {
            electronicSign.setLandlordSignTime(LocalDateTime.now());
        }

        // 更新状态
        String newStatus = (customerSign && landlordSign) ? "SIGNED" : electronicSign.getSignStatus();
        electronicSign.setSignStatus(newStatus);
        updateById(electronicSign);

        return newStatus;
    }

}

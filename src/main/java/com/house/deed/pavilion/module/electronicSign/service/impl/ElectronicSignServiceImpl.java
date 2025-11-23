package com.house.deed.pavilion.module.electronicSign.service.impl;

import cn.hutool.core.lang.UUID;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.common.aspect.annotation.AgentDataPermission;
import com.house.deed.pavilion.common.exception.BusinessException;
import com.house.deed.pavilion.common.util.ContractValidationUtil;
import com.house.deed.pavilion.common.util.TenantContext;
import com.house.deed.pavilion.module.contract.entity.Contract;
import com.house.deed.pavilion.module.contract.service.IContractService;
import com.house.deed.pavilion.module.electronicSign.entity.ElectronicSign;
import com.house.deed.pavilion.module.electronicSign.mapper.ElectronicSignMapper;
import com.house.deed.pavilion.module.electronicSign.service.IElectronicSignService;
import jakarta.annotation.Resource;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    @Resource
    private ContractValidationUtil contractValidationUtil; // 复用合同校验工具类

    /**
     * 创建电子签：关联合同创建人权限，仅合同创建人/管理员可操作
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @AgentDataPermission(
            operation = AgentDataPermission.OperationType.CREATE,
            entityClass = Contract.class, // 关联合同实体
            dataIdParam = "contractId",   // 合同ID参数
            creatorField = "agentId"      // 合同创建人字段（agent_id）
    )
    public ElectronicSign createElectronicSign(Long contractId, String signPlatform) {
        Long tenantId = TenantContext.getTenantId();

        // 1. 校验合同存在性及租户归属（复用通用校验工具）
        Contract contract = contractValidationUtil.validateContract(contractId, tenantId);
        // 2. 校验合同类型（例如：仅买卖合同可发起电子签）
        if (!"SALE".equals(contract.getContractType())) {
            throw new BusinessException(400, "仅买卖合同支持电子签约");
        }
        // 3. 校验是否已存在电子签记录（一个合同仅允许一条电子签）
        if (getByContractId(contractId) != null) {
            throw new BusinessException(400, "该合同已创建电子签记录，不可重复创建");
        }

        // 4. 调用电子签平台API生成签约链接（实际场景需替换为真实接口）
        String signUrl = generateSignUrl(contractId, signPlatform);
        // 5. 生成电子合同PDF（模拟：实际需调用平台生成并存储）
        String contractPdfUrl = generateContractPdf(contract);

        // 6. 保存电子签记录
        ElectronicSign electronicSign = new ElectronicSign();
        electronicSign.setTenantId(tenantId);
        electronicSign.setContractId(contractId);
        electronicSign.setSignPlatform(signPlatform);
        electronicSign.setSignUrl(signUrl);
        electronicSign.setContractPdfUrl(contractPdfUrl);
        electronicSign.setSignStatus("PENDING"); // 初始状态：待签
        electronicSign.setCreateTime(LocalDateTime.now());
        save(electronicSign);

        return electronicSign;
    }

    /**
     * 更新签约状态：支持第三方回调，同步更新合同状态
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String updateSignStatus(Long signId, boolean customerSign, boolean landlordSign) {
        Long tenantId = TenantContext.getTenantId();
        // 1. 校验电子签记录存在性及租户归属
        ElectronicSign electronicSign = getById(signId);
        if (electronicSign == null || !electronicSign.getTenantId().equals(tenantId)) {
            throw new BusinessException(404, "电子签记录不存在或无权访问");
        }
        // 2. 校验当前状态是否允许更新（已签/过期状态不允许再更新）
        String currentStatus = electronicSign.getSignStatus();
        if ("SIGNED".equals(currentStatus) || "EXPIRED".equals(currentStatus) || "REJECTED".equals(currentStatus)) {
            throw new BusinessException(400, "当前状态不允许更新（状态：" + currentStatus + "）");
        }

        // 3. 更新签署时间
        LocalDateTime now = LocalDateTime.now();
        if (customerSign && electronicSign.getCustomerSignTime() == null) {
            electronicSign.setCustomerSignTime(now);
        }
        if (landlordSign && electronicSign.getLandlordSignTime() == null) {
            electronicSign.setLandlordSignTime(now);
        }

        // 4. 确定新状态
        String newStatus = currentStatus;
        if (customerSign && landlordSign) {
            newStatus = "SIGNED"; // 双方已签
            // 生成防篡改哈希（实际需基于PDF内容生成）
            electronicSign.setSignHash(generateSignHash(electronicSign.getContractPdfUrl()));
            // 联动更新合同状态为"已签约"
            contractService.updateContractStatus(electronicSign.getContractId(), "SIGNED");
        } else if ("REJECTED".equals(currentStatus)) {
            newStatus = "REJECTED"; // 若已拒签，保持状态
        }

        electronicSign.setSignStatus(newStatus);
        electronicSign.setUpdateTime(now);
        updateById(electronicSign);

        return newStatus;
    }

    /**
     * 按合同ID查询：关联合同权限，仅合同相关人员可查看
     */
    @Override
    @AgentDataPermission(
            operation = AgentDataPermission.OperationType.QUERY,
            entityClass = Contract.class,
            dataIdParam = "contractId",
            creatorField = "agentId"
    )
    public ElectronicSign getByContractId(Long contractId) {
        Long tenantId = TenantContext.getTenantId();
        return baseMapper.selectOne(new LambdaQueryWrapper<ElectronicSign>()
                .eq(ElectronicSign::getTenantId, tenantId)
                .eq(ElectronicSign::getContractId, contractId)
        );
    }

    /**
     * 批量查询：用于合同列表页关联展示电子签状态
     */
    @Override
    @AgentDataPermission(
            operation = AgentDataPermission.OperationType.QUERY,
            entityClass = Contract.class,
            dataIdParam = "contractIds",
            creatorField = "agentId"
    )
    public Map<Long, ElectronicSign> getBatchByContractIds(List<Long> contractIds) {
        Long tenantId = TenantContext.getTenantId();
        List<ElectronicSign> signList = baseMapper.selectList(new LambdaQueryWrapper<ElectronicSign>()
                .eq(ElectronicSign::getTenantId, tenantId)
                .in(ElectronicSign::getContractId, contractIds)
        );
        return signList.stream()
                .collect(Collectors.toMap(ElectronicSign::getContractId, Function.identity()));
    }

    // ------------------------------ 工具方法（模拟实现） ------------------------------
    /**
     * 生成签约链接（实际需调用电子签平台API）
     */
    private String generateSignUrl(Long contractId, String platform) {
        return "https://" + platform + ".example.com/sign?contractId=" + contractId + "&nonce=" + UUID.randomUUID();
    }

    /**
     * 生成电子合同PDF（实际需调用平台生成）
     */
    private String generateContractPdf(Contract contract) {
        return "https://storage.example.com/contracts/" + contract.getContractNo() + ".pdf";
    }

    /**
     * 生成电子签名哈希（防篡改）
     */
    private String generateSignHash(String pdfUrl) {
        // 实际应基于PDF内容计算SHA256哈希
        return DigestUtils.sha256Hex(pdfUrl + System.currentTimeMillis());
    }

}

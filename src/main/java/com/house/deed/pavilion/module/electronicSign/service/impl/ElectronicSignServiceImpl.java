package com.house.deed.pavilion.module.electronicSign.service.impl;

import cn.hutool.core.lang.UUID;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.common.aspect.annotation.AgentDataPermission;
import com.house.deed.pavilion.common.exception.BusinessException;
import com.house.deed.pavilion.common.util.ContractValidationUtil;
import com.house.deed.pavilion.common.util.RoleUtil;
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean invalidSign(Long signId) {
        // 1. 权限校验：仅管理员可操作
        if (!RoleUtil.isAdmin()) {
            throw new BusinessException(403, "无权作废电子签：仅管理员可操作");
        }

        // 2. 校验电子签记录存在性及租户归属
        Long tenantId = TenantContext.getTenantId();
        ElectronicSign electronicSign = this.getById(signId);
        if (electronicSign == null) {
            throw new BusinessException(404, "电子签记录不存在");
        }
        if (!electronicSign.getTenantId().equals(tenantId)) {
            throw new BusinessException(403, "无权操作其他租户的电子签记录");
        }

        // 3. 状态校验：已作废状态不允许重复作废
        if ("INVALID".equals(electronicSign.getSignStatus())) {
            throw new BusinessException(400, "该电子签已处于作废状态");
        }

        // 4. 特殊状态校验：已完成的电子签不允许作废（根据业务需求调整）
        if ("COMPLETED".equals(electronicSign.getSignStatus())) {
            throw new BusinessException(400, "已完成的电子签不允许作废");
        }

        // 5. 更新状态为作废
        electronicSign.setSignStatus("INVALID");
        electronicSign.setUpdateTime(LocalDateTime.now());
        return this.updateById(electronicSign);
    }

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String updateSignStatus(Long signId, boolean customerSign, boolean landlordSign,
                                   LocalDateTime customerSignTime, LocalDateTime landlordSignTime) {
        Long tenantId = TenantContext.getTenantId();

        // 1. 校验电子签记录存在性及租户归属（原有逻辑保留）
        ElectronicSign electronicSign = this.getById(signId);
        if (electronicSign == null || !electronicSign.getTenantId().equals(tenantId)) {
            throw new BusinessException(404, "电子签记录不存在或不属于当前租户");
        }

        // 2. 校验当前状态是否允许更新（已完成/过期/拒签状态不允许再更新）
        String currentStatus = electronicSign.getSignStatus();
        if ("COMPLETED".equals(currentStatus) || "EXPIRED".equals(currentStatus) || "REJECTED".equals(currentStatus)) {
            throw new BusinessException(400, "当前状态不允许更新（状态：" + currentStatus + "）");
        }

        // 2. 核心补充：记录客户签名时间（仅首次签名时记录）
        if (customerSign && electronicSign.getCustomerSignTime() == null) {
            // 优先使用第三方提供的时间，无则用系统当前时间
            electronicSign.setCustomerSignTime(
                    customerSignTime != null ? customerSignTime : LocalDateTime.now()
            );
        }

        // 3. 核心补充：记录房东签名时间（仅首次签名时记录）
        if (landlordSign && electronicSign.getLandlordSignTime() == null) {
            electronicSign.setLandlordSignTime(
                    landlordSignTime != null ? landlordSignTime : LocalDateTime.now()
            );
        }

        // 4. 状态更新（原有逻辑保留，与签名时间联动）
        String newStatus = currentStatus;
        if (customerSign && landlordSign) {
            newStatus = "SIGNED"; // 与仓库中合同状态保持一致（原计划2.3用SIGNED触发合同更新）
            electronicSign.setSignStatus(newStatus);
        } else if (customerSign || landlordSign) {
            newStatus = "PARTIALLY_SIGNED"; // 部分签署（新增中间状态，便于监控）
            electronicSign.setSignStatus(newStatus);
        } else {
            newStatus = electronicSign.getSignStatus(); // 无变化，保持原状态
        }

        // 5. 更新时间戳（原有逻辑保留）
        electronicSign.setUpdateTime(LocalDateTime.now());
        this.updateById(electronicSign);

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

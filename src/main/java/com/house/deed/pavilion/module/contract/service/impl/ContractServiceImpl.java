package com.house.deed.pavilion.module.contract.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.common.exception.BusinessException;
import com.house.deed.pavilion.common.util.ContractValidationUtil;
import com.house.deed.pavilion.common.util.TenantContext;
import com.house.deed.pavilion.common.util.ValidateUtil;
import com.house.deed.pavilion.module.agent.entity.Agent;
import com.house.deed.pavilion.module.agent.service.IAgentService;
import com.house.deed.pavilion.module.contract.entity.Contract;
import com.house.deed.pavilion.module.contract.mapper.ContractMapper;
import com.house.deed.pavilion.module.contract.service.IContractService;
import com.house.deed.pavilion.module.contract.vo.ContractDetailVO;
import com.house.deed.pavilion.module.contractAttachment.entity.ContractAttachment;
import com.house.deed.pavilion.module.contractAttachment.mapper.ContractAttachmentMapper;
import com.house.deed.pavilion.module.contractAttachment.service.IContractAttachmentService;
import com.house.deed.pavilion.module.contractLeaseTerms.entity.ContractLeaseTerms;
import com.house.deed.pavilion.module.customer.entity.Customer;
import com.house.deed.pavilion.module.customer.service.ICustomerService;
import com.house.deed.pavilion.module.house.entity.House;
import com.house.deed.pavilion.module.house.repository.HouseStatus;
import com.house.deed.pavilion.module.house.service.IHouseService;
import com.house.deed.pavilion.module.houseStatusLog.entity.HouseStatusLog;
import com.house.deed.pavilion.module.houseStatusLog.service.IHouseStatusLogService;
import com.house.deed.pavilion.module.visitRecord.entity.VisitRecord;
import com.house.deed.pavilion.module.visitRecord.service.IVisitRecordService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
@Slf4j
public class ContractServiceImpl extends ServiceImpl<ContractMapper, Contract> implements IContractService {

    @Resource
    private IHouseService houseService;
    @Resource
    private IHouseStatusLogService houseStatusLogService;
    @Resource
    private ICustomerService customerService;
    @Resource
    private IVisitRecordService visitRecordService; // 带看记录服务
    @Resource
    private IAgentService agentService; // 经纪人服务
    @Resource
    private ContractValidationUtil contractValidationUtil; // 依赖领域服务

    @Resource
    private ContractAttachmentMapper contractAttachmentMapper; // 注入附件服务

    @Override
    public ContractDetailVO getDetailWithAttachments(Long contractId) {
        Long tenantId = TenantContext.getTenantId();
        // 1. 查询合同基本信息并校验权限
        Contract contract = getById(contractId);
        if (contract == null || !contract.getTenantId().equals(tenantId)) {
            throw new BusinessException(404, "合同不存在或无权访问");
        }
        // 2. 转换为VO并查询附件
        ContractDetailVO vo = new ContractDetailVO();
        BeanUtils.copyProperties(contract, vo);
        List<ContractAttachment> attachments = contractAttachmentMapper.selectList(
                new LambdaQueryWrapper<ContractAttachment>()
                .eq(ContractAttachment::getContractId, contractId)
        );
        vo.setAttachments(attachments);
        return vo;
    }

    @Override
    public List<Contract> getByCustomerId(Long customerId, Long tenantId) {
        return lambdaQuery()
                .eq(Contract::getCustomerId, customerId)
                .eq(Contract::getTenantId, tenantId)
                .orderByDesc(Contract::getSignTime)
                .list();
    }

    @Override
    public List<Contract> getByHouseId(Long houseId) {
        ValidateUtil.notNull(houseId, "房源ID不能为空");
        Long tenantId = TenantContext.getTenantId();
        ValidateUtil.notNull(tenantId, "租户上下文获取失败");

        return baseMapper.selectList(new LambdaQueryWrapper<Contract>()
                .eq(Contract::getHouseId, houseId)
                .eq(Contract::getTenantId, tenantId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeContract(Long contractId) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BusinessException(400, "租户上下文获取失败");
        }

        // 1. 校验合同存在性及租户归属
        Contract contract = getByIdWithTenant(contractId);
        if (contract == null) {
            throw new BusinessException(404, "合同不存在或无权访问");
        }

        contractValidationUtil.validateNoDependenciesBeforeDelete(contractId);
        if ("RENT".equals(contract.getContractType())) {
            ContractLeaseTerms terms = contractValidationUtil.validateContractLeaseTerms(contractId, tenantId);
            if (terms != null) {
                throw new BusinessException(400, "租赁合同存在附加条款，无法删除");
            }
        }

        // 3. 检查合同状态（终态合同不允许删除）
        if ("COMPLETED".equals(contract.getStatus()) || "TERMINATED".equals(contract.getStatus())) {
            throw new BusinessException(400, "已完成或已终止的合同不允许删除");
        }

        // 4. 删除合同
        return removeById(contractId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateContract(Contract contract) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BusinessException(400, "租户上下文获取失败");
        }
        Long contractId = contract.getId();
        if (contractId == null) {
            throw new BusinessException(400, "合同ID不能为空");
        }

        // 1. 校验合同存在性及租户归属
        Contract existContract = getByIdWithTenant(contractId);
        if (existContract == null) {
            throw new BusinessException(404, "合同不存在或无权访问");
        }

        // 2. 禁止更新状态字段（状态更新通过专门接口）
        if (contract.getStatus() != null) {
            throw new BusinessException(400, "不允许通过此接口更新合同状态，请使用状态更新接口");
        }

        // 3. 填充不可修改字段（防止篡改）
        contract.setTenantId(tenantId);
        contract.setContractNo(existContract.getContractNo()); // 合同编号不可修改
        contract.setUpdateTime(LocalDateTime.now());

        // 4. 更新非状态字段
        return updateById(contract);
    }

    @Override
    public Contract getByIdWithTenant(Long contractId) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BusinessException(400, "租户上下文获取失败");
        }
        // 仅查询当前租户的合同
        return baseMapper.selectOne(new LambdaQueryWrapper<Contract>()
                .eq(Contract::getId, contractId)
                .eq(Contract::getTenantId, tenantId));
    }

    /**
     * 创建合同（包含房源/客户校验、合同编号生成、linkVisitRecords）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createContract(Contract contract) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BusinessException(400, "租户上下文获取失败");
        }

        // 1. 校验房源合法性（存在且归属当前租户）
        House house = houseService.getById(contract.getHouseId());
        if (house == null || !house.getTenantId().equals(tenantId)) {
            throw new BusinessException(404, "房源不存在或不属于当前租户");
        }

        // 2. 校验客户合法性（存在且归属当前租户）
        Customer customer = customerService.getById(contract.getCustomerId());
        if (customer == null || !customer.getTenantId().equals(tenantId)) {
            throw new BusinessException(404, "客户不存在或不属于当前租户");
        }

        // 3. 生成租户内唯一合同编号（优化：增加自增序列减少重复风险）
        String contractNo = generateContractNo(tenantId);
        contract.setContractNo(contractNo);
        contract.setTenantId(tenantId); // 确保租户ID正确设置
        contract.setCreateTime(LocalDateTime.now());
        contract.setUpdateTime(LocalDateTime.now());

        // 4. 保存合同
        boolean saveSuccess = save(contract);
        if (!saveSuccess) {
            log.error("合同创建失败，合同信息：{}", contract);
            throw new BusinessException(500, "合同创建失败");
        }

        // 5. 关联最近一次带看记录（客户+房源维度）
        linkVisitRecords(contract, tenantId);

        return true;
    }

    /**
     * 合同状态流转（包含房源状态自动更新、带看记录关联）
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean updateContractStatus(Long contractId, String targetStatus) {
        // 1. 校验合同存在性
        Contract contract = getById(contractId);
        if (contract == null) {
            throw new BusinessException(404, "合同不存在");
        }

        // 2. 校验状态流转合法性
        validateStatusTransition(contract.getStatus(), targetStatus);

        // 3. 签约状态特殊处理（更新房源状态+linkVisitRecords）
        if ("SIGNED".equals(targetStatus) && !"SIGNED".equals(contract.getStatus())) {
            // 更新房源状态（买卖→已售，租赁→已租）
            updateHouseStatusAfterSign(contract);
            // linkVisitRecords（若创建时未关联成功）
            linkVisitRecords(contract, contract.getTenantId());
        }

        // 4. 更新合同状态
        contract.setStatus(targetStatus);
        contract.setUpdateTime(LocalDateTime.now());
        return updateById(contract);
    }

    /**
     * 合同签约后更新房源状态并记录日志
     */
    private void updateHouseStatusAfterSign(Contract contract) {
        Long houseId = contract.getHouseId();
        Long tenantId = contract.getTenantId();

        // 1. 再次校验房源归属（防并发问题）
        House house = houseService.getById(houseId);
        if (house == null || !house.getTenantId().equals(tenantId)) {
            throw new BusinessException(404, "房源不存在或不属于当前租户");
        }

        // 2. 确定目标状态（基于合同类型）
        HouseStatus targetHouseStatus;
        String changeReason;
        if ("SALE".equals(contract.getContractType())) {
            targetHouseStatus = HouseStatus.SOLD;
            changeReason = "客户签约买卖合同（合同号：" + contract.getContractNo() + "）";
        } else if ("RESERVED".equals(contract.getContractType())) {
            targetHouseStatus = HouseStatus.RESERVED;
            changeReason = "客户签约租赁合同（合同号：" + contract.getContractNo() + "）";
        } else {
            throw new BusinessException(400, "不支持的合同类型：" + contract.getContractType());
        }

        // 3. 避免重复更新
        if (targetHouseStatus.equals(house.getStatus())) {
            log.info("房源ID={}已处于{}状态，无需更新", houseId, targetHouseStatus);
            return;
        }

        // 4. 更新房源状态
        HouseStatus oldStatus = house.getStatus();
        house.setStatus(targetHouseStatus);
        house.setUpdateTime(LocalDateTime.now());
        boolean updateSuccess = houseService.updateById(house);
        if (!updateSuccess) {
            throw new BusinessException(500, "更新房源状态失败，房源ID：" + houseId);
        }

        // 5. 记录状态变更日志
        HouseStatusLog statusLog = new HouseStatusLog();
        statusLog.setTenantId(tenantId);
        statusLog.setHouseId(houseId);
        statusLog.setStatusBefore(oldStatus.name());
        statusLog.setStatusAfter(targetHouseStatus.name());
        statusLog.setChangeReason(changeReason);
        statusLog.setOperatorId(contract.getAgentId());
        statusLog.setOperatorName(getAgentName(contract.getAgentId()));
        statusLog.setCreateTime(LocalDateTime.now());
        houseStatusLogService.save(statusLog);
    }

    /**
     * 关联客户-房源的最近一次带看记录
     */
    private void linkVisitRecords(Contract contract, Long tenantId) {
        VisitRecord latestVisit = visitRecordService.getOne(
                new LambdaQueryWrapper<VisitRecord>()
                        .eq(VisitRecord::getTenantId, tenantId)
                        .eq(VisitRecord::getCustomerId, contract.getCustomerId())
                        .eq(VisitRecord::getHouseId, contract.getHouseId())
                        .orderByDesc(VisitRecord::getVisitTime)
                        .last("LIMIT 1")
        );

        if (latestVisit != null && latestVisit.getContractId() == null) {
            latestVisit.setContractId(contract.getId());
            visitRecordService.updateById(latestVisit);
            log.info("带看记录ID={}关联合同成功，合同ID：{}", latestVisit.getId(), contract.getId());
        }
    }

    /**
     * 校验状态流转合法性（补充初始状态支持）
     */
    private void validateStatusTransition(String currentStatus, String targetStatus) {
        List<String> validTransitions = switch (currentStatus) {
            case "DRAFT" -> List.of("SIGNED", "TERMINATED"); // 草稿可签约或终止
            case "SIGNED" -> List.of("EXECUTING", "TERMINATED"); // 已签约可执行或终止
            case "EXECUTING" -> List.of("COMPLETED", "TERMINATED"); // 执行中可完成或终止
            case "COMPLETED", "TERMINATED" -> List.of(); // 终态不可流转
            default -> List.of();
        };

        if (!validTransitions.contains(targetStatus)) {
            throw new BusinessException(400,
                    String.format("不允许从[%s]状态流转至[%s]状态", currentStatus, targetStatus));
        }
    }

    /**
     * 生成租户内唯一合同编号（优化：日期+自增序号减少重复）
     */
    private String generateContractNo(Long tenantId) {
        // 实际生产环境建议使用数据库自增序列或Redis生成序号
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String seq = String.format("%03d", RandomUtil.randomInt(1, 999));
        return String.format("T%d_CONTRACT%s%s", tenantId, dateStr, seq);
    }

    /**
     * 获取经纪人姓名
     */
    private String getAgentName(Long agentId) {
        if (agentId == null) {
            return "未知";
        }
        Agent agent = agentService.getById(agentId);
        return agent != null ? agent.getName() : "未知";
    }
}
package com.house.deed.pavilion.module.contract.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.common.aspect.annotation.AgentDataPermission;
import com.house.deed.pavilion.common.exception.BusinessException;
import com.house.deed.pavilion.common.redis.RedisSequenceGenerator;
import com.house.deed.pavilion.common.util.*;
import com.house.deed.pavilion.module.agent.entity.Agent;
import com.house.deed.pavilion.module.agent.service.IAgentService;
import com.house.deed.pavilion.module.contract.entity.Contract;
import com.house.deed.pavilion.module.contract.mapper.ContractMapper;
import com.house.deed.pavilion.module.contract.service.IContractService;
import com.house.deed.pavilion.module.contract.vo.ContractDetailVO;
import com.house.deed.pavilion.module.contractAttachment.entity.ContractAttachment;
import com.house.deed.pavilion.module.contractAttachment.mapper.ContractAttachmentMapper;
import com.house.deed.pavilion.module.contractLeaseTerms.entity.ContractLeaseTerms;
import com.house.deed.pavilion.module.customer.entity.Customer;
import com.house.deed.pavilion.module.customer.service.ICustomerService;
import com.house.deed.pavilion.module.house.entity.House;
import com.house.deed.pavilion.module.house.repository.HouseStatus;
import com.house.deed.pavilion.module.house.service.IHouseService;
import com.house.deed.pavilion.module.houseHandover.dto.HouseHandoverDTO;
import com.house.deed.pavilion.module.houseHandover.entity.HouseHandover;
import com.house.deed.pavilion.module.houseHandover.service.IHouseHandoverService;
import com.house.deed.pavilion.module.houseStatusLog.entity.HouseStatusLog;
import com.house.deed.pavilion.module.houseStatusLog.service.IHouseStatusLogService;
import com.house.deed.pavilion.module.visitRecord.entity.VisitRecord;
import com.house.deed.pavilion.module.visitRecord.service.IVisitRecordService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
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
    private IVisitRecordService visitRecordService;
    @Resource
    private IAgentService agentService;
    @Resource
    private ContractValidationUtil contractValidationUtil;
    @Resource
    private ContractAttachmentMapper contractAttachmentMapper;
    @Resource
    private RedisSequenceGenerator redisSequenceGenerator;
    @Autowired
    @Lazy
    private IHouseHandoverService houseHandoverService;

    @Override
    @AgentDataPermission(
            operation = AgentDataPermission.OperationType.QUERY,
            entityClass = Contract.class,
            dataIdParam = "contractId",
            creatorField = "agentId"
    )
    public ContractDetailVO getDetailWithAttachments(Long contractId) {
        // 权限校验由注解自动完成，直接查询合同
        Contract contract = getById(contractId);
        ContractDetailVO vo = new ContractDetailVO();
        BeanUtils.copyProperties(contract, vo);

        // 附件查询自动继承租户隔离（通过注解切面）
        List<ContractAttachment> attachments = contractAttachmentMapper.selectList(
                new LambdaQueryWrapper<ContractAttachment>()
                        .eq(ContractAttachment::getContractId, contractId)
        );
        vo.setAttachments(attachments);
        return vo;
    }

    @Override
    @AgentDataPermission(
            operation = AgentDataPermission.OperationType.QUERY,
            entityClass = Contract.class,
            creatorField = "agentId"
    )
    public List<Contract> getByCustomerId(Long customerId, Long tenantId) {
        // 租户隔离和权限过滤由注解自动处理
        return lambdaQuery()
                .eq(Contract::getCustomerId, customerId)
                .orderByDesc(Contract::getSignTime)
                .list();
    }

    @Override
    @AgentDataPermission(
            operation = AgentDataPermission.OperationType.QUERY,
            entityClass = Contract.class,
            creatorField = "agentId"
    )
    public List<Contract> getByHouseId(Long houseId) {
        ValidateUtil.notNull(houseId, "房源ID不能为空");
        Long currentAgentId = AgentContext.getAgentId();
        ValidateUtil.notNull(currentAgentId, "经纪人上下文获取失败");

        // 房源权限校验（保留，与房源模块权限逻辑对齐）
        House house = houseService.getById(houseId);
        if (house == null) {
            throw new BusinessException(404, "房源不存在");
        }
        if (!house.getCreateAgentId().equals(currentAgentId)) {
            throw new BusinessException(403, "无权访问该房源的合同：仅房源创建人可查看");
        }

        // 合同查询自动添加租户和权限过滤
        return baseMapper.selectList(new LambdaQueryWrapper<Contract>()
                .eq(Contract::getHouseId, houseId)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @AgentDataPermission(
            operation = AgentDataPermission.OperationType.DELETE,
            entityClass = Contract.class,
            dataIdParam = "contractId",
            creatorField = "agentId"
    )
    public boolean removeContract(Long contractId) {
        // 权限校验由注解完成，直接获取合同
        Contract contract = getById(contractId);

        // 新增：角色权限校验（仅管理员/店长可删除）
        if (!RoleUtil.isAdmin() && !RoleUtil.isStoreManager()) {
            throw new BusinessException(403, "无权删除合同：仅管理员或店长可操作");
        }

        // 原有业务校验保留
        contractValidationUtil.validateNoDependenciesBeforeDelete(contractId);
        if ("RENT".equals(contract.getContractType())) {
            ContractLeaseTerms terms = contractValidationUtil.validateContractLeaseTerms(contractId, contract.getTenantId());
            if (terms != null) {
                throw new BusinessException(400, "租赁合同存在附加条款，无法删除");
            }
        }

        // 原有状态校验保留
        if ("COMPLETED".equals(contract.getStatus()) || "TERMINATED".equals(contract.getStatus())) {
            throw new BusinessException(400, "已完成或已终止的合同不允许删除");
        }

        return removeById(contractId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @AgentDataPermission(
            operation = AgentDataPermission.OperationType.UPDATE,
            entityClass = Contract.class,
            dataIdParam = "contract.id",
            creatorField = "agentId"
    )
    public boolean updateContract(Contract contract) {
        Long contractId = contract.getId();
        ValidateUtil.notNull(contractId, "合同ID不能为空");

        // 权限校验由注解完成，直接获取合同
        Contract existContract = getById(contractId);

        // 状态更新限制保留
        if (contract.getStatus() != null) {
            throw new BusinessException(400, "不允许通过此接口更新合同状态，请使用状态更新接口");
        }

        // 不可修改字段保护
        contract.setTenantId(existContract.getTenantId());
        contract.setContractNo(existContract.getContractNo());
        contract.setUpdateTime(LocalDateTime.now());

        return updateById(contract);
    }

    @Override
    @AgentDataPermission(
            operation = AgentDataPermission.OperationType.QUERY,
            entityClass = Contract.class,
            dataIdParam = "contractId",
            creatorField = "agentId"
    )
    public Contract getByIdWithTenant(Long contractId) {
        // 权限和租户过滤由注解自动处理
        return getById(contractId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @AgentDataPermission(
            operation = AgentDataPermission.OperationType.CREATE,
            entityClass = Contract.class,
            creatorField = "agentId"
    )
    public boolean createContract(Contract contract) {
        Long tenantId = TenantContext.getTenantId();
        ValidateUtil.notNull(tenantId, "租户上下文获取失败");

        // 房源合法性校验保留
        House house = houseService.getById(contract.getHouseId());
        if (house == null || !house.getTenantId().equals(tenantId)) {
            throw new BusinessException(404, "房源不存在或不属于当前租户");
        }

        // 客户合法性校验保留
        Customer customer = customerService.getById(contract.getCustomerId());
        if (customer == null || !customer.getTenantId().equals(tenantId)) {
            throw new BusinessException(404, "客户不存在或不属于当前租户");
        }

        // 合同编号生成
        String contractNo = generateContractNo(tenantId);
        contract.setContractNo(contractNo);
        contract.setTenantId(tenantId);
        contract.setCreateTime(LocalDateTime.now());
        contract.setUpdateTime(LocalDateTime.now());
        contract.setAgentId(AgentContext.getAgentId()); // 创建者自动绑定当前经纪人

        boolean saveSuccess = save(contract);
        if (!saveSuccess) {
            log.error("合同创建失败，合同信息：{}", contract);
            throw new BusinessException(500, "合同创建失败");
        }

        linkVisitRecords(contract, tenantId);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @AgentDataPermission(
            operation = AgentDataPermission.OperationType.UPDATE,
            entityClass = Contract.class,
            dataIdParam = "contractId",
            creatorField = "agentId"
    )
    public boolean updateContractStatus(Long contractId, String targetStatus) {
        // 权限校验由注解完成，直接获取合同
        Contract contract = getById(contractId);

        // 状态流转校验保留
        validateStatusTransition(contract.getStatus(), targetStatus);

        // 新增：若目标状态为终止（TERMINATED），校验角色权限
        if ("TERMINATED".equals(targetStatus)) {
            if (!RoleUtil.isAdmin() && !RoleUtil.isStoreManager()) {
                throw new BusinessException(403, "无权终止合同：仅管理员或店长可操作");
            }
        }

        // 原有签约状态处理保留
        if ("SIGNED".equals(targetStatus) && !"SIGNED".equals(contract.getStatus())) {
            updateHouseStatusAfterSign(contract);
            linkVisitRecords(contract, contract.getTenantId());
        }

        // 原有退租记录校验保留
        if (("TERMINATED".equals(targetStatus) || "COMPLETED".equals(targetStatus))
                && "RENT".equals(contract.getContractType())) {

            HouseHandover latestCheckOut = houseHandoverService
                    .getLatestCheckOutByHouseAndContract(contract.getHouseId(), contractId);

            if (latestCheckOut == null) {
                throw new BusinessException(400, "未查询到退租交接记录，无法终止/完成合同");
            }
            if (!"CONFIRMED".equals(latestCheckOut.getStatus())) {
                throw new BusinessException(400, "退租交接未确认，无法终止/完成合同");
            }
        }

        // 原有生成退租草稿逻辑保留
        if ("TERMINATED".equals(targetStatus) && "RENT".equals(contract.getContractType())) {
            generateCheckoutHandoverDraft(contract);
        }

        // 更新状态
        contract.setStatus(targetStatus);
        contract.setUpdateTime(LocalDateTime.now());
        return updateById(contract);
    }

    // 以下为原有工具方法，保持不变
    private void generateCheckoutHandoverDraft(Contract contract) {
        HouseHandoverDTO draft = new HouseHandoverDTO();
        draft.setContractId(contract.getId());
        draft.setHouseId(contract.getHouseId());
        draft.setHandoverType("CHECK_OUT");
        draft.setStatus("DRAFT");
        draft.setHandoverTime(LocalDateTime.now());
        houseHandoverService.createHandover(draft);
    }

    private void updateHouseStatusAfterSign(Contract contract) {
        Long houseId = contract.getHouseId();
        Long tenantId = contract.getTenantId();

        House house = houseService.getById(houseId);
        if (house == null || !house.getTenantId().equals(tenantId)) {
            throw new BusinessException(404, "房源不存在或不属于当前租户");
        }

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

        if (targetHouseStatus.equals(house.getStatus())) {
            log.info("房源ID={}已处于{}状态，无需更新", houseId, targetHouseStatus);
            return;
        }

        HouseStatus oldStatus = house.getStatus();
        house.setStatus(targetHouseStatus);
        house.setUpdateTime(LocalDateTime.now());
        boolean updateSuccess = houseService.updateById(house);
        if (!updateSuccess) {
            throw new BusinessException(500, "更新房源状态失败，房源ID：" + houseId);
        }

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

    private void validateStatusTransition(String currentStatus, String targetStatus) {
        List<String> validTransitions = switch (currentStatus) {
            case "DRAFT" -> List.of("SIGNED", "TERMINATED");
            case "SIGNED" -> List.of("EXECUTING", "TERMINATED");
            case "EXECUTING" -> List.of("COMPLETED", "TERMINATED");
            case "COMPLETED", "TERMINATED" -> List.of();
            default -> List.of();
        };

        if (!validTransitions.contains(targetStatus)) {
            throw new BusinessException(400,
                    String.format("不允许从[%s]状态流转至[%s]状态", currentStatus, targetStatus));
        }
    }

    private String generateContractNo(Long tenantId) {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int seq = redisSequenceGenerator.getDailySequence("contract", tenantId);
        String seqStr = String.format("%03d", seq);
        return String.format("T%d_CONTRACT%s%s", tenantId, dateStr, seqStr);
    }

    private String getAgentName(Long agentId) {
        if (agentId == null) {
            return "未知";
        }
        Agent agent = agentService.getById(agentId);
        return agent != null ? agent.getName() : "未知";
    }
}
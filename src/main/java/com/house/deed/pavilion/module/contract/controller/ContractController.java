package com.house.deed.pavilion.module.contract.controller;

import com.house.deed.pavilion.common.dto.ResultDTO;
import com.house.deed.pavilion.common.exception.BusinessException;
import com.house.deed.pavilion.common.util.AgentContext;
import com.house.deed.pavilion.common.util.TenantContext;
import com.house.deed.pavilion.module.agentPerformance.service.IAgentPerformanceService;
import com.house.deed.pavilion.module.contract.entity.Contract;
import com.house.deed.pavilion.module.contract.service.IContractService;
import com.house.deed.pavilion.module.contract.vo.ContractDetailVO;
import com.house.deed.pavilion.module.customerFollowUp.entity.CustomerFollowUp;
import com.house.deed.pavilion.module.customerFollowUp.service.ICustomerFollowUpService;
import com.house.deed.pavilion.module.houseHandover.entity.HouseHandover;
import com.house.deed.pavilion.module.houseHandover.service.IHouseHandoverService;
import com.house.deed.pavilion.module.visitRecord.entity.VisitRecord;
import com.house.deed.pavilion.module.visitRecord.service.IVisitRecordService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 合同控制器
 */
@RestController
@RequestMapping("/module/contract")
@Slf4j
public class ContractController {

    @Resource
    private IContractService contractService;

    @Resource
    private ICustomerFollowUpService customerFollowUpService;

    @Autowired
    @Lazy
    private IHouseHandoverService houseHandoverService;

    @Resource
    private IVisitRecordService visitRecordService;

    @Autowired
    @Lazy
    private IAgentPerformanceService agentPerformanceService;

    @GetMapping("/detail/{id}")
    @Operation(summary = "查询合同详情（含关联附件）")
    public ResultDTO<ContractDetailVO> getDetailWithAttachments(@PathVariable Long id) {
        ContractDetailVO detailVO = contractService.getDetailWithAttachments(id);
        return ResultDTO.success(detailVO);
    }

    /**
     * 通过房源ID查询关联合同
     */
    @GetMapping("/by-house/{houseId}")
    public ResultDTO<List<Contract>> getByHouseId(@PathVariable Long houseId) {
        List<Contract> contracts = contractService.getByHouseId(houseId);
        return ResultDTO.success(contracts);
    }

    /**
     * 删除合同
     */
    @DeleteMapping("/{id}")
    public ResultDTO<Boolean> deleteContract(@PathVariable Long id) {
        boolean success = contractService.removeContract(id);
        return ResultDTO.success(success);
    }

    /**
     * 更新合同非状态字段（金额、付款方式等）
     */
    @PutMapping("/{id}")
    public ResultDTO<Boolean> updateContract(
            @PathVariable Long id,
            @RequestBody Contract contract) {
        // 校验ID一致性
        if (!id.equals(contract.getId())) {
            throw new BusinessException(400, "路径ID与请求体ID不匹配");
        }
        boolean success = contractService.updateContract(contract);
        return ResultDTO.success(success);
    }

    /**
     * 创建交易合同（自动校验房源和客户归属）
     */
    @PostMapping("/createContract")
    public ResultDTO<Boolean> createContract(@RequestBody Contract contract) {
        // 校验必填字段
        if (contract.getHouseId() == null || contract.getCustomerId() == null) {
            throw new BusinessException(400, "房源ID和客户ID不能为空");
        }
        boolean success = contractService.createContract(contract);
        return ResultDTO.success(success);
    }

    /**
     * 查询合同详情（补充租户隔离）
     */
    @GetMapping("/{id}")
    public ResultDTO<Contract> getContractById(@PathVariable Long id) {
        Long tenantId = TenantContext.getTenantId();
        Long currentAgentId = AgentContext.getAgentId();

        Contract contract = contractService.getById(id);
        if (contract == null || !contract.getTenantId().equals(tenantId)) {
            throw new BusinessException(404, "合同不存在或无权访问");
        }
        // 校验是否为签约经纪人
        if (!contract.getAgentId().equals(currentAgentId)) {
            throw new BusinessException(403, "无权访问非本人签约的合同");
        }
        return ResultDTO.success(contract);
    }

    // ContractController.java
    @GetMapping("/{contractId}/latest-checkout")
    @Operation(summary = "查询合同最新退租记录", description = "获取指定租赁合同的最新退租交接信息")
    public ResultDTO<HouseHandover> getContractLatestCheckout(@PathVariable Long contractId) {
        Contract contract = contractService.getById(contractId);
        if (contract == null) {
            throw new BusinessException(404, "合同不存在");
        }
        // 仅允许查询租赁合同的退租记录
        if (!"RENT".equals(contract.getContractType())) {
            throw new BusinessException(400, "仅租赁合同支持查询退租记录");
        }
        // 调用方法查询
        HouseHandover latestCheckout = houseHandoverService
                .getLatestCheckOutByHouseAndContract(contract.getHouseId(), contractId);
        return ResultDTO.success(latestCheckout);
    }

    /**
     * 更新合同状态为签约
     */
    @PatchMapping("/{id}/status")
    public ResultDTO<Boolean> signContract(@PathVariable Long id) {
        // 1. 查询合同并校验状态
        Contract contract = contractService.getById(id);
        if (contract == null) {
            throw new BusinessException(404, "合同不存在");
        }
        if ("SIGNED".equals(contract.getStatus())) {
            throw new BusinessException(400, "合同已签约");
        }

        // 2. 更新合同状态为签约
        contract.setStatus("SIGNED");
        boolean updated = contractService.updateById(contract);
        if (!updated) {
            throw new BusinessException(500, "合同状态更新失败");
        }

        // 3. 查询该合同关联的带看记录（取最新一条有效带看）
        List<VisitRecord> visitRecords = visitRecordService.getByContractId(id, contract.getTenantId());
        if (visitRecords.isEmpty()) {
            log.warn("合同{}签约但未找到关联带看记录，不生成业绩", id);
            return ResultDTO.success(true);
        }
        VisitRecord latestVisit = visitRecords.get(0); // 假设按时间排序取最新

        // 4. 生成业绩（使用合同实际金额计算）
        // 假设合同金额字段为dealAmount（万元），佣金比例为1%
        BigDecimal dealAmount = contract.getAmount(); // 从合同中获取成交金额
        BigDecimal commissionAmount = dealAmount.multiply(new BigDecimal("10000")) // 转换为元
                .multiply(new BigDecimal("0.01")); // 1%佣金比例

        agentPerformanceService.createPerformanceFromVisit(
                latestVisit,
                id, // 合同ID
                dealAmount, // 成交金额（万元）
                commissionAmount // 佣金金额（元）
        );

        return ResultDTO.success(true);
    }

    // 新增：查询合同关联的带看记录（包含VisitRecord和CustomerFollowUp）
    @GetMapping("/{contractId}/visit-records")
    public ResultDTO<ContractVisitRecordsVO> getVisitRecordsByContractId(@PathVariable Long contractId) {
        Contract contract = contractService.getById(contractId);
        if (contract == null) {
            throw new BusinessException(404, "合同不存在");
        }
        Long tenantId = contract.getTenantId();

        // 查询带看记录
        List<VisitRecord> visitRecords = visitRecordService.getByContractId(contractId, tenantId);
        // 查询跟进记录
        List<CustomerFollowUp> followUps = customerFollowUpService.getByContractId(contractId, tenantId);

        ContractVisitRecordsVO result = new ContractVisitRecordsVO();
        result.setContractId(contractId);
        result.setVisitRecords(visitRecords);
        result.setFollowUps(followUps);
        return ResultDTO.success(result);
    }

    // 内部VO类：封装合同关联的带看记录
    @Setter
    @Getter
    public static class ContractVisitRecordsVO {
        // getter/setter
        private Long contractId;
        private List<VisitRecord> visitRecords;
        private List<CustomerFollowUp> followUps;

    }
}
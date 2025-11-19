package com.house.deed.pavilion.module.contract.controller;

import com.house.deed.pavilion.common.dto.ResultDTO;
import com.house.deed.pavilion.common.exception.BusinessException;
import com.house.deed.pavilion.common.util.AgentContext;
import com.house.deed.pavilion.common.util.TenantContext;
import com.house.deed.pavilion.module.contract.entity.Contract;
import com.house.deed.pavilion.module.contract.service.IContractService;
import com.house.deed.pavilion.module.contract.vo.ContractDetailVO;
import com.house.deed.pavilion.module.customerFollowUp.entity.CustomerFollowUp;
import com.house.deed.pavilion.module.customerFollowUp.service.ICustomerFollowUpService;
import com.house.deed.pavilion.module.house.service.IHouseService;
import com.house.deed.pavilion.module.houseHandover.entity.HouseHandover;
import com.house.deed.pavilion.module.houseHandover.service.IHouseHandoverService;
import com.house.deed.pavilion.module.visitRecord.entity.VisitRecord;
import com.house.deed.pavilion.module.visitRecord.service.IVisitRecordService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 合同控制器
 */
@RestController
@RequestMapping("/module/contract")
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
     * 更新合同状态
     */
    @PatchMapping("/{id}/status")
    public ResultDTO<Boolean> updateContractStatus(
            @PathVariable Long id,
            @RequestParam String targetStatus) {

        // 校验状态参数合法性
        List<String> validStatus = List.of("SIGNED", "EXECUTING", "COMPLETED", "TERMINATED");
        if (!validStatus.contains(targetStatus)) {
            throw new BusinessException(400, "状态只能是SIGNED/EXECUTING/COMPLETED/TERMINATED");
        }

        boolean success = contractService.updateContractStatus(id, targetStatus);
        return ResultDTO.success(success);
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
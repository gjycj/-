package com.house.deed.pavilion.module.customerFollowUp.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.house.deed.pavilion.common.dto.ResultDTO;
import com.house.deed.pavilion.common.exception.BusinessException;
import com.house.deed.pavilion.common.util.TenantContext;
import com.house.deed.pavilion.module.agent.entity.Agent;
import com.house.deed.pavilion.module.agent.service.IAgentService;
import com.house.deed.pavilion.module.contract.entity.Contract;
import com.house.deed.pavilion.module.contract.service.impl.ContractServiceImpl;
import com.house.deed.pavilion.module.customer.entity.Customer;
import com.house.deed.pavilion.module.customer.service.ICustomerService;
import com.house.deed.pavilion.module.customerFollowUp.entity.CustomerFollowUp;
import com.house.deed.pavilion.module.customerFollowUp.service.ICustomerFollowUpService;
import com.house.deed.pavilion.module.visitRecord.entity.VisitRecord;
import com.house.deed.pavilion.module.visitRecord.service.impl.VisitRecordServiceImpl;
import io.micrometer.common.util.StringUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 客户跟进记录表（租户级数据） 前端控制器
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
@RestController
@RequestMapping("/module/customerFollowUp")
@Slf4j
public class CustomerFollowUpController {


    @Resource
    private ICustomerFollowUpService customerFollowUpService;

    @Resource
    private ICustomerService customerService;

    @Resource
    private VisitRecordServiceImpl visitRecordService;

    @Resource
    private ContractServiceImpl contractService;

    @Resource
    private IAgentService agentService;

    // 新增：通过合同ID查询带看记录
    @GetMapping("/by-contract")
    public ResultDTO<List<CustomerFollowUp>> getByContractId(@RequestParam Long contractId) {
        Long tenantId = TenantContext.getTenantId();
        List<CustomerFollowUp> followUps = customerFollowUpService.getByContractId(contractId, tenantId);
        return ResultDTO.success(followUps);
    }

    // 2. 查询单个跟进记录
    @GetMapping("/{id}")
    public ResultDTO<CustomerFollowUp> getById(@PathVariable Long id) {
        CustomerFollowUp followUp = customerFollowUpService.getById(id);
        if (followUp == null || !followUp.getTenantId().equals(TenantContext.getTenantId())) {
            throw new BusinessException(404, "跟进记录不存在或无权访问");
        }
        return ResultDTO.success(followUp);
    }

    // 新增跟进记录
    @PostMapping("/addFollowUp")
    public ResultDTO<Boolean> addFollowUp(@RequestBody CustomerFollowUp followUp) {
        // 1. 基础非空校验
        if (followUp.getCustomerId() == null) {
            throw new BusinessException(400, "客户ID不能为空");
        }
        if (followUp.getAgentId() == null) {
            throw new BusinessException(400, "经纪人ID不能为空");
        }
        if (followUp.getFollowTime() == null) {
            throw new BusinessException(400, "跟进时间不能为空");
        }
        if (StringUtils.isBlank(followUp.getContent())) {
            throw new BusinessException(400, "跟进内容不能为空");
        }

        // 2. 租户隔离校验（确保操作当前租户数据）
        Long currentTenantId = TenantContext.getTenantId();
        if (followUp.getTenantId() == null || !followUp.getTenantId().equals(currentTenantId)) {
            throw new BusinessException(403, "租户信息不匹配，禁止操作");
        }

        // 3. 关联客户合法性校验（存在且属于当前租户）
        Customer customer = customerService.getById(followUp.getCustomerId());
        if (customer == null) {
            throw new BusinessException(404, "客户不存在");
        }
        if (!customer.getTenantId().equals(currentTenantId)) {
            throw new BusinessException(403, "无权访问该客户");
        }

        // 4. 关联经纪人合法性校验（存在、在职且属于当前租户）
        Agent agent = agentService.getById(followUp.getAgentId());
        if (agent == null) {
            throw new BusinessException(404, "经纪人不存在");
        }
        if (!agent.getTenantId().equals(currentTenantId)) {
            throw new BusinessException(403, "无权访问该经纪人");
        }
        if (agent.getStatus() != 1) { // 1-在职（参考agent表status字段定义）
            throw new BusinessException(400, "经纪人状态异常（非在职），无法执行跟进");
        }

        // 5. 跟进时间合理性校验
        LocalDateTime now = LocalDateTime.now();
        // 跟进时间不能晚于当前时间（禁止记录未来的跟进）
        if (followUp.getFollowTime().isAfter(now)) {
            throw new BusinessException(400, "跟进时间不能晚于当前时间");
        }
        // 跟进时间不能早于客户创建时间（避免时序矛盾）
        if (followUp.getFollowTime().isBefore(customer.getCreateTime())) {
            throw new BusinessException(400, "跟进时间不能早于客户创建时间");
        }

        // 6. 业务参数长度校验
        if (followUp.getContent().length() > 2000) {
            throw new BusinessException(400, "跟进内容不能超过2000字");
        }
        if (followUp.getDemandChange() != null && followUp.getDemandChange().length() > 1000) {
            throw new BusinessException(400, "需求调整记录不能超过1000字");
        }
        if (followUp.getNextFollowPlan() != null && followUp.getNextFollowPlan().length() > 1000) {
            throw new BusinessException(400, "下次跟进计划不能超过1000字");
        }

        // 7. 自动填充创建时间（覆盖前端传入值，确保准确性）
        followUp.setCreateTime(now);

        // 8. 保存跟进记录
        Long tenantId = TenantContext.getTenantId();
        // 校验关联的带看记录（若存在）
        if (followUp.getVisitRecordId() != null) {
            VisitRecord visit = visitRecordService.getById(followUp.getVisitRecordId());
            if (visit == null || !visit.getTenantId().equals(tenantId)
                    || !visit.getCustomerId().equals(followUp.getCustomerId())) {
                throw new BusinessException(400, "关联的带看记录不存在或不匹配当前客户");
            }
        }

        // 校验关联的合同（若存在）
        if (followUp.getContractId() != null) {
            Contract contract = contractService.getById(followUp.getContractId());
            if (contract == null || !contract.getTenantId().equals(tenantId)
                    || !contract.getCustomerId().equals(followUp.getCustomerId())) {
                throw new BusinessException(400, "关联的合同不存在或不匹配当前客户");
            }
        }

        // 保存跟进记录
        boolean success = customerFollowUpService.saveWithTimeCheck(followUp);
        if (!success) {
            log.error("客户跟进记录保存失败：{}", followUp);
            throw new BusinessException(500, "跟进记录创建失败");
        }
        return ResultDTO.success(success);
    }

    // 4. 更新跟进记录
    @PutMapping("/{id}")
    public ResultDTO<Boolean> updateFollowUp(@PathVariable Long id, @RequestBody CustomerFollowUp followUp) {
        if (!id.equals(followUp.getId())) {
            throw new BusinessException(400, "ID不匹配");
        }
        // 校验数据归属
        CustomerFollowUp existing = customerFollowUpService.getById(id);
        if (existing == null || !existing.getTenantId().equals(TenantContext.getTenantId())) {
            throw new BusinessException(404, "跟进记录不存在或无权访问");
        }
        // 禁止修改租户ID
        followUp.setTenantId(existing.getTenantId());
        boolean updated = customerFollowUpService.updateById(followUp);
        return ResultDTO.success(updated);
    }

    // 5. 删除跟进记录
    @DeleteMapping("/{id}")
    public ResultDTO<Boolean> deleteFollowUp(@PathVariable Long id) {
        CustomerFollowUp existing = customerFollowUpService.getById(id);
        if (existing == null || !existing.getTenantId().equals(TenantContext.getTenantId())) {
            throw new BusinessException(404, "跟进记录不存在或无权访问");
        }
        boolean deleted = customerFollowUpService.removeById(id);
        return ResultDTO.success(deleted);
    }

    // 3. 查询客户的所有跟进记录（分页）
    @GetMapping("/customer/{customerId}")
    public ResultDTO<Page<CustomerFollowUp>> getByCustomerId(
            @PathVariable Long customerId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        Page<CustomerFollowUp> page = new Page<>(pageNum, pageSize);
        Page<CustomerFollowUp> resultPage = customerFollowUpService.getByCustomerId(page, customerId, TenantContext.getTenantId());
        return ResultDTO.success(resultPage);
    }

}

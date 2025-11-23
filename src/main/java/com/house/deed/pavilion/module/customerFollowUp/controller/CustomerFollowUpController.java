package com.house.deed.pavilion.module.customerFollowUp.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.house.deed.pavilion.common.dto.ResultDTO;
import com.house.deed.pavilion.common.exception.BusinessException;
import com.house.deed.pavilion.common.util.AgentContext;
import com.house.deed.pavilion.common.util.RoleUtil;
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

    // 1. 更新跟进记录：简化ID匹配校验（保留核心）
    @PutMapping("/{id}")
    public ResultDTO<Boolean> updateFollowUp(@PathVariable Long id, @RequestBody CustomerFollowUp followUp) {
        if (!id.equals(followUp.getId())) {
            throw new BusinessException(400, "路径ID与请求体ID不匹配");
        }
        // 直接调用Service（权限+业务校验由Service处理）
        boolean success = customerFollowUpService.updateFollowUp(followUp);
        return ResultDTO.success(success);
    }

    // 2. 按合同ID查询：简化租户校验（Service注解已处理）
    @GetMapping("/by-contract")
    public ResultDTO<List<CustomerFollowUp>> getByContractId(@RequestParam Long contractId) {
        Long tenantId = TenantContext.getTenantId();
        List<CustomerFollowUp> followUps = customerFollowUpService.getByContractId(contractId, tenantId);
        return ResultDTO.success(followUps);
    }

    // 3. 按ID查询：调用带权限的Service方法（移除冗余校验）
    @GetMapping("/{id}")
    public ResultDTO<CustomerFollowUp> getById(@PathVariable Long id) {
        // 直接调用Service带权限的查询方法
        CustomerFollowUp followUp = customerFollowUpService.getByIdWithPermission(id);
        return ResultDTO.success(followUp);
    }

    // 4. 新增跟进记录：保留业务校验+简化权限校验（注解覆盖创建人权限）
    @PostMapping("/addFollowUp")
    public ResultDTO<Boolean> addFollowUp(@RequestBody CustomerFollowUp followUp) {
        Long currentAgentId = AgentContext.getAgentId();
        Long currentTenantId = TenantContext.getTenantId();

        // 强制绑定：创建人=当前经纪人，租户=当前租户（防止篡改）
        followUp.setAgentId(currentAgentId);
        followUp.setTenantId(currentTenantId);

        // ---------------------- 核心业务校验（保留，注解不覆盖） ----------------------
        // （1）基础非空校验
        if (followUp.getCustomerId() == null) throw new BusinessException(400, "客户ID不能为空");
        if (StringUtils.isBlank(followUp.getContent())) throw new BusinessException(400, "跟进内容不能为空");
        if (followUp.getFollowTime() == null) followUp.setFollowTime(LocalDateTime.now()); // 默认当前时间

        // （2）客户合法性校验（存在+租户匹配）
        Customer customer = customerService.getById(followUp.getCustomerId());
        if (customer == null) throw new BusinessException(404, "客户不存在");
        if (!customer.getTenantId().equals(currentTenantId)) throw new BusinessException(403, "无权访问该客户");

        // （3）经纪人合法性校验（在职+租户匹配）
        Agent agent = agentService.getById(currentAgentId);
        if (agent == null || agent.getStatus() != 1) throw new BusinessException(400, "经纪人状态异常（非在职），无法跟进");
        if (!agent.getTenantId().equals(currentTenantId)) throw new BusinessException(403, "经纪人不属于当前租户");

        // （4）时间合理性校验（保留原有逻辑）
        LocalDateTime now = LocalDateTime.now();
        if (followUp.getFollowTime().isAfter(now)) throw new BusinessException(400, "跟进时间不能晚于当前时间");
        if (followUp.getFollowTime().isBefore(customer.getCreateTime())) throw new BusinessException(400, "跟进时间不能早于客户创建时间");

        // （5）字段长度校验
        if (followUp.getContent().length() > 2000) throw new BusinessException(400, "跟进内容不能超过2000字");
        if (followUp.getDemandChange() != null && followUp.getDemandChange().length() > 1000) throw new BusinessException(400, "需求调整记录不能超过1000字");
        if (followUp.getNextFollowPlan() != null && followUp.getNextFollowPlan().length() > 1000) throw new BusinessException(400, "下次跟进计划不能超过1000字");

        // （6）关联记录校验（带看/合同必须匹配客户+租户）
        if (followUp.getVisitRecordId() != null) {
            VisitRecord visit = visitRecordService.getById(followUp.getVisitRecordId());
            if (visit == null || !visit.getTenantId().equals(currentTenantId) || !visit.getCustomerId().equals(followUp.getCustomerId())) {
                throw new BusinessException(400, "关联的带看记录不存在或不匹配当前客户");
            }
        }
        if (followUp.getContractId() != null) {
            Contract contract = contractService.getById(followUp.getContractId());
            if (contract == null || !contract.getTenantId().equals(currentTenantId) || !contract.getCustomerId().equals(followUp.getCustomerId())) {
                throw new BusinessException(400, "关联的合同不存在或不匹配当前客户");
            }
        }
        // ---------------------- 业务校验结束 ----------------------

        // 自动填充创建时间
        followUp.setCreateTime(now);

        // 调用带权限+业务校验的Service方法
        boolean success = customerFollowUpService.saveWithTimeCheck(followUp);
        if (!success) {
            log.error("客户跟进记录保存失败：{}", followUp);
            throw new BusinessException(500, "跟进记录创建失败");
        }
        return ResultDTO.success(success);
    }

    // 5. 按客户ID分页查询：删除冗余权限校验（Service注解覆盖）
    @GetMapping("/customer/{customerId}")
    public ResultDTO<Page<CustomerFollowUp>> getByCustomerId(
            @PathVariable Long customerId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        Long tenantId = TenantContext.getTenantId();
        Long currentAgentId = AgentContext.getAgentId();

        // （1）客户基础校验（存在+租户匹配）
        Customer customer = customerService.getById(customerId);
        if (customer == null || !customer.getTenantId().equals(tenantId)) {
            throw new BusinessException(404, "客户不存在或无权访问");
        }

        // （2）角色权限增强：管理员/店长可查看所有，普通经纪人仅看自己的（保留）
        if (!RoleUtil.isAdmin() && !RoleUtil.isStoreManager()) {
            if (customer.getCreateAgentId() == null || !customer.getCreateAgentId().equals(currentAgentId)) {
                throw new BusinessException(403, "无权查看非本人创建的客户的跟进记录");
            }
        }

        // （3）调用Service带权限查询（自动过滤当前经纪人+租户，无需手动eq(AgentId)）
        Page<CustomerFollowUp> page = new Page<>(pageNum, pageSize);
        Page<CustomerFollowUp> resultPage = customerFollowUpService.getByCustomerId(page, customerId, tenantId);
        return ResultDTO.success(resultPage);
    }

    // 6. 删除跟进记录：简化校验（Service注解+校验全覆盖）
    @DeleteMapping("/{id}")
    public ResultDTO<Boolean> deleteFollowUp(@PathVariable Long id) {
        // 直接调用Service带权限的删除方法
        boolean deleted = customerFollowUpService.deleteFollowUp(id);
        return ResultDTO.success(deleted);
    }
}
package com.house.deed.pavilion.module.visitRecord.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.house.deed.pavilion.common.dto.ResultDTO;
import com.house.deed.pavilion.common.exception.BusinessException;
import com.house.deed.pavilion.common.util.AgentContext;
import com.house.deed.pavilion.common.util.TenantContext;
import com.house.deed.pavilion.module.agent.entity.Agent;
import com.house.deed.pavilion.module.agent.service.IAgentService;
import com.house.deed.pavilion.module.contract.entity.Contract;
import com.house.deed.pavilion.module.contract.service.impl.ContractServiceImpl;
import com.house.deed.pavilion.module.customer.entity.Customer;
import com.house.deed.pavilion.module.customer.service.ICustomerService;
import com.house.deed.pavilion.module.house.entity.House;
import com.house.deed.pavilion.module.house.service.IHouseService;
import com.house.deed.pavilion.module.visitRecord.entity.VisitRecord;
import com.house.deed.pavilion.module.visitRecord.service.IVisitRecordService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * 带看记录表（租户级数据） 前端控制器
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
@RestController
@RequestMapping("/module/visitRecord")
@Slf4j
public class VisitRecordController {

    @Resource
    private IVisitRecordService visitRecordService;

    @Resource
    private IHouseService houseService;

    @Resource
    private ICustomerService customerService;

    @Resource
    private ContractServiceImpl contractService;

    @Resource
    private IAgentService agentService;

    // 新增：删除带看记录接口
    @DeleteMapping("/{id}")
    public ResultDTO<Boolean> deleteVisitRecord(@PathVariable Long id) {
        Long tenantId = TenantContext.getTenantId();
        Long currentAgentId = AgentContext.getAgentId();

        VisitRecord record = visitRecordService.getById(id);
        if (record == null || !record.getTenantId().equals(tenantId)) {
            throw new BusinessException(404, "带看记录不存在或无权访问");
        }
        // 校验是否为自己创建的记录
        if (!record.getAgentId().equals(currentAgentId)) {
            throw new BusinessException(403, "无权删除他人创建的带看记录");
        }

        boolean success = visitRecordService.removeById(id);
        return ResultDTO.success(success);
    }

    /**
     * 通过房源ID查询带看记录
     */
    @GetMapping("/by-house")
    public ResultDTO<List<VisitRecord>> getByHouseId(@RequestParam Long houseId) {
        Long tenantId = TenantContext.getTenantId();
        Long currentAgentId = AgentContext.getAgentId();
        // 新增：查询条件添加agent_id = 当前经纪人ID
        List<VisitRecord> visitRecords = visitRecordService.lambdaQuery()
                .eq(VisitRecord::getHouseId, houseId)
                .eq(VisitRecord::getTenantId, tenantId)
                .eq(VisitRecord::getAgentId, currentAgentId) // 权限过滤
                .list();
        return ResultDTO.success(visitRecords);
    }

    // 新增：通过合同ID查询带看记录
    @GetMapping("/by-contract")
    public ResultDTO<List<VisitRecord>> getByContractId(@RequestParam Long contractId) {
        Long tenantId = TenantContext.getTenantId();
        List<VisitRecord> visitRecords = visitRecordService.getByContractId(contractId, tenantId);
        return ResultDTO.success(visitRecords);
    }

    @PostMapping("/createVisitRecord")
    public ResultDTO<Boolean> createVisitRecord(@RequestBody VisitRecord record) {
        // 1. 基础非空校验
        if (record.getHouseId() == null) {
            throw new BusinessException(400, "房源ID不能为空");
        }
        if (record.getCustomerId() == null) {
            throw new BusinessException(400, "客户ID不能为空");
        }
        if (record.getAgentId() == null) {
            throw new BusinessException(400, "经纪人ID不能为空");
        }

        if (record.getVisitTime() == null) {
            throw new BusinessException(400, "带看时间不能为空");
        }



        // 2. 租户隔离校验（确保操作当前租户数据）
        Long currentTenantId = TenantContext.getTenantId();
        if (record.getTenantId() == null || !record.getTenantId().equals(currentTenantId)) {
            throw new BusinessException(403, "租户信息不匹配，禁止操作");
        }

        Long currentAgentId = AgentContext.getAgentId(); // 获取当前经纪人ID
        // 新增：强制带看记录的agent_id为当前经纪人（覆盖前端传入值）
        record.setAgentId(currentAgentId);

        // 3. 关联房源合法性校验（存在且属于当前租户）
        House house = houseService.getById(record.getHouseId());
        if (house == null || !house.getTenantId().equals(currentTenantId)) {
            throw new BusinessException(404, "房源不存在或无权访问");
        }
        // 额外校验：房源状态必须为可带看状态（如在售/在租）
        if (!Arrays.asList("ON_SALE", "FOR_RENT").contains(house.getStatus().toString())) {
            throw new BusinessException(400, "房源状态不允许带看（当前状态：" + house.getStatus() + "）");
        }

        // 4. 关联客户合法性校验（存在且属于当前租户）
        Customer customer = customerService.getById(record.getCustomerId());
        if (customer == null || !customer.getTenantId().equals(currentTenantId)) {
            throw new BusinessException(404, "客户不存在或无权访问");
        }

        // 新增：校验客户状态是否为活跃
        if (!"ACTIVE".equals(customer.getStatus())) {
            throw new BusinessException(400, "客户状态非活跃（当前状态：" + customer.getStatus() + "），无法进行带看");
        }

        // 5. 关联经纪人合法性校验（存在、在职且属于当前租户）
        Agent agent = agentService.getById(record.getAgentId());
        if (agent == null || !agent.getTenantId().equals(currentTenantId)) {
            throw new BusinessException(404, "经纪人不存在或无权访问");
        }
        if (agent.getStatus() != 1) { // 1-在职
            throw new BusinessException(400, "经纪人状态异常（非在职），无法执行带看");
        }

        // 6. 带看时间合理性校验
        LocalDateTime now = LocalDateTime.now();
        if (record.getVisitTime().isAfter(now)) {
            throw new BusinessException(400, "带看时间不能晚于当前时间");
        }
        // 带看时间不能早于客户创建时间（避免逻辑矛盾）
        if (record.getVisitTime().isBefore(customer.getCreateTime())) {
            throw new BusinessException(400, "带看时间不能早于客户创建时间");
        }

        // 7. 带看方式合法性校验（枚举值校验）
        List<String> validVisitTypes = Arrays.asList("线下", "VR");
        if (record.getVisitType() != null && !validVisitTypes.contains(record.getVisitType())) {
            throw new BusinessException(400, "带看方式无效，允许值：" + String.join("/", validVisitTypes));
        }

        // 8. 意向程度范围校验（1-3）
        if (record.getIntentionLevel() != null
                && (record.getIntentionLevel() < 1 || record.getIntentionLevel() > 3)) {
            throw new BusinessException(400, "意向程度必须为1-3（1-低，2-中，3-高）");
        }

        // 9. 自动填充创建时间（覆盖前端传入值，确保准确性）
        record.setCreateTime(now);

        // 10. 保存带看记录
        boolean success = visitRecordService.save(record);
        if (!success) {
            log.error("带看记录保存失败：{}", record);
            throw new BusinessException(500, "带看记录创建失败");
        }
        return ResultDTO.success(true);
    }

    // 新增：通过带看记录ID查询后续签约的合同
    @GetMapping("/{visitId}/contract")
    public ResultDTO<Contract> getContractByVisitId(@PathVariable Long visitId) {
        Long tenantId = TenantContext.getTenantId();
        // 1. 校验带看记录归属
        VisitRecord visitRecord = visitRecordService.getById(visitId);
        if (visitRecord == null || !visitRecord.getTenantId().equals(tenantId)) {
            throw new BusinessException(404, "带看记录不存在或无权访问");
        }
        // 2. 查询关联的合同
        Contract contract = contractService.getOne(
                new LambdaQueryWrapper<Contract>()
                        .eq(Contract::getTenantId, tenantId)
                        .eq(Contract::getHouseId, visitRecord.getHouseId())
                        .eq(Contract::getCustomerId, visitRecord.getCustomerId())
                        .eq(Contract::getStatus, "SIGNED") // 只查询已签约合同
                        .orderByDesc(Contract::getSignTime)
                        .last("LIMIT 1")
        );
        return ResultDTO.success(contract);
    }

    // 查询客户的带看记录
    @GetMapping("/customer/{customerId}")
    public ResultDTO<List<VisitRecord>> getByCustomerId(@PathVariable Long customerId) {
        List<VisitRecord> records = visitRecordService.list(
                Wrappers.<VisitRecord>lambdaQuery().eq(VisitRecord::getCustomerId, customerId)
        );
        return ResultDTO.success(records);
    }


}

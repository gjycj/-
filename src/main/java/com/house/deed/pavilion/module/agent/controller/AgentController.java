package com.house.deed.pavilion.module.agent.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.house.deed.pavilion.common.dto.ResultDTO;
import com.house.deed.pavilion.common.exception.BusinessException;
import com.house.deed.pavilion.module.agent.entity.Agent;
import com.house.deed.pavilion.module.agent.service.IAgentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 经纪人信息控制器
 */
@RestController
@RequestMapping("/module/agent")
public class AgentController {

    @Autowired
    private IAgentService agentService;

    /**
     * 新增经纪人
     */
    @PostMapping
    public ResultDTO<Boolean> addAgent(@RequestBody Agent agent) {
        if (agent.getName() == null || agent.getPhone() == null || agent.getAgentCode() == null) {
            throw new BusinessException(400, "经纪人姓名、电话和工号不能为空");
        }
        boolean success = agentService.save(agent);
        return ResultDTO.success(success);
    }

    /**
     * 查询单个经纪人
     */
    @GetMapping("/{id}")
    public ResultDTO<Agent> getAgentById(@PathVariable Long id) {
        Agent agent = agentService.getById(id);
        if (agent == null) {
            throw new BusinessException(404, "经纪人不存在");
        }
        return ResultDTO.success(agent);
    }

    /**
     * 分页查询经纪人
     */
    @GetMapping("/page")
    public ResultDTO<Page<Agent>> getAgentPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<Agent> page = new Page<>(pageNum, pageSize);
        Page<Agent> resultPage = agentService.page(page);
        return ResultDTO.success(resultPage);
    }

    /**
     * 更新经纪人状态（在职/离职）
     */
    @PatchMapping("/{id}/status")
    public ResultDTO<Boolean> updateAgentStatus(@PathVariable Long id, @RequestParam Byte status) {
        Agent agent = agentService.getById(id);
        if (agent == null) {
            throw new BusinessException(404, "经纪人不存在");
        }
        agent.setStatus(status);
        boolean success = agentService.updateById(agent);
        return ResultDTO.success(success);
    }
}
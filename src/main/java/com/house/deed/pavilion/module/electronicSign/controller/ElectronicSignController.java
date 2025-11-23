package com.house.deed.pavilion.module.electronicSign.controller;

import com.house.deed.pavilion.common.dto.ResultDTO;
import com.house.deed.pavilion.module.electronicSign.entity.ElectronicSign;
import com.house.deed.pavilion.module.electronicSign.service.IElectronicSignService;
import io.micrometer.common.util.StringUtils;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 电子签约信息表（租户级数据） 前端控制器
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
@RestController
@RequestMapping("/module/electronicSign")
public class ElectronicSignController {

    @Resource
    private IElectronicSignService electronicSignService;

    /**
     * 创建电子签记录（需登录，合同创建人权限）
     */
    @PostMapping("/create")
    public ResultDTO<ElectronicSign> create(
            @RequestParam Long contractId,
            @RequestParam String signPlatform) {
        return ResultDTO.success(electronicSignService.createElectronicSign(contractId, signPlatform));
    }

    /**
     * 通过合同ID查询电子签记录（需登录，合同关联权限）
     */
    @GetMapping("/contract/{contractId}")
    public ResultDTO<ElectronicSign> getByContractId(@PathVariable Long contractId) {
        return ResultDTO.success(electronicSignService.getByContractId(contractId));
    }

    /**
     * 第三方电子签平台回调接口（无需登录，公开访问）
     * 用于同步签署状态（客户/房东签署后回调）
     */
    @PostMapping("/callback")
    public ResultDTO<String> signCallback(
            @RequestParam Long signId,
            @RequestParam boolean customerSign,
            @RequestParam boolean landlordSign,
            @RequestParam(required = false) String rejectReason) {

        // 若有拒签理由，更新状态为拒签
        if (StringUtils.isNotBlank(rejectReason)) {
            // 此处简化处理，实际需扩展updateSignStatus支持拒签逻辑
            ElectronicSign sign = electronicSignService.getById(signId);
            sign.setSignStatus("REJECTED");
            electronicSignService.updateById(sign);
            return ResultDTO.success("REJECTED");
        }

        // 正常更新签署状态
        String newStatus = electronicSignService.updateSignStatus(signId, customerSign, landlordSign);
        return ResultDTO.success(newStatus);
    }

    /**
     * 批量查询电子签记录（用于合同列表页）
     */
    @PostMapping("/batch")
    public ResultDTO<Map<Long, ElectronicSign>> batchGet(@RequestBody List<Long> contractIds) {
        return ResultDTO.success(electronicSignService.getBatchByContractIds(contractIds));
    }
}
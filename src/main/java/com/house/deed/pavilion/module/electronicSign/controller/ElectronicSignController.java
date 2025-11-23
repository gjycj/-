package com.house.deed.pavilion.module.electronicSign.controller;

import com.house.deed.pavilion.common.dto.ResultDTO;
import com.house.deed.pavilion.common.exception.BusinessException;
import com.house.deed.pavilion.common.util.TenantContext;
import com.house.deed.pavilion.module.electronicSign.entity.ElectronicSign;
import com.house.deed.pavilion.module.electronicSign.service.IElectronicSignService;
import io.micrometer.common.util.StringUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.annotation.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
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
     * 作废电子签
     */
    @PostMapping("/invalid/{signId}")
    @Operation(summary = "作废电子签", description = "仅管理员可操作，将电子签状态更新为INVALID")
    public ResultDTO<Boolean> invalidSign(
            @PathVariable Long signId) {
        boolean success = electronicSignService.invalidSign(signId);
        return ResultDTO.success(success);
    }

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
    @Operation(summary = "电子签回调接口", description = "接收第三方平台签署状态通知，同步签名时间和状态")

    public ResultDTO<String> signCallback(
            @RequestParam Long signId,
            @RequestParam boolean customerSign,
            @RequestParam boolean landlordSign,
            @RequestParam(required = false) String rejectReason,
            // 新增：接收第三方传递的签名时间（格式：yyyy-MM-dd HH:mm:ss）
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime customerSignTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime landlordSignTime) {

        // 拒签逻辑保持不变
        if (StringUtils.isNotBlank(rejectReason)) {
            ElectronicSign sign = electronicSignService.getById(signId);
            if (sign == null || !sign.getTenantId().equals(TenantContext.getTenantId())) {
                throw new BusinessException(404, "电子签记录不存在");
            }
            sign.setSignStatus("REJECTED");
            electronicSignService.updateById(sign);
            return ResultDTO.success("REJECTED");
        }

        // 调用服务层方法：传递签名时间参数，同步状态和时间
        String newStatus = electronicSignService.updateSignStatus(signId, customerSign, landlordSign, customerSignTime, landlordSignTime);
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
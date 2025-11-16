package com.house.deed.pavilion.module.visitRecord.controller;

import com.house.deed.pavilion.common.dto.ResultDTO;
import com.house.deed.pavilion.common.util.TenantContext;
import com.house.deed.pavilion.module.visitRecord.entity.VisitRecord;
import com.house.deed.pavilion.module.visitRecord.service.IVisitRecordService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
public class VisitRecordController {

    @Resource
    private IVisitRecordService visitRecordService;

    // 新增：通过合同ID查询带看记录
    @GetMapping("/by-contract")
    public ResultDTO<List<VisitRecord>> getByContractId(@RequestParam Long contractId) {
        Long tenantId = TenantContext.getTenantId();
        List<VisitRecord> visitRecords = visitRecordService.getByContractId(contractId, tenantId);
        return ResultDTO.success(visitRecords);
    }

}

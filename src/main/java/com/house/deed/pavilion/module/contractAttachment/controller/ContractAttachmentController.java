package com.house.deed.pavilion.module.contractAttachment.controller;

import com.house.deed.pavilion.common.dto.ResultDTO;
import com.house.deed.pavilion.common.exception.BusinessException;
import com.house.deed.pavilion.common.util.TenantContext;
import com.house.deed.pavilion.module.contractAttachment.entity.ContractAttachment;
import com.house.deed.pavilion.module.contractAttachment.service.IContractAttachmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 合同附件表（租户级数据） 前端控制器
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
@RestController
@RequestMapping("/module/contractAttachment")
public class ContractAttachmentController {

    @Resource
    private IContractAttachmentService attachmentService;

    @Operation(summary = "按类型分组查询合同附件", description = "通过合同ID查询所有附件，并按附件类型（attachment_type）分组返回")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "404", description = "合同不存在或无附件")
    })
    @GetMapping("/grouped/contract/{contractId}")
    public ResultDTO<Map<String, List<ContractAttachment>>> getGroupedByContractId(
            @Parameter(description = "合同ID", required = true, example = "1") @PathVariable("contractId") Long contractId) {  // 明确指定path variable名称

        Long tenantId = TenantContext.getTenantId();
        Map<String, List<ContractAttachment>> groupedAttachments =
                attachmentService.getGroupedByContractId(contractId, tenantId);

        return ResultDTO.success(groupedAttachments);
    }

    /**
     * 上传合同附件
     */
    @PostMapping("/upload")
    public ResultDTO<ContractAttachment> upload(
            @RequestParam Long contractId,
            @RequestParam MultipartFile file,
            @RequestParam String attachmentType,
            @RequestParam Long uploaderId) {

        if (file.isEmpty()) {
            throw new BusinessException(400, "文件不能为空");
        }
        ContractAttachment attachment = attachmentService.uploadAttachment(contractId, file, attachmentType, uploaderId);
        return ResultDTO.success(attachment);
    }

    /**
     * 查询合同附件列表
     */
    @GetMapping("/contract/{contractId}")
    public ResultDTO<List<ContractAttachment>> getByContractId(@PathVariable Long contractId) {
        List<ContractAttachment> attachments = attachmentService.getByContractId(contractId);
        return ResultDTO.success(attachments);
    }

}

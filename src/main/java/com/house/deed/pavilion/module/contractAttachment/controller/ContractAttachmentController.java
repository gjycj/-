package com.house.deed.pavilion.module.contractAttachment.controller;

import com.house.deed.pavilion.common.dto.ResultDTO;
import com.house.deed.pavilion.common.exception.BusinessException;
import com.house.deed.pavilion.module.contractAttachment.entity.ContractAttachment;
import com.house.deed.pavilion.module.contractAttachment.service.IContractAttachmentService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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

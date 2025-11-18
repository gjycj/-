package com.house.deed.pavilion.module.contractAttachment.service.impl;

import cn.hutool.core.lang.UUID;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.common.exception.BusinessException;
import com.house.deed.pavilion.common.util.ContractValidationUtil;
import com.house.deed.pavilion.common.util.TenantContext;
import com.house.deed.pavilion.module.contract.entity.Contract;
import com.house.deed.pavilion.module.contractAttachment.entity.ContractAttachment;
import com.house.deed.pavilion.module.contractAttachment.mapper.ContractAttachmentMapper;
import com.house.deed.pavilion.module.contractAttachment.service.IContractAttachmentService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 合同附件表（租户级数据） 服务实现类
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
@Service
public class ContractAttachmentServiceImpl extends ServiceImpl<ContractAttachmentMapper, ContractAttachment> implements IContractAttachmentService {

    @Resource
    private ContractValidationUtil contractValidationUtil;

    @Value("${file.upload.path}")
    private String uploadPath;

    // 实现类
    // 实现新增的分组查询方法
    @Override
    public Map<String, List<ContractAttachment>> getGroupedByContractId(Long contractId, Long tenantId) {
        // 1. 校验合同是否存在（可选，增强安全性）
         contractValidationUtil.validateContract(contractId, tenantId);
        // 2. 查询该合同下的所有附件
        List<ContractAttachment> attachments = baseMapper.selectList(
                new LambdaQueryWrapper<ContractAttachment>()
                        .eq(ContractAttachment::getTenantId, tenantId)
                        .eq(ContractAttachment::getContractId, contractId)
        );

        // 3. 按附件类型（attachmentType）分组
        return attachments.stream()
                .collect(Collectors.groupingBy(ContractAttachment::getAttachmentType));
    }

    @Override
    public ContractAttachment uploadAttachment(Long contractId, MultipartFile file, String attachmentType, Long uploaderId) {
        Long tenantId = TenantContext.getTenantId();

        // 1. 校验合同存在性及租户归属
        // 校验合同合法性（通过领域服务）
        Contract contract = contractValidationUtil.validateContract(contractId, tenantId);// 替代原contractService调用
        if (contract == null || !contract.getTenantId().equals(tenantId)) {
            throw new BusinessException(404, "合同不存在或无权访问");
        }

        // 2. 上传文件（简化实现，实际应使用OSS等存储）
        String originalFilename = file.getOriginalFilename();
        assert originalFilename != null;
        String fileSuffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        String fileName = UUID.randomUUID() + fileSuffix;
        String filePath = uploadPath + File.separator + "contract" + File.separator + contractId;
        File saveDir = new File(filePath);
        if (!saveDir.exists()) {
            saveDir.mkdirs();
        }
        try {
            file.transferTo(new File(filePath + File.separator + fileName));
        } catch (IOException e) {
            throw new BusinessException(500, "文件上传失败");
        }

        // 3. 保存附件记录
        ContractAttachment attachment = new ContractAttachment();
        attachment.setTenantId(tenantId);
        attachment.setContractId(contractId);
        attachment.setAttachmentType(attachmentType);
        attachment.setAttachmentUrl("/upload/contract/" + contractId + "/" + fileName); // 访问URL
        attachment.setFileName(originalFilename);
        attachment.setUploadTime(LocalDateTime.now());
        attachment.setUploaderId(uploaderId);
        save(attachment);

        return attachment;
    }

    @Override
    public List<ContractAttachment> getByContractId(Long contractId) {
        Long tenantId = TenantContext.getTenantId();
        return baseMapper.selectList(new LambdaQueryWrapper<ContractAttachment>()
                .eq(ContractAttachment::getTenantId, tenantId)
                .eq(ContractAttachment::getContractId, contractId));
    }

}

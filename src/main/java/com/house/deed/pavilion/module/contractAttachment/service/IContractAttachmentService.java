package com.house.deed.pavilion.module.contractAttachment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.house.deed.pavilion.module.contractAttachment.entity.ContractAttachment;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * <p>
 * 合同附件表（租户级数据） 服务类
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
public interface IContractAttachmentService extends IService<ContractAttachment> {

    /**
     * 上传合同附件
     * @param contractId 合同ID
     * @param file 上传文件
     * @param attachmentType 附件类型（ID_CARD/PROPERTY_CERT等）
     * @param uploaderId 上传人ID
     * @return 附件记录
     */
    ContractAttachment uploadAttachment(Long contractId, MultipartFile file, String attachmentType, Long uploaderId);

    /**
     * 根据合同ID查询附件列表
     * @param contractId 合同ID
     * @return 附件列表
     */
    List<ContractAttachment> getByContractId(Long contractId);

}

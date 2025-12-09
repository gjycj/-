package com.house.deed.pavilion.service;

import com.house.deed.pavilion.entity.ContractAttachment;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

/**
 * <p>
 * 合同附件表（租户级数据） 服务类
 * 核心能力：提供合同附件的查询、新增、删除等操作，强制租户隔离，确保数据安全
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
public interface ContractAttachmentService extends IService<ContractAttachment> {

    /**
     * 根据租户ID和合同ID查询附件列表
     * 场景：查看某合同的所有附件（如身份证、房产证扫描件）
     *
     * @param tenantId   租户ID（必传，数据隔离）
     * @param contractId 合同ID（必传，关联合同）
     * @return 合同对应的附件列表（同租户下）
     */
    List<ContractAttachment> getByContractId(Long tenantId, Long contractId);

    /**
     * 根据租户ID和附件ID查询单个附件
     * 场景：查看单个附件详情（如下载附件前校验权限）
     *
     * @param tenantId     租户ID（必传，权限校验）
     * @param attachmentId 附件ID（必传，目标附件）
     * @return 符合条件的附件（若不存在或不属于该租户，返回null）
     */
    ContractAttachment getById(Long tenantId, Long attachmentId);

    /**
     * 保存单个合同附件（自动绑定租户ID）
     * 场景：上传单个合同附件（如补充上传房东身份证）
     *
     * @param tenantId   租户ID（必传，数据归属）
     * @param attachment 附件信息（需包含contractId、attachmentType等核心字段）
     * @return 是否保存成功
     */
    boolean save(Long tenantId, ContractAttachment attachment);

    /**
     * 批量保存合同附件（自动绑定租户ID）
     * 场景：一次性上传多个合同附件（如签约时同时上传合同扫描件、双方身份证）
     *
     * @param tenantId    租户ID（必传，数据归属）
     * @param attachments 附件列表（每个附件需包含contractId、attachmentType等核心字段）
     * @return 是否批量保存成功
     */
    boolean batchSave(Long tenantId, List<ContractAttachment> attachments);

    /**
     * 删除单个合同附件（校验租户权限）
     * 场景：删除无效或错误的附件（如重复上传的文件）
     *
     * @param tenantId     租户ID（必传，权限校验）
     * @param attachmentId 附件ID（必传，目标附件）
     * @return 是否删除成功（若附件不属于该租户，返回false）
     */
    boolean remove(Long tenantId, Long attachmentId);

    /**
     * 批量删除合同附件（校验租户权限）
     * 场景：批量清理某合同的过期附件
     *
     * @param tenantId      租户ID（必传，权限校验）
     * @param attachmentIds 附件ID列表（必传，目标附件集合）
     * @return 是否批量删除成功（若存在不属于该租户的附件，返回false）
     */
    boolean batchRemove(Long tenantId, List<Long> attachmentIds);

    /**
     * 批量修改合同附件（支持修改附件类型、URL、文件名）
     * @param tenantId 租户ID（数据隔离）
     * @param attachments 待修改的附件列表（需包含ID及要更新的字段）
     * @return 是否修改成功
     */
    boolean batchUpdate(Long tenantId, List<ContractAttachment> attachments);
}
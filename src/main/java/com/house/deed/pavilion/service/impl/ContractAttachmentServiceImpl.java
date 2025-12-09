package com.house.deed.pavilion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.entity.ContractAttachment;
import com.house.deed.pavilion.mapper.ContractAttachmentMapper;
import com.house.deed.pavilion.service.ContractAttachmentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * <p>
 * 合同附件表（租户级数据） 服务实现类
 * 修正说明：严格匹配实体类的自动填充规则和字段约束，修复上传时间手动设置、上传人ID校验缺失等问题
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Service
public class ContractAttachmentServiceImpl extends ServiceImpl<ContractAttachmentMapper, ContractAttachment> implements ContractAttachmentService {

    // 复用实体类中定义的URL格式正则（保持校验规则一致）
    private static final Pattern URL_PATTERN = Pattern.compile("^(https?://|oss://|/).*$");

    @Override
    public List<ContractAttachment> getByContractId(Long tenantId, Long contractId) {
        Assert.notNull(tenantId, "租户ID不能为空");
        Assert.notNull(contractId, "合同ID不能为空");

        LambdaQueryWrapper<ContractAttachment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ContractAttachment::getTenantId, tenantId)
                .eq(ContractAttachment::getContractId, contractId)
                .orderByDesc(ContractAttachment::getUploadTime); // 按实体类自动填充的上传时间排序

        return baseMapper.selectList(queryWrapper);
    }

    @Override
    public ContractAttachment getById(Long tenantId, Long attachmentId) {
        Assert.notNull(tenantId, "租户ID不能为空");
        Assert.notNull(attachmentId, "附件ID不能为空");

        ContractAttachment attachment = baseMapper.selectById(attachmentId);
        // 校验租户归属时，补充实体类中存在的tenantId字段判断
        if (attachment == null || !Objects.equals(attachment.getTenantId(), tenantId)) {
            return null;
        }
        return attachment;
    }

    @Override
    public boolean save(Long tenantId, ContractAttachment attachment) {
        Assert.notNull(tenantId, "租户ID不能为空");
        Assert.notNull(attachment, "附件信息不能为空");
        // 补充实体类中必填的contractId、uploaderId校验（之前缺失）
        Assert.notNull(attachment.getContractId(), "附件关联的合同ID不能为空");
        Assert.notNull(attachment.getUploaderId(), "上传人ID不能为空");
        // 校验实体类中attachmentUrl的格式约束
        Assert.hasText(attachment.getAttachmentUrl(), "附件URL不能为空");
        Assert.isTrue(URL_PATTERN.matcher(attachment.getAttachmentUrl()).matches(),
                "附件URL格式错误（支持HTTP/HTTPS/OSS/本地路径）");
        Assert.hasText(attachment.getFileName(), "文件名称不能为空");
        Assert.isTrue(attachment.getFileName().length() <= 100, "文件名称长度不能超过100字符");

        // 修正：移除手动设置uploadTime，遵循实体类@TableField(fill = FieldFill.INSERT)的自动填充规则
        attachment.setTenantId(tenantId); // 仅设置租户ID，上传时间由数据库自动填充

        return baseMapper.insert(attachment) > 0;
    }

    /**
     * 批量修改合同附件
     * 实现说明：仅允许修改attachmentType/attachmentUrl/fileName字段，严格校验租户归属和字段合法性
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchUpdate(Long tenantId, List<ContractAttachment> attachments) {
        // 1. 基础参数校验
        Assert.notNull(tenantId, "租户ID不能为空");
        Assert.isTrue(!CollectionUtils.isEmpty(attachments), "待修改的附件列表不能为空");

        // 2. 提取待修改的附件ID，校验是否存在且属于当前租户
        Set<Long> attachmentIds = attachments.stream()
                .map(ContractAttachment::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Assert.isTrue(attachmentIds.size() == attachments.size(), "所有附件必须包含ID");

        // 2.1 查询数据库中这些ID对应的记录，校验租户归属
        LambdaQueryWrapper<ContractAttachment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ContractAttachment::getTenantId, tenantId)
                .in(ContractAttachment::getId, attachmentIds);
        long validCount = baseMapper.selectCount(queryWrapper);
        Assert.isTrue(validCount == attachmentIds.size(), "存在不属于当前租户或不存在的附件，批量修改失败");

        // 3. 逐个校验修改字段的合法性（符合实体类约束）
        attachments.forEach(attachment -> {
            // 3.1 附件类型校验（枚举值）
            if (StringUtils.hasText(attachment.getAttachmentType())) {
                Set<String> validTypes = Set.of("ID_CARD", "PROPERTY_CERT", "LAND_CERT", "CONTRACT_SCAN", "OTHER");
                Assert.isTrue(validTypes.contains(attachment.getAttachmentType()),
                        "附件类型错误，支持：ID_CARD/PROPERTY_CERT/LAND_CERT/CONTRACT_SCAN/OTHER");
            }

            // 3.2 附件URL校验（格式+长度）
            if (StringUtils.hasText(attachment.getAttachmentUrl())) {
                Assert.isTrue(attachment.getAttachmentUrl().length() <= 500, "附件URL长度不能超过500字符");
                Assert.isTrue(URL_PATTERN.matcher(attachment.getAttachmentUrl()).matches(),
                        "附件URL格式错误（支持HTTP/HTTPS/OSS/本地路径）");
            }

            // 3.3 文件名校验（非空+长度）
            if (StringUtils.hasText(attachment.getFileName())) {
                Assert.isTrue(attachment.getFileName().length() <= 100, "文件名称长度不能超过100字符");
            }

            // 3.4 禁止修改租户ID、合同ID、上传人、上传时间（这些字段不可变更）
            attachment.setTenantId(null); // 强制置空，避免恶意修改
            attachment.setContractId(null);
            attachment.setUploaderId(null);
            attachment.setUploadTime(null);
        });

        // 4. 执行批量更新（仅更新非空字段，依赖MyBatis-Plus的字段策略）
        return updateBatchById(attachments);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchSave(Long tenantId, List<ContractAttachment> attachments) {
        Assert.notNull(tenantId, "租户ID不能为空");
        Assert.isTrue(!CollectionUtils.isEmpty(attachments), "附件列表不能为空");

        attachments.forEach(attachment -> {
            // 批量保存时同样强化实体类的必填字段校验
            Assert.notNull(attachment.getContractId(), "存在未关联合同的附件");
            Assert.notNull(attachment.getUploaderId(), "存在未指定上传人的附件");
            Assert.hasText(attachment.getAttachmentUrl(), "存在未设置URL的附件");
            Assert.isTrue(URL_PATTERN.matcher(attachment.getAttachmentUrl()).matches(),
                    "附件URL格式错误：" + attachment.getAttachmentUrl());
            Assert.hasText(attachment.getFileName(), "存在未指定文件名的附件");
            Assert.isTrue(attachment.getFileName().length() <= 100,
                    "文件名过长：" + attachment.getFileName());

            attachment.setTenantId(tenantId);
            // 修正：移除手动设置uploadTime，依赖实体类的自动填充
        });

        return saveBatch(attachments);
    }

    @Override
    public boolean remove(Long tenantId, Long attachmentId) {
        Assert.notNull(tenantId, "租户ID不能为空");
        Assert.notNull(attachmentId, "附件ID不能为空");

        ContractAttachment attachment = baseMapper.selectById(attachmentId);
        if (attachment == null || !Objects.equals(attachment.getTenantId(), tenantId)) {
            return false;
        }

        return baseMapper.deleteById(attachmentId) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchRemove(Long tenantId, List<Long> attachmentIds) {
        Assert.notNull(tenantId, "租户ID不能为空");
        Assert.isTrue(!CollectionUtils.isEmpty(attachmentIds), "附件ID列表不能为空");

        LambdaQueryWrapper<ContractAttachment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ContractAttachment::getTenantId, tenantId)
                .in(ContractAttachment::getId, attachmentIds);
        long validCount = baseMapper.selectCount(queryWrapper);
        if (validCount != attachmentIds.size()) {
            throw new IllegalArgumentException("存在不属于当前租户的附件，批量删除失败");
        }

        return baseMapper.deleteBatchIds(attachmentIds) > 0;
    }
}
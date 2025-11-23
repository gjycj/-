package com.house.deed.pavilion.module.houseTag.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.common.aspect.annotation.AgentDataPermission;
import com.house.deed.pavilion.common.exception.BusinessException;
import com.house.deed.pavilion.common.util.AgentContext;
import com.house.deed.pavilion.common.util.TenantContext;
import com.house.deed.pavilion.module.house.entity.House;
import com.house.deed.pavilion.module.house.service.IHouseService;
import com.house.deed.pavilion.module.houseTag.entity.HouseTag;
import com.house.deed.pavilion.module.houseTag.mapper.HouseTagMapper;
import com.house.deed.pavilion.module.houseTag.service.IHouseTagService;
import com.house.deed.pavilion.module.tag.entity.Tag;
import com.house.deed.pavilion.module.tag.service.ITagService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 房源与标签关联表（租户级数据） 服务实现类
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
@Service
public class HouseTagServiceImpl extends ServiceImpl<HouseTagMapper, HouseTag> implements IHouseTagService {


    @Resource
    private IHouseService houseService;

    @Resource
    private ITagService tagService;

    @Override
    @AgentDataPermission(
            operation = AgentDataPermission.OperationType.QUERY,
            entityClass = HouseTag.class
    )
    public List<Long> getTagIdsByHouseId(Long houseId) {
        // 校验房源是否存在
        if (!houseService.existsById(houseId)) {
            throw new BusinessException(404, "房源不存在");
        }

        return baseMapper.selectTagIdsByHouseId(houseId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @AgentDataPermission(
            operation = AgentDataPermission.OperationType.CREATE,
            entityClass = HouseTag.class
    )
    public boolean batchAddTags(Long houseId, List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return true;
        }

        Long currentAgentId = AgentContext.getAgentId();
        Long tenantId = TenantContext.getTenantId();

        // 校验房源归属
        House house = houseService.getById(houseId);
        if (house == null || !house.getTenantId().equals(tenantId)) {
            throw new BusinessException(404, "房源不存在或无权访问");
        }

        // 校验是否为创建人
        if (!house.getCreateAgentId().equals(currentAgentId)) {
            throw new BusinessException(403, "无权操作：仅创建人可管理标签");
        }

        // 校验标签是否存在且属于当前租户
        List<Tag> tags = tagService.listByIds(tagIds);
        if (tags.size() != tagIds.size()) {
            List<Long> existingIds = tags.stream().map(Tag::getId).toList();
            List<Long> invalidIds = tagIds.stream()
                    .filter(id -> !existingIds.contains(id))
                    .toList();
            throw new BusinessException(400, "标签不存在：" + invalidIds);
        }

        // 检查标签是否已关联
        List<Long> existingTagIds = baseMapper.selectTagIdsByHouseId(houseId);
        List<Long> newTagIds = tagIds.stream()
                .filter(id -> !existingTagIds.contains(id))
                .toList();

        if (newTagIds.isEmpty()) {
            return true;
        }

        // 批量添加新标签关联
        List<HouseTag> houseTags = new ArrayList<>();
        for (Long tagId : newTagIds) {
            HouseTag houseTag = new HouseTag();
            houseTag.setTenantId(tenantId);
            houseTag.setHouseId(houseId);
            houseTag.setTagId(tagId);
            houseTags.add(houseTag);
        }

        return saveBatch(houseTags);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @AgentDataPermission(
            operation = AgentDataPermission.OperationType.DELETE,
            entityClass = HouseTag.class
    )
    public boolean removeTags(Long houseId, List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return true;
        }

        // 校验房源归属（权限由注解控制）
        if (!houseService.existsById(houseId)) {
            throw new BusinessException(404, "房源不存在");
        }

        return baseMapper.deleteByHouseIdAndTagIds(houseId, tagIds) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean replaceTags(Long houseId, List<Long> tagIds) {
        // 先删除已有标签
        LambdaQueryWrapper<HouseTag> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(HouseTag::getHouseId, houseId);
        remove(queryWrapper);

        // 再添加新标签
        return batchAddTags(houseId, tagIds);
    }
}

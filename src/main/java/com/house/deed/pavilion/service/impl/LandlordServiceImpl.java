package com.house.deed.pavilion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.entity.Landlord;
import com.house.deed.pavilion.mapper.LandlordMapper;
import com.house.deed.pavilion.service.LandlordService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * 房东信息服务实现类
 *
 * <p>实现房东信息的增删改查及批量操作，所有方法均包含租户级数据隔离和业务校验</p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Service
public class LandlordServiceImpl extends ServiceImpl<LandlordMapper, Landlord> implements LandlordService {

    /**
     * 新增房东信息
     *
     * <p>业务校验：</p>
     * <ul>
     *   <li>同一租户下手机号必须唯一（phone字段非空且唯一）</li>
     *   <li>同一租户下身份证号必须唯一（idCard字段非空且唯一）</li>
     * </ul>
     * <p>技术实现：自动填充createTime（通过实体类的@TableField(fill = FieldFill.INSERT)配置）</p>
     *
     * @param landlord 房东实体对象，需包含租户ID、手机号、身份证号等必填信息
     * @return 新增成功返回true，否则返回false
     * @throws IllegalArgumentException 当手机号或身份证号已存在时抛出
     */
    @Override
    public boolean saveLandlord(Landlord landlord) {
        // 校验租户内手机号唯一性
        LambdaQueryWrapper<Landlord> phoneWrapper = new LambdaQueryWrapper<>();
        phoneWrapper.eq(Landlord::getTenantId, landlord.getTenantId())
                .eq(Landlord::getPhone, landlord.getPhone());
        long phoneCount = baseMapper.selectCount(phoneWrapper);
        if (phoneCount > 0) {
            throw new IllegalArgumentException("当前租户下手机号已存在：" + landlord.getPhone());
        }

        // 校验租户内身份证号唯一性
        LambdaQueryWrapper<Landlord> idCardWrapper = new LambdaQueryWrapper<>();
        idCardWrapper.eq(Landlord::getTenantId, landlord.getTenantId())
                .eq(Landlord::getIdCard, landlord.getIdCard());
        long idCardCount = baseMapper.selectCount(idCardWrapper);
        if (idCardCount > 0) {
            throw new IllegalArgumentException("当前租户下身份证号已存在：" + landlord.getIdCard());
        }

        // 执行新增操作（createTime自动填充）
        return baseMapper.insert(landlord) > 0;
    }

    /**
     * 根据ID更新房东信息
     *
     * <p>业务校验：</p>
     * <ul>
     *   <li>数据必须存在且属于当前租户</li>
     *   <li>更新手机号/身份证号时，需校验新值在租户内的唯一性（排除自身）</li>
     * </ul>
     * <p>技术实现：自动填充updateTime（通过实体类的@TableField(fill = FieldFill.INSERT_UPDATE)配置）</p>
     *
     * @param landlord 房东实体对象，需包含主键ID、租户ID及需要更新的字段
     * @return 更新成功返回true，否则返回false
     * @throws IllegalArgumentException 当数据不存在、无权限操作或手机号/身份证号已存在时抛出
     */
    @Override
    public boolean updateLandlordById(Landlord landlord) {
        // 校验数据存在且属于当前租户
        Landlord existLandlord = baseMapper.selectById(landlord.getId());
        if (existLandlord == null || !existLandlord.getTenantId().equals(landlord.getTenantId())) {
            throw new IllegalArgumentException("房东不存在或无权限操作");
        }

        // 校验更新后的手机号唯一性（排除自身）
        if (landlord.getPhone() != null) {
            LambdaQueryWrapper<Landlord> phoneWrapper = new LambdaQueryWrapper<>();
            phoneWrapper.eq(Landlord::getTenantId, landlord.getTenantId())
                    .eq(Landlord::getPhone, landlord.getPhone())
                    .ne(Landlord::getId, landlord.getId());
            long phoneCount = baseMapper.selectCount(phoneWrapper);
            if (phoneCount > 0) {
                throw new IllegalArgumentException("新手机号已存在：" + landlord.getPhone());
            }
        }

        // 校验更新后的身份证号唯一性（排除自身）
        if (landlord.getIdCard() != null) {
            LambdaQueryWrapper<Landlord> idCardWrapper = new LambdaQueryWrapper<>();
            idCardWrapper.eq(Landlord::getTenantId, landlord.getTenantId())
                    .eq(Landlord::getIdCard, landlord.getIdCard())
                    .ne(Landlord::getId, landlord.getId());
            long idCardCount = baseMapper.selectCount(idCardWrapper);
            if (idCardCount > 0) {
                throw new IllegalArgumentException("新身份证号已存在：" + landlord.getIdCard());
            }
        }

        // 执行更新操作（updateTime自动填充）
        return baseMapper.updateById(landlord) > 0;
    }

    /**
     * 根据ID物理删除房东信息
     *
     * <p>业务校验：数据必须存在且属于当前租户</p>
     *
     * @param id 房东主键ID
     * @param tenantId 租户ID，用于数据隔离验证
     * @return 删除成功返回true，否则返回false
     * @throws IllegalArgumentException 当数据不存在或无权限操作时抛出
     */
    @Override
    public boolean removeLandlordById(Long id, Long tenantId) {
        // 校验数据存在且属于当前租户
        Landlord existLandlord = baseMapper.selectById(id);
        if (existLandlord == null || !existLandlord.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("房东不存在或无权限操作");
        }
        return baseMapper.deleteById(id) > 0;
    }

    /**
     * 根据ID查询房东详细信息（租户隔离）
     *
     * <p>查询时自动应用租户隔离条件，确保只能查询到当前租户的数据</p>
     *
     * @param id 房东主键ID
     * @param tenantId 租户ID，用于数据隔离
     * @return 符合条件的房东实体对象，未找到返回null
     */
    @Override
    public Landlord getLandlordById(Long id, Long tenantId) {
        LambdaQueryWrapper<Landlord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Landlord::getId, id)
                .eq(Landlord::getTenantId, tenantId);
        return baseMapper.selectOne(queryWrapper);
    }

    /**
     * 多条件分页查询房东信息
     *
     * <p>支持以下查询条件：</p>
     * <ul>
     *   <li>姓名模糊查询</li>
     *   <li>手机号精确查询</li>
     *   <li>身份证号精确查询</li>
     *   <li>地址模糊查询</li>
     *   <li>录入经纪人精确查询</li>
     * </ul>
     * <p>默认按创建时间降序排列（最新的记录在前）</p>
     *
     * @param page 分页参数对象，包含页码和每页大小
     * @param landlord 查询条件实体对象，非空字段将作为查询条件
     * @param tenantId 租户ID，用于数据隔离
     * @return 分页查询结果，包含房东列表和分页信息
     */
    @Override
    public IPage<Landlord> pageQuery(Page<Landlord> page, Landlord landlord, Long tenantId) {
        LambdaQueryWrapper<Landlord> queryWrapper = new LambdaQueryWrapper<>();
        // 强制租户隔离
        queryWrapper.eq(Landlord::getTenantId, tenantId);

        // 动态拼接查询条件（非空字段才参与筛选）
        if (landlord.getName() != null) {
            queryWrapper.like(Landlord::getName, landlord.getName());
        }
        if (landlord.getPhone() != null) {
            queryWrapper.eq(Landlord::getPhone, landlord.getPhone());
        }
        if (landlord.getIdCard() != null) {
            queryWrapper.eq(Landlord::getIdCard, landlord.getIdCard());
        }
        if (landlord.getAddress() != null) {
            queryWrapper.like(Landlord::getAddress, landlord.getAddress());
        }
        if (landlord.getCreateAgentId() != null) {
            queryWrapper.eq(Landlord::getCreateAgentId, landlord.getCreateAgentId());
        }

        // 按创建时间降序排列
        queryWrapper.orderByDesc(Landlord::getCreateTime);

        // 执行分页查询
        return baseMapper.selectPage(page, queryWrapper);
    }

    /**
     * 批量新增房东信息（事务保证）
     *
     * <p>在单个事务中执行批量新增，任一记录校验失败或保存失败将导致整个操作回滚</p>
     * <p>批量校验每一条记录的手机号和身份证号在租户内的唯一性</p>
     * <p>建议在业务层控制批量操作的数据量（如每次不超过100条）</p>
     *
     * @param landlords 房东实体对象列表
     * @return 批量新增成功返回true，否则返回false
     * @throws IllegalArgumentException 当任意记录的手机号或身份证号已存在时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchSaveLandlords(List<Landlord> landlords) {
        if (landlords.isEmpty()) {
            return false;
        }

        // 批量校验唯一性
        for (Landlord landlord : landlords) {
            // 手机号校验
            LambdaQueryWrapper<Landlord> phoneWrapper = new LambdaQueryWrapper<>();
            phoneWrapper.eq(Landlord::getTenantId, landlord.getTenantId())
                    .eq(Landlord::getPhone, landlord.getPhone());
            if (baseMapper.selectCount(phoneWrapper) > 0) {
                throw new IllegalArgumentException("批量新增失败：手机号已存在" + landlord.getPhone());
            }

            // 身份证号校验
            LambdaQueryWrapper<Landlord> idCardWrapper = new LambdaQueryWrapper<>();
            idCardWrapper.eq(Landlord::getTenantId, landlord.getTenantId())
                    .eq(Landlord::getIdCard, landlord.getIdCard());
            if (baseMapper.selectCount(idCardWrapper) > 0) {
                throw new IllegalArgumentException("批量新增失败：身份证号已存在" + landlord.getIdCard());
            }
        }

        // 批量保存（事务保证）
        return saveBatch(landlords);
    }

    /**
     * 批量删除房东信息（事务保证）
     *
     * <p>在单个事务中执行批量删除，任一记录校验失败或删除失败将导致整个操作回滚</p>
     * <p>先验证所有ID都属于当前租户，然后执行批量删除</p>
     *
     * @param ids 待删除的房东ID列表
     * @param tenantId 租户ID，用于数据隔离验证
     * @return 批量删除成功返回true，否则返回false
     * @throws IllegalArgumentException 当存在不属于当前租户的房东ID时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchRemoveLandlords(List<Long> ids, Long tenantId) {
        if (ids.isEmpty()) {
            return false;
        }

        // 校验所有ID都属于当前租户
        validateLandlordIdsBelongToTenant(tenantId, ids);

        // 批量删除（事务保证）
        return baseMapper.deleteBatchIds(ids) > 0;
    }

    /**
     * 验证房东ID列表是否全部属于当前租户
     *
     * <p>两步验证：</p>
     * <ol>
     *   <li>检查ID是否存在（是否存在未查询到的ID）</li>
     *   <li>检查存在的ID是否属于当前租户</li>
     * </ol>
     * <p>验证失败时抛出具体的异常信息，便于定位问题</p>
     *
     * @param tenantId 租户ID
     * @param landlordIds 待验证的房东ID列表
     * @throws IllegalArgumentException 当存在不存在的ID或不属于当前租户的ID时抛出
     */
    @Override
    public void validateLandlordIdsBelongToTenant(Long tenantId, List<Long> landlordIds) {
        if (landlordIds.isEmpty()) {
            return;
        }

        // 使用普通 QueryWrapper 而不是 LambdaQueryWrapper
        QueryWrapper<Landlord> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id", "tenant_id");  // 使用数据库字段名，而不是实体类属性名
        queryWrapper.in("id", landlordIds);

        // 查询数据库中存在的房东ID及其租户ID
        List<Landlord> landlords = baseMapper.selectList(queryWrapper);

        // 检查是否存在未查询到的ID（不存在的ID）
        List<Long> existingIds = landlords.stream().map(Landlord::getId).toList();
        List<Long> nonExistentIds = landlordIds.stream()
                .filter(id -> !existingIds.contains(id))
                .toList();

        if (!nonExistentIds.isEmpty()) {
            throw new IllegalArgumentException("房东ID不存在: " + nonExistentIds);
        }

        // 检查存在的ID是否属于当前租户
        List<Long> invalidIds = landlords.stream()
                .filter(landlord -> !landlord.getTenantId().equals(tenantId))
                .map(Landlord::getId)
                .toList();

        if (!invalidIds.isEmpty()) {
            throw new IllegalArgumentException("无权限操作房东ID: " + invalidIds);
        }
    }
}
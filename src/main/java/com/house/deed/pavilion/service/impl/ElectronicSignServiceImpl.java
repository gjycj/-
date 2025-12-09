package com.house.deed.pavilion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.entity.ElectronicSign;
import com.house.deed.pavilion.mapper.ElectronicSignMapper;
import com.house.deed.pavilion.service.ElectronicSignService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

/**
 * <p>
 * 电子签约信息表（租户级数据） 服务实现类
 * </p>
 * <p>
 * 负责电子签约业务逻辑的实现，包括增删改查、批量操作、状态管理等核心功能。
 * 所有操作均强制进行租户数据隔离校验，确保数据安全性和一致性。
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Service
public class ElectronicSignServiceImpl extends ServiceImpl<ElectronicSignMapper, ElectronicSign> implements ElectronicSignService {

    /**
     * 签约状态有效值常量定义
     */
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_SIGNED = "SIGNED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_EXPIRED = "EXPIRED";

    /**
     * 新增电子签约记录
     *
     * @param electronicSign 电子签约实体对象，包含签约相关信息
     * @return boolean 新增成功返回true，失败返回false
     * @throws IllegalArgumentException 当参数校验失败时抛出
     *
     * 执行流程：
     * 1. 基础参数校验（租户ID、合同ID、签约平台等必填字段）
     * 2. 签约状态合法性校验
     * 3. 已签约状态特殊字段校验（签约时间、PDF地址等）
     * 4. 调用MyBatis-Plus保存方法持久化数据
     */
    @Override
    public boolean saveElectronicSign(ElectronicSign electronicSign) {
        // 1. 基础参数校验 - 确保业务核心字段不为空
        Assert.notNull(electronicSign.getTenantId(), "租户ID不能为空");
        Assert.notNull(electronicSign.getContractId(), "合同ID不能为空");
        Assert.hasText(electronicSign.getSignPlatform(), "电子签平台不能为空");
        Assert.hasText(electronicSign.getSignUrl(), "签约链接不能为空");
        Assert.hasText(electronicSign.getSignStatus(), "签约状态不能为空");
        Assert.hasText(electronicSign.getSignHash(), "电子签名哈希值不能为空");

        // 2. 签约状态合法性校验
        validateSignStatus(electronicSign.getSignStatus());

        // 3. 已签约状态特殊字段校验
        if (STATUS_SIGNED.equals(electronicSign.getSignStatus())) {
            Assert.notNull(electronicSign.getCustomerSignTime(),
                    "客户签约时间不能为空（已签状态）");
            Assert.notNull(electronicSign.getLandlordSignTime(),
                    "房东签约时间不能为空（已签状态）");
            Assert.hasText(electronicSign.getContractPdfUrl(),
                    "电子合同PDF地址不能为空（已签状态）");
        }

        // 4. 保存数据到数据库
        return save(electronicSign);
    }

    /**
     * 更新电子签约记录
     *
     * @param electronicSign 更新后的电子签约实体对象
     * @return boolean 更新成功返回true，失败返回false
     * @throws IllegalArgumentException 当参数校验失败或权限不足时抛出
     *
     * 执行流程：
     * 1. ID和租户ID非空校验
     * 2. 记录存在性校验和租户归属校验
     * 3. 签约状态合法性校验及特殊字段校验
     * 4. 防止核心字段被篡改的保护机制
     * 5. 执行数据库更新操作
     */
    @Override
    public boolean updateElectronicSignById(ElectronicSign electronicSign) {
        // 1. 基础参数校验
        Assert.notNull(electronicSign.getId(), "记录ID不能为空");
        Assert.notNull(electronicSign.getTenantId(), "租户ID不能为空");

        // 2. 记录存在性及租户归属校验
        ElectronicSign exist = getById(electronicSign.getId());
        Assert.notNull(exist, "电子签约记录不存在");
        Assert.isTrue(Objects.equals(exist.getTenantId(), electronicSign.getTenantId()),
                "无权限操作此记录");

        // 3. 签约状态校验
        if (StringUtils.hasText(electronicSign.getSignStatus())) {
            validateSignStatus(electronicSign.getSignStatus());

            // 状态变更为已签约时，补充必要字段校验
            if (STATUS_SIGNED.equals(electronicSign.getSignStatus())
                    && !STATUS_SIGNED.equals(exist.getSignStatus())) {
                Assert.notNull(electronicSign.getCustomerSignTime(),
                        "客户签约时间不能为空（已签状态）");
                Assert.notNull(electronicSign.getLandlordSignTime(),
                        "房东签约时间不能为空（已签状态）");
                Assert.hasText(electronicSign.getContractPdfUrl(),
                        "电子合同PDF地址不能为空（已签状态）");
            }
        }

        // 4. 防止核心不可变字段被篡改
        electronicSign.setSignHash(exist.getSignHash());      // 电子签名哈希值不可更改
        electronicSign.setContractId(exist.getContractId());   // 关联合同ID不可更改
        electronicSign.setSignPlatform(exist.getSignPlatform()); // 签约平台不可更改

        // 5. 执行更新操作
        return updateById(electronicSign);
    }

    /**
     * 删除电子签约记录
     *
     * @param id 记录ID
     * @param tenantId 租户ID（用于权限校验）
     * @return boolean 删除成功返回true，失败返回false
     * @throws IllegalArgumentException 当参数校验失败或权限不足时抛出
     *
     * 执行流程：
     * 1. 参数非空校验
     * 2. 记录存在性及租户归属校验
     * 3. 执行物理删除操作
     */
    @Override
    public boolean removeElectronicSignById(Long id, Long tenantId) {
        Assert.notNull(id, "记录ID不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");

        // 记录存在性及租户归属校验
        ElectronicSign exist = getById(id);
        Assert.notNull(exist, "电子签约记录不存在");
        Assert.isTrue(Objects.equals(exist.getTenantId(), tenantId),
                "无权限操作此记录");

        return removeById(id);
    }

    /**
     * 按ID查询电子签约记录（租户隔离）
     *
     * @param id 记录ID
     * @param tenantId 租户ID（用于数据隔离）
     * @return ElectronicSign 电子签约实体对象，不存在时返回null
     * @throws IllegalArgumentException 当参数为空时抛出
     *
     * 说明：此方法强制使用租户ID进行数据隔离，确保租户只能访问自己的数据
     */
    @Override
    public ElectronicSign getElectronicSignById(Long id, Long tenantId) {
        Assert.notNull(id, "记录ID不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");

        return getOne(new LambdaQueryWrapper<ElectronicSign>()
                .eq(ElectronicSign::getId, id)
                .eq(ElectronicSign::getTenantId, tenantId));
    }

    /**
     * 多条件分页查询电子签约记录
     *
     * @param page 分页参数对象，包含页码、每页大小等信息
     * @param query 查询条件实体对象，各字段作为查询条件
     * @return IPage<ElectronicSign> 分页结果对象，包含数据列表和分页信息
     * @throws IllegalArgumentException 当租户ID为空时抛出
     *
     * 说明：
     * 1. 强制要求租户ID，确保租户数据隔离
     * 2. 支持按合同ID、签约平台、签约状态等条件查询
     * 3. 默认按创建时间倒序排列（最新记录在前）
     */
    @Override
    public IPage<ElectronicSign> pageQuery(Page<ElectronicSign> page, ElectronicSign query) {
        Assert.notNull(query.getTenantId(), "租户ID不能为空");
        LambdaQueryWrapper<ElectronicSign> wrapper = buildQueryWrapper(query);
        return baseMapper.selectPage(page, wrapper);
    }

    /**
     * 多条件列表查询电子签约记录
     *
     * @param query 查询条件实体对象
     * @return List<ElectronicSign> 符合条件的电子签约记录列表
     * @throws IllegalArgumentException 当租户ID为空时抛出
     *
     * 说明：此方法使用与分页查询相同的条件构建逻辑，但不进行分页处理
     */
    @Override
    public List<ElectronicSign> listByConditions(ElectronicSign query) {
        Assert.notNull(query.getTenantId(), "租户ID不能为空");
        LambdaQueryWrapper<ElectronicSign> wrapper = buildQueryWrapper(query);
        return baseMapper.selectList(wrapper);
    }

    /**
     * 按合同ID查询电子签约记录
     *
     * @param contractId 合同ID
     * @param tenantId 租户ID（用于数据隔离）
     * @return List<ElectronicSign> 该合同下的所有电子签约记录列表
     * @throws IllegalArgumentException 当合同ID或租户ID为空时抛出
     *
     * 说明：通常一个合同可能对应多个签约记录（如多次签约尝试）
     */
    @Override
    public List<ElectronicSign> listByContractId(Long contractId, Long tenantId) {
        Assert.notNull(contractId, "合同ID不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");

        ElectronicSign query = new ElectronicSign();
        query.setTenantId(tenantId);
        query.setContractId(contractId);
        return listByConditions(query);
    }

    /**
     * 批量新增电子签约记录
     *
     * @param signList 电子签约记录列表
     * @return boolean 批量新增成功返回true，失败返回false（事务回滚）
     * @throws IllegalArgumentException 当参数校验失败时抛出
     *
     * 执行流程：
     * 1. 列表非空校验
     * 2. 租户一致性校验（批量记录必须属于同一租户）
     * 3. 逐条记录核心字段校验
     * 4. 已签约状态特殊字段校验
     * 5. 批量保存到数据库（事务保证一致性）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchSaveElectronicSigns(List<ElectronicSign> signList) {
        if (CollectionUtils.isEmpty(signList)) {
            return true;
        }

        // 1. 租户一致性校验
        Long tenantId = signList.get(0).getTenantId();
        Assert.notNull(tenantId, "租户ID不能为空");
        boolean hasInvalidTenant = signList.stream()
                .anyMatch(sign -> !Objects.equals(sign.getTenantId(), tenantId));
        Assert.isTrue(!hasInvalidTenant, "批量记录必须属于同一租户");

        // 2. 逐条记录校验
        for (ElectronicSign sign : signList) {
            Assert.notNull(sign.getContractId(), "合同ID不能为空");
            Assert.hasText(sign.getSignPlatform(), "电子签平台不能为空");
            Assert.hasText(sign.getSignUrl(), "签约链接不能为空");
            Assert.hasText(sign.getSignStatus(), "签约状态不能为空");
            Assert.hasText(sign.getSignHash(), "电子签名哈希值不能为空");
            validateSignStatus(sign.getSignStatus());

            // 已签约状态特殊字段校验
            if (STATUS_SIGNED.equals(sign.getSignStatus())) {
                Assert.notNull(sign.getCustomerSignTime(),
                        "客户签约时间不能为空（已签状态）");
                Assert.notNull(sign.getLandlordSignTime(),
                        "房东签约时间不能为空（已签状态）");
                Assert.hasText(sign.getContractPdfUrl(),
                        "电子合同PDF地址不能为空（已签状态）");
            }
        }

        return saveBatch(signList);
    }

    /**
     * 批量删除电子签约记录
     *
     * @param ids 待删除记录ID列表
     * @param tenantId 租户ID（用于权限校验）
     * @return boolean 批量删除成功返回true，失败返回false（事务回滚）
     * @throws IllegalArgumentException 当参数校验失败或存在跨租户记录时抛出
     *
     * 执行流程：
     * 1. 参数非空校验
     * 2. 跨租户记录校验（防止越权删除）
     * 3. 批量删除操作
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchRemoveElectronicSigns(List<Long> ids, Long tenantId) {
        Assert.notNull(tenantId, "租户ID不能为空");
        Assert.notEmpty(ids, "记录ID列表不能为空");

        // 跨租户记录校验
        long invalidCount = baseMapper.selectCount(new LambdaQueryWrapper<ElectronicSign>()
                .in(ElectronicSign::getId, ids)
                .ne(ElectronicSign::getTenantId, tenantId));
        Assert.isTrue(invalidCount == 0, "存在跨租户记录ID，无法删除");

        return removeByIds(ids);
    }

    /**
     * 批量更新签约状态
     *
     * @param ids 待更新记录ID列表
     * @param signStatus 目标签约状态
     * @param tenantId 租户ID（用于权限校验）
     * @return boolean 批量更新成功返回true，失败返回false（事务回滚）
     * @throws IllegalArgumentException 当参数校验失败或存在跨租户记录时抛出
     *
     * 说明：此方法仅更新签约状态字段，其他字段保持不变
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchUpdateStatus(List<Long> ids, String signStatus, Long tenantId) {
        Assert.notNull(tenantId, "租户ID不能为空");
        Assert.notEmpty(ids, "记录ID列表不能为空");
        Assert.hasText(signStatus, "签约状态不能为空");
        validateSignStatus(signStatus);

        // 跨租户记录校验
        long invalidCount = baseMapper.selectCount(new LambdaQueryWrapper<ElectronicSign>()
                .in(ElectronicSign::getId, ids)
                .ne(ElectronicSign::getTenantId, tenantId));
        Assert.isTrue(invalidCount == 0, "存在跨租户记录ID，无法更新");

        // 构建更新条件和实体
        ElectronicSign updateEntity = new ElectronicSign();
        updateEntity.setSignStatus(signStatus);

        LambdaQueryWrapper<ElectronicSign> updateWrapper = new LambdaQueryWrapper<>();
        updateWrapper.in(ElectronicSign::getId, ids)
                .eq(ElectronicSign::getTenantId, tenantId);

        return baseMapper.update(updateEntity, updateWrapper) > 0;
    }

    /**
     * 构建查询条件包装器
     *
     * @param query 查询条件实体对象
     * @return LambdaQueryWrapper<ElectronicSign> 查询条件包装器
     *
     * 说明：
     * 1. 强制添加租户ID条件，确保数据隔离
     * 2. 动态添加非空字段的查询条件
     * 3. 签约时间使用大于等于(ge)条件，支持时间范围查询
     * 4. 默认按创建时间倒序排列，最新记录优先显示
     */
    private LambdaQueryWrapper<ElectronicSign> buildQueryWrapper(ElectronicSign query) {
        LambdaQueryWrapper<ElectronicSign> wrapper = new LambdaQueryWrapper<>();

        // 强制租户隔离 - 核心安全机制
        wrapper.eq(ElectronicSign::getTenantId, query.getTenantId());

        // 动态条件构建
        if (query.getContractId() != null) {
            wrapper.eq(ElectronicSign::getContractId, query.getContractId());
        }
        if (StringUtils.hasText(query.getSignPlatform())) {
            wrapper.eq(ElectronicSign::getSignPlatform, query.getSignPlatform());
        }
        if (StringUtils.hasText(query.getSignStatus())) {
            wrapper.eq(ElectronicSign::getSignStatus, query.getSignStatus());
        }
        if (query.getCustomerSignTime() != null) {
            // 使用大于等于条件，支持查询指定时间之后的签约记录
            wrapper.ge(ElectronicSign::getCustomerSignTime, query.getCustomerSignTime());
        }
        if (query.getLandlordSignTime() != null) {
            wrapper.ge(ElectronicSign::getLandlordSignTime, query.getLandlordSignTime());
        }
        if (StringUtils.hasText(query.getSignHash())) {
            // 哈希值精确匹配，用于防篡改校验
            wrapper.eq(ElectronicSign::getSignHash, query.getSignHash());
        }

        // 默认排序规则：按创建时间倒序（最新记录在前）
        wrapper.orderByDesc(ElectronicSign::getCreateTime);

        return wrapper;
    }

    /**
     * 校验签约状态合法性
     *
     * @param signStatus 待校验的签约状态值
     * @throws IllegalArgumentException 当状态值不在允许范围内时抛出
     *
     * 有效状态值：
     * - PENDING：待签约
     * - SIGNED：已签约
     * - REJECTED：已拒绝
     * - EXPIRED：已过期
     */
    private void validateSignStatus(String signStatus) {
        List<String> validStatus = List.of(STATUS_PENDING, STATUS_SIGNED,
                STATUS_REJECTED, STATUS_EXPIRED);
        Assert.isTrue(validStatus.contains(signStatus),
                "签约状态无效，允许值：" + String.join("/", validStatus));
    }
}
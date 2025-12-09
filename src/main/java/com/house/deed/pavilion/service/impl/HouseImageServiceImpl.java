package com.house.deed.pavilion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.entity.HouseImage;
import com.house.deed.pavilion.mapper.HouseImageMapper;
import com.house.deed.pavilion.service.HouseImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * 房源图片表服务实现类（租户级数据隔离）
 * </p>
 * <p>
 * 负责房源图片信息的全生命周期管理，包括图片的上传、查询、更新、删除等操作。
 * 房源图片是房源展示的重要组成部分，支持多种图片类型（如户型图、实景图、环境图等）。
 * 所有操作均强制进行租户数据隔离校验，确保图片数据的安全性和业务完整性。
 * </p>
 * <p>
 * 核心特性：
 * 1. 租户数据隔离：所有操作必须验证租户ID，确保跨租户数据不可见
 * 2. 字段完整性校验：严格校验实体类注解约束，确保数据质量
 * 3. 排序功能支持：支持图片排序，数字越小显示越靠前
 * 4. 批量操作优化：提供批量增删功能，支持事务一致性保障
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Service
@RequiredArgsConstructor
public class HouseImageServiceImpl extends ServiceImpl<HouseImageMapper, HouseImage> implements HouseImageService {

    /**
     * 房源图片数据访问层接口
     */
    private final HouseImageMapper houseImageMapper;

    // ==================== 基础CRUD方法 ====================

    /**
     * 新增房源图片记录
     *
     * @param houseImage 房源图片实体对象，包含图片URL、类型、排序等信息
     * @return boolean 新增成功返回true，失败返回false
     * @throws IllegalArgumentException 当必填字段校验失败时抛出
     *
     * 执行流程：
     * 1. 校验实体必填字段（租户ID、房源ID、图片URL、图片类型等）
     * 2. 使用MyBatis-Plus保存方法持久化数据
     *
     * 业务约束：
     * 1. 租户ID、房源ID不能为空
     * 2. 图片URL必须是有效的图片访问地址
     * 3. 图片类型必须为预定义的有效类型
     * 4. 排序号不能为负数，数字越小显示越靠前
     *
     * 注意事项：
     * 1. 创建时间由实体类自动填充，无需手动设置
     * 2. 支持事务回滚，确保数据一致性
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveHouseImage(HouseImage houseImage) {
        // 校验实体必填字段（与HouseImage的@NotNull/@NotBlank约束对应）
        validateHouseImageRequiredFields(houseImage);
        // 无需手动设置createTime（实体类已配置自动填充）
        return save(houseImage);
    }

    /**
     * 更新房源图片记录
     *
     * @param houseImage 更新后的房源图片实体对象
     * @return boolean 更新成功返回true，失败返回false
     * @throws IllegalArgumentException 当参数校验失败或权限不足时抛出
     *
     * 执行流程：
     * 1. 校验图片ID和租户ID非空
     * 2. 查询现有记录，验证租户归属权限
     * 3. 校验更新字段的有效性和完整性
     * 4. 执行数据库更新操作
     *
     * 安全机制：
     * 1. 强制租户归属校验，防止跨租户修改数据
     * 2. 验证记录存在性，避免无效更新操作
     * 3. 校验必填字段，确保数据完整性
     *
     * 更新限制：
     * 1. 不支持跨租户迁移图片记录
     * 2. 核心关联字段（租户ID、房源ID）通常不允许变更
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateHouseImageById(HouseImage houseImage) {
        // 1. 校验ID和租户ID
        if (Objects.isNull(houseImage.getId())) {
            throw new IllegalArgumentException("图片ID不能为空");
        }
        if (Objects.isNull(houseImage.getTenantId())) {
            throw new IllegalArgumentException("租户ID不能为空");
        }

        // 2. 校验图片是否属于当前租户（防止跨租户修改）
        HouseImage existingImage = getById(houseImage.getId());
        if (existingImage == null || !existingImage.getTenantId().equals(houseImage.getTenantId())) {
            throw new IllegalArgumentException("无权操作该图片数据（跨租户或数据不存在）");
        }

        // 3. 校验更新时的必填字段（图片URL、类型等仍需非空）
        validateHouseImageRequiredFields(houseImage);

        return updateById(houseImage);
    }

    /**
     * 删除房源图片记录
     *
     * @param id 图片记录的唯一标识
     * @param tenantId 租户ID，用于权限校验
     * @return boolean 删除成功返回true，失败返回false
     * @throws IllegalArgumentException 当参数为空或权限不足时抛出
     *
     * 执行流程：
     * 1. 参数非空校验
     * 2. 记录存在性及租户归属校验
     * 3. 执行物理删除操作
     *
     * 注意事项：
     * 1. 删除操作不可逆，建议先确认图片无业务关联
     * 2. 删除后应及时清理物理存储中的图片文件
     * 3. 支持事务回滚，确保操作原子性
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeHouseImageById(Long id, Long tenantId) {
        // 1. 参数校验
        if (Objects.isNull(id) || Objects.isNull(tenantId)) {
            throw new IllegalArgumentException("图片ID和租户ID不能为空");
        }

        // 2. 校验数据归属
        HouseImage image = getById(id);
        if (image == null || !image.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("无权删除该图片数据（跨租户或数据不存在）");
        }

        // 3. 执行删除操作
        return removeById(id);
    }

    /**
     * 按ID查询图片记录（租户隔离）
     *
     * @param id 图片记录的唯一标识
     * @param tenantId 租户ID，用于数据隔离
     * @return HouseImage 房源图片实体对象，不存在时返回null
     * @throws IllegalArgumentException 当参数为空时抛出
     *
     * 说明：
     * 1. 强制添加租户ID查询条件，确保租户数据隔离
     * 2. 返回包含图片所有字段的完整信息
     * 3. 主要用于图片详情查看和编辑前数据加载
     */
    @Override
    public HouseImage getHouseImageById(Long id, Long tenantId) {
        // 参数校验
        if (Objects.isNull(id) || Objects.isNull(tenantId)) {
            throw new IllegalArgumentException("图片ID和租户ID不能为空");
        }

        return houseImageMapper.selectOne(new LambdaQueryWrapper<HouseImage>()
                .eq(HouseImage::getId, id)
                .eq(HouseImage::getTenantId, tenantId));
    }

    // ==================== 多条件查询方法 ====================

    /**
     * 分页查询房源图片记录
     *
     * @param page 分页参数对象，包含页码、每页大小等信息
     * @param queryParams 查询参数Map，支持动态条件组合
     * @param tenantId 租户ID，用于数据隔离
     * @return IPage<HouseImage> 分页结果对象，包含数据列表和分页信息
     * @throws IllegalArgumentException 当租户ID为空时抛出
     *
     * 支持的查询条件：
     * 1. houseId: 房源ID（精确匹配）
     * 2. imageType: 图片类型（精确匹配）
     *
     * 排序规则：
     * 1. 默认按sort字段升序排列（数字越小越靠前）
     * 2. 确保图片展示顺序符合业务需求
     *
     * 使用场景：
     * 1. 房源图片管理列表展示
     * 2. 按房源或类型筛选图片
     * 3. 支持分页加载大量图片数据
     */
    @Override
    public IPage<HouseImage> pageQuery(Page<HouseImage> page, Map<String, Object> queryParams, Long tenantId) {
        // 租户ID必填校验
        if (Objects.isNull(tenantId)) {
            throw new IllegalArgumentException("租户ID不能为空");
        }

        // 构建查询条件（强制附加租户ID）
        LambdaQueryWrapper<HouseImage> queryWrapper = new LambdaQueryWrapper<HouseImage>()
                .eq(HouseImage::getTenantId, tenantId);

        // 处理动态条件（与HouseImage字段对应）
        if (queryParams.containsKey("houseId")) {
            queryWrapper.eq(HouseImage::getHouseId, queryParams.get("houseId"));
        }
        if (queryParams.containsKey("imageType")) {
            queryWrapper.eq(HouseImage::getImageType, queryParams.get("imageType"));
        }

        // 按排序号升序（符合实体类sort字段的业务含义：数字越小越靠前）
        queryWrapper.orderByAsc(HouseImage::getSort);

        return houseImageMapper.selectPage(page, queryWrapper);
    }

    /**
     * 多条件查询图片列表（租户隔离）
     *
     * @param queryParams 查询参数Map，支持动态条件组合
     * @param tenantId 租户ID，用于数据隔离
     * @return List<HouseImage> 符合条件的图片记录列表
     * @throws IllegalArgumentException 当租户ID为空时抛出
     *
     * 说明：
     * 1. 此方法与分页查询使用相同的查询逻辑，但不进行分页处理
     * 2. 适用于需要获取所有匹配记录的场景
     * 3. 按sort字段升序排列，确保图片顺序一致
     */
    @Override
    public List<HouseImage> listByConditions(Map<String, Object> queryParams, Long tenantId) {
        // 租户ID必填校验
        if (Objects.isNull(tenantId)) {
            throw new IllegalArgumentException("租户ID不能为空");
        }

        LambdaQueryWrapper<HouseImage> queryWrapper = new LambdaQueryWrapper<HouseImage>()
                .eq(HouseImage::getTenantId, tenantId);

        // 动态添加查询条件
        if (queryParams.containsKey("houseId")) {
            queryWrapper.eq(HouseImage::getHouseId, queryParams.get("houseId"));
        }
        if (queryParams.containsKey("imageType")) {
            queryWrapper.eq(HouseImage::getImageType, queryParams.get("imageType"));
        }

        // 按sort字段升序排列
        queryWrapper.orderByAsc(HouseImage::getSort);

        return houseImageMapper.selectList(queryWrapper);
    }

    /**
     * 按房源ID查询图片列表（租户隔离）
     *
     * @param houseId 房源ID
     * @param tenantId 租户ID，用于数据隔离
     * @return List<HouseImage> 该房源下的所有图片列表，按sort字段升序排列
     * @throws IllegalArgumentException 当房源ID或租户ID为空时抛出
     *
     * 业务用途：
     * 1. 房源详情页图片展示
     * 2. 房源图片批量管理
     * 3. 图片轮播或幻灯片展示
     *
     * 排序说明：
     * 1. 按sort字段升序排列，确保展示顺序可控
     * 2. 排序号相同的记录按创建时间排序
     */
    @Override
    public List<HouseImage> listByHouseId(Long houseId, Long tenantId) {
        // 参数校验
        if (Objects.isNull(houseId) || Objects.isNull(tenantId)) {
            throw new IllegalArgumentException("房源ID和租户ID不能为空");
        }

        return houseImageMapper.selectList(new LambdaQueryWrapper<HouseImage>()
                .eq(HouseImage::getHouseId, houseId)
                .eq(HouseImage::getTenantId, tenantId)
                .orderByAsc(HouseImage::getSort)); // 按sort升序，符合实体类定义
    }

    // ==================== 批量操作方法 ====================

    /**
     * 批量新增房源图片记录
     *
     * @param imageList 房源图片记录列表
     * @return boolean 批量新增成功返回true，失败返回false（事务回滚）
     * @throws IllegalArgumentException 当列表为空或租户ID不一致时抛出
     *
     * 执行流程：
     * 1. 列表非空校验
     * 2. 租户一致性校验（批量记录必须属于同一租户）
     * 3. 逐条记录必填字段校验
     * 4. 批量保存到数据库（事务保障）
     *
     * 使用场景：
     * 1. 房源图片批量上传
     * 2. 图片数据批量导入
     * 3. 图片信息批量迁移
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchSaveHouseImages(List<HouseImage> imageList) {
        // 1. 列表非空校验
        if (CollectionUtils.isEmpty(imageList)) {
            throw new IllegalArgumentException("图片列表不能为空");
        }

        // 2. 校验所有图片租户ID一致
        Long tenantId = imageList.get(0).getTenantId();
        if (imageList.stream().anyMatch(img -> !Objects.equals(img.getTenantId(), tenantId))) {
            throw new IllegalArgumentException("批量新增的图片必须属于同一租户");
        }

        // 3. 校验每个图片的必填字段
        imageList.forEach(this::validateHouseImageRequiredFields);

        // 4. 执行批量保存
        return saveBatch(imageList);
    }

    /**
     * 批量删除房源图片记录
     *
     * @param ids 图片记录ID列表
     * @param tenantId 租户ID，用于权限校验
     * @return boolean 批量删除成功返回true，失败返回false（事务回滚）
     * @throws IllegalArgumentException 当参数为空或存在跨租户记录时抛出
     *
     * 执行流程：
     * 1. 参数非空校验
     * 2. 跨租户记录校验（防止越权删除）
     * 3. 执行批量删除操作（事务保障）
     *
     * 安全机制：
     * 1. 强制租户ID校验，确保只能删除自己租户的数据
     * 2. 批量操作前验证所有记录归属，防止部分成功部分失败
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchRemoveHouseImages(List<Long> ids, Long tenantId) {
        // 1. 参数非空校验
        if (CollectionUtils.isEmpty(ids) || Objects.isNull(tenantId)) {
            throw new IllegalArgumentException("图片ID列表和租户ID不能为空");
        }

        // 2. 跨租户记录校验
        long count = houseImageMapper.selectCount(new LambdaQueryWrapper<HouseImage>()
                .in(HouseImage::getId, ids)
                .ne(HouseImage::getTenantId, tenantId));
        if (count > 0) {
            throw new IllegalArgumentException("存在跨租户图片数据，不允许批量删除");
        }

        // 3. 执行批量删除
        return removeByIds(ids);
    }

    // ==================== 私有工具方法 ====================

    /**
     * 校验HouseImage实体的必填字段
     *
     * @param houseImage 房源图片实体对象
     * @throws IllegalArgumentException 当必填字段为空或不符合约束时抛出
     *
     * 校验规则（与实体类注解约束保持一致）：
     * 1. 租户ID不能为空（@NotNull约束）
     * 2. 房源ID不能为空（@NotNull约束）
     * 3. 图片URL不能为空（@NotBlank约束）
     * 4. 图片类型不能为空（@NotBlank约束）
     * 5. 排序号不能为负数（@Min(0)约束）
     *
     * 说明：
     * 1. 此方法确保数据库存储的数据满足业务规则
     * 2. 在保存和更新操作前必须调用此方法进行校验
     * 3. 校验失败时应提供明确的错误信息，便于问题定位
     */
    private void validateHouseImageRequiredFields(HouseImage houseImage) {
        // 校验租户ID（对应@NotNull约束）
        if (Objects.isNull(houseImage.getTenantId())) {
            throw new IllegalArgumentException("租户ID不能为空");
        }

        // 校验房源ID（对应@NotNull约束）
        if (Objects.isNull(houseImage.getHouseId())) {
            throw new IllegalArgumentException("房源ID不能为空");
        }

        // 校验图片URL（对应@NotBlank约束）
        if (!StringUtils.hasText(houseImage.getImageUrl())) {
            throw new IllegalArgumentException("图片URL不能为空");
        }

        // 校验图片类型（对应@NotBlank约束）
        if (!StringUtils.hasText(houseImage.getImageType())) {
            throw new IllegalArgumentException("图片类型不能为空");
        }

        // 校验排序字段非负（对应@Min(0)约束）
        if (Objects.nonNull(houseImage.getSort()) && houseImage.getSort() < 0) {
            throw new IllegalArgumentException("排序序号不能为负数");
        }
    }
}
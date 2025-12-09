package com.house.deed.pavilion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.entity.House;
import com.house.deed.pavilion.mapper.HouseMapper;
import com.house.deed.pavilion.service.HouseService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 房源信息表（租户核心数据） 服务实现类
 * </p>
 * <p>
 * 负责房源信息的全生命周期管理，包括房源信息的增删改查、批量操作、多条件查询等功能。
 * 所有操作均强制进行租户数据隔离校验，确保数据安全性和业务完整性。
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Service
public class HouseServiceImpl extends ServiceImpl<HouseMapper, House> implements HouseService {

    /**
     * 产权年限最大值（根据中国住宅产权政策）
     */
    private static final int MAX_PROPERTY_RIGHT_YEARS = 70;

    /**
     * 抵押状态：已抵押
     */
    private static final String MORTGAGE_STATUS_MORTGAGED = "MORTGAGED";

    // ==================== 基础CRUD实现 ====================

    /**
     * 新增房源信息
     *
     * @param house 房源实体对象，包含房源所有信息字段
     * @param tenantId 租户ID，用于数据隔离和权限控制
     * @return boolean 新增成功返回true，失败返回false
     * @throws IllegalArgumentException 当参数校验失败或业务规则不满足时抛出
     *
     * 执行流程：
     * 1. 核心参数校验（房源编号、面积、价格等必填字段）
     * 2. 房源字段数值范围校验
     * 3. 强制绑定租户ID，防止跨租户写入
     * 4. 校验租户内房源编号唯一性
     * 5. 调用MyBatis-Plus保存方法持久化数据
     */
    @Override
    public boolean saveHouse(House house, Long tenantId) {
        // 1. 核心参数校验 - 确保业务核心字段不为空
        Assert.notNull(tenantId, "租户ID不能为空");
        Assert.hasText(house.getHouseNo(), "房源编号不能为空");
        Assert.notNull(house.getArea(), "房源面积不能为空");
        Assert.notNull(house.getPrice(), "房源价格不能为空");
        Assert.hasText(house.getHouseType(), "户型不能为空");
        Assert.notNull(house.getStatus(), "房源状态不能为空");

        // 2. 房源字段数值范围校验
        validateHouseFields(house);

        // 3. 强制绑定租户ID（防止跨租户写入）
        house.setTenantId(tenantId);

        // 4. 校验租户内房源编号唯一性
        long count = baseMapper.selectCount(new LambdaQueryWrapper<House>()
                .eq(House::getTenantId, tenantId)
                .eq(House::getHouseNo, house.getHouseNo()));
        Assert.isTrue(count == 0, "当前租户下房源编号已存在：" + house.getHouseNo());

        // 5. 保存数据到数据库
        return save(house);
    }

    /**
     * 验证房源字段的数值范围
     *
     * @param house 房源实体对象
     * @throws IllegalArgumentException 当字段值不符合业务规则时抛出
     *
     * 校验规则：
     * 1. 建筑面积必须大于0
     * 2. 套内面积不能为负数（可为null）
     * 3. 挂牌价必须大于0
     * 4. 楼层必须大于0且所在楼层不能大于总楼层
     * 5. 产权年限在1-70年之间
     * 6. 抵押状态为已抵押时，抵押详情不能为空
     */
    private void validateHouseFields(House house) {
        // 1. 面积校验
        if (house.getArea() != null) {
            Assert.isTrue(house.getArea().compareTo(BigDecimal.ZERO) > 0,
                    "建筑面积必须大于0");
        }

        // 2. 套内面积校验（可为null）
        if (house.getInsideArea() != null) {
            Assert.isTrue(house.getInsideArea().compareTo(BigDecimal.ZERO) >= 0,
                    "套内面积不能为负数");
        }

        // 3. 价格校验
        if (house.getPrice() != null) {
            Assert.isTrue(house.getPrice().compareTo(BigDecimal.ZERO) > 0,
                    "挂牌价必须大于0");
        }

        // 4. 楼层校验
        if (house.getFloor() != null) {
            Assert.isTrue(house.getFloor() > 0, "所在楼层必须大于0");
        }

        if (house.getTotalFloor() != null) {
            Assert.isTrue(house.getTotalFloor() > 0, "总楼层必须大于0");
        }

        if (house.getFloor() != null && house.getTotalFloor() != null) {
            Assert.isTrue(house.getFloor() <= house.getTotalFloor(),
                    "所在楼层不能大于总楼层");
        }

        // 5. 产权年限校验
        if (house.getPropertyRightYears() != null) {
            Assert.isTrue(house.getPropertyRightYears() >= 1,
                    "产权年限不能小于1");
            Assert.isTrue(house.getPropertyRightYears() <= MAX_PROPERTY_RIGHT_YEARS,
                    "产权年限不能超过" + MAX_PROPERTY_RIGHT_YEARS + "年");
        }

        // 6. 抵押状态校验
        if (MORTGAGE_STATUS_MORTGAGED.equals(house.getMortgageStatus())) {
            Assert.hasText(house.getMortgageDetails(),
                    "抵押状态为已抵押时，抵押详情不能为空");
        }
    }

    /**
     * 更新房源信息
     *
     * @param house 更新后的房源实体对象
     * @param tenantId 租户ID，用于权限校验
     * @return boolean 更新成功返回true，失败返回false
     * @throws IllegalArgumentException 当参数校验失败或权限不足时抛出
     *
     * 执行流程：
     * 1. 房源ID和租户ID非空校验
     * 2. 记录存在性校验和租户归属校验
     * 3. 若更新房源编号，校验新编号在租户内的唯一性
     * 4. 执行数据库更新操作
     */
    @Override
    public boolean updateHouseById(House house, Long tenantId) {
        // 1. 基础校验
        Assert.notNull(house.getId(), "房源ID不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");

        // 2. 校验房源存在且归属当前租户
        House exist = getById(house.getId());
        Assert.notNull(exist, "房源不存在");
        Assert.isTrue(Objects.equals(exist.getTenantId(), tenantId),
                "无权限操作其他租户的房源");

        // 3. 若更新房源编号，校验新编号唯一性
        if (StringUtils.hasText(house.getHouseNo()) &&
                !house.getHouseNo().equals(exist.getHouseNo())) {
            long count = baseMapper.selectCount(new LambdaQueryWrapper<House>()
                    .eq(House::getTenantId, tenantId)
                    .eq(House::getHouseNo, house.getHouseNo())
                    .ne(House::getId, house.getId()));
            Assert.isTrue(count == 0, "新房源编号已存在：" + house.getHouseNo());
        }

        return updateById(house);
    }

    /**
     * 删除房源信息
     *
     * @param id 房源ID
     * @param tenantId 租户ID，用于权限校验
     * @return boolean 删除成功返回true，失败返回false
     * @throws IllegalArgumentException 当参数校验失败或权限不足时抛出
     *
     * 执行流程：
     * 1. 房源ID和租户ID非空校验
     * 2. 记录存在性校验和租户归属校验
     * 3. 执行物理删除操作
     */
    @Override
    public boolean removeHouseById(Long id, Long tenantId) {
        // 1. 基础校验
        Assert.notNull(id, "房源ID不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");

        // 2. 校验房源存在且归属当前租户
        House exist = getById(id);
        Assert.notNull(exist, "房源不存在");
        Assert.isTrue(Objects.equals(exist.getTenantId(), tenantId),
                "无权限操作其他租户的房源");

        return removeById(id);
    }

    /**
     * 按ID查询房源信息（租户隔离）
     *
     * @param id 房源ID
     * @param tenantId 租户ID，用于数据隔离
     * @return House 房源实体对象，不存在时返回null
     * @throws IllegalArgumentException 当参数为空时抛出
     *
     * 说明：此方法强制使用租户ID进行数据隔离，确保租户只能访问自己的房源数据
     */
    @Override
    public House getHouseById(Long id, Long tenantId) {
        // 1. 基础校验
        Assert.notNull(id, "房源ID不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");

        // 2. 租户隔离查询
        return getOne(new LambdaQueryWrapper<House>()
                .eq(House::getId, id)
                .eq(House::getTenantId, tenantId));
    }

    // ==================== 多条件查询实现 ====================

    /**
     * 多条件分页查询房源信息
     *
     * @param page 分页参数对象，包含页码、每页大小等信息
     * @param queryParams 查询条件Map，支持多种组合条件
     * @param tenantId 租户ID，用于数据隔离
     * @return IPage<House> 分页结果对象，包含数据列表和分页信息
     * @throws IllegalArgumentException 当租户ID为空时抛出
     *
     * 说明：
     * 1. 强制要求租户ID，确保租户数据隔离
     * 2. 支持按房源编号、状态、户型、面积范围、价格范围等条件查询
     * 3. 默认按创建时间倒序排列（最新房源在前）
     */
    @Override
    public IPage<House> pageQuery(Page<House> page, Map<String, Object> queryParams, Long tenantId) {
        Assert.notNull(tenantId, "租户ID不能为空");
        LambdaQueryWrapper<House> wrapper = buildQueryWrapper(queryParams, tenantId);
        return baseMapper.selectPage(page, wrapper);
    }

    /**
     * 多条件列表查询房源信息
     *
     * @param queryParams 查询条件Map
     * @param tenantId 租户ID，用于数据隔离
     * @return List<House> 符合条件的房源列表
     * @throws IllegalArgumentException 当租户ID为空时抛出
     *
     * 说明：此方法使用与分页查询相同的条件构建逻辑，但不进行分页处理
     */
    @Override
    public List<House> listByConditions(Map<String, Object> queryParams, Long tenantId) {
        Assert.notNull(tenantId, "租户ID不能为空");
        LambdaQueryWrapper<House> wrapper = buildQueryWrapper(queryParams, tenantId);
        return baseMapper.selectList(wrapper);
    }

    /**
     * 按楼盘ID查询房源信息
     *
     * @param propertyId 楼盘ID
     * @param tenantId 租户ID，用于数据隔离
     * @return List<House> 该楼盘下的所有房源列表
     * @throws IllegalArgumentException 当楼盘ID或租户ID为空时抛出
     *
     * 说明：
     * 1. 查询同一租户下指定楼盘的所有房源
     * 2. 按创建时间倒序排列，最新房源在前
     */
    @Override
    public List<House> listByPropertyId(Long propertyId, Long tenantId) {
        // 参数校验
        Assert.notNull(propertyId, "楼盘ID不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");

        // 查询同一租户下指定楼盘的所有房源，按创建时间倒序排列
        LambdaQueryWrapper<House> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(House::getTenantId, tenantId)
                .eq(House::getBuildingId, propertyId)  // 使用 buildingId 字段关联楼盘
                .orderByDesc(House::getCreateTime);

        return baseMapper.selectList(wrapper);
    }

    // ==================== 批量操作实现 ====================

    /**
     * 批量新增房源信息
     *
     * @param houseList 房源记录列表
     * @param tenantId 租户ID，用于数据隔离
     * @return boolean 批量新增成功返回true，失败返回false（事务回滚）
     * @throws IllegalArgumentException 当参数校验失败时抛出
     *
     * 执行流程：
     * 1. 参数非空校验
     * 2. 逐条记录核心字段校验
     * 3. 强制绑定租户ID，确保数据归属正确
     * 4. 校验房源编号在租户内的唯一性
     * 5. 批量保存到数据库（事务保证一致性）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchSaveHouses(List<House> houseList, Long tenantId) {
        // 1. 基础校验
        Assert.notNull(tenantId, "租户ID不能为空");
        Assert.notEmpty(houseList, "房源列表不能为空");

        // 2. 校验批次内房源编号唯一性（避免同一批次内重复）
        Set<String> batchHouseNos = new HashSet<>();
        for (House house : houseList) {
            String houseNo = house.getHouseNo();
            if (batchHouseNos.contains(houseNo)) {
                throw new IllegalArgumentException("同一批次中存在重复的房源编号：" + houseNo);
            }
            batchHouseNos.add(houseNo);
        }

        // 3. 校验租户一致性+核心字段
        houseList.forEach(house -> {
            Assert.hasText(house.getHouseNo(), "房源编号不能为空");
            Assert.notNull(house.getArea(), "房源面积不能为空");
            Assert.notNull(house.getPrice(), "房源价格不能为空");

            // 强制绑定租户ID
            house.setTenantId(tenantId);

            // 校验数值范围
            validateHouseFields(house);

            // 注意：这里不要校验编号唯一性，因为批量插入时可能都还不存在
        });

        // 4. 批量保存前再次校验数据库唯一性（使用批量查询优化）
        List<String> houseNos = houseList.stream()
                .map(House::getHouseNo)
                .distinct()
                .collect(Collectors.toList());

        if (!houseNos.isEmpty()) {
            // 批量查询已存在的房源编号
            List<String> existingHouseNos = baseMapper.selectList(
                            new LambdaQueryWrapper<House>()
                                    .select(House::getHouseNo)
                                    .eq(House::getTenantId, tenantId)
                                    .in(House::getHouseNo, houseNos)
                    ).stream()
                    .map(House::getHouseNo)
                    .collect(Collectors.toList());

            if (!existingHouseNos.isEmpty()) {
                throw new IllegalArgumentException("房源编号已存在：" + existingHouseNos);
            }
        }

        // 5. 执行批量保存
        return saveBatch(houseList);
    }

    /**
     * 批量更新房源状态
     *
     * @param ids 待更新记录ID列表
     * @param status 目标状态值
     * @param tenantId 租户ID，用于权限校验
     * @return boolean 批量更新成功返回true，失败返回false（事务回滚）
     * @throws IllegalArgumentException 当参数校验失败或存在跨租户记录时抛出
     *
     * 说明：此方法仅更新房源状态字段，其他字段保持不变
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchUpdateStatus(List<Long> ids, String status, Long tenantId) {
        // 1. 基础校验
        Assert.notNull(tenantId, "租户ID不能为空");
        Assert.notEmpty(ids, "房源ID列表不能为空");
        Assert.notNull(status, "目标状态不能为空");

        // 2. 校验所有ID归属当前租户
        long invalidCount = baseMapper.selectCount(new LambdaQueryWrapper<House>()
                .in(House::getId, ids)
                .ne(House::getTenantId, tenantId));
        Assert.isTrue(invalidCount == 0, "存在跨租户的房源ID，无法批量更新");

        // 3. 批量更新状态
        House updateEntity = new House();
        updateEntity.setStatus(status);
        LambdaQueryWrapper<House> wrapper = new LambdaQueryWrapper<House>()
                .in(House::getId, ids)
                .eq(House::getTenantId, tenantId);

        return baseMapper.update(updateEntity, wrapper) > 0;
    }

    /**
     * 批量删除房源信息
     *
     * @param ids 待删除记录ID列表
     * @param tenantId 租户ID，用于权限校验
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
    public boolean batchRemoveHouses(List<Long> ids, Long tenantId) {
        // 1. 基础校验
        Assert.notNull(tenantId, "租户ID不能为空");
        Assert.notEmpty(ids, "房源ID列表不能为空");

        // 2. 跨租户记录校验
        long invalidCount = baseMapper.selectCount(new LambdaQueryWrapper<House>()
                .in(House::getId, ids)
                .ne(House::getTenantId, tenantId));
        Assert.isTrue(invalidCount == 0, "存在跨租户的房源ID，无法批量删除");

        return removeByIds(ids);
    }

    // ==================== 私有工具方法 ====================

    /**
     * 构建多条件查询条件
     *
     * @param queryParams 查询条件Map，支持动态条件组合
     * @param tenantId 租户ID，用于数据隔离
     * @return LambdaQueryWrapper<House> 查询条件包装器
     *
     * 支持的查询条件：
     * 1. houseNo: 房源编号（精确匹配）
     * 2. status: 房源状态（精确匹配：如1=在售，2=已租，3=已售等）
     * 3. houseType: 户型（精确匹配：如"一室一厅"、"两室两厅"）
     * 4. minArea/maxArea: 面积范围（大于等于/小于等于）
     * 5. minPrice/maxPrice: 价格范围（大于等于/小于等于）
     * 6. orientation: 朝向（模糊匹配：如"南"、"南北"）
     * 7. decoration: 装修类型（精确匹配：如"精装"、"简装"）
     *
     * 说明：
     * 1. 强制添加租户ID条件，确保数据隔离
     * 2. 动态添加非空字段的查询条件
     * 3. 默认按创建时间倒序排列，最新房源优先显示
     */
    private LambdaQueryWrapper<House> buildQueryWrapper(Map<String, Object> queryParams, Long tenantId) {
        LambdaQueryWrapper<House> wrapper = new LambdaQueryWrapper<>();

        // 强制租户隔离（所有查询必带）
        wrapper.eq(House::getTenantId, tenantId);

        if (CollectionUtils.isEmpty(queryParams)) {
            wrapper.orderByDesc(House::getCreateTime);
            return wrapper;
        }

        // 1. 房源编号（精确匹配）
        if (StringUtils.hasText((String) queryParams.get("houseNo"))) {
            wrapper.eq(House::getHouseNo, queryParams.get("houseNo"));
        }

        // 2. 房源状态（精确匹配：如1=在售，2=已租，3=已售等）
        if (queryParams.get("status") != null) {
            wrapper.eq(House::getStatus, queryParams.get("status"));
        }

        // 3. 户型（精确匹配：如"一室一厅"、"两室两厅"）
        if (StringUtils.hasText((String) queryParams.get("houseType"))) {
            wrapper.eq(House::getHouseType, queryParams.get("houseType"));
        }

        // 4. 面积范围（大于等于最小值）
        if (queryParams.get("minArea") != null) {
            wrapper.ge(House::getArea, queryParams.get("minArea"));
        }
        // 面积范围（小于等于最大值）
        if (queryParams.get("maxArea") != null) {
            wrapper.le(House::getArea, queryParams.get("maxArea"));
        }

        // 5. 价格范围（大于等于最小值）
        if (queryParams.get("minPrice") != null) {
            wrapper.ge(House::getPrice, queryParams.get("minPrice"));
        }
        // 价格范围（小于等于最大值）
        if (queryParams.get("maxPrice") != null) {
            wrapper.le(House::getPrice, queryParams.get("maxPrice"));
        }

        // 6. 朝向（模糊匹配：如"南"、"南北"）
        if (StringUtils.hasText((String) queryParams.get("orientation"))) {
            wrapper.like(House::getOrientation, queryParams.get("orientation"));
        }

        // 7. 装修类型（精确匹配：如"精装"、"简装"）
        if (StringUtils.hasText((String) queryParams.get("decoration"))) {
            wrapper.eq(House::getDecoration, queryParams.get("decoration"));
        }

        // 默认排序：按创建时间倒序（最新房源在前）
        wrapper.orderByDesc(House::getCreateTime);

        return wrapper;
    }
}
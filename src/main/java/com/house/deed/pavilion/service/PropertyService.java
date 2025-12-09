package com.house.deed.pavilion.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.house.deed.pavilion.entity.Property;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 楼盘信息表（租户级数据隔离） 服务类
 * </p>
 * 核心业务：楼盘信息的CRUD、多条件查询、批量操作（严格遵循租户隔离+实体约束）
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
public interface PropertyService extends IService<Property> {

    // ==================== 基础CRUD（租户隔离+实体约束校验） ====================
    /**
     * 新增楼盘信息（严格校验实体类约束：字段非空、长度、数值范围等）
     * @param property 楼盘实体
     * @return 是否新增成功
     */
    boolean saveProperty(Property property);

    /**
     * 根据ID更新楼盘信息（禁止修改创建人/租户ID，仅允许修改业务字段）
     * @param property 楼盘实体
     * @return 是否更新成功
     */
    boolean updatePropertyById(Property property);

    /**
     * 根据ID删除楼盘信息（租户隔离，仅当前租户可删除自身楼盘）
     * @param id 楼盘ID
     * @param tenantId 租户ID
     * @return 是否删除成功
     */
    boolean removePropertyById(Long id, Long tenantId);

    /**
     * 根据ID查询楼盘信息（租户隔离，仅当前租户可查询自身楼盘）
     * @param id 楼盘ID
     * @param tenantId 租户ID
     * @return 楼盘实体
     */
    Property getPropertyById(Long id, Long tenantId);

    // ==================== 多条件查询（贴合实体类字段+业务场景） ====================
    /**
     * 分页查询楼盘信息（多条件+租户隔离）
     * @param page 分页参数
     * @param queryParams 查询条件（propertyName/regionId/developer/completionYear等）
     * @param tenantId 租户ID
     * @return 分页结果
     */
    IPage<Property> pageQuery(Page<Property> page, Map<String, Object> queryParams, Long tenantId);

    /**
     * 多条件查询楼盘列表（租户隔离）
     * @param queryParams 查询条件
     * @param tenantId 租户ID
     * @return 楼盘列表
     */
    List<Property> listByConditions(Map<String, Object> queryParams, Long tenantId);

    /**
     * 按区域ID查询楼盘（租户隔离）
     * @param regionId 区域ID
     * @param tenantId 租户ID
     * @return 楼盘列表
     */
    List<Property> listByRegionId(Long regionId, Long tenantId);

    /**
     * 按开发商名称模糊查询楼盘（租户隔离）
     * @param developer 开发商名称（支持模糊匹配）
     * @param tenantId 租户ID
     * @return 楼盘列表
     */
    List<Property> listByDeveloper(String developer, Long tenantId);

    /**
     * 按建成年份范围查询楼盘（租户隔离）
     * @param startYear 起始年份
     * @param endYear 结束年份
     * @param tenantId 租户ID
     * @return 楼盘列表
     */
    List<Property> listByCompletionYearRange(Integer startYear, Integer endYear, Long tenantId);

    // ==================== 批量操作（事务保障+租户隔离） ====================
    /**
     * 批量新增楼盘信息（同一租户，事务保证）
     * @param propertyList 楼盘列表
     * @return 是否批量新增成功
     */
    boolean batchSaveProperties(List<Property> propertyList);

    /**
     * 批量更新楼盘信息（同一租户，仅允许修改业务字段，事务保证）
     * @param propertyList 楼盘列表
     * @param tenantId 租户ID
     * @return 是否批量更新成功
     */
    boolean batchUpdateProperties(List<Property> propertyList, Long tenantId);

    /**
     * 批量删除楼盘信息（租户隔离，事务保证）
     * @param ids 楼盘ID列表
     * @param tenantId 租户ID
     * @return 是否批量删除成功
     */
    boolean batchRemoveProperties(List<Long> ids, Long tenantId);

    /**
     * 校验楼盘ID列表是否属于当前租户
     * @param tenantId 租户ID
     * @param propertyIds 楼盘ID列表
     */
    void validatePropertyIdsBelongToTenant(Long tenantId, List<Long> propertyIds);
}
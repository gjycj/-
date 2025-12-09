package com.house.deed.pavilion.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.house.deed.pavilion.entity.HouseImage;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 房源图片表（租户级数据） 服务类
 * </p>
 *
 * <p>
 * 提供房源图片的CRUD、多条件查询及批量操作功能，强制租户数据隔离，确保图片资源归属清晰。
 * 支持按房源ID、图片类型等条件筛选，满足房源图片管理的核心业务需求。
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
public interface HouseImageService extends IService<HouseImage> {

    /**
     * 新增房源图片（带租户校验）
     *
     * @param houseImage 房源图片实体（需包含租户ID、房源ID、图片URL等核心字段）
     * @return 是否新增成功
     * @throws IllegalArgumentException 当必填参数缺失或业务规则不满足时抛出
     */
    boolean saveHouseImage(HouseImage houseImage);

    /**
     * 更新房源图片（带租户校验）
     *
     * @param houseImage 图片实体（需包含ID和租户ID）
     * @return 是否更新成功
     * @throws IllegalArgumentException 当参数无效或无权限时抛出
     */
    boolean updateHouseImageById(HouseImage houseImage);

    /**
     * 删除房源图片（带租户校验）
     *
     * @param id       图片ID
     * @param tenantId 租户ID
     * @return 是否删除成功
     * @throws IllegalArgumentException 当参数无效或无权限时抛出
     */
    boolean removeHouseImageById(Long id, Long tenantId);

    /**
     * 按ID查询图片（租户隔离）
     *
     * @param id       图片ID
     * @param tenantId 租户ID
     * @return 图片实体（不存在或无权限时返回null）
     */
    HouseImage getHouseImageById(Long id, Long tenantId);

    /**
     * 分页查询房源图片（多条件+租户隔离）
     *
     * @param page        分页参数
     * @param queryParams 查询条件（支持：houseId、imageType、uploadTime范围等）
     * @param tenantId    租户ID
     * @return 分页结果
     */
    IPage<HouseImage> pageQuery(Page<HouseImage> page, Map<String, Object> queryParams, Long tenantId);

    /**
     * 多条件查询图片列表（租户隔离）
     *
     * @param queryParams 查询条件
     * @param tenantId    租户ID
     * @return 图片列表
     */
    List<HouseImage> listByConditions(Map<String, Object> queryParams, Long tenantId);

    /**
     * 按房源ID查询图片列表（租户隔离）
     *
     * @param houseId  房源ID
     * @param tenantId 租户ID
     * @return 图片列表（按排序号升序）
     */
    List<HouseImage> listByHouseId(Long houseId, Long tenantId);

    /**
     * 批量新增房源图片（同一租户）
     *
     * @param imageList 图片列表（需包含相同租户ID）
     * @return 是否批量新增成功
     * @throws IllegalArgumentException 当列表为空或租户ID不一致时抛出
     */
    boolean batchSaveHouseImages(List<HouseImage> imageList);

    /**
     * 批量删除房源图片（租户隔离）
     *
     * @param ids      图片ID列表
     * @param tenantId 租户ID
     * @return 是否批量删除成功
     * @throws IllegalArgumentException 当参数无效或存在跨租户数据时抛出
     */
    boolean batchRemoveHouseImages(List<Long> ids, Long tenantId);
}
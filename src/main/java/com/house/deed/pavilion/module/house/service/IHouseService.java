package com.house.deed.pavilion.module.house.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.house.deed.pavilion.module.house.dto.HouseAddDTO;
import com.house.deed.pavilion.module.house.entity.House;
import com.house.deed.pavilion.common.exception.BusinessException;

/**
 * <p>
 * 房源信息表（租户核心数据）服务接口
 * 负责房源的CRUD、分页查询、录入校验、删除备份等核心业务逻辑
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
public interface IHouseService extends IService<House> {

    /**
     * 分页查询房源列表（支持按房号和状态筛选）
     * <p>
     * 自动过滤当前租户数据，房号支持模糊匹配，状态为精确匹配
     * </p>
     *
     * @param page     分页参数（包含页码、每页条数）
     * @param houseNo  房号筛选条件（可为空，空则不筛选）
     * @param status   房源状态筛选条件（可为空，空则不筛选）
     * @return 分页查询结果，包含房源列表和分页信息
     */
    Page<House> getHousePage(Page<House> page, String houseNo, String status);

    /**
     * 检查房源是否存在（基于ID）
     * <p>
     * 仅判断当前租户下的房源是否存在，用于操作前的有效性校验
     * </p>
     *
     * @param id 房源ID
     * @return true-存在，false-不存在
     */
    boolean existsById(Long id);

    /**
     * 房源录入（核心业务：保存房源信息并关联相关数据）
     * <p>
     * 业务流程：
     * 1. 校验DTO参数合法性（必填字段、数据格式等）
     * 2. 转换DTO为House实体，设置创建人（当前经纪人ID）和租户ID（自动填充）
     * 3. 保存房源主表信息
     * 4. 关联房东信息（基于DTO中的landlordIds）
     * 5. 关联标签信息（基于DTO中的tagIds）
     * 6. 保存房源图片（基于DTO中的imageList）
     * </p>
     *
     * @param dto           房源录入请求DTO（包含房源基本信息、关联房东ID列表等）
     * @param currentAgentId 当前登录经纪人ID（用于记录房源创建人）
     * @return 录入成功的房源ID（自增主键）
     * @throws BusinessException 当参数校验失败、关联数据不存在或保存失败时抛出
     */
    Long addHouse(HouseAddDTO dto, Long currentAgentId);

    /**
     * 删除房源并备份（逻辑删除+数据存档）
     * <p>
     * 业务流程：
     * 1. 校验房源是否存在（当前租户下）
     * 2. 将房源信息备份到house_backup表（包含删除操作人、删除时间）
     * 3. 逻辑删除房源主表记录（或物理删除，根据业务需求）
     * 4. 级联处理关联数据（如解除房东关联、标记图片为失效等）
     * </p>
     *
     * @param id       要删除的房源ID
     * @param operator 操作人（通常为当前登录用户账号）
     * @return true-删除和备份均成功，false-操作失败
     * @throws BusinessException 当房源不存在、备份失败或删除失败时抛出
     */
    boolean deleteAndBackup(Long id, String operator);
}
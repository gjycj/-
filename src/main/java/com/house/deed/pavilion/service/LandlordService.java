package com.house.deed.pavilion.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.house.deed.pavilion.entity.Landlord;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

/**
 * 房东信息服务接口
 *
 * <p>提供房东信息的增删改查及批量操作功能，所有操作均支持租户级数据隔离</p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
public interface LandlordService extends IService<Landlord> {

    /**
     * 新增房东信息
     *
     * @param landlord 房东实体对象，包含需要新增的房东信息
     * @return 新增成功返回true，否则返回false
     */
    boolean saveLandlord(Landlord landlord);

    /**
     * 根据ID更新房东信息
     *
     * @param landlord 房东实体对象，需包含主键ID和需要更新的字段
     * @return 更新成功返回true，否则返回false
     */
    boolean updateLandlordById(Landlord landlord);

    /**
     * 根据ID物理删除房东信息（支持租户隔离）
     *
     * @param id 房东主键ID
     * @param tenantId 租户ID，用于数据隔离验证
     * @return 删除成功返回true，否则返回false
     */
    boolean removeLandlordById(Long id, Long tenantId);

    /**
     * 根据ID查询房东详细信息（租户隔离）
     *
     * @param id 房东主键ID
     * @param tenantId 租户ID，用于数据隔离验证
     * @return 对应的房东实体对象，未找到返回null
     */
    Landlord getLandlordById(Long id, Long tenantId);

    /**
     * 多条件分页查询房东信息
     *
     * @param page 分页参数对象
     * @param landlord 查询条件实体对象（支持模糊查询条件）
     * @param tenantId 租户ID，用于数据隔离
     * @return 分页查询结果，包含房东列表和分页信息
     */
    IPage<Landlord> pageQuery(Page<Landlord> page, Landlord landlord, Long tenantId);

    /**
     * 批量新增房东信息
     *
     * @param landlords 房东实体对象列表
     * @return 批量新增成功返回true，否则返回false
     * @apiNote 建议在业务层控制批量操作的数据量
     */
    boolean batchSaveLandlords(List<Landlord> landlords);

    /**
     * 批量删除房东信息（支持租户隔离）
     *
     * @param ids 待删除的房东ID列表
     * @param tenantId 租户ID，用于数据隔离验证
     * @return 批量删除成功返回true，否则返回false
     */
    boolean batchRemoveLandlords(List<Long> ids, Long tenantId);

    /**
     * 验证房东ID列表是否属于指定租户
     *
     * @param tenantId 租户ID
     * @param landlordIds 待验证的房东ID列表
     */
    void validateLandlordIdsBelongToTenant(Long tenantId, List<Long> landlordIds);
}
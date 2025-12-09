package com.house.deed.pavilion.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.house.deed.pavilion.entity.ElectronicSign;
import java.util.List;

/**
 * <p>
 * 电子签约信息表（租户级数据） 服务类
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
public interface ElectronicSignService extends IService<ElectronicSign> {

    // ==================== 单条CRUD（租户隔离增强） ====================
    /**
     * 新增电子签约记录（带租户校验）
     * @param electronicSign 电子签约实体
     * @return 是否新增成功
     */
    boolean saveElectronicSign(ElectronicSign electronicSign);

    /**
     * 更新电子签约记录（带租户校验）
     * @param electronicSign 电子签约实体
     * @return 是否更新成功
     */
    boolean updateElectronicSignById(ElectronicSign electronicSign);

    /**
     * 删除电子签约记录（带租户校验）
     * @param id 记录ID
     * @param tenantId 租户ID
     * @return 是否删除成功
     */
    boolean removeElectronicSignById(Long id, Long tenantId);

    /**
     * 按ID查询电子签约记录（带租户隔离）
     * @param id 记录ID
     * @param tenantId 租户ID
     * @return 电子签约实体
     */
    ElectronicSign getElectronicSignById(Long id, Long tenantId);

    // ==================== 多条件查询 ====================
    /**
     * 分页查询电子签约记录（多条件+租户隔离）
     * @param page 分页参数
     * @param query 查询条件（含租户ID）
     * @return 分页结果
     */
    IPage<ElectronicSign> pageQuery(Page<ElectronicSign> page, ElectronicSign query);

    /**
     * 多条件查询电子签约列表（租户隔离）
     * @param query 查询条件（含租户ID）
     * @return 电子签约列表
     */
    List<ElectronicSign> listByConditions(ElectronicSign query);

    /**
     * 按合同ID查询电子签约记录（租户隔离）
     * @param contractId 合同ID
     * @param tenantId 租户ID
     * @return 电子签约列表（按创建时间倒序）
     */
    List<ElectronicSign> listByContractId(Long contractId, Long tenantId);

    // ==================== 批量操作 ====================
    /**
     * 批量新增电子签约记录（同一租户）
     * @param signList 电子签约列表
     * @return 是否批量新增成功
     */
    boolean batchSaveElectronicSigns(List<ElectronicSign> signList);

    /**
     * 批量删除电子签约记录（租户隔离）
     * @param ids 记录ID列表
     * @param tenantId 租户ID
     * @return 是否批量删除成功
     */
    boolean batchRemoveElectronicSigns(List<Long> ids, Long tenantId);

    /**
     * 批量更新签约状态（租户隔离）
     * @param ids 记录ID列表
     * @param signStatus 目标状态
     * @param tenantId 租户ID
     * @return 是否批量更新成功
     */
    boolean batchUpdateStatus(List<Long> ids, String signStatus, Long tenantId);
}
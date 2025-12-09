package com.house.deed.pavilion.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.house.deed.pavilion.entity.LoanMaterial;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 贷款材料提交记录表（租户级数据） 服务类
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
public interface LoanMaterialService extends IService<LoanMaterial> {

    // ==================== 基础CRUD（带租户隔离） ====================
    /**
     * 新增贷款材料（带租户校验）
     * @param loanMaterial 贷款材料实体
     * @return 是否新增成功
     */
    boolean saveLoanMaterial(LoanMaterial loanMaterial);

    /**
     * 根据ID更新贷款材料（带租户校验）
     * @param loanMaterial 贷款材料实体
     * @return 是否更新成功
     */
    boolean updateLoanMaterialById(LoanMaterial loanMaterial);

    /**
     * 根据ID删除贷款材料（带租户校验）
     * @param id 材料ID
     * @param tenantId 租户ID
     * @return 是否删除成功
     */
    boolean removeLoanMaterialById(Long id, Long tenantId);

    /**
     * 根据ID查询贷款材料（带租户隔离）
     * @param id 材料ID
     * @param tenantId 租户ID
     * @return 贷款材料实体
     */
    LoanMaterial getLoanMaterialById(Long id, Long tenantId);


    // ==================== 自定义查询（已存在，补充说明） ====================
    /**
     * 分页查询贷款材料记录（支持多条件+租户隔离）
     * @param page 分页参数
     * @param queryParams 查询条件（loanId、materialType、status等）
     * @param tenantId 租户ID
     * @return 分页结果
     */
    IPage<LoanMaterial> pageQuery(Page<LoanMaterial> page, Map<String, Object> queryParams, Long tenantId);

    /**
     * 多条件查询贷款材料列表（租户隔离）
     * @param queryParams 查询条件
     * @param tenantId 租户ID
     * @return 材料列表
     */
    List<LoanMaterial> listByConditions(Map<String, Object> queryParams, Long tenantId);

    /**
     * 根据贷款ID查询材料列表
     * @param loanId 贷款ID
     * @param tenantId 租户ID
     * @return 材料列表
     */
    List<LoanMaterial> listByLoanId(Long loanId, Long tenantId);


    // ==================== 批量操作 ====================
    /**
     * 批量新增贷款材料（事务保证）
     * @param loanMaterials 材料列表
     * @return 是否批量新增成功
     */
    boolean batchSaveLoanMaterials(List<LoanMaterial> loanMaterials);

    /**
     * 批量更新材料状态（事务保证）
     * @param ids 材料ID列表
     * @param status 目标状态
     * @param tenantId 租户ID
     * @return 是否批量更新成功
     */
    boolean batchUpdateStatus(List<Long> ids, String status, Long tenantId);

    /**
     * 批量删除贷款材料（事务保证）
     * @param ids 材料ID列表
     * @param tenantId 租户ID
     * @return 是否批量删除成功
     */
    boolean batchRemoveLoanMaterials(List<Long> ids, Long tenantId);

    /**
     * 验证材料ID列表是否属于当前租户
     * @param tenantId 租户ID
     * @param materialIds 材料ID列表
     */
    void validateMaterialIdsBelongToTenant(Long tenantId, List<Long> materialIds);
}
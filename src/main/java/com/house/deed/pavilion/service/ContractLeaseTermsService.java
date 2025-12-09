package com.house.deed.pavilion.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.house.deed.pavilion.entity.ContractLeaseTerms;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 租赁合同附加条款表（租户级数据） 服务类
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
public interface ContractLeaseTermsService extends IService<ContractLeaseTerms> {

    // ==================== 基础CRUD扩展（带租户隔离） ====================
    /**
     * 新增租赁合同附加条款（带租户校验）
     * @param terms 附加条款实体
     * @return 是否新增成功
     */
    boolean saveTerms(ContractLeaseTerms terms);

    /**
     * 根据ID更新条款附加条款（带租户校验）
     * @param terms 附加条款实体（含ID和租户ID）
     * @return 是否更新成功
     */
    boolean updateTermsById(ContractLeaseTerms terms);

    /**
     * 根据ID删除条款（带租户校验）
     * @param id 条款ID
     * @param tenantId 租户ID
     * @return 是否删除成功
     */
    boolean removeTermsById(Long id, Long tenantId);

    /**
     * 根据ID查询条款（带租户隔离）
     * @param id 条款ID
     * @param tenantId 租户ID
     * @return 条款实体
     */
    ContractLeaseTerms getTermsById(Long id, Long tenantId);


    // ==================== 多条件查询（带租户隔离） ====================
    /**
     * 分页查询条款（多条件+租户隔离）
     * @param page 分页参数
     * @param queryParams 查询条件（支持：contractId、allowPet、allowSublet等）
     * @param tenantId 租户ID
     * @return 分页结果
     */
    IPage<ContractLeaseTerms> pageQuery(Page<ContractLeaseTerms> page, Map<String, Object> queryParams, Long tenantId);

    /**
     * 多条件查询条款列表（租户隔离）
     * @param queryParams 查询条件
     * @param tenantId 租户ID
     * @return 条款列表
     */
    List<ContractLeaseTerms> listByConditions(Map<String, Object> queryParams, Long tenantId);

    /**
     * 根据合同ID查询条款（租户隔离）
     * @param contractId 合同ID
     * @param tenantId 租户ID
     * @return 条款列表（一个合同可能对应多个附加条款）
     */
    List<ContractLeaseTerms> listByContractId(Long contractId, Long tenantId);


    // ==================== 批量操作（带租户隔离+事务） ====================
    /**
     * 批量新增条款（事务保证+租户校验）
     * @param termsList 条款列表
     * @return 是否全部新增成功
     */
    boolean batchSaveTerms(List<ContractLeaseTerms> termsList);

    /**
     * 批量更新条款状态（如批量修改允许养宠物/转租状态）
     * @param ids 条款ID列表
     * @param allowPet 是否允许养宠物（可为null，不更新此字段）
     * @param allowSublet 是否允许转租（可为null，不更新此字段）
     * @param tenantId 租户ID
     * @return 是否更新成功
     */
    boolean batchUpdateTerms(List<Long> ids, Byte allowPet, Byte allowSublet, Long tenantId);

    /**
     * 批量删除条款（事务保证+租户校验）
     * @param ids 条款ID列表
     * @param tenantId 租户ID
     * @return 是否删除成功
     */
    boolean batchRemoveTerms(List<Long> ids, Long tenantId);
}
package com.house.deed.pavilion.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.house.deed.pavilion.entity.LoanInfo;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

/**
 * 贷款信息服务接口
 *
 * <p>提供贷款信息的增删改查及批量操作功能，所有操作均支持租户级数据隔离</p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
public interface LoanInfoService extends IService<LoanInfo> {

    /**
     * 新增贷款信息
     *
     * @param loanInfo 贷款信息实体对象，包含贷款相关信息
     * @return 新增成功返回true，否则返回false
     * @throws IllegalArgumentException 当违反业务规则（如贷款编号重复）时抛出
     */
    boolean saveLoanInfo(LoanInfo loanInfo);

    /**
     * 根据ID更新贷款信息
     *
     * @param loanInfo 贷款信息实体对象，需包含主键ID和需要更新的字段
     * @return 更新成功返回true，否则返回false
     * @throws IllegalArgumentException 当数据不存在、无权限操作或违反业务规则时抛出
     */
    boolean updateLoanInfoById(LoanInfo loanInfo);

    /**
     * 根据ID物理删除贷款信息（支持租户隔离）
     *
     * @param id 贷款信息主键ID
     * @param tenantId 租户ID，用于数据隔离验证
     * @return 删除成功返回true，否则返回false
     * @throws IllegalArgumentException 当数据不存在或无权限操作时抛出
     */
    boolean removeLoanInfoById(Long id, Long tenantId);

    /**
     * 根据ID查询贷款详细信息（租户隔离）
     *
     * @param id 贷款信息主键ID
     * @param tenantId 租户ID，用于数据隔离验证
     * @return 对应的贷款信息实体对象，未找到返回null
     */
    LoanInfo getLoanInfoById(Long id, Long tenantId);

    /**
     * 多条件分页查询贷款信息
     *
     * @param page 分页参数对象
     * @param loanInfo 查询条件实体对象（支持多种条件筛选）
     * @param tenantId 租户ID，用于数据隔离
     * @return 分页查询结果，包含贷款信息列表和分页信息
     */
    IPage<LoanInfo> pageQuery(Page<LoanInfo> page, LoanInfo loanInfo, Long tenantId);

    /**
     * 批量新增贷款信息
     *
     * @param loanInfos 贷款信息实体对象列表
     * @return 批量新增成功返回true，否则返回false
     * @throws IllegalArgumentException 当任意记录违反业务规则时抛出
     * @apiNote 建议在业务层控制批量操作的数据量，并在单个事务中执行
     */
    boolean batchSaveLoanInfos(List<LoanInfo> loanInfos);

    /**
     * 批量删除贷款信息（支持租户隔离）
     *
     * @param ids 待删除的贷款信息ID列表
     * @param tenantId 租户ID，用于数据隔离验证
     * @return 批量删除成功返回true，否则返回false
     * @throws IllegalArgumentException 当存在不属于当前租户的贷款ID时抛出
     */
    boolean batchRemoveLoanInfos(List<Long> ids, Long tenantId);

    /**
     * 验证贷款ID列表是否属于指定租户
     *
     * @param tenantId 租户ID
     * @param loanIds 待验证的贷款信息ID列表
     * @throws IllegalArgumentException 当存在不属于该租户的贷款ID时抛出
     */
    void validateLoanIdsBelongToTenant(Long tenantId, List<Long> loanIds);
}
package COM.House.Deed.Pavilion.Service;

import COM.House.Deed.Pavilion.Entity.Customer;
import COM.House.Deed.Pavilion.Entity.CustomerBackup;
import COM.House.Deed.Pavilion.Mapper.CustomerBackupMapper;
import COM.House.Deed.Pavilion.Mapper.CustomerMapper;
import COM.House.Deed.Pavilion.Utils.PageResult;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 客户备份表完整服务（含查询、恢复功能）
 */
@Service
public class CustomerBackupService {

    @Resource
    private CustomerBackupMapper customerBackupMapper;

    @Resource
    private CustomerMapper customerMapper; // 注入客户主表Mapper，用于恢复数据


    /**
     * 根据原客户ID查询备份记录
     */
    public CustomerBackup getBackupByCustomerId(Long customerId) {
        if (customerId == null || customerId <= 0) {
            throw new RuntimeException("客户ID无效（必须为正整数）");
        }
        CustomerBackup backup = customerBackupMapper.selectByCustomerId(customerId);
        if (backup == null) {
            throw new RuntimeException("未找到客户ID为" + customerId + "的备份记录");
        }
        return backup;
    }

    /**
     * 分页查询所有客户备份记录（按备份时间倒序）
     */
    public PageResult<CustomerBackup> getAllCustomerBackups(Integer pageNum, Integer pageSize) {
        // 处理分页参数默认值和范围
        pageNum = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        pageSize = (pageSize == null || pageSize < 1 || pageSize > 100) ? 10 : pageSize;

        PageHelper.startPage(pageNum, pageSize);
        Page<CustomerBackup> backupPage = customerBackupMapper.selectAll();

        return getBackupPageResult(backupPage);
    }

    /**
     * 分页查询指定操作人删除的客户备份记录
     */
    public PageResult<CustomerBackup> getBackupsByOperator(Long operatorId, Integer pageNum, Integer pageSize) {
        if (operatorId == null || operatorId <= 0) {
            throw new RuntimeException("操作人ID无效（必须为正整数）");
        }
        pageNum = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        pageSize = (pageSize == null || pageSize < 1 || pageSize > 100) ? 10 : pageSize;

        PageHelper.startPage(pageNum, pageSize);
        Page<CustomerBackup> backupPage = customerBackupMapper.selectByBackupOperator(operatorId);

        return getBackupPageResult(backupPage);
    }


    // -------------------------- 新增：按备份时间范围查询 --------------------------

    /**
     * 按备份时间范围分页查询客户备份记录（支持查询某段时间内删除的客户）
     */
    public PageResult<CustomerBackup> getBackupsByTimeRange(
            LocalDateTime startTime,
            LocalDateTime endTime,
            Integer pageNum,
            Integer pageSize
    ) {
        // 校验时间参数（结束时间不能早于开始时间）
        if (startTime != null && endTime != null && endTime.isBefore(startTime)) {
            throw new RuntimeException("结束时间不能早于开始时间");
        }
        pageNum = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        pageSize = (pageSize == null || pageSize < 1 || pageSize > 100) ? 10 : pageSize;

        PageHelper.startPage(pageNum, pageSize);
        Page<CustomerBackup> backupPage = customerBackupMapper.selectByTimeRange(startTime, endTime);

        return getBackupPageResult(backupPage);
    }

    /**
     * 封装分页结果
     */
    private PageResult<CustomerBackup> getBackupPageResult(Page<CustomerBackup> customerBackups) {
        PageResult<CustomerBackup> pageResult = new PageResult<>();
        pageResult.setList(customerBackups.getResult());
        pageResult.setTotal(customerBackups.getTotal());
        pageResult.setPages(customerBackups.getPages());
        pageResult.setPageNum(customerBackups.getPageNum());
        pageResult.setPageSize(customerBackups.getPageSize());
        return pageResult;
    }


    // -------------------------- 新增：从备份表恢复客户数据到主表 --------------------------

    /**
     * 从备份表恢复客户数据（恢复后可选择保留或删除备份）
     *
     * @param customerId  原客户ID（备份表ID）
     * @param operatorId  执行恢复操作的用户ID
     * @param keepBackup  恢复后是否保留备份记录（true-保留，false-删除）
     */
    @Transactional
    public void restoreCustomer(Long customerId, Long operatorId, boolean keepBackup) {
        // 1. 校验参数
        if (customerId == null || customerId <= 0) {
            throw new RuntimeException("客户ID无效（必须为正整数）");
        }
        if (operatorId == null || operatorId <= 0) {
            throw new RuntimeException("操作人ID无效（必须为正整数）");
        }

        // 2. 查询备份记录
        CustomerBackup backup = getBackupByCustomerId(customerId);

        // 3. 校验主表中是否已存在该客户（避免ID冲突）
        Customer existingCustomer = customerMapper.selectById(customerId);
        if (existingCustomer != null) {
            throw new RuntimeException("客户ID为" + customerId + "的记录已存在，无法恢复（避免重复）");
        }

        // 4. 将备份数据转换为客户主表实体（恢复原数据，更新操作人信息）
        Customer customer = new Customer();
        customer.setId(backup.getId());
        customer.setName(backup.getName());
        customer.setPhone(backup.getPhone());
        customer.setGender(backup.getGender());
        customer.setDemand(backup.getDemand());
        customer.setSource(backup.getSource());
        customer.setStatus(backup.getStatus());
        customer.setRemark(backup.getRemark());
        customer.setCreatedAt(backup.getCreatedAt()); // 保留原创建时间
        customer.setUpdatedAt(LocalDateTime.now()); // 恢复时间作为最新更新时间
        customer.setCreatedBy(backup.getCreatedBy()); // 保留原创建人
        customer.setUpdatedBy(operatorId); // 恢复操作人作为更新人

        // 5. 插入到客户主表
        int insertRows = customerMapper.insert(customer);
        if (insertRows != 1) {
            throw new RuntimeException("恢复客户数据失败，请重试");
        }

        // 6. 根据需求决定是否删除备份记录
        if (!keepBackup) {
            int deleteRows = customerBackupMapper.deleteById(customerId);
            if (deleteRows != 1) {
                throw new RuntimeException("恢复成功，但删除备份记录失败（可手动清理）");
            }
        }
    }
}

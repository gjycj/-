package COM.House.Deed.Pavilion.Mapper;

import COM.House.Deed.Pavilion.Entity.CustomerBackup;
import com.github.pagehelper.Page;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * 客户备份表Mapper接口（操作customer_backup表）
 */
public interface CustomerBackupMapper {

    /**
     * 新增客户备份记录（删除客户时调用）
     *
     * @param backup 备份信息
     * @return 影响行数（1-成功，0-失败）
     */
    int insert(CustomerBackup backup);

    // 新增：按备份时间范围查询
    Page<CustomerBackup> selectByTimeRange(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    // 新增：删除备份记录（用于恢复后清理）
    int deleteById(Long id);

    /**
     * 根据备份ID查询客户备份记录
     * @param id 备份记录ID（与原客户ID一致）
     * @return 客户备份详情
     */
    CustomerBackup selectById(Long id);

    /**
     * 根据原客户ID查询备份记录（与selectById功能一致，语义更清晰）
     * @param customerId 原客户ID
     * @return 客户备份详情
     */
    CustomerBackup selectByCustomerId(Long customerId);

    /**
     * 分页查询所有客户备份记录（支持按备份时间排序）
     * @return 分页的备份记录列表
     */
    Page<CustomerBackup> selectAll();

    /**
     * 根据操作人ID查询其删除的客户备份记录
     * @param backupById 执行删除的操作人ID
     * @return 分页的备份记录列表
     */
    Page<CustomerBackup> selectByBackupOperator(Long backupById);
}

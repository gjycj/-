package COM.House.Deed.Pavilion.Mapper;

import COM.House.Deed.Pavilion.Entity.VisitRecordBackup;
import com.github.pagehelper.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * 带看记录备份表Mapper接口（操作visit_record_backup表）
 */
@Mapper
public interface VisitRecordBackupMapper {

    /**
     * 新增带看记录备份（删除带看记录时调用）
     *
     * @param backup 备份信息
     * @return 影响行数（1-成功，0-失败）
     */
    int insert(VisitRecordBackup backup);

    /**
     * 根据备份ID查询带看备份记录
     * @param id 备份记录ID（与原带看记录ID一致）
     * @return 带看备份详情
     */
    VisitRecordBackup selectById(Long id);

    /**
     * 根据原带看记录ID查询备份（与selectById语义一致）
     * @param visitRecordId 原带看记录ID
     * @return 带看备份详情
     */
    VisitRecordBackup selectByVisitRecordId(Long visitRecordId);

    /**
     * 根据客户ID查询其相关的带看备份记录
     * @param customerId 客户ID
     * @return 分页的备份记录列表
     */
    Page<VisitRecordBackup> selectByCustomerId(Long customerId);

    /**
     * 分页查询所有带看备份记录
     * @return 分页的备份记录列表
     */
    Page<VisitRecordBackup> selectAll();

    // 新增：按备份时间范围查询（查询某段时间内删除的带看记录）
    Page<VisitRecordBackup> selectByTimeRange(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    // 新增：删除备份记录（恢复后清理用）
    int deleteById(Long id);
}

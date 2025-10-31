package COM.House.Deed.Pavilion.Service;

import COM.House.Deed.Pavilion.Entity.VisitRecord;
import COM.House.Deed.Pavilion.Entity.VisitRecordBackup;
import COM.House.Deed.Pavilion.Mapper.VisitRecordBackupMapper;
import COM.House.Deed.Pavilion.Mapper.VisitRecordMapper;
import COM.House.Deed.Pavilion.Utils.PageResult;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 带看记录备份完整服务（含查询、恢复）
 */
@Service
public class VisitRecordBackupService {

    @Resource
    private VisitRecordBackupMapper visitRecordBackupMapper;

    @Resource
    private VisitRecordMapper visitRecordMapper; // 注入主表Mapper用于恢复


    // -------------------------- 原有查询方法（完善参数校验） --------------------------

    /**
     * 根据原带看记录ID查询备份
     */
    public VisitRecordBackup getBackupByVisitRecordId(Long visitRecordId) {
        if (visitRecordId == null || visitRecordId <= 0) {
            throw new RuntimeException("带看记录ID无效（必须为正整数）");
        }
        VisitRecordBackup backup = visitRecordBackupMapper.selectByVisitRecordId(visitRecordId);
        if (backup == null) {
            throw new RuntimeException("未找到带看记录ID为" + visitRecordId + "的备份");
        }
        return backup;
    }

    /**
     * 按客户ID查询带看备份
     */
    public PageResult<VisitRecordBackup> getBackupsByCustomerId(Long customerId, Integer pageNum, Integer pageSize) {
        if (customerId == null || customerId <= 0) {
            throw new RuntimeException("客户ID无效");
        }
        pageNum = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        pageSize = (pageSize == null || pageSize < 1 || pageSize > 100) ? 10 : pageSize;

        PageHelper.startPage(pageNum, pageSize);
        Page<VisitRecordBackup> backupPage = visitRecordBackupMapper.selectByCustomerId(customerId);

        return getBackupPageResult(backupPage);
    }

    /**
     * 分页查询所有带看备份
     */
    public PageResult<VisitRecordBackup> getAllVisitBackups(Integer pageNum, Integer pageSize) {
        pageNum = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        pageSize = (pageSize == null || pageSize < 1 || pageSize > 100) ? 10 : pageSize;

        PageHelper.startPage(pageNum, pageSize);
        Page<VisitRecordBackup> backupPage = visitRecordBackupMapper.selectAll();

        return getBackupPageResult(backupPage);
    }


    // -------------------------- 新增：按时间范围查询 --------------------------

    /**
     * 按备份时间范围查询带看记录备份
     */
    public PageResult<VisitRecordBackup> getBackupsByTimeRange(
            LocalDateTime startTime,
            LocalDateTime endTime,
            Integer pageNum,
            Integer pageSize
    ) {
        if (startTime != null && endTime != null && endTime.isBefore(startTime)) {
            throw new RuntimeException("结束时间不能早于开始时间");
        }
        pageNum = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        pageSize = (pageSize == null || pageSize < 1 || pageSize > 100) ? 10 : pageSize;

        PageHelper.startPage(pageNum, pageSize);
        Page<VisitRecordBackup> backupPage = visitRecordBackupMapper.selectByTimeRange(startTime, endTime);

        return getBackupPageResult(backupPage);
    }

    /**
     * 封装带看备份分页结果（复用逻辑）
     */
    private PageResult<VisitRecordBackup> getBackupPageResult(Page<VisitRecordBackup> backupPage) {
        PageResult<VisitRecordBackup> pageResult = new PageResult<>();
        pageResult.setList(backupPage.getResult());
        pageResult.setTotal(backupPage.getTotal());
        pageResult.setPages(backupPage.getPages());
        pageResult.setPageNum(backupPage.getPageNum());
        pageResult.setPageSize(backupPage.getPageSize());

        return pageResult;
    }


    // -------------------------- 新增：从备份恢复带看记录到主表 --------------------------

    /**
     * 恢复带看记录（支持恢复后保留/删除备份）
     *
     * @param visitRecordId 原带看记录ID（备份表ID）
     * @param operatorId    执行恢复的用户ID
     * @param keepBackup    是否保留备份
     */
    @Transactional
    public void restoreVisitRecord(Long visitRecordId, Long operatorId, boolean keepBackup) {
        // 1. 参数校验
        if (visitRecordId == null || visitRecordId <= 0) {
            throw new RuntimeException("带看记录ID无效");
        }
        if (operatorId == null || operatorId <= 0) {
            throw new RuntimeException("操作人ID无效");
        }

        // 2. 查询备份记录
        VisitRecordBackup backup = getBackupByVisitRecordId(visitRecordId);

        // 3. 校验主表是否已存在该记录（避免主键冲突）
        VisitRecord existingRecord = visitRecordMapper.selectById(visitRecordId);
        if (existingRecord != null) {
            throw new RuntimeException("带看记录ID为" + visitRecordId + "已存在，无法恢复");
        }

        // 4. 转换备份数据到主表实体
        VisitRecord record = new VisitRecord();
        record.setId(backup.getId());
        record.setHouseId(backup.getHouseId());
        record.setCustomerId(backup.getCustomerId());
        record.setAgentId(backup.getAgentId());
        record.setVisitTime(backup.getVisitTime());
        record.setFeedback(backup.getFeedback());
        record.setNextPlan(backup.getNextPlan());
        record.setCreatedAt(backup.getCreatedAt()); // 保留原创建时间
        record.setUpdatedAt(LocalDateTime.now()); // 恢复时间作为更新时间
        record.setCreatedBy(backup.getCreatedBy()); // 保留原创建人
        record.setUpdatedBy(operatorId); // 恢复操作人作为更新人

        // 5. 插入主表
        int insertRows = visitRecordMapper.insert(record);
        if (insertRows != 1) {
            throw new RuntimeException("恢复带看记录失败，请重试");
        }

        // 6. 按需删除备份
        if (!keepBackup) {
            int deleteRows = visitRecordBackupMapper.deleteById(visitRecordId);
            if (deleteRows != 1) {
                throw new RuntimeException("恢复成功，但备份记录删除失败（可手动清理）");
            }
        }
    }
}

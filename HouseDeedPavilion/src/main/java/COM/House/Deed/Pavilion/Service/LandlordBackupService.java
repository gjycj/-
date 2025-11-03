package COM.House.Deed.Pavilion.Service;

import COM.House.Deed.Pavilion.DTO.LandlordBackupQueryDTO;
import COM.House.Deed.Pavilion.Entity.Landlord;
import COM.House.Deed.Pavilion.Entity.LandlordBackup;
import COM.House.Deed.Pavilion.Mapper.LandlordBackupMapper;
import COM.House.Deed.Pavilion.Mapper.LandlordMapper;
import COM.House.Deed.Pavilion.Converter.LandlordConverter;
import COM.House.Deed.Pavilion.Utils.PageResult;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 房东备份服务实现
 */
@Service
public class LandlordBackupService {

    @Resource
    private LandlordBackupMapper backupMapper;
    @Resource
    private LandlordMapper landlordMapper;
    @Resource
    private LandlordConverter converter;

    /**
     * 按备份ID查询
     */
    public LandlordBackup getBackupById(Long backupId) {
        if (backupId == null || backupId <= 0) {
            return null;
        }
        return backupMapper.selectById(backupId);
    }

    /**
     * 按原房东ID查询备份（分页）
     */
    public PageResult<LandlordBackup> getByOriginalId(Long originalId, Integer pageNum, Integer pageSize) {
        pageNum = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        pageSize = (pageSize == null || pageSize < 1) ? 10 : pageSize;

        PageHelper.startPage(pageNum, pageSize);
        Page<LandlordBackup> backups = (Page<LandlordBackup>) backupMapper.selectByOriginalId(originalId);

        return getLandlordBackupPageResult(backups);
    }

    /**
     * 多条件分页查询
     */
    public PageResult<LandlordBackup> getByCondition(LandlordBackupQueryDTO queryDTO) {
        if (queryDTO == null) {
            queryDTO = new LandlordBackupQueryDTO();
        }
        int pageNum = queryDTO.getPageNum() == null ? 1 : queryDTO.getPageNum();
        int pageSize = queryDTO.getPageSize() == null ? 10 : queryDTO.getPageSize();

        PageHelper.startPage(pageNum, pageSize);
        Page<LandlordBackup> backups = (Page<LandlordBackup>) backupMapper.selectByCondition(queryDTO);

        return getLandlordBackupPageResult(backups);
    }

    /**
     * 从备份恢复房东
     */
    @Transactional(rollbackFor = Exception.class)
    public Long recoverFromBackup(Long backupId, Long operatorId, boolean keepBackup) {
        // 1. 校验备份存在
        LandlordBackup backup = backupMapper.selectById(backupId);
        if (backup == null) {
            throw new RuntimeException("备份不存在，ID：" + backupId);
        }

        // 2. 转换为原房东实体（重置ID和操作信息）
        Landlord landlord = converter.convertToOriginal(backup);
        landlord.setId(null); // 重新生成ID
        landlord.setCreatedAt(LocalDateTime.now());
        landlord.setUpdatedAt(LocalDateTime.now());
        landlord.setCreatedBy(operatorId);
        landlord.setUpdatedBy(operatorId);

        // 3. 保存恢复的房东
        landlordMapper.insert(landlord);
        if (landlord.getId() == null) {
            throw new RuntimeException("恢复房东失败");
        }

        // 4. 按需删除备份
        if (!keepBackup) {
            backupMapper.deleteById(backupId);
        }

        return landlord.getId();
    }

    /**
     * 单个删除备份
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteBackup(Long backupId, Long operatorId) {
        LandlordBackup backup = backupMapper.selectById(backupId);
        if (backup == null) {
            throw new RuntimeException("备份不存在，无法删除：" + backupId);
        }
        int rows = backupMapper.deleteById(backupId);
        if (rows != 1) {
            throw new RuntimeException("删除备份失败：" + backupId);
        }
    }

    /**
     * 批量删除备份
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteBackups(List<Long> backupIds, Long operatorId) {
        if (backupIds == null || backupIds.isEmpty()) {
            throw new RuntimeException("请选择需要删除的备份");
        }
        int rows = backupMapper.deleteByIds(backupIds);
        if (rows == 0) {
            throw new RuntimeException("所选备份均不存在");
        }
    }

    /**
     * 封装房源分页结果
     */
    private PageResult<LandlordBackup> getLandlordBackupPageResult(Page<LandlordBackup> landlordBackups) {
        PageResult<LandlordBackup> pageResult = new PageResult<>();
        pageResult.setList(landlordBackups.getResult());
        pageResult.setTotal(landlordBackups.getTotal());
        pageResult.setPages(landlordBackups.getPages());
        pageResult.setPageNum(landlordBackups.getPageNum());
        pageResult.setPageSize(landlordBackups.getPageSize());
        return pageResult;
    }
}
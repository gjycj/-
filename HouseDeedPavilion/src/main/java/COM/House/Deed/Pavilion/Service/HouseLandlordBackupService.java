package COM.House.Deed.Pavilion.Service;

import COM.House.Deed.Pavilion.DTO.HouseLandlordBackupQueryDTO;
import COM.House.Deed.Pavilion.Entity.HouseLandlord;
import COM.House.Deed.Pavilion.Entity.HouseLandlordBackup;
import COM.House.Deed.Pavilion.Mapper.HouseLandlordBackupMapper;
import COM.House.Deed.Pavilion.Utils.PageResult;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 房源-房东关联备份服务类
 * 仅提供备份记录的插入和查询功能，不支持删除备份
 */
@Service
public class HouseLandlordBackupService {

    @Resource
    private HouseLandlordBackupMapper backupMapper;

    /**
     * 创建备份记录（删除主表记录时调用）
     *
     * @param original     原关联记录（house_landlord表的记录）
     * @param deleteReason 删除原因
     * @param operatorId   操作人ID（执行删除的用户）
     */
    @Transactional
    public void createBackup(HouseLandlord original, String deleteReason, Long operatorId) {
        // 1. 校验原记录
        if (original == null || original.getId() == null) {
            throw new RuntimeException("原关联记录为空或ID无效，无法创建备份");
        }

        // 2. 校验操作人ID
        if (operatorId == null || operatorId <= 0) {
            throw new RuntimeException("操作人ID无效（必须为正整数）");
        }

        // 3. 构建备份实体
        HouseLandlordBackup backup = new HouseLandlordBackup();
        backup.setOriginalId(original.getId());
        backup.setHouseId(original.getHouseId());
        backup.setLandlordId(original.getLandlordId());
        backup.setIsMain(original.getIsMain());
        backup.setDeleteReason(deleteReason);
        backup.setDeletedAt(LocalDateTime.now()); // 删除时间=当前时间
        backup.setDeletedBy(operatorId);
        backup.setBackupTime(LocalDateTime.now()); // 备份时间=当前时间

        // 4. 执行插入
        int rows = backupMapper.insert(backup);
        if (rows != 1) {
            throw new RuntimeException("创建关联关系备份失败，请重试");
        }

    }

    /**
     * 根据备份ID查询备份记录
     *
     * @param id 备份记录ID
     * @return 备份详情
     */
    public HouseLandlordBackup getBackupById(Long id) {
        // 1. 校验ID
        if (id == null || id <= 0) {
            throw new RuntimeException("备份记录ID无效（必须为正整数）");
        }

        // 2. 查询并校验存在性
        HouseLandlordBackup backup = backupMapper.selectById(id);
        if (backup == null) {
            throw new RuntimeException("未找到ID为" + id + "的备份记录");
        }

        return backup;
    }

    /**
     * 多条件分页查询备份记录
     *
     * @param queryDTO 包含查询条件和分页参数
     * @return 分页的备份记录列表
     */
    public PageResult<HouseLandlordBackup> getBackupsByCondition(HouseLandlordBackupQueryDTO queryDTO) {
        // 1. 处理空参数
        if (queryDTO == null) {
            queryDTO = new HouseLandlordBackupQueryDTO();
        }

        // 2. 校验分页参数
        if (queryDTO.getPageNum() < 1) {
            queryDTO.setPageNum(1);
        }
        if (queryDTO.getPageSize() < 1 || queryDTO.getPageSize() > 100) {
            queryDTO.setPageSize(10);
        }

        // 3. 执行分页查询
        PageHelper.startPage(queryDTO.getPageNum(), queryDTO.getPageSize());
        Page<HouseLandlordBackup> backupPage = backupMapper.selectByCondition(queryDTO);

        // 4. 封装分页结果
        return getHouseLandlordPageResult(backupPage);
    }

    /**
     * 封装房东-房源分页结果（复用逻辑）
     */
    private PageResult<HouseLandlordBackup> getHouseLandlordPageResult(Page<HouseLandlordBackup> backupPage) {
        PageResult<HouseLandlordBackup> pageResult = new PageResult<>();
        pageResult.setList(backupPage.getResult());
        pageResult.setTotal(backupPage.getTotal());
        pageResult.setPages(backupPage.getPages());
        pageResult.setPageNum(backupPage.getPageNum());
        pageResult.setPageSize(backupPage.getPageSize());

        return pageResult;
    }


}

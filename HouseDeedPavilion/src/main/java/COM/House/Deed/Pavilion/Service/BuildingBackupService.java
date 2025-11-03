package COM.House.Deed.Pavilion.Service;

import COM.House.Deed.Pavilion.DTO.BuildingBackupQueryDTO;
import COM.House.Deed.Pavilion.Entity.Building;
import COM.House.Deed.Pavilion.Entity.BuildingBackup;
import COM.House.Deed.Pavilion.Mapper.BuildingBackupMapper;
import COM.House.Deed.Pavilion.Mapper.BuildingMapper;
import COM.House.Deed.Pavilion.Utils.PageResult;
import COM.House.Deed.Pavilion.Converter.BuildingConverter;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

/**
 * 楼栋备份业务层实现
 * 处理楼栋备份的创建、查询、恢复、删除等核心业务
 */
@Service
public class BuildingBackupService {

    @Resource
    private BuildingBackupMapper buildingBackupMapper;

    @Resource
    private BuildingMapper buildingMapper; // 依赖原楼栋表Mapper（用于恢复操作）

    @Resource
    private BuildingConverter buildingConverter; // 实体转换工具类


    // ========================== 一、备份创建（删除楼栋时自动调用） ==========================

    /**
     * 创建楼栋备份（原楼栋删除前调用）
     * @param originalBuilding 原楼栋实体（待删除的楼栋）
     * @param deleteReason  删除原因
     * @param deletedBy 删除人ID
     * @return 新增的备份记录ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createBackup(Building originalBuilding, String deleteReason, Long deletedBy) {
        // 1. 校验原楼栋合法性
        if (originalBuilding == null || originalBuilding.getId() == null) {
            throw new RuntimeException("原楼栋信息为空，无法创建备份");
        }

        // 2. 转换原楼栋为备份实体（复制原字段+设置备份专用字段）
        BuildingBackup backup = buildingConverter.convertToBackup(originalBuilding);
        backup.setBackupTime(LocalDateTime.now()); // 备份时间：当前时间
        backup.setDeleteReason(deleteReason);      // 删除原因
        backup.setDeletedBy(deletedBy);            // 删除人

        // 3. 保存备份记录
        int insertCount = buildingBackupMapper.insert(backup);
        if (insertCount != 1) {
            throw new RuntimeException("创建楼栋备份失败，原楼栋ID：" + originalBuilding.getId());
        }

        return backup.getId(); // 返回备份ID
    }


    // ========================== 二、备份查询（多场景查询） ==========================

    /**
     * 按备份ID查询单条备份记录
     * @param backupId 备份ID
     * @return 备份实体（null表示无此记录）
     */
    public BuildingBackup getBackupById(Long backupId) {
        if (backupId == null) {
            return null;
        }
        return buildingBackupMapper.selectById(backupId);
    }

    /**
     * 按原楼栋ID查询所有备份（查询某楼栋的历史备份）
     * @param originalId 原楼栋ID
     * @param pageNum 页码
     * @param pageSize 页大小
     * @return 分页结果
     */
    public PageResult<BuildingBackup> getBackupsByOriginalId(Long originalId, Integer pageNum, Integer pageSize) {
        // 处理分页参数默认值
        pageNum = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        pageSize = (pageSize == null || pageSize < 1 || pageSize > 100) ? 10 : pageSize;

        // 分页查询
        PageHelper.startPage(pageNum, pageSize);
        Page<BuildingBackup> backupPage = buildingBackupMapper.selectByOriginalId(originalId);

        // 封装分页结果
        return getBuildingBackupPageResult(backupPage);
    }

    /**
     * 多条件分页查询备份记录
     * @param queryDTO 包含查询条件和分页参数
     * @return 分页结果
     */
    public PageResult<BuildingBackup> getBackupsByCondition(BuildingBackupQueryDTO queryDTO) {
        // 处理空DTO和分页参数
        if (queryDTO == null) {
            queryDTO = new BuildingBackupQueryDTO();
        }
        Integer pageNum = queryDTO.getPageNum();
        Integer pageSize = queryDTO.getPageSize();
        pageNum = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        pageSize = (pageSize == null || pageSize < 1 || pageSize > 100) ? 10 : pageSize;
        queryDTO.setPageNum(pageNum);
        queryDTO.setPageSize(pageSize);

        // 分页查询
        PageHelper.startPage(pageNum, pageSize);
        Page<BuildingBackup> backupPage = buildingBackupMapper.selectByCondition(queryDTO);

        // 封装分页结果
        return getBuildingBackupPageResult(backupPage);
    }


    // ========================== 三、备份恢复（从备份重建原楼栋） ==========================

    /**
     * 从备份恢复楼栋（核心功能）
     * @param backupId 备份ID
     * @param operatorId 操作人ID（恢复操作的执行人）
     * @param keepBackup 是否保留备份（true=恢复后不删除备份，false=恢复后删除备份）
     * @return 恢复后的楼栋ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long recoverFromBackup(Long backupId, Long operatorId, boolean keepBackup) {
        // 1. 校验备份是否存在
        BuildingBackup backup = buildingBackupMapper.selectById(backupId);
        if (backup == null) {
            throw new RuntimeException("楼栋备份不存在，无法恢复，备份ID：" + backupId);
        }

        // 2. 校验操作人ID
        if (operatorId == null) {
            throw new RuntimeException("操作人ID不能为空");
        }

        // 3. 将备份转换为原楼栋实体（重置部分字段）
        Building recoveredBuilding = buildingConverter.convertToOriginal(backup);
        // 恢复时重置：原楼栋ID（重新生成，避免与已存在的记录冲突）
        recoveredBuilding.setId(null);
        // 恢复时更新：创建/更新时间为当前时间，操作人为当前执行人
        recoveredBuilding.setCreatedAt(LocalDateTime.now());
        recoveredBuilding.setUpdatedAt(LocalDateTime.now());
        recoveredBuilding.setCreatedBy(operatorId);
        recoveredBuilding.setUpdatedBy(operatorId);

        // 4. 保存恢复的楼栋到原表
        int insertCount = buildingMapper.insert(recoveredBuilding);
        if (insertCount != 1) {
            throw new RuntimeException("恢复楼栋失败，备份ID：" + backupId);
        }

        // 5. 按需删除备份（如果keepBackup=false）
        if (!keepBackup) {
            buildingBackupMapper.deleteById(backupId);
        }

        return recoveredBuilding.getId(); // 返回恢复后的楼栋ID
    }


    // ========================== 四、备份删除（清理备份数据） ==========================

    /**
     * 单个删除备份
     * @param backupId 备份ID
     * @param operatorId 操作人ID（用于日志审计）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteBackupById(Long backupId, Long operatorId) {
        // 1. 校验备份是否存在
        BuildingBackup backup = buildingBackupMapper.selectById(backupId);
        if (backup == null) {
            throw new RuntimeException("楼栋备份不存在，无法删除，ID：" + backupId);
        }

        // 2. 执行删除
        int deleteCount = buildingBackupMapper.deleteById(backupId);
        if (deleteCount != 1) {
            throw new RuntimeException("删除楼栋备份失败，ID：" + backupId);
        }

        // （可选）记录操作日志：operatorId删除了backupId对应的备份
    }

    /**
     * 封装楼栋备份分页结果
     */
    private PageResult<BuildingBackup> getBuildingBackupPageResult(Page<BuildingBackup> buildingBackups) {
        PageResult<BuildingBackup> pageResult = new PageResult<>();
        pageResult.setList(buildingBackups.getResult());
        pageResult.setTotal(buildingBackups.getTotal());
        pageResult.setPages(buildingBackups.getPages());
        pageResult.setPageNum(buildingBackups.getPageNum());
        pageResult.setPageSize(buildingBackups.getPageSize());
        return pageResult;
    }
}
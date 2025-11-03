package COM.House.Deed.Pavilion.Service;

import COM.House.Deed.Pavilion.DTO.PropertyBackupQueryDTO;
import COM.House.Deed.Pavilion.Entity.Property;
import COM.House.Deed.Pavilion.Entity.PropertyBackup;
import COM.House.Deed.Pavilion.Mapper.PropertyBackupMapper;
import COM.House.Deed.Pavilion.Mapper.PropertyMapper;
import COM.House.Deed.Pavilion.Utils.PageResult;
import COM.House.Deed.Pavilion.Converter.PropertyConverter;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 楼盘备份服务（完整实现，与Controller方法严格对齐）
 */
@Service
public class PropertyBackupService {

    @Resource
    private PropertyBackupMapper propertyBackupMapper;
    @Resource
    private PropertyMapper propertyMapper;
    @Resource
    private PropertyConverter propertyConverter;


    // ========================== 1. 备份查询方法 ==========================

    /**
     * 按备份ID查询单条记录（对应Controller的getBackupById）
     */
    public PropertyBackup getBackupById(Long backupId) {
        if (backupId == null || backupId <= 0) {
            return null;
        }
        return propertyBackupMapper.selectById(backupId);
    }

    /**
     * 按原楼盘ID查询备份（分页，对应Controller的getBackupsByOriginalId）
     */
    public PageResult<PropertyBackup> getBackupsByOriginalId(Long originalId, Integer pageNum, Integer pageSize) {
        // 处理分页参数默认值
        pageNum = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        pageSize = (pageSize == null || pageSize < 1 || pageSize > 100) ? 10 : pageSize;

        // 分页查询
        PageHelper.startPage(pageNum, pageSize);
        Page<PropertyBackup> backupPage = propertyBackupMapper.selectByOriginalId(originalId);

        // 封装分页结果
        return getPropertyBackupPageResult(backupPage);
    }

    /**
     * 多条件分页查询（对应Controller的getBackupsByCondition）
     */
    public PageResult<PropertyBackup> getBackupsByCondition(PropertyBackupQueryDTO queryDTO) {
        if (queryDTO == null) {
            queryDTO = new PropertyBackupQueryDTO();
        }
        // 处理分页参数
        Integer pageNum = queryDTO.getPageNum();
        Integer pageSize = queryDTO.getPageSize();
        pageNum = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        pageSize = (pageSize == null || pageSize < 1 || pageSize > 100) ? 10 : pageSize;
        queryDTO.setPageNum(pageNum);
        queryDTO.setPageSize(pageSize);

        // 分页查询
        PageHelper.startPage(pageNum, pageSize);
        Page<PropertyBackup> backupPage = propertyBackupMapper.selectByCondition(queryDTO);

        // 封装结果
        return getPropertyBackupPageResult(backupPage);
    }


    // ========================== 2. 备份创建方法（供删除楼盘时调用） ==========================

    /**
     * 创建楼盘备份（对应PropertyService的deletePropertyWithBackup）
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createBackup(Property originalProperty, String deleteReason, Long deletedBy) {
        if (originalProperty == null || originalProperty.getId() == null) {
            throw new RuntimeException("原楼盘信息为空，无法创建备份");
        }

        // 转换原楼盘为备份实体
        PropertyBackup backup = propertyConverter.convertToBackup(originalProperty);
        backup.setBackupTime(LocalDateTime.now());
        backup.setDeleteReason(deleteReason);
        backup.setDeletedBy(deletedBy);

        // 保存备份
        int insertCount = propertyBackupMapper.insert(backup);
        if (insertCount != 1) {
            throw new RuntimeException("创建楼盘备份失败，原楼盘ID：" + originalProperty.getId());
        }
        return backup.getId();
    }


    // ========================== 3. 备份恢复方法（对应Controller的recoverFromBackup） ==========================

    /**
     * 从备份恢复楼盘
     */
    @Transactional(rollbackFor = Exception.class)
    public Long recoverFromBackup(Long backupId, Long operatorId, boolean keepBackup) {
        // 1. 校验备份存在性
        PropertyBackup backup = propertyBackupMapper.selectById(backupId);
        if (backup == null) {
            throw new RuntimeException("楼盘备份不存在，无法恢复，备份ID：" + backupId);
        }

        // 2. 校验操作人
        if (operatorId == null || operatorId <= 0) {
            throw new RuntimeException("操作人ID无效");
        }

        // 3. 转换备份为原楼盘实体（重置ID和操作信息）
        Property recoveredProperty = propertyConverter.convertToOriginal(backup);
        recoveredProperty.setId(null); // 重新生成ID，避免冲突
        recoveredProperty.setCreatedAt(LocalDateTime.now());
        recoveredProperty.setUpdatedAt(LocalDateTime.now());
        recoveredProperty.setCreatedBy(operatorId);
        recoveredProperty.setUpdatedBy(operatorId);

        // 4. 保存恢复的楼盘
        int insertCount = propertyMapper.insert(recoveredProperty);
        if (insertCount != 1) {
            throw new RuntimeException("恢复楼盘失败，备份ID：" + backupId);
        }

        // 5. 按需删除备份
        if (!keepBackup) {
            propertyBackupMapper.deleteById(backupId);
        }

        return recoveredProperty.getId();
    }


    // ========================== 4. 备份删除方法（对应Controller的删除接口） ==========================

    /**
     * 单个删除备份（对应Controller的deleteBackupById）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteBackupById(Long backupId, Long operatorId) {
        // 校验备份存在性
        PropertyBackup backup = propertyBackupMapper.selectById(backupId);
        if (backup == null) {
            throw new RuntimeException("楼盘备份不存在，无法删除，ID：" + backupId);
        }
        // 校验操作人
        if (operatorId == null || operatorId <= 0) {
            throw new RuntimeException("操作人ID无效");
        }
        // 执行删除
        int deleteCount = propertyBackupMapper.deleteById(backupId);
        if (deleteCount != 1) {
            throw new RuntimeException("删除楼盘备份失败，ID：" + backupId);
        }
    }

    /**
     * 批量删除备份（对应Controller的deleteBackupByIds）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteBackupByIds(List<Long> backupIds, Long operatorId) {
        // 校验参数
        if (backupIds == null || backupIds.isEmpty()) {
            throw new RuntimeException("请选择需要删除的备份记录");
        }
        if (operatorId == null || operatorId <= 0) {
            throw new RuntimeException("操作人ID无效");
        }
        // 执行批量删除
        int deleteCount = propertyBackupMapper.deleteByIds(backupIds);
        if (deleteCount == 0) {
            throw new RuntimeException("所选备份记录均不存在，删除失败");
        }
    }

    /**
     * 封装房源分页结果
     */
    private PageResult<PropertyBackup> getPropertyBackupPageResult(Page<PropertyBackup> propertyBackups) {
        PageResult<PropertyBackup> pageResult = new PageResult<>();
        pageResult.setList(propertyBackups.getResult());
        pageResult.setTotal(propertyBackups.getTotal());
        pageResult.setPages(propertyBackups.getPages());
        pageResult.setPageNum(propertyBackups.getPageNum());
        pageResult.setPageSize(propertyBackups.getPageSize());
        return pageResult;
    }

}
package COM.House.Deed.Pavilion.Service;

import COM.House.Deed.Pavilion.DTO.HouseBackupQueryDTO;
import COM.House.Deed.Pavilion.Utils.PageResult;
import COM.House.Deed.Pavilion.Entity.House;
import COM.House.Deed.Pavilion.Entity.HouseBackup;
import COM.House.Deed.Pavilion.Mapper.HouseBackupMapper;
import COM.House.Deed.Pavilion.Mapper.HouseMapper;
import COM.House.Deed.Pavilion.Converter.HouseConverter;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 房源备份服务（处理房源删除备份、查询备份、恢复数据等业务）
 */
@Service
public class HouseBackupService {

    @Resource
    private HouseBackupMapper houseBackupMapper;

    @Resource
    private HouseMapper houseMapper;


    /**
     * 创建房源备份记录（删除房源时调用，确保先备份再删除）
     *
     * @param original      原房源实体（待删除的房源）
     * @param deleteReason  删除原因（审计必填）
     * @param operatorId    执行删除的操作人ID
     */
    @Transactional
    public void createBackup(House original, String deleteReason, Long operatorId) {
        // 1. 参数校验（核心参数非空+有效性）
        if (original == null || original.getId() == null) {
            throw new RuntimeException("原房源记录为空或ID无效，无法创建备份");
        }
        if (operatorId == null || operatorId <= 0) {
            throw new RuntimeException("操作人ID无效（必须为正整数）");
        }
        // 2. 转换为备份实体（复用Converter工具类）
        HouseBackup backup = HouseConverter.convertHouseToBackup(original, operatorId, deleteReason);
        // 3. 执行备份插入（确保备份成功）
        int rows = houseBackupMapper.insert(backup);
        if (rows != 1) {
            throw new RuntimeException("房源备份失败，终止删除操作");
        }
    }


    /**
     * 根据备份ID查询单条备份记录
     *
     * @param backupId 备份记录ID
     * @return 备份详情
     */
    public HouseBackup getBackupById(Long backupId) {
        // 1. 校验ID有效性
        if (backupId == null || backupId <= 0) {
            throw new RuntimeException("备份记录ID无效（必须为正整数）");
        }
        // 2. 查询并校验存在性
        HouseBackup backup = houseBackupMapper.selectById(backupId);
        if (backup == null) {
            throw new RuntimeException("未找到ID为" + backupId + "的房源备份记录");
        }
        return backup;
    }


    /**
     * 根据原房源ID分页查询备份记录（支持同一房源多次删除的备份追溯）
     *
     * @param originalId 原房源ID
     * @param pageNum    页码（默认1）
     * @param pageSize   页大小（默认10，最大100）
     * @return 分页的备份列表
     */
    public PageResult<HouseBackup> getBackupsByOriginalId(Long originalId, Integer pageNum, Integer pageSize) {
        // 1. 校验参数
        if (originalId == null || originalId <= 0) {
            throw new RuntimeException("原房源ID无效（必须为正整数）");
        }
        // 2. 处理分页参数（默认值+范围限制）
        pageNum = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        pageSize = (pageSize == null || pageSize < 1 || pageSize > 100) ? 10 : pageSize;
        // 3. 分页查询
        PageHelper.startPage(pageNum, pageSize);
        Page<HouseBackup> backupPage = houseBackupMapper.selectByOriginalId(originalId);
        // 4. 封装分页结果（复用项目统一的PageResult）
        return getHouseBackupsPageResult(backupPage);
    }

    /**
     * 封装房源备份分页结果
     */
    private PageResult<HouseBackup> getHouseBackupsPageResult(Page<HouseBackup> backupPage) {
        PageResult<HouseBackup> pageResult = new PageResult<>();
        pageResult.setList(backupPage.getResult());
        pageResult.setTotal(backupPage.getTotal());
        pageResult.setPages(backupPage.getPages());
        pageResult.setPageNum(backupPage.getPageNum());
        pageResult.setPageSize(backupPage.getPageSize());
        return pageResult;
    }


    /**
     * 多条件分页查询备份记录
     * 处理分页参数，调用Mapper查询，封装结果
     */
    public PageResult<HouseBackup> getBackupsByCondition(HouseBackupQueryDTO queryDTO) {
        // 1. 处理空DTO（避免空指针）
        if (queryDTO == null) {
            queryDTO = new HouseBackupQueryDTO();
        }

        // 2. 处理分页参数（设置默认值和范围限制）
        Integer pageNum = queryDTO.getPageNum();
        Integer pageSize = queryDTO.getPageSize();
        pageNum = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        pageSize = (pageSize == null || pageSize < 1 || pageSize > 100) ? 10 : pageSize;
        // 回写处理后的分页参数（可选，便于日志跟踪）
        queryDTO.setPageNum(pageNum);
        queryDTO.setPageSize(pageSize);

        // 3. 分页查询（使用PageHelper启动分页）
        PageHelper.startPage(pageNum, pageSize);
        Page<HouseBackup> backupPage = houseBackupMapper.selectByCondition(queryDTO);

        // 4. 封装分页结果（与项目中PageResult工具类适配）
        return getHouseBackupsPageResult(backupPage);
    }


    /**
     * 从备份恢复房源
     * @param backupId 备份ID
     * @param operatorId 操作人ID
     * @param keepBackup 恢复后是否保留备份（true-保留，false-删除）
     */
    @Transactional(rollbackFor = Exception.class) // 事务保证：恢复和备份删除要么都成功，要么都失败
    public void recoverFromBackup(Long backupId, Long operatorId, boolean keepBackup) {
        // 1. 查询备份记录
        HouseBackup backup = houseBackupMapper.selectById(backupId);
        if (backup == null) {
            throw new RuntimeException("备份记录不存在，无法恢复");
        }
        Long originalHouseId = backup.getOriginalId(); // 原房源ID
        Long buildingId = backup.getBuildingId();      // 楼栋ID（用于冲突校验）
        String houseNumber = backup.getHouseNumber();  // 门牌号（用于冲突校验）

        // 2. 转换备份为房源实体
        House house = HouseConverter.convertBackupToHouse(backup);
        house.setUpdatedBy(operatorId);
        house.setUpdatedAt(LocalDateTime.now()); // 强制更新时间为当前操作时间

        // 3. 查询原房源是否物理存在（无论是否有效）
        House existingHouse = houseMapper.selectById(originalHouseId);

        if (existingHouse != null) {
            // 3.1 原记录存在：直接更新（覆盖为备份数据，状态设为有效）
            house.setId(originalHouseId); // 强制使用原ID，避免新增
            houseMapper.updateById(house);
        } else {
            // 3.2 原记录物理删除：先校验门牌号是否被占用
            Integer conflictCount = houseMapper.countByBuildingAndNumber(buildingId, houseNumber);
            if (conflictCount != null && conflictCount > 0) {
                throw new RuntimeException(
                        "恢复失败：楼栋ID【" + buildingId + "】下已存在门牌号【" + houseNumber + "】的有效房源"
                );
            }
            // 校验通过：插入新记录（使用原ID或自增ID，根据表设计）
            // 若house表id是自增主键，可注释此行，由数据库自动生成
            house.setId(originalHouseId);
            houseMapper.insert(house);
        }

        // 4. 根据keepBackup决定是否删除备份
        if (!keepBackup) {
            int deleteCount = houseBackupMapper.deleteById(backupId);
            if (deleteCount == 0) {
                throw new RuntimeException("房源恢复成功，但备份记录删除失败");
            }
        }
    }
}
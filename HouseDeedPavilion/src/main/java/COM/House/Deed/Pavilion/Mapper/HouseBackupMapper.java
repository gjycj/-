package COM.House.Deed.Pavilion.Mapper;

import COM.House.Deed.Pavilion.DTO.HouseBackupQueryDTO;
import COM.House.Deed.Pavilion.Entity.HouseBackup;
import com.github.pagehelper.Page;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * 房源备份Mapper接口（操作house_backup表）
 */
public interface HouseBackupMapper {

    /**
     * 新增房源备份记录（删除房源时调用）
     *
     * @param houseBackup 备份实体
     * @return 影响行数（1-成功，0-失败）
     */
    int insert(HouseBackup houseBackup);

    /**
     * 根据备份ID查询记录
     *
     * @param id 备份记录ID
     * @return 备份详情（null-未查询到）
     */
    HouseBackup selectById(Long id);

    /**
     * 根据原房源ID查询备份记录（可能有多条，按备份时间倒序）
     *
     * @param originalId 原房源ID
     * @return 分页的备份记录列表
     */
    Page<HouseBackup> selectByOriginalId(@Param("originalId") Long originalId);

    /**
     * 多条件分页查询备份记录
     * @param queryDTO 包含所有查询条件和分页参数（由PageHelper处理分页）
     * @return 分页结果（PageHelper自动分页）
     */
    Page<HouseBackup> selectByCondition(HouseBackupQueryDTO queryDTO);

    /**
     * 根据备份ID删除备份记录
     * @param id 备份记录ID
     * @return 影响行数（1-删除成功，0-删除失败）
     */
    int deleteById(Long id);
}
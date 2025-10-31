package COM.House.Deed.Pavilion.Mapper;

import COM.House.Deed.Pavilion.DTO.HouseLandlordBackupQueryDTO;
import COM.House.Deed.Pavilion.Entity.HouseLandlordBackup;
import com.github.pagehelper.Page;

/**
 * 房源-房东关联备份Mapper接口（操作house_landlord_backup表）
 * 仅支持插入备份记录和查询备份，不提供删除功能
 */
public interface HouseLandlordBackupMapper {

    /**
     * 新增备份记录（删除主表记录时自动调用）
     *
     * @param backup 备份信息（包含原记录字段和删除相关信息）
     * @return 影响行数（1-成功，0-失败）
     */
    int insert(HouseLandlordBackup backup);

    /**
     * 根据备份ID查询备份记录
     *
     * @param id 备份记录ID
     * @return 备份实体（null-未查询到）
     */
    HouseLandlordBackup selectById(Long id);

    /**
     * 根据原关联记录ID查询备份（追溯单条删除记录）
     *
     * @param originalId 原关联记录ID（house_landlord表的id）
     * @return 备份实体（null-未查询到）
     */
    HouseLandlordBackup selectByOriginalId(Long originalId);

    /**
     * 多条件分页查询备份记录
     *
     * @param queryDTO 包含查询条件（房源ID、房东ID、时间范围等）和分页参数
     * @return 分页的备份记录列表
     */
    Page<HouseLandlordBackup> selectByCondition(HouseLandlordBackupQueryDTO queryDTO);
}

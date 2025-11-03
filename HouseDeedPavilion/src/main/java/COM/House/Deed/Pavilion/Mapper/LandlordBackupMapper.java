package COM.House.Deed.Pavilion.Mapper;

import COM.House.Deed.Pavilion.DTO.LandlordBackupQueryDTO;
import COM.House.Deed.Pavilion.Entity.LandlordBackup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 房东备份Mapper接口
 * 对应landlord_backup表的CRUD操作
 */
@Mapper
public interface LandlordBackupMapper {

    /**
     * 新增房东备份记录
     */
    int insert(LandlordBackup backup);

    /**
     * 按备份ID查询
     */
    LandlordBackup selectById(Long id);

    /**
     * 按原房东ID查询备份（分页）
     */
    List<LandlordBackup> selectByOriginalId(@Param("originalId") Long originalId);

    /**
     * 多条件分页查询备份
     */
    List<LandlordBackup> selectByCondition(LandlordBackupQueryDTO queryDTO);

    /**
     * 按备份ID删除
     */
    int deleteById(Long id);

    /**
     * 批量删除备份
     */
    int deleteByIds(@Param("ids") List<Long> ids);
}
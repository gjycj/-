package COM.House.Deed.Pavilion.Mapper;

import COM.House.Deed.Pavilion.DTO.BuildingBackupQueryDTO;
import COM.House.Deed.Pavilion.Entity.BuildingBackup;
import com.github.pagehelper.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 楼栋备份Mapper接口（原生MyBatis版）
 * 提供楼栋备份表的查询、新增、删除等操作
 */
@Mapper
public interface BuildingBackupMapper {

    /**
     * 根据备份ID查询单条记录
     * @param id 备份记录ID（自增主键）
     * @return 楼栋备份实体（null表示无此记录）
     */
    BuildingBackup selectById(Long id);

    /**
     * 根据原楼栋ID查询备份记录（查询某楼栋的所有历史备份）
     * @param originalId 原楼栋ID（对应building表的id）
     * @return 分页结果（PageHelper自动分页）
     */
    Page<BuildingBackup> selectByOriginalId(@Param("originalId") Long originalId);

    /**
     * 多条件分页查询备份记录
     * @param queryDTO 包含查询条件（如楼盘ID、名称、时间范围等）和分页参数
     * @return 分页结果（PageHelper自动分页）
     */
    Page<BuildingBackup> selectByCondition(BuildingBackupQueryDTO queryDTO);

    /**
     * 新增楼栋备份记录（删除楼栋时自动创建）
     * @param buildingBackup 楼栋备份实体（需包含所有非空字段）
     * @return 影响行数（1=成功，0=失败）
     */
    int insert(BuildingBackup buildingBackup);

    /**
     * 根据备份ID删除单条记录
     * @param id 备份记录ID
     * @return 影响行数（1=成功，0=无此记录）
     */
    int deleteById(Long id);

}
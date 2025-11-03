package COM.House.Deed.Pavilion.Mapper;

import COM.House.Deed.Pavilion.DTO.PropertyBackupQueryDTO;
import COM.House.Deed.Pavilion.Entity.PropertyBackup;
import com.github.pagehelper.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PropertyBackupMapper {

    /**
     * 按备份ID查询
     */
    PropertyBackup selectById(Long id);

    /**
     * 按原楼盘ID查询备份
     */
    Page<PropertyBackup> selectByOriginalId(@Param("originalId") Long originalId);

    /**
     * 多条件分页查询
     */
    Page<PropertyBackup> selectByCondition(PropertyBackupQueryDTO queryDTO);

    /**
     * 新增备份记录
     */
    int insert(PropertyBackup propertyBackup);

    /**
     * 单个删除备份
     */
    int deleteById(Long id);

    /**
     * 批量删除备份
     */
    int deleteByIds(@Param("ids") Iterable<Long> ids);
}
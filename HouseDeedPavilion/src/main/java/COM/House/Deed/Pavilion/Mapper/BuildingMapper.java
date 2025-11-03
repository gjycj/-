package COM.House.Deed.Pavilion.Mapper;

import COM.House.Deed.Pavilion.DTO.BuildingQueryDTO;
import COM.House.Deed.Pavilion.Entity.Building;
import com.github.pagehelper.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 楼栋信息Mapper接口
 */
@Mapper
public interface BuildingMapper {

    /**
     * 新增楼栋
     *
     * @param building 楼栋信息
     * @return 影响行数
     */
    int insert(Building building);

    /**
     * 根据ID和名称查询楼栋
     *
     * @param name       楼栋名称
     * @param propertyId 楼栋ID
     * @return 楼栋详情
     */
    List<Building> selectByNameAndPropertyId(@Param("name") String name, @Param("propertyId") Long propertyId);

    /**
     * 根据ID查询楼栋
     *
     * @param id 楼栋ID
     * @return 楼栋详情
     */
    Building selectById(@Param("id") Long id);

    /**
     * 根据楼盘ID查询楼栋列表（分页）
     *
     * @param propertyId 所属楼盘ID
     * @return 分页的楼栋列表
     */
    Page<Building> selectByPropertyId(@Param("propertyId") Long propertyId);

    /**
     * 多条件分页查询楼栋
     *
     * @param queryDTO 查询条件（包含楼盘ID、名称模糊查询等）
     * @return 分页结果
     */
    Page<Building> selectByCondition(BuildingQueryDTO queryDTO);

    /**
     * 根据ID更新楼栋
     *
     * @param building 包含ID和待更新字段的楼栋信息
     * @return 影响行数
     */
    int updateById(Building building);

    /**
     * 根据楼栋ID删除记录
     * @param id 楼栋ID（原表主键）
     * @return 影响行数（1=删除成功，0=无此记录）
     */
    int deleteById(Long id);

    // 在BuildingMapper接口中新增
    /**
     * 统计指定楼盘下的楼栋数量（用于删除楼盘时的关联校验）
     */
    int countByPropertyId(@Param("propertyId") Long propertyId);

}
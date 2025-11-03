package COM.House.Deed.Pavilion.Mapper;

import COM.House.Deed.Pavilion.DTO.PropertyQueryDTO;
import COM.House.Deed.Pavilion.Entity.Property;
import com.github.pagehelper.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper // 标记为MyBatis映射接口
public interface PropertyMapper {

    /**
     * 新增楼盘
     * @param property 楼盘信息
     * @return 影响行数（1表示成功，0表示失败）
     */
    int insert(Property property);

    /**
     * 根据ID查询楼盘信息
     * @param id 楼盘ID
     * @return 楼盘详情（不存在则返回null）
     */
    Property selectById(@Param("id") Long id);

    /**
     * 根据ID更新楼盘信息（只更新非null字段）
     * @param property 楼盘信息（包含ID和待更新字段）
     * @return 影响行数（1表示成功，0表示未找到该楼盘）
     */
    int updateById(Property property);

    /**
     * 根据多条件分页查询楼盘
     * @param queryDTO 包含查询条件和分页参数
     * @return 分页结果
     */
    Page<Property> selectByCondition(PropertyQueryDTO queryDTO);

    /**
     * 分页查询楼盘（支持按物业类型筛选）
     * @param propertyType 物业类型（可选，为null时查询全部）
     * @return 分页结果（包含当前页数据和分页信息）
     */
    Page<Property> selectByPage(@Param("propertyType") String propertyType);

    /**
     * 根据楼盘ID删除记录
     * @param id 楼盘ID（原表主键）
     * @return 影响行数（1=删除成功，0=无此记录）
     */
    int deleteById(Long id);

    // 补充：之前在PropertyService中用到的关联校验校验方法（已补充）
    int countByPropertyId(@Param("propertyId") Long propertyId);
}
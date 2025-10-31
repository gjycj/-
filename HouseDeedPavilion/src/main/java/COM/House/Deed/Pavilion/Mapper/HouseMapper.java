package COM.House.Deed.Pavilion.Mapper;

import COM.House.Deed.Pavilion.DTO.HouseQueryDTO;
import COM.House.Deed.Pavilion.Entity.House;
import com.github.pagehelper.Page;
import org.apache.ibatis.annotations.Param;

/**
 * 房源Mapper接口（操作house表）
 */
public interface HouseMapper {

    /**
     * 新增房源
     *
     * @param house 房源实体（包含待插入的字段值）
     * @return 影响行数（1-成功，0-失败）
     */
    int insert(House house);

    /**
     * 根据ID查询房源
     *
     * @param id 房源ID（主键）
     * @return 房源实体（null-未查询到）
     */
    House selectById(Long id);

    /**
     * 根据楼栋ID和门牌号查询房源（用于唯一性校验）
     *
     * @param buildingId  所属楼栋ID
     * @param houseNumber 门牌号
     * @param excludeId   排除的房源ID（更新时使用，避免与自身冲突）
     * @return 匹配的房源（null-无匹配）
     */
    House selectByBuildingIdAndHouseNumber(
            @Param("buildingId") Long buildingId,
            @Param("houseNumber") String houseNumber,
            @Param("excludeId") Long excludeId
    );

    /**
     * 根据ID更新房源信息
     *
     * @param house 包含ID和待更新字段的实体
     * @return 影响行数（1-成功，0-失败）
     */
    int updateById(House house);

    /**
     * 多条件分页查询房源
     *
     * @param queryDTO 包含查询条件和分页参数的DTO
     * @return 分页的房源列表（Page对象）
     */
    Page<House> selectByCondition(HouseQueryDTO queryDTO);

    /**
     * 根据楼栋ID分页查询房源
     *
     * @param buildingId 所属楼栋ID
     * @return 分页的房源列表（Page对象）
     */
    Page<House> selectByBuildingId(@Param("buildingId") Long buildingId);
}

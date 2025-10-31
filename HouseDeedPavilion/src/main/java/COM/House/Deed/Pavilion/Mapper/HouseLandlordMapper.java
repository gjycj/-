package COM.House.Deed.Pavilion.Mapper;

import COM.House.Deed.Pavilion.DTO.HouseLandlordQueryDTO;
import COM.House.Deed.Pavilion.Entity.HouseLandlord;
import COM.House.Deed.Pavilion.Entity.Landlord;
import com.github.pagehelper.Page;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 房源-房东关联Mapper接口（操作house_landlord表）
 */
public interface HouseLandlordMapper {

    /**
     * 新增房源-房东关联关系
     *
     * @param houseLandlord 关联信息（包含houseId、landlordId等必填字段）
     * @return 影响行数（1-成功，0-失败）
     */
    int insert(HouseLandlord houseLandlord);

    /**
     * 根据ID更新关联信息（主要用于更新是否为主要房东）
     *
     * @param houseLandlord 包含ID和待更新字段的关联实体
     * @return 影响行数（1-成功，0-失败）
     */
    int updateById(HouseLandlord houseLandlord);

    /**
     * 根据ID删除关联关系
     *
     * @param id 关联记录ID
     * @return 影响行数（1-成功，0-失败）
     */
    int deleteById(Long id);

    /**
     * 根据房源ID和房东ID删除关联关系（用于解除特定关联）
     *
     * @param houseId 房源ID
     * @param landlordId 房东ID
     * @return 影响行数（1-成功，0-失败）
     */
    int deleteByHouseAndLandlord(
            @Param("houseId") Long houseId,
            @Param("landlordId") Long landlordId
    );

    /**
     * 根据ID查询关联记录
     *
     * @param id 关联记录ID
     * @return 关联实体（null-未查询到）
     */
    HouseLandlord selectById(Long id);

    /**
     * 检查房源与房东的关联
     是否已存在（唯一键校验）
     *
     * @param houseId 房源ID
     * @param landlordId 房东ID
     * @return 匹配的关联记录（null-不存在）
     */
    HouseLandlord selectByHouseAndLandlord(
            @Param("houseId") Long houseId,
            @Param("landlordId") Long landlordId
    );

    /**
     * 根据房源ID查询关联的所有房东ID
     *
     * @param houseId 房源ID
     * @return 房东ID列表
     */
    List<Long> selectLandlordIdsByHouseId(Long houseId);

    /**
     * 根据房源ID查询关联的房东列表（包含房东详情）
     *
     * @param houseId 房源ID
     * @return 房东信息列表
     */
    List<Landlord> selectLandlordsByHouseId(Long houseId);

    /**
     * 多条件分页查询关联关系
     *
     * @param queryDTO 包含查询条件和分页参数的DTO
     * @return 分页的关联记录列表
     */
    Page<HouseLandlord> selectByCondition(HouseLandlordQueryDTO queryDTO);
}

package COM.House.Deed.Pavilion.Mapper;

import COM.House.Deed.Pavilion.DTO.LandlordQueryDTO;
import COM.House.Deed.Pavilion.Entity.Landlord;
import com.github.pagehelper.Page;
import org.apache.ibatis.annotations.Param;

/**
 * 房东Mapper接口（操作landlord表）
 */
public interface LandlordMapper {

    /**
     * 新增房东信息
     *
     * @param landlord 房东实体（包含待插入的字段值）
     * @return 影响行数（1-成功，0-失败）
     */
    int insert(Landlord landlord);

    /**
     * 根据ID查询房东信息
     *
     * @param id 房东ID（主键）
     * @return 房东实体（null-未查询到）
     */
    Landlord selectById(Long id);

    /**
     * 根据联系电话查询房东（用于唯一性校验）
     *
     * @param phone  房东联系电话
     * @param excludeId 排除的房东ID（更新时使用，避免与自身冲突）
     * @return 匹配的房东（null-无匹配）
     */
    Landlord selectByPhone(
            @Param("phone") String phone,
            @Param("excludeId") Long excludeId
    );

    /**
     * 根据ID更新房东信息
     *
     * @param landlord 包含ID和待更新字段的实体
     * @return 影响行数（1-成功，0-失败）
     */
    int updateById(Landlord landlord);

    /**
     * 多条件分页查询房东
     *
     * @param queryDTO 包含查询条件和分页参数的DTO
     * @return 分页的房东列表（Page对象）
     */
    Page<Landlord> selectByCondition(LandlordQueryDTO queryDTO);

    /**
     * 根据姓名模糊查询房东（分页）
     *
     * @param name 房东姓名（模糊匹配）
     * @return 分页的房东列表
     */
    Page<Landlord> selectByName(@Param("name") String name);
}

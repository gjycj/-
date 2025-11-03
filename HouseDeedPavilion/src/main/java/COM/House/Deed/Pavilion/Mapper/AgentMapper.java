package COM.House.Deed.Pavilion.Mapper;

import COM.House.Deed.Pavilion.DTO.AgentQueryDTO;
import COM.House.Deed.Pavilion.Entity.Agent;
import com.github.pagehelper.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 经纪人Mapper接口（操作agent表）
 */
@Mapper
public interface AgentMapper {

    /**
     * 新增经纪人
     *
     * @param agent 经纪人实体（包含待插入的字段值）
     * @return 影响行数（1-成功，0-失败）
     */
    int insert(Agent agent);

    /**
     * 根据ID查询经纪人
     *
     * @param id 经纪人ID（主键）
     * @return 经纪人实体（null-未查询到）
     */
    Agent selectById(Long id);

    /**
     * 根据工号查询经纪人（用于唯一性校验）
     *
     * @param employeeId 经纪人工号
     * @param excludeId  排除的经纪人ID（更新时使用，避免与自身冲突）
     * @return 匹配的经纪人（null-无匹配）
     */
    Agent selectByEmployeeId(
            @Param("employeeId") String employeeId,
            @Param("excludeId") Long excludeId
    );

    /**
     * 根据ID更新经纪人信息
     *
     * @param agent 包含ID和待更新字段的实体
     * @return 影响行数（1-成功，0-失败）
     */
    int updateById(Agent agent);

    /**
     * 多条件分页查询经纪人
     *
     * @param queryDTO 包含查询条件和分页参数的DTO
     * @return 分页的经纪人列表（Page对象）
     */
    Page<Agent> selectByCondition(AgentQueryDTO queryDTO);

    /**
     * 根据门店ID分页查询经纪人
     *
     * @param storeId 所属门店ID
     * @return 分页的经纪人列表（Page对象）
     */
    Page<Agent> selectByStoreId(@Param("storeId") Long storeId);

    /**
     * 根据ID删除经纪人记录（物理删除）
     * @param id 经纪人ID（agent表的id）
     * @return 影响行数（1=删除成功）
     */
    int deleteById(Long id);

    /**
     * 根据工号统计经纪人数量（用于唯一性校验）
     * @param employeeId 经纪人工号
     * @param excludeId 排除的ID（更新时使用）
     * @return 匹配的数量
     */
    Long countByEmployeeId(
            @Param("employeeId") String employeeId,
            @Param("excludeId") Long excludeId
    );
}
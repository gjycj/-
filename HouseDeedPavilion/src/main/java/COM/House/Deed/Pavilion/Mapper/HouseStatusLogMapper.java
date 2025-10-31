package COM.House.Deed.Pavilion.Mapper;

import COM.House.Deed.Pavilion.DTO.HouseStatusLogQueryDTO;
import COM.House.Deed.Pavilion.Entity.HouseStatusLog;
import com.github.pagehelper.Page;

import java.util.List;

/**
 * 房源状态变更记录Mapper接口（操作house_status_log表）
 * 支持状态变更记录的插入和查询
 */
public interface HouseStatusLogMapper {

    /**
     * 新增状态变更记录
     *
     * @param log 状态变更信息（包含房源ID、状态变更前后值等）
     * @return 影响行数（1-成功，0-失败）
     */
    int insert(HouseStatusLog log);

    /**
     * 根据ID查询状态变更记录
     *
     * @param id 记录ID
     * @return 状态变更记录（null-未查询到）
     */
    HouseStatusLog selectById(Long id);

    /**
     * 根据房源ID查询所有状态变更记录（按时间倒序）
     *
     * @param houseId 房源ID
     * @return 状态变更记录列表
     */
    List<HouseStatusLog> selectByHouseId(Long houseId);

    /**
     * 多条件分页查询状态变更记录
     *
     * @param queryDTO 包含查询条件和分页参数的DTO
     * @return 分页的状态变更记录列表
     */
    Page<HouseStatusLog> selectByCondition(HouseStatusLogQueryDTO queryDTO);
}

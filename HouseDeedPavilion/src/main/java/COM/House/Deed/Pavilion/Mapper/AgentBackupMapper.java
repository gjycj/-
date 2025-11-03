package COM.House.Deed.Pavilion.Mapper;

import COM.House.Deed.Pavilion.DTO.AgentBackupQueryDTO;
import COM.House.Deed.Pavilion.Entity.AgentBackup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 经纪人备份表Mapper接口
 * 处理agent_backup表的CRUD及查询操作
 */
@Mapper
public interface AgentBackupMapper {

    /**
     * 新增经纪人备份记录（删除经纪人时调用）
     * @param agentBackup 经纪人备份实体
     * @return 影响行数（1=成功）
     */
    int insert(AgentBackup agentBackup);

    /**
     * 根据备份ID查询单条备份记录
     * @param id 备份记录ID（agent_backup.id）
     * @return 经纪人备份实体
     */
    AgentBackup selectById(Long id);

    /**
     * 根据原经纪人ID查询备份记录（原agent表的id）
     * @param originalId 原经纪人ID
     * @return 备份记录（一个经纪人可能有多次备份，返回列表）
     */
    List<AgentBackup> selectByOriginalId(Long originalId);

    /**
     * 按工号查询备份记录（工号唯一，返回单条）
     * @param employeeId 原经纪人工号
     * @return 经纪人备份实体
     */
    AgentBackup selectByEmployeeId(String employeeId);

    /**
     * 多条件分页查询备份记录
     * @param queryDTO 包含查询条件（姓名模糊、电话、备份时间范围等）
     * @return 符合条件的备份列表
     */
    List<AgentBackup> selectByCondition(AgentBackupQueryDTO queryDTO);

    /**
     * 根据备份ID删除备份记录（如恢复后清理备份）
     * @param id 备份记录ID
     * @return 影响行数（1=成功）
     */
    int deleteById(Long id);

    /**
     * 批量删除过期备份（按备份时间筛选，用于数据清理）
     * @param deadline 过期时间（备份时间早于该时间的记录会被删除）
     * @return 影响行数
     */
    int batchDeleteExpired(@Param("deadline") String deadline);
}
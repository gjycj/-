package COM.House.Deed.Pavilion.Mapper;

import COM.House.Deed.Pavilion.Entity.AgentBackup;
import org.apache.ibatis.annotations.Mapper;

/**
 * 经纪人备份表Mapper接口（操作agent_backup表）
 */
@Mapper
public interface AgentBackupMapper {

    /**
     * 新增经纪人备份记录（删除经纪人时调用）
     *
     * @param agentBackup 备份实体（包含原经纪人信息和删除详情）
     * @return 影响行数（1-成功，0-失败）
     */
    int insert(AgentBackup agentBackup);

    /**
     * 根据备份ID删除备份记录
     *
     * @param id 备份记录ID（agent_backup表的主键）
     * @return 影响行数（1-成功，0-失败）
     */
    int deleteById(Long id);

    /**
     * 根据原经纪人ID查询备份记录（用于校验是否已备份）
     *
     * @param originalId 原经纪人ID（agent表的id）
     * @return 备份实体（null-未找到）
     */
    AgentBackup selectByOriginalId(Long originalId);

    /**
     * 根据备份ID查询备份记录（用于查看或恢复备份）
     *
     * @param id 备份记录ID
     * @return 备份实体（null-未找到）
     */
    AgentBackup selectById(Long id);


    int countByEmployeeId(String employeeId);
}
package COM.House.Deed.Pavilion.Converter;

import COM.House.Deed.Pavilion.Entity.Agent;
import COM.House.Deed.Pavilion.Entity.AgentBackup;
import org.springframework.stereotype.Component;

/**
 * 经纪人实体与备份实体的转换工具
 * 负责Agent与AgentBackup之间的字段复制，隔离业务字段与备份特有字段
 */
@Component
public class AgentConverter {

    /**
     * 将原经纪人实体转换为备份实体（删除经纪人时调用）
     * 仅复制业务字段，备份特有字段（如backupTime）由Service层补充
     *
     * @param original 原经纪人实体（从agent表查询的记录）
     * @return 经纪人备份实体（待插入agent_backup表）
     */
    public AgentBackup convertToBackup(Agent original) {
        if (original == null) {
            return null;
        }

        AgentBackup backup = new AgentBackup();
        // 1. 关联原记录ID（核心关联字段）
        backup.setOriginalId(original.getId());

        // 2. 复制原表所有业务字段（与agent表字段一一对应）
        backup.setName(original.getName());
        backup.setEmployeeId(original.getEmployeeId());
        backup.setPhone(original.getPhone());
        backup.setStoreId(original.getStoreId());
        backup.setPosition(original.getPosition());
        backup.setStatus(original.getStatus());
        backup.setCreatedAt(original.getCreatedAt()); // 保留原创建时间
        backup.setUpdatedAt(original.getUpdatedAt()); // 保留原最后更新时间

        // 3. 备份特有字段（backupTime/deleteReason/deletedBy）不在这里设置，由Service层处理
        return backup;
    }

    /**
     * 将备份实体转换为原经纪人实体（从备份恢复时调用）
     * 仅复制业务字段，重置ID和时间（由恢复操作生成新记录）
     *
     * @param backup 经纪人备份实体（从agent_backup表查询的记录）
     * @return 原经纪人实体（待插入agent表的新记录）
     */
    public Agent convertToOriginal(AgentBackup backup) {
        if (backup == null) {
            return null;
        }

        Agent agent = new Agent();
        // 1. 复制备份中的业务字段（恢复原始业务数据）
        agent.setName(backup.getName());
        agent.setEmployeeId(backup.getEmployeeId());
        agent.setPhone(backup.getPhone());
        agent.setStoreId(backup.getStoreId());
        agent.setPosition(backup.getPosition());
        agent.setStatus(backup.getStatus());

        // 2. 重置ID和时间字段（恢复时生成新记录，不复用原ID）
        agent.setId(null); // 由数据库自增生成新ID
        // createdAt/updatedAt由Service层设置为恢复操作的时间
        // createdBy/updatedAt由Service层设置为恢复操作人ID

        // 3. 忽略备份特有字段（originalId/backupTime等不进入原表）
        return agent;
    }
}
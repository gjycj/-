package COM.House.Deed.Pavilion.Service;

import COM.House.Deed.Pavilion.Entity.Agent;
import COM.House.Deed.Pavilion.Entity.AgentBackup;
import COM.House.Deed.Pavilion.Mapper.AgentBackupMapper;
import COM.House.Deed.Pavilion.Mapper.AgentMapper;
import io.micrometer.common.util.StringUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 经纪人备份表Service（处理备份记录的新增、删除、查询等业务）
 */
@Service
public class AgentBackupService {

    @Resource
    private AgentBackupMapper agentBackupMapper;

    @Resource
    private AgentMapper agentMapper;

    /**
     * 新增经纪人备份记录
     *
     * @param agentBackup 备份实体（需包含原经纪人信息和备份时间）
     */
    @Transactional
    public void insertBackup(AgentBackup agentBackup) {
        // 1. 非空校验（补充实体注解外的业务校验）
        if (agentBackup == null) {
            throw new RuntimeException("备份信息不能为空");
        }
        Long originalId = agentBackup.getOriginalId();
        if (originalId == null || originalId <= 0) {
            throw new RuntimeException("原经纪人ID无效（必须为正整数）");
        }

        // 2. 校验原经纪人是否存在（避免备份已删除的经纪人）
        Agent originalAgent = agentMapper.selectById(originalId); // 需要注入agentMapper
        if (originalAgent == null) {
            throw new RuntimeException("原经纪人（ID：" + originalId + "）不存在，无法备份");
        }

        // 3. 校验备份工号是否已存在（因uk_agent_backup_employee_id约束）
        String employeeId = agentBackup.getEmployeeId();
        int empIdCount = agentBackupMapper.countByEmployeeId(employeeId); // 自定义方法：按工号统计备份数
        if (empIdCount > 0) {
            throw new RuntimeException("工号【" + employeeId + "】已存在备份记录，无法重复备份");
        }

        // 4. 校验核心字段完整性（冗余校验，避免实体注解失效）
        if (StringUtils.isBlank(agentBackup.getName())) {
            throw new RuntimeException("经纪人姓名不能为空");
        }
        if (StringUtils.isBlank(agentBackup.getPhone())) {
            throw new RuntimeException("经纪人电话不能为空");
        }

        // 5. 执行备份
        int rows = agentBackupMapper.insert(agentBackup);
        if (rows != 1) {
            throw new RuntimeException("备份记录保存失败");
        }
    }

    /**
     * 根据备份ID删除备份记录
     *
     * @param backupId 备份记录ID（agent_backup表主键）
     */
    @Transactional
    public void deleteBackupById(Long backupId) {
        // 1. 校验备份ID合法性
        if (backupId == null || backupId <= 0) {
            throw new RuntimeException("备份记录ID无效（必须为正整数）");
        }

        // 2. 校验备份记录是否存在
        AgentBackup existing = agentBackupMapper.selectById(backupId);
        if (existing == null) {
            throw new RuntimeException("备份记录不存在（ID：" + backupId + "），无法删除");
        }

        // 3. 执行删除
        int rows = agentBackupMapper.deleteById(backupId);
        if (rows != 1) {
            throw new RuntimeException("删除备份记录失败");
        }
    }

    /**
     * 根据原经纪人ID查询备份记录
     *
     * @param originalAgentId 原经纪人ID（agent表的id）
     * @return 备份实体（null表示无对应备份）
     */
    public AgentBackup getBackupByOriginalId(Long originalAgentId) {
        // 1. 校验原经纪人ID合法性
        if (originalAgentId == null || originalAgentId <= 0) {
            throw new RuntimeException("原经纪人ID无效（必须为正整数）");
        }

        // 2. 执行查询
        return agentBackupMapper.selectByOriginalId(originalAgentId);
    }

    /**
     * 根据备份ID查询备份记录
     *
     * @param backupId 备份记录ID（agent_backup表主键）
     * @return 备份实体
     */
    public AgentBackup getBackupById(Long backupId) {
        // 1. 校验备份ID合法性
        if (backupId == null || backupId <= 0) {
            throw new RuntimeException("备份记录ID无效（必须为正整数）");
        }

        // 2. 执行查询
        AgentBackup backup = agentBackupMapper.selectById(backupId);
        if (backup == null) {
            throw new RuntimeException("未找到ID为" + backupId + "的备份记录");
        }
        return backup;
    }
}
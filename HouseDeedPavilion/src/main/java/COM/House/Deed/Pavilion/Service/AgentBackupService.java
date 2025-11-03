package COM.House.Deed.Pavilion.Service;

import COM.House.Deed.Pavilion.Constant.MessageConstant;
import COM.House.Deed.Pavilion.DTO.AgentBackupQueryDTO;
import COM.House.Deed.Pavilion.Entity.Agent;
import COM.House.Deed.Pavilion.Entity.AgentBackup;
import COM.House.Deed.Pavilion.Exception.BusinessException;
import COM.House.Deed.Pavilion.Mapper.AgentBackupMapper;
import COM.House.Deed.Pavilion.Mapper.AgentMapper;
import COM.House.Deed.Pavilion.Converter.AgentConverter;
import COM.House.Deed.Pavilion.Utils.PageResult;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 经纪人备份服务实现
 */
@Service
public class AgentBackupService {

    @Resource
    private AgentMapper agentMapper; // 原经纪人表Mapper

    @Resource
    private AgentBackupMapper backupMapper; // 备份表Mapper

    @Resource
    private AgentConverter agentConverter; // 实体转换工具（原经纪人 ↔ 备份经纪人）


    /**
     * 删除经纪人并备份（核心业务：先备份再删除，事务保证原子性）
     */
    @Transactional(rollbackFor = Exception.class)
    public Long deleteAndBackup(Long agentId, Long operatorId, String deleteReason) {
        // 1. 校验经纪人是否存在
        Agent originalAgent = agentMapper.selectById(agentId);
        if (originalAgent == null) {
            throw new BusinessException(String.format(MessageConstant.AGENT_NOT_FOUND, agentId));
        }

        // 2. 转换为备份实体并补充备份字段
        AgentBackup backup = agentConverter.convertToBackup(originalAgent);
        backup.setBackupTime(LocalDateTime.now()); // 备份时间=删除时间
        backup.setDeletedBy(operatorId); // 记录删除人
        backup.setDeleteReason(deleteReason); // 记录删除原因

        // 3. 插入备份表
        int backupRows = backupMapper.insert(backup);
        if (backupRows != 1) {
            throw new BusinessException(MessageConstant.AGENT_BACKUP_FAIL);
        }

        // 4. 删除原经纪人表记录（需确保关联表外键处理正确，如house表agent_id置空）
        int deleteRows = agentMapper.deleteById(agentId);
        if (deleteRows != 1) {
            throw new BusinessException(MessageConstant.AGENT_DELETE_FAIL);
        }
        return agentId;
    }


    /**
     * 从备份恢复经纪人（核心业务：校验冲突 → 恢复数据 → 按需清理备份）
     */
    @Transactional(rollbackFor = Exception.class)
    public Long recoverFromBackup(Long backupId, Long operatorId, boolean keepBackup) {
        // 1. 校验备份记录是否存在
        AgentBackup backup = backupMapper.selectById(backupId);
        if (backup == null) {
            throw new BusinessException(String.format(MessageConstant.AGENT_BACKUP_NOT_FOUND, backupId));
        }

        // 2. 校验原工号是否已存在（工号唯一，避免冲突）
        Agent existingAgent = agentMapper.selectByEmployeeId(backup.getEmployeeId(), backup.getId());
        if (existingAgent != null) {
            throw new BusinessException(String.format(MessageConstant.AGENT_ALREADY_EXISTS, backup.getEmployeeId()));
        }

        // 3. 转换为原经纪人实体并补充恢复信息
        Agent agent = agentConverter.convertToOriginal(backup);
        // 重置ID（新生成），并更新创建/更新人（恢复操作人）
        agent.setId(null);
        agent.setCreatedAt(LocalDateTime.now()); // 恢复时间作为新创建时间
        agent.setUpdatedAt(LocalDateTime.now());

        // 4. 插入原经纪人表
        int insertRows = agentMapper.insert(agent);
        if (insertRows != 1) {
            throw new BusinessException(MessageConstant.AGENT_RECOVER_FAIL);
        }

        // 5. 按需删除备份记录
        if (!keepBackup) {
            backupMapper.deleteById(backupId);
        }

        return agent.getId(); // 返回新生成的经纪人ID
    }


    /**
     * 根据备份ID查询单条备份
     */
    public AgentBackup getBackupById(Long backupId) {
        if (backupId == null || backupId <= 0) {
            throw new BusinessException("备份ID无效（必须为正整数）");
        }
        return backupMapper.selectById(backupId);
    }


    /**
     * 根据原经纪人ID查询备份列表
     */
    public List<AgentBackup> getBackupsByOriginalId(Long originalAgentId) {
        if (originalAgentId == null || originalAgentId <= 0) {
            throw new BusinessException("原经纪人ID无效（必须为正整数）");
        }
        return backupMapper.selectByOriginalId(originalAgentId);
    }


    /**
     * 多条件分页查询备份
     */
    public PageResult<AgentBackup> queryBackups(AgentBackupQueryDTO queryDTO) {
        // 处理分页参数（默认页码1，页大小10）
        int pageNum = queryDTO.getPageNum() == null ? 1 : queryDTO.getPageNum();
        int pageSize = queryDTO.getPageSize() == null ? 10 : queryDTO.getPageSize();

        // 分页查询
        PageHelper.startPage(pageNum, pageSize);
        Page<AgentBackup> backupPage = (Page<AgentBackup>) backupMapper.selectByCondition(queryDTO);

        // 封装分页结果
        return getAgentBackupPageResult(backupPage);
    }

    /**
     * 封装房源分页结果
     */
    private PageResult<AgentBackup> getAgentBackupPageResult(Page<AgentBackup> agentBackups) {
        PageResult<AgentBackup> pageResult = new PageResult<>();
        pageResult.setList(agentBackups.getResult());
        pageResult.setTotal(agentBackups.getTotal());
        pageResult.setPages(agentBackups.getPages());
        pageResult.setPageNum(agentBackups.getPageNum());
        pageResult.setPageSize(agentBackups.getPageSize());
        return pageResult;
    }

}
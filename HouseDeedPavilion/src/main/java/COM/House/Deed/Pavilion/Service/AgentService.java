package COM.House.Deed.Pavilion.Service;

import COM.House.Deed.Pavilion.DTO.AgentQueryDTO;
import COM.House.Deed.Pavilion.Entity.Agent;
import COM.House.Deed.Pavilion.Entity.AgentBackup;
import COM.House.Deed.Pavilion.Mapper.AgentBackupMapper;
import COM.House.Deed.Pavilion.Mapper.AgentMapper;
import COM.House.Deed.Pavilion.Mapper.HouseMapper;
import COM.House.Deed.Pavilion.Utils.PageResult;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.micrometer.common.util.StringUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AgentService {

    @Resource
    private AgentMapper agentMapper;

    @Resource // 注入备份表Mapper
    private AgentBackupMapper agentBackupMapper;

    @Resource
    private HouseMapper houseMapper;

    /**
     * 根据ID删除经纪人（先备份再物理删除）
     *
     * @param id 经纪人ID
     */
    @Transactional
    public void deleteAgentById(Long id, String deleteReason, Long operatorId) {
        // 1. 基础ID校验
        if (id == null || id <= 0) {
            throw new RuntimeException("经纪人ID无效（必须为正整数）");
        }
        // 新增：校验操作人ID合法性
        if (operatorId == null || operatorId <= 0) {
            throw new RuntimeException("操作人ID无效（必须为正整数）");
        }

        // 2. 校验经纪人是否存在
        Agent existing = agentMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("经纪人不存在（ID：" + id + "），无法删除");
        }

        // 3. 校验是否关联有效房源（外键冲突预检查）
        int validHouseCount = houseMapper.countByAgentIdAndValid(existing.getId()); // 自定义方法：查询有效房源数
        if (validHouseCount > 0) {
            throw new RuntimeException("该经纪人仍有" + validHouseCount + "条有效房源，无法删除");
        }

        // 4. 校验是否已备份（避免重复备份，因有唯一索引）
        AgentBackup existingBackup = agentBackupMapper.selectByOriginalId(id);
        if (existingBackup != null) {
            throw new RuntimeException("经纪人已备份（备份ID：" + existingBackup.getId() + "），无需重复删除");
        }
    // 3. 备份到agent_backup表（关键：设置操作人信息）
        AgentBackup backup = new AgentBackup();
        // 修正：备份的originalId关联原经纪人ID（原代码错误地用了existing.getId()作为备份ID，应改为originalId）
        backup.setOriginalId(existing.getId());
        backup.setName(existing.getName());
        backup.setEmployeeId(existing.getEmployeeId());
        backup.setPhone(existing.getPhone());
        backup.setStoreId(existing.getStoreId());
        backup.setPosition(existing.getPosition());
        backup.setStatus(existing.getStatus());
        backup.setCreatedAt(existing.getCreatedAt());
        backup.setUpdatedAt(existing.getUpdatedAt());
        backup.setBackupTime(LocalDateTime.now()); // 备份时间
        // 核心：设置删除操作人ID
        backup.setDeletedBy(operatorId);
        // 可选：设置删除原因
        backup.setDeleteReason(deleteReason);
        backup.setCreatedBy(operatorId); // 补充设置创建人ID（需从上下文获取当前登录用户ID）
        backup.setUpdatedBy(operatorId);

        // 执行备份插入
        int backupRows = agentBackupMapper.insert(backup);
        if (backupRows != 1) {
            throw new RuntimeException("备份经纪人信息失败，删除操作终止");
        }

        // 4. 执行删除（备份成功后再删除）
        int deleteRows = agentMapper.deleteById(id);
        if (deleteRows != 1) {
            throw new RuntimeException("删除经纪人失败，请重试");
        }
    }

    /**
     * 新增经纪人
     *
     * @param agent 经纪人信息（包含姓名、工号等必填字段）
     * @return 新增的经纪人ID
     */
    @Transactional // 开启事务，确保数据一致性
    public Long addAgent(Agent agent, Long operatorId) {
        // 1. 校验工号唯一性（核心业务校验）
        validateEmployeeIdUnique(agent.getEmployeeId(), null);

        // 设置操作人字段
        agent.setCreatedBy(operatorId);  // 创建人=当前操作人
        agent.setUpdatedBy(operatorId);  // 初始更新人=当前操作人

        // 2. 设置时间字段（若数据库未配置默认值，手动赋值）
        LocalDateTime now = LocalDateTime.now();
        agent.setCreatedAt(now);
        agent.setUpdatedAt(now);
        // 3. 执行插入
        int rows = agentMapper.insert(agent);

        if (rows == 0) {
            throw new RuntimeException("新增经纪人失败，请重试");
        }
        // 4. 返回新增的经纪人ID（MyBatis自动回填自增主键）
        return agent.getId();
    }



    /**
     * 根据ID查询经纪人详情
     *
     * @param id 经纪人ID
     * @return 经纪人完整信息
     */
    public Agent getAgentById(Long id) {
        // 1. 校验ID合法性
        if (id == null || id <= 0) {
            throw new RuntimeException("经纪人ID无效（必须为正整数）");
        }

        // 2. 执行查询
        Agent agent = agentMapper.selectById(id);
        if (agent == null) {
            throw new RuntimeException("未找到ID为" + id + "的经纪人");
        }

        return agent;
    }

    /**
     * 判断指定ID的经纪人是否存在
     *
     * @param id 经纪人ID
     * @return true-存在，false-不存在（ID无效时也返回false）
     */
    public boolean existsAgentById(Long id) {
        // 1. 校验ID合法性（无效ID直接返回不存在）
        if (id == null || id <= 0) {
            return false;
        }

        // 2. 执行查询（仅判断是否存在，无需返回完整信息）
        Agent agent = agentMapper.selectById(id);
        return agent != null; // 存在则返回true，否则返回false
    }

    /**
     * 根据工号统计现有经纪人数量（用于恢复校验）
     *
     * @param employeeId 经纪人工号
     * @return 数量（0表示未被占用）
     */
    public Long countByEmployeeId(String employeeId) {
        if (StringUtils.isBlank(employeeId)) {
            throw new RuntimeException("经纪人工号不能为空");
        }
        // 恢复时无需排除ID，excludeId传null
        return agentMapper.countByEmployeeId(employeeId, null);
    }

    /**
     * 校验工号唯一性（通过计数方式）
     */
    private void validateEmployeeIdUnique(String employeeId, Long excludeId) {
        Long count = agentMapper.countByEmployeeId(employeeId, excludeId);
        if (count > 0) {
            throw new RuntimeException("工号【" + employeeId + "】已存在，无法操作");
        }
    }

    /**
     * 根据ID更新经纪人信息
     *
     * @param agent 包含ID和待更新字段的经纪人信息
     */
    @Transactional
    public void updateAgentById(Agent agent, Long operatorId) {
        // 1. 校验经纪人ID合法性
        Long agentId = agent.getId();
        if (agentId == null || agentId <= 0) {
            throw new RuntimeException("经纪人ID无效（必须为正整数）");
        }

        // 2. 校验经纪人是否存在
        Agent existing = agentMapper.selectById(agentId);
        if (existing == null) {
            throw new RuntimeException("经纪人不存在（经纪人ID：" + agentId + "），无法更新");
        }

        // 3. 若更新了工号，校验新工号是否存在
        String newEmployeeId = agent.getEmployeeId();
        if (newEmployeeId != null) { // 仅当传递了新工号时才校验
            validateEmployeeIdUnique(newEmployeeId, agent.getId());
        }

        // 设置更新人=当前操作人
        agent.setUpdatedBy(operatorId);
        // 4. 执行更新（设置更新时间）
        agent.setUpdatedAt(LocalDateTime.now());
        int rows = agentMapper.updateById(agent);
        if (rows != 1) {
            throw new RuntimeException("更新经纪人失败，请重试");
        }
    }

    /**
     * 多条件分页查询经纪人
     *
     * @param queryDTO 包含查询条件（姓名、工号、状态等）和分页参数
     * @return 分页的经纪人列表
     */
    public PageResult<Agent> getAgentsByCondition(AgentQueryDTO queryDTO) {
        // 1. 校验分页参数
        if (queryDTO.getPageNum() < 1) {
            queryDTO.setPageNum(1);
        }
        if (queryDTO.getPageSize() < 1 || queryDTO.getPageSize() > 100) {
            queryDTO.setPageSize(10); // 限制最大每页100条
        }

        // 2. 开启分页并执行多条件查询
        PageHelper.startPage(queryDTO.getPageNum(), queryDTO.getPageSize());
        Page<Agent> agentPage = agentMapper.selectByCondition(queryDTO);

        // 3. 封装分页结果
        return getAgentPageResult(agentPage);
    }

    /**
     * 根据门店ID分页查询经纪人列表
     *
     * @param storeId  所属门店ID
     * @param pageNum  页码（默认1）
     * @param pageSize 每页条数（默认10）
     * @return 分页的经纪人列表
     */
    public PageResult<Agent> getAgentsByStoreId(Long storeId, Integer pageNum, Integer pageSize) {
        // 1. 校验门店ID合法性
        if (storeId == null || storeId <= 0) {
            throw new RuntimeException("门店ID无效（必须为正整数）");
        }

        // 2. 校验分页参数
        if (pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize < 1 || pageSize > 100) {
            pageSize = 10; // 限制最大每页100条
        }

        // 3. 开启分页并执行查询
        PageHelper.startPage(pageNum, pageSize);
        Page<Agent> agentPage = agentMapper.selectByStoreId(storeId);

        // 4. 封装分页结果
        return getAgentPageResult(agentPage);
    }

    /**
     * 封装经纪人分页结果（复用逻辑）
     */
    private PageResult<Agent> getAgentPageResult(Page<Agent> agentPage) {
        PageResult<Agent> pageResult = new PageResult<>();
        pageResult.setList(agentPage.getResult());
        pageResult.setTotal(agentPage.getTotal());
        pageResult.setPages(agentPage.getPages());
        pageResult.setPageNum(agentPage.getPageNum());
        pageResult.setPageSize(agentPage.getPageSize());

        return pageResult;
    }
}

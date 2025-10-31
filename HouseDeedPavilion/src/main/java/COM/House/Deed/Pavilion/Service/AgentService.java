package COM.House.Deed.Pavilion.Service;

import COM.House.Deed.Pavilion.DTO.AgentQueryDTO;
import COM.House.Deed.Pavilion.Entity.Agent;
import COM.House.Deed.Pavilion.Mapper.AgentMapper;
import COM.House.Deed.Pavilion.Utils.PageResult;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AgentService {

    @Resource
    private AgentMapper agentMapper;

    /**
     * 新增经纪人
     *
     * @param agent 经纪人信息（包含姓名、工号等必填字段）
     * @return 新增的经纪人ID
     */
    @Transactional // 开启事务，确保数据一致性
    public Long addAgent(Agent agent) {
        // 1. 校验工号唯一性（核心业务校验）
        String employeeId = agent.getEmployeeId();
        Agent existAgent = agentMapper.selectByEmployeeId(employeeId, null);
        if (existAgent != null) {
            throw new RuntimeException("工号【" + employeeId + "】已存在，无法新增经纪人");
        }

        // 2. 设置时间字段（若数据库未配置默认值，手动赋值）
        LocalDateTime now = LocalDateTime.now();
        agent.setCreatedAt(now);
        agent.setUpdatedAt(now);

        // 3. 执行插入
        int rows = agentMapper.insert(agent);
        if (rows != 1) {
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
     * 根据ID更新经纪人信息
     *
     * @param agent 包含ID和待更新字段的经纪人信息
     */
    @Transactional
    public void updateAgentById(Agent agent) {
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
            Agent conflictAgent = agentMapper.selectByEmployeeId(newEmployeeId, agentId);
            if (conflictAgent != null) {
                throw new RuntimeException("工号【" + newEmployeeId + "】已被占用，无法更新");
            }
        }

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

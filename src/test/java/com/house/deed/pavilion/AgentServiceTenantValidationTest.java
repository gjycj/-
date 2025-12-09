package com.house.deed.pavilion;

import com.house.deed.pavilion.entity.Agent;
import com.house.deed.pavilion.service.AgentService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AgentService 租户权限校验测试（与实体类字段严格匹配）
 * 重点验证：经纪人ID的租户归属校验逻辑
 */
@SpringBootTest
@Transactional // 测试数据自动回滚
public class AgentServiceTenantValidationTest {

    @Resource
    private AgentService agentService;

    // 测试用租户ID
    private final Long currentTenantId = 1001L;
    private final Long otherTenantId = 1002L;

    // 测试数据ID（当前租户）
    private Long currentAgentId;
    // 其他租户的经纪人ID
    private Long otherAgentId;

    /**
     * 测试前准备：插入符合实体类规则的测试数据
     */
    @BeforeEach
    void setUp() {
        // 1. 创建当前租户的经纪人（符合Agent实体字段约束）
        Agent currentAgent = new Agent();
        currentAgent.setTenantId(currentTenantId); // 核心隔离字段
        currentAgent.setStoreId(201L); // 所属门店ID（非空）
        currentAgent.setAgentCode("BJ001"); // 符合格式：BJ+3位数字
        currentAgent.setName("张三");
        currentAgent.setPhone("13800138000"); // 11位手机号
        currentAgent.setIdCard("330106199001011234"); // 18位身份证
        currentAgent.setPosition("经纪人");
        currentAgent.setLevel("SENIOR"); // 合法等级
        currentAgent.setEntryTime(LocalDate.of(2020, 1, 1));
        currentAgent.setStatus((byte) 1); // 1=在职
        currentAgent.setCreateAgentId(3001L); // 创建人ID（同租户）
        agentService.save(currentAgent);
        currentAgentId = currentAgent.getId();
        assertNotNull(currentAgentId, "当前租户经纪人数据插入失败");

        // 2. 创建其他租户的经纪人（用于越权测试）
        Agent otherAgent = new Agent();
        otherAgent.setTenantId(otherTenantId); // 不同租户
        otherAgent.setStoreId(202L);
        otherAgent.setAgentCode("BJ002");
        otherAgent.setName("李四");
        otherAgent.setPhone("13900139000");
        otherAgent.setIdCard("330106199202024321");
        otherAgent.setPosition("经纪人");
        otherAgent.setLevel("JUNIOR");
        otherAgent.setEntryTime(LocalDate.of(2021, 3, 15));
        otherAgent.setStatus((byte) 1);
        otherAgent.setCreateAgentId(3002L); // 其他租户的创建人
        agentService.save(otherAgent);
        otherAgentId = otherAgent.getId();
        assertNotNull(otherAgentId, "其他租户经纪人数据插入失败");
    }

    /**
     * 测试后清理：确保数据不残留（事务回滚可省略，双重保障）
     */
    @AfterEach
    void tearDown() {
        if (currentAgentId != null) {
            agentService.removeById(currentAgentId);
        }
        if (otherAgentId != null) {
            agentService.removeById(otherAgentId);
        }
    }

    /**
     * 测试场景1：ID列表为空 → 校验通过（不抛出异常）
     */
    @Test
    void validateAgentIdsBelongToTenant_EmptyIds() {
        // 执行校验（空列表）
        assertDoesNotThrow(() ->
                agentService.validateAgentIdsBelongToTenant(currentTenantId, Collections.emptyList())
        );
    }

    /**
     * 测试场景2：所有ID均属于当前租户 → 校验通过
     */
    @Test
    void validateAgentIdsBelongToTenant_AllCurrentTenant() {
        // 构造当前租户的ID列表
        List<Long> ids = Collections.singletonList(currentAgentId);

        // 执行校验（无异常）
        assertDoesNotThrow(() ->
                agentService.validateAgentIdsBelongToTenant(currentTenantId, ids)
        );
    }

    /**
     * 测试场景3：包含其他租户的ID → 校验失败（抛出异常）
     */
    @Test
    void validateAgentIdsBelongToTenant_ContainOtherTenant() {
        // 构造混合ID列表（含当前租户和其他租户）
        List<Long> ids = Arrays.asList(currentAgentId, otherAgentId);

        // 执行校验（预期抛出异常）
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                agentService.validateAgentIdsBelongToTenant(currentTenantId, ids)
        );
        assertEquals("存在无权限操作的经纪人", exception.getMessage());
    }

    /**
     * 测试场景4：ID不存在（数据库中无记录）→ 视为无权限（抛出异常）
     */
    @Test
    void validateAgentIdsBelongToTenant_NonExistentId() {
        // 测试场景：传入不存在的经纪人ID
        Long tenantId = 1001L;
        List<Long> agentIds = List.of(999999L); // 该ID在数据库中不存在

        // 预期抛出 IllegalArgumentException
        assertThrows(IllegalArgumentException.class,
                () -> agentService.validateAgentIdsBelongToTenant(tenantId, agentIds));
    }

    /**
     * 辅助测试：验证实体类租户隔离字段的正确性
     */
    @Test
    void testAgentTenantIsolationField() {
        // 查询当前租户经纪人
        Agent currentAgent = agentService.getById(currentAgentId);
        assertNotNull(currentAgent);
        assertEquals(currentTenantId, currentAgent.getTenantId(), "当前租户ID不匹配");
        assertEquals("BJ001", currentAgent.getAgentCode(), "经纪人工号不符合实体规则");

        // 查询其他租户经纪人
        Agent otherAgent = agentService.getById(otherAgentId);
        assertNotNull(otherAgent);
        assertEquals(otherTenantId, otherAgent.getTenantId(), "其他租户ID不匹配");
    }
}
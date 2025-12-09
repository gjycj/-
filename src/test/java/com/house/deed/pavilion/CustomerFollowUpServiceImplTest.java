package com.house.deed.pavilion;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.house.deed.pavilion.entity.CustomerFollowUp;
import com.house.deed.pavilion.service.impl.CustomerFollowUpServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CustomerFollowUpServiceImpl 业务逻辑测试
 * 只测试业务逻辑，不涉及数据库操作
 */
@ExtendWith(MockitoExtension.class)
class CustomerFollowUpServiceImplTest {

    private CustomerFollowUpServiceImpl customerFollowUpService;

    @BeforeEach
    void setUp() {
        customerFollowUpService = new CustomerFollowUpServiceImpl();
    }

    @Test
    void buildQueryWrapper_WithAllConditions() {
        // 准备
        CustomerFollowUp query = new CustomerFollowUp();
        query.setTenantId(1L);
        query.setCustomerId(100L);
        query.setAgentId(200L);
        query.setHouseId(300L);
        query.setFollowTime(LocalDateTime.now());
        query.setContent("测试内容");
        query.setContractId(400L);

        // 执行
        LambdaQueryWrapper<CustomerFollowUp> wrapper = customerFollowUpService.buildQueryWrapper(query);

        // 验证
        assertNotNull(wrapper);
    }

    @Test
    void buildQueryWrapper_WithMinimalConditions() {
        // 准备
        CustomerFollowUp query = new CustomerFollowUp();
        query.setTenantId(1L);

        // 执行
        LambdaQueryWrapper<CustomerFollowUp> wrapper = customerFollowUpService.buildQueryWrapper(query);

        // 验证
        assertNotNull(wrapper);
    }

    @Test
    void parameterValidation_NullChecks() {
        CustomerFollowUp followUp = new CustomerFollowUp();

        // 测试租户ID为空
        followUp.setTenantId(null);
        followUp.setCustomerId(100L);
        followUp.setAgentId(200L);
        followUp.setFollowTime(LocalDateTime.now());
        followUp.setContent("内容");
        followUp.setNextFollowPlan("计划");

        assertThrows(IllegalArgumentException.class,
                () -> customerFollowUpService.saveFollowUp(followUp));
    }

    @Test
    void batchValidation_DifferentTenants() {
        // 准备不同租户的记录
        CustomerFollowUp followUp1 = createTestFollowUp();
        followUp1.setTenantId(1L);

        CustomerFollowUp followUp2 = createTestFollowUp();
        followUp2.setTenantId(2L); // 不同的租户

        List<CustomerFollowUp> followUpList = Arrays.asList(followUp1, followUp2);

        // 执行 & 验证
        assertThrows(IllegalArgumentException.class,
                () -> customerFollowUpService.batchSaveFollowUps(followUpList));
    }

    @Test
    void batchValidation_MissingRequiredFields() {
        // 准备缺少必需字段的记录
        CustomerFollowUp invalidFollowUp = createTestFollowUp();
        invalidFollowUp.setCustomerId(null); // 缺少客户ID

        List<CustomerFollowUp> followUpList = Collections.singletonList(invalidFollowUp);

        // 执行 & 验证
        assertThrows(IllegalArgumentException.class,
                () -> customerFollowUpService.batchSaveFollowUps(followUpList));
    }

    @Test
    void batchValidation_EmptyList() {
        // 执行
        boolean result = customerFollowUpService.batchSaveFollowUps(Collections.emptyList());

        // 验证
        assertTrue(result);
    }

    // 辅助方法
    private CustomerFollowUp createTestFollowUp() {
        CustomerFollowUp followUp = new CustomerFollowUp();
        followUp.setTenantId(1L);
        followUp.setCustomerId(100L);
        followUp.setAgentId(200L);
        followUp.setHouseId(300L);
        followUp.setFollowTime(LocalDateTime.now());
        followUp.setContent("测试内容");
        followUp.setDemandChange("需求变更");
        followUp.setNextFollowPlan("下次计划");
        followUp.setContractId(400L);
        return followUp;
    }
}
package com.house.deed.pavilion;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.house.deed.pavilion.entity.House;
import com.house.deed.pavilion.service.HouseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HouseServiceImpl 集成测试
 */
@SpringBootTest
@Transactional
class HouseServiceImplTest {

    @Autowired
    private HouseService houseService;

    // 租户ID常量
    private static final Long DEFAULT_TENANT_ID = 1L;
    private static final Long OTHER_TENANT_ID = 2L;

    // 经纪人ID常量
    private static final Long DEFAULT_AGENT_ID = 1001L;

    // 楼栋ID常量

    // 房源编号计数器（确保唯一性）
    private final AtomicLong houseNoCounter = new AtomicLong(1000L);
    private final AtomicLong buildingIdCounter = new AtomicLong(500L);
    private final AtomicLong agentIdCounter = new AtomicLong(1000L);

    @BeforeEach
    void setUp() {
        // 重置计数器，确保每个测试开始时序列一致
        houseNoCounter.set(1000L);
        buildingIdCounter.set(500L);
        agentIdCounter.set(1000L);

        // 清理测试租户的数据
        houseService.listByConditions(new HashMap<>(), DEFAULT_TENANT_ID)
                .forEach(house -> houseService.removeHouseById(house.getId(), DEFAULT_TENANT_ID));
    }


    // 生成唯一房源编号
    private String generateUniqueHouseNo() {
        return "A" + houseNoCounter.getAndIncrement() + "单元";
    }

    // 生成唯一楼栋ID
    private Long generateUniqueBuildingId() {
        return buildingIdCounter.getAndIncrement();
    }

    /**
     * 创建测试房源对象 - 在售状态
     */
    private House createTestHouse() {
        House house = new House();

        // 必填字段
        house.setTenantId(DEFAULT_TENANT_ID);
        house.setBuildingId(generateUniqueBuildingId());
        house.setHouseNo(generateUniqueHouseNo());
        house.setHouseType("3室2厅");
        house.setArea(new BigDecimal("120.50"));
        house.setInsideArea(new BigDecimal("105.30"));
        house.setFloor(10);
        house.setTotalFloor(18);
        house.setOrientation("南北通透");
        house.setDecoration("DELUXE");
        house.setPropertyRight("商品房");
        house.setPropertyRightCertNo("浙房地权证杭字第" + UUID.randomUUID().toString().substring(0, 8) + "号");
        house.setPropertyRightYears(70);
        house.setMortgageStatus("NONE");
        house.setPrice(new BigDecimal("180.00"));
        house.setTransactionType("SALE");
        house.setStatus("ON_SALE");
        house.setCreateAgentId(DEFAULT_AGENT_ID);

        // 可选字段
        house.setDescription("中间楼层，南北通透，配套成熟，交通便利");

        return house;
    }

    /**
     * 创建测试房源对象 - 已出租状态
     */
    private House createTestRentedHouse() {
        House house = createTestHouse();
        house.setTransactionType("RENT");
        house.setStatus("SOLD");
        house.setPrice(new BigDecimal("0.50")); // 月租金0.5万元
        house.setMortgageStatus("MORTGAGED");
        house.setMortgageDetails("中国工商银行杭州分行，抵押金额50万元");
        return house;
    }

    /**
     * 创建测试房源对象 - 可售可租状态
     */
    private House createTestBothTypeHouse() {
        House house = createTestHouse();
        house.setTransactionType("BOTH");
        house.setHouseType("2室1厅");
        house.setArea(new BigDecimal("85.00"));
        house.setPrice(new BigDecimal("2.50")); // 月租金和出售价不同逻辑处理
        return house;
    }

    /**
     * 保存并返回房源对象
     */
    private House saveAndGetHouse() {
        House house = createTestHouse();
        houseService.saveHouse(house, DEFAULT_TENANT_ID);
        return house;
    }

    @Test
    void saveHouse_ValidData_Success() {
        // 准备
        House house = createTestHouse();

        // 执行
        boolean result = houseService.saveHouse(house, DEFAULT_TENANT_ID);

        // 验证
        assertTrue(result);
        assertNotNull(house.getId());
        assertEquals(DEFAULT_TENANT_ID, house.getTenantId());
        assertEquals("ON_SALE", house.getStatus());
        assertEquals("SALE", house.getTransactionType());
        assertEquals("DELUXE", house.getDecoration());
        assertEquals("NONE", house.getMortgageStatus());
        assertNotNull(house.getCreateTime());
        assertNotNull(house.getUpdateTime());
    }

    @Test
    void saveHouse_RentedHouse_Success() {
        // 准备
        House house = createTestRentedHouse();

        // 执行
        boolean result = houseService.saveHouse(house, DEFAULT_TENANT_ID);

        // 验证
        assertTrue(result);
        assertNotNull(house.getId());
        assertEquals("RENT", house.getTransactionType());
        assertEquals("SOLD", house.getStatus()); // 已出租
        assertEquals("MORTGAGED", house.getMortgageStatus());
        assertNotNull(house.getMortgageDetails());
    }

    @Test
    void saveHouse_WithoutTenantId_ThrowsException() {
        // 准备
        House house = createTestHouse();

        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> houseService.saveHouse(house, null));
        assertEquals("租户ID不能为空", exception.getMessage());
    }

    @Test
    void saveHouse_WithoutHouseNo_ThrowsException() {
        // 准备
        House house = createTestHouse();
        house.setHouseNo(null);

        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> houseService.saveHouse(house, DEFAULT_TENANT_ID));
        assertEquals("房源编号不能为空", exception.getMessage());
    }

    @Test
    void saveHouse_WithoutArea_ThrowsException() {
        // 准备
        House house = createTestHouse();
        house.setArea(null);

        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> houseService.saveHouse(house, DEFAULT_TENANT_ID));
        assertEquals("房源面积不能为空", exception.getMessage());
    }

    @Test
    void saveHouse_NegativeArea_ThrowsException() {
        // 准备
        House house = createTestHouse();
        house.setArea(new BigDecimal("-10.00"));

        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> houseService.saveHouse(house, DEFAULT_TENANT_ID));
        assertTrue(exception.getMessage().contains("建筑面积必须大于0")); // 修改这里
    }

    @Test
    void saveHouse_ZeroArea_ThrowsException() {
        // 准备
        House house = createTestHouse();
        house.setArea(BigDecimal.ZERO);

        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> houseService.saveHouse(house, DEFAULT_TENANT_ID));
        assertTrue(exception.getMessage().contains("建筑面积必须大于0")); // 同样修改
    }


    @Test
    void saveHouse_ZeroPrice_ThrowsException() {
        // 准备
        House house = createTestHouse();
        house.setPrice(BigDecimal.ZERO);

        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> houseService.saveHouse(house, DEFAULT_TENANT_ID));
        assertTrue(exception.getMessage().contains("挂牌价必须大于0")); // 同样修改
    }

    @Test
    void saveHouse_WithoutPrice_ThrowsException() {
        // 准备
        House house = createTestHouse();
        house.setPrice(null);

        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> houseService.saveHouse(house, DEFAULT_TENANT_ID));
        assertEquals("房源价格不能为空", exception.getMessage());
    }

    @Test
    void saveHouse_NegativePrice_ThrowsException() {
        // 准备
        House house = createTestHouse();
        house.setPrice(new BigDecimal("-100.00"));

        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> houseService.saveHouse(house, DEFAULT_TENANT_ID));
        assertTrue(exception.getMessage().contains("挂牌价必须大于0"));
    }

    @Test
    void saveHouse_WithoutHouseType_ThrowsException() {
        // 准备
        House house = createTestHouse();
        house.setHouseType(null);

        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> houseService.saveHouse(house, DEFAULT_TENANT_ID));
        assertEquals("户型不能为空", exception.getMessage());
    }

    @Test
    void saveHouse_WithoutStatus_ThrowsException() {
        // 准备
        House house = createTestHouse();
        house.setStatus(null);

        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> houseService.saveHouse(house, DEFAULT_TENANT_ID));
        assertEquals("房源状态不能为空", exception.getMessage());
    }

    @Test
    void saveHouse_DuplicateHouseNo_ThrowsException() {
        // 准备 - 保存第一个房源
        House house1 = createTestHouse();
        String duplicateHouseNo = house1.getHouseNo();
        houseService.saveHouse(house1, DEFAULT_TENANT_ID);

        // 准备第二个房源，使用相同的房源编号
        House house2 = createTestHouse();
        house2.setHouseNo(duplicateHouseNo);

        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> houseService.saveHouse(house2, DEFAULT_TENANT_ID));
        assertTrue(exception.getMessage().contains("当前租户下房源编号已存在"));
    }

    @Test
    void saveHouse_DifferentTenantSameHouseNo_Success() {
        // 准备 - 在租户1下保存房源
        House house1 = createTestHouse();
        String sameHouseNo = house1.getHouseNo();
        houseService.saveHouse(house1, DEFAULT_TENANT_ID);

        // 准备 - 在租户2下使用相同房源编号
        House house2 = createTestHouse();
        house2.setHouseNo(sameHouseNo);

        // 执行 & 验证 - 应该成功，因为不同租户可以相同房源编号
        boolean result = houseService.saveHouse(house2, OTHER_TENANT_ID);
        assertTrue(result);
        assertNotNull(house2.getId());
        assertEquals(OTHER_TENANT_ID, house2.getTenantId());
    }

    @Test
    void saveHouse_FloorGreaterThanTotalFloor_ThrowsException() {
        // 准备 - 楼层大于总楼层（业务上不合理，服务层应该校验）
        House house = createTestHouse();
        house.setFloor(20);
        house.setTotalFloor(18); // 楼层 > 总楼层

        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> houseService.saveHouse(house, DEFAULT_TENANT_ID));
        assertTrue(exception.getMessage().contains("所在楼层不能大于总楼层"));
    }

    @Test
    void saveHouse_PropertyRightYearsExceeds70_ThrowsException() {
        // 准备 - 产权年限超过70年
        House house = createTestHouse();
        house.setPropertyRightYears(80);

        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> houseService.saveHouse(house, DEFAULT_TENANT_ID));
        assertTrue(exception.getMessage().contains("产权年限不能超过70年"));
    }

    @Test
    void updateHouseById_ValidUpdate_Success() {
        // 准备
        House savedHouse = saveAndGetHouse();
        String originalHouseNo = savedHouse.getHouseNo();

        // 更新字段
        savedHouse.setHouseType("4室2厅");
        savedHouse.setArea(new BigDecimal("150.00"));
        savedHouse.setPrice(new BigDecimal("250.00"));
        savedHouse.setStatus("RESERVED");

        // 执行
        boolean result = houseService.updateHouseById(savedHouse, DEFAULT_TENANT_ID);

        // 验证
        assertTrue(result);

        // 重新查询验证更新
        House updated = houseService.getHouseById(savedHouse.getId(), DEFAULT_TENANT_ID);
        assertEquals("4室2厅", updated.getHouseType());
        assertEquals(new BigDecimal("150.00"), updated.getArea());
        assertEquals(new BigDecimal("250.00"), updated.getPrice());
        assertEquals("RESERVED", updated.getStatus());
        assertEquals(originalHouseNo, updated.getHouseNo()); // 房源编号未改变
    }

    @Test
    void updateHouseById_UpdateHouseNo_Success() {
        // 准备
        House savedHouse = saveAndGetHouse();
        String newHouseNo = "B" + UUID.randomUUID().toString().substring(0, 8);
        savedHouse.setHouseNo(newHouseNo);

        // 执行
        boolean result = houseService.updateHouseById(savedHouse, DEFAULT_TENANT_ID);

        // 验证
        assertTrue(result);

        House updated = houseService.getHouseById(savedHouse.getId(), DEFAULT_TENANT_ID);
        assertEquals(newHouseNo, updated.getHouseNo());
    }

    @Test
    void updateHouseById_UpdateHouseNoDuplicate_ThrowsException() {
        // 准备 - 保存两个房源
        House house1 = saveAndGetHouse();
        House house2 = saveAndGetHouse();

        // 尝试将第二个房源的编号改为第一个的编号
        house2.setHouseNo(house1.getHouseNo());

        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> houseService.updateHouseById(house2, DEFAULT_TENANT_ID));
        assertTrue(exception.getMessage().contains("新房源编号已存在"));
    }

    @Test
    void updateHouseById_WithoutId_ThrowsException() {
        // 准备
        House house = createTestHouse();
        house.setId(null);

        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> houseService.updateHouseById(house, DEFAULT_TENANT_ID));
        assertEquals("房源ID不能为空", exception.getMessage());
    }

    @Test
    void updateHouseById_WithoutTenantId_ThrowsException() {
        // 准备
        House savedHouse = saveAndGetHouse();

        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> houseService.updateHouseById(savedHouse, null));
        assertEquals("租户ID不能为空", exception.getMessage());
    }

    @Test
    void updateHouseById_NonExistentHouse_ThrowsException() {
        // 准备
        House house = createTestHouse();
        house.setId(999999L);

        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> houseService.updateHouseById(house, DEFAULT_TENANT_ID));
        assertEquals("房源不存在", exception.getMessage());
    }

    @Test
    void updateHouseById_TenantMismatch_ThrowsException() {
        // 准备
        House savedHouse = saveAndGetHouse();

        // 使用错误的租户ID更新
        savedHouse.setHouseNo("尝试更新");

        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> houseService.updateHouseById(savedHouse, OTHER_TENANT_ID));
        assertEquals("无权限操作其他租户的房源", exception.getMessage());
    }

    @Test
    void updateHouseById_MortgagedToNoneWithDetails_Success() {
        // 准备 - 创建一个已抵押的房源
        House house = createTestRentedHouse();
        houseService.saveHouse(house, DEFAULT_TENANT_ID);

        // 更新为无抵押状态，但保留抵押详情（业务上可能不合理）
        house.setMortgageStatus("NONE");
        // mortgageDetails 保持原值

        // 执行
        boolean result = houseService.updateHouseById(house, DEFAULT_TENANT_ID);

        // 验证
        assertTrue(result);
        House updated = houseService.getHouseById(house.getId(), DEFAULT_TENANT_ID);
        assertEquals("NONE", updated.getMortgageStatus());
        // 注意：抵押状态为NONE时，抵押详情应该清空，但当前逻辑允许保留
    }

    @Test
    void removeHouseById_Success() {
        // 准备
        House savedHouse = saveAndGetHouse();

        // 执行
        boolean result = houseService.removeHouseById(savedHouse.getId(), DEFAULT_TENANT_ID);

        // 验证
        assertTrue(result);

        // 验证记录已删除
        House deleted = houseService.getHouseById(savedHouse.getId(), DEFAULT_TENANT_ID);
        assertNull(deleted);
    }

    @Test
    void removeHouseById_WithoutId_ThrowsException() {
        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> houseService.removeHouseById(null, DEFAULT_TENANT_ID));
        assertEquals("房源ID不能为空", exception.getMessage());
    }

    @Test
    void removeHouseById_WithoutTenantId_ThrowsException() {
        // 准备
        House savedHouse = saveAndGetHouse();

        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> houseService.removeHouseById(savedHouse.getId(), null));
        assertEquals("租户ID不能为空", exception.getMessage());
    }

    @Test
    void removeHouseById_TenantMismatch_ThrowsException() {
        // 准备
        House savedHouse = saveAndGetHouse();

        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> houseService.removeHouseById(savedHouse.getId(), OTHER_TENANT_ID));
        assertEquals("无权限操作其他租户的房源", exception.getMessage());
    }

    @Test
    void getHouseById_Success() {
        // 准备
        House savedHouse = saveAndGetHouse();

        // 执行
        House result = houseService.getHouseById(savedHouse.getId(), DEFAULT_TENANT_ID);

        // 验证
        assertNotNull(result);
        assertEquals(savedHouse.getId(), result.getId());
        assertEquals(savedHouse.getTenantId(), result.getTenantId());
        assertEquals(savedHouse.getHouseNo(), result.getHouseNo());
        assertEquals(savedHouse.getHouseType(), result.getHouseType());
        assertEquals(savedHouse.getArea(), result.getArea());
        assertEquals(savedHouse.getPrice(), result.getPrice());
        assertEquals(savedHouse.getStatus(), result.getStatus());
    }

    @Test
    void getHouseById_TenantMismatch_ReturnsNull() {
        // 准备
        House savedHouse = saveAndGetHouse();

        // 执行
        House result = houseService.getHouseById(savedHouse.getId(), OTHER_TENANT_ID);

        // 验证 - 租户不匹配应返回null
        assertNull(result);
    }

    @Test
    void pageQuery_Success() {
        // 准备 - 创建多个测试记录
        for (int i = 0; i < 5; i++) {
            saveAndGetHouse();
        }

        // 执行
        Page<House> page = new Page<>(1, 3);
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("tenantId", DEFAULT_TENANT_ID); // 注意：service内部会强制添加租户ID

        IPage<House> result = houseService.pageQuery(page, queryParams, DEFAULT_TENANT_ID);

        // 验证
        assertNotNull(result);
        assertFalse(result.getRecords().isEmpty());
        assertEquals(3, result.getRecords().size());

        // 验证所有返回记录都属于指定租户
        assertTrue(result.getRecords().stream()
                .allMatch(h -> DEFAULT_TENANT_ID.equals(h.getTenantId())));

        // 验证按创建时间倒序排列
        List<House> houses = result.getRecords();
        for (int i = 0; i < houses.size() - 1; i++) {
            assertTrue(houses.get(i).getCreateTime().isAfter(houses.get(i + 1).getCreateTime()) ||
                    houses.get(i).getCreateTime().isEqual(houses.get(i + 1).getCreateTime()));
        }
    }

    @Test
    void pageQuery_WithHouseNoFilter() {
        // 准备
        House house1 = createTestHouse();
        house1.setHouseNo("A1001单元");
        houseService.saveHouse(house1, DEFAULT_TENANT_ID);

        House house2 = createTestHouse();
        house2.setHouseNo("B2002单元");
        houseService.saveHouse(house2, DEFAULT_TENANT_ID);

        // 执行 - 查询房源编号为A1001单元的记录
        Page<House> page = new Page<>(1, 10);
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("houseNo", "A1001单元");

        IPage<House> result = houseService.pageQuery(page, queryParams, DEFAULT_TENANT_ID);

        // 验证
        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
        assertEquals("A1001单元", result.getRecords().get(0).getHouseNo());
    }

    @Test
    void pageQuery_WithStatusFilter() {
        // 准备
        House house1 = createTestHouse();
        house1.setStatus("ON_SALE");
        houseService.saveHouse(house1, DEFAULT_TENANT_ID);

        House house2 = createTestHouse();
        house2.setStatus("SOLD");
        houseService.saveHouse(house2, DEFAULT_TENANT_ID);

        // 执行 - 查询状态为在售的记录
        Page<House> page = new Page<>(1, 10);
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("status", "ON_SALE");

        IPage<House> result = houseService.pageQuery(page, queryParams, DEFAULT_TENANT_ID);

        // 验证
        assertNotNull(result);
        assertTrue(result.getRecords().stream()
                .allMatch(h -> "ON_SALE".equals(h.getStatus())));
    }

    @Test
    void pageQuery_WithHouseTypeFilter() {
        // 准备
        House house1 = createTestHouse();
        house1.setHouseType("3室2厅");
        houseService.saveHouse(house1, DEFAULT_TENANT_ID);

        House house2 = createTestHouse();
        house2.setHouseType("2室1厅");
        houseService.saveHouse(house2, DEFAULT_TENANT_ID);

        // 执行 - 查询户型为3室2厅的记录
        Page<House> page = new Page<>(1, 10);
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("houseType", "3室2厅");

        IPage<House> result = houseService.pageQuery(page, queryParams, DEFAULT_TENANT_ID);

        // 验证
        assertNotNull(result);
        assertTrue(result.getRecords().stream()
                .allMatch(h -> "3室2厅".equals(h.getHouseType())));
    }

    @Test
    void pageQuery_WithAreaRangeFilter() {
        // 准备
        House house1 = createTestHouse();
        house1.setArea(new BigDecimal("80.00"));
        houseService.saveHouse(house1, DEFAULT_TENANT_ID);

        House house2 = createTestHouse();
        house2.setArea(new BigDecimal("120.00"));
        houseService.saveHouse(house2, DEFAULT_TENANT_ID);

        House house3 = createTestHouse();
        house3.setArea(new BigDecimal("150.00"));
        houseService.saveHouse(house3, DEFAULT_TENANT_ID);

        // 执行 - 查询面积在100-130之间的记录
        Page<House> page = new Page<>(1, 10);
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("minArea", new BigDecimal("100.00"));
        queryParams.put("maxArea", new BigDecimal("130.00"));

        IPage<House> result = houseService.pageQuery(page, queryParams, DEFAULT_TENANT_ID);

        // 验证
        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
        assertEquals(new BigDecimal("120.00"), result.getRecords().get(0).getArea());
    }

    @Test
    void pageQuery_WithPriceRangeFilter() {
        // 准备
        House house1 = createTestHouse();
        house1.setPrice(new BigDecimal("100.00"));
        houseService.saveHouse(house1, DEFAULT_TENANT_ID);

        House house2 = createTestHouse();
        house2.setPrice(new BigDecimal("200.00"));
        houseService.saveHouse(house2, DEFAULT_TENANT_ID);

        House house3 = createTestHouse();
        house3.setPrice(new BigDecimal("300.00"));
        houseService.saveHouse(house3, DEFAULT_TENANT_ID);

        // 执行 - 查询价格在150-250之间的记录
        Page<House> page = new Page<>(1, 10);
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("minPrice", new BigDecimal("150.00"));
        queryParams.put("maxPrice", new BigDecimal("250.00"));

        IPage<House> result = houseService.pageQuery(page, queryParams, DEFAULT_TENANT_ID);

        // 验证
        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
        assertEquals(new BigDecimal("200.00"), result.getRecords().get(0).getPrice());
    }

    @Test
    void pageQuery_WithOrientationFilter() {
        // 准备
        House house1 = createTestHouse();
        house1.setOrientation("南");
        houseService.saveHouse(house1, DEFAULT_TENANT_ID);

        House house2 = createTestHouse();
        house2.setOrientation("南北");
        houseService.saveHouse(house2, DEFAULT_TENANT_ID);

        House house3 = createTestHouse();
        house3.setOrientation("东西");
        houseService.saveHouse(house3, DEFAULT_TENANT_ID);

        // 执行 - 查询朝向包含"南"的记录
        Page<House> page = new Page<>(1, 10);
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("orientation", "南");

        IPage<House> result = houseService.pageQuery(page, queryParams, DEFAULT_TENANT_ID);

        // 验证
        assertNotNull(result);
        assertEquals(2, result.getRecords().size()); // "南"和"南北"都包含"南"
        assertTrue(result.getRecords().stream()
                .allMatch(h -> h.getOrientation().contains("南")));
    }

    @Test
    void pageQuery_WithDecorationFilter() {
        // 准备
        House house1 = createTestHouse();
        house1.setDecoration("DELUXE");
        houseService.saveHouse(house1, DEFAULT_TENANT_ID);

        House house2 = createTestHouse();
        house2.setDecoration("SIMPLE");
        houseService.saveHouse(house2, DEFAULT_TENANT_ID);

        // 执行 - 查询装修为精装的记录
        Page<House> page = new Page<>(1, 10);
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("decoration", "DELUXE");

        IPage<House> result = houseService.pageQuery(page, queryParams, DEFAULT_TENANT_ID);

        // 验证
        assertNotNull(result);
        assertTrue(result.getRecords().stream()
                .allMatch(h -> "DELUXE".equals(h.getDecoration())));
    }

    @Test
    void pageQuery_WithoutTenantId_ThrowsException() {
        // 准备
        Page<House> page = new Page<>(1, 10);
        Map<String, Object> queryParams = new HashMap<>();

        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> houseService.pageQuery(page, queryParams, null));
        assertEquals("租户ID不能为空", exception.getMessage());
    }

    @Test
    void listByConditions_Success() {
        // 准备
        for (int i = 0; i < 3; i++) {
            saveAndGetHouse();
        }

        // 执行
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("status", "ON_SALE");

        List<House> result = houseService.listByConditions(queryParams, DEFAULT_TENANT_ID);

        // 验证
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.stream()
                .allMatch(h -> DEFAULT_TENANT_ID.equals(h.getTenantId())));
        assertTrue(result.stream()
                .allMatch(h -> "ON_SALE".equals(h.getStatus())));
    }

    @Test
    void listByPropertyId_Success() {
        // 准备 - 为同一个楼盘创建多条记录
        long testPropertyId = generateUniqueBuildingId();

        House house1 = createTestHouse();
        house1.setBuildingId(testPropertyId);
        houseService.saveHouse(house1, DEFAULT_TENANT_ID);

        House house2 = createTestHouse();
        house2.setBuildingId(testPropertyId);
        houseService.saveHouse(house2, DEFAULT_TENANT_ID);

        // 为另一个楼盘创建记录
        House house3 = createTestHouse();
        houseService.saveHouse(house3, DEFAULT_TENANT_ID);

        // 执行
        List<House> result = houseService.listByPropertyId(testPropertyId, DEFAULT_TENANT_ID);

        // 验证
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(h -> h.getBuildingId().equals(testPropertyId)));
    }

    @Test
    void listByPropertyId_WithoutPropertyId_ThrowsException() {
        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> houseService.listByPropertyId(null, DEFAULT_TENANT_ID));
        assertEquals("楼盘ID不能为空", exception.getMessage());
    }

    @Test
    void batchSaveHouses_Success() {
        // 准备
        House house1 = createTestHouse();
        House house2 = createTestRentedHouse();
        House house3 = createTestBothTypeHouse();

        List<House> houseList = Arrays.asList(house1, house2, house3);

        // 执行
        boolean result = houseService.batchSaveHouses(houseList, DEFAULT_TENANT_ID);

        // 验证
        assertTrue(result);
        assertNotNull(house1.getId());
        assertNotNull(house2.getId());
        assertNotNull(house3.getId());

        // 验证租户ID已正确设置
        assertEquals(DEFAULT_TENANT_ID, house1.getTenantId());
        assertEquals(DEFAULT_TENANT_ID, house2.getTenantId());
        assertEquals(DEFAULT_TENANT_ID, house3.getTenantId());
    }

    @Test
    void batchSaveHouses_EmptyList_ThrowsException() {
        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> houseService.batchSaveHouses(Collections.emptyList(), DEFAULT_TENANT_ID));
        assertEquals("房源列表不能为空", exception.getMessage());
    }

    @Test
    void batchSaveHouses_DuplicateHouseNo_ThrowsException() {
        // 准备
        String duplicateHouseNo = generateUniqueHouseNo();

        House house1 = createTestHouse();
        house1.setHouseNo(duplicateHouseNo);

        House house2 = createTestHouse();
        house2.setHouseNo(duplicateHouseNo); // 相同房源编号

        List<House> houseList = Arrays.asList(house1, house2);

        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> houseService.batchSaveHouses(houseList, DEFAULT_TENANT_ID));

        // 注意：这里是批次内重复，不是数据库重复
        assertTrue(exception.getMessage().contains("同一批次中存在重复的房源编号") ||
                exception.getMessage().contains("房源编号已存在"));
    }

    @Test
    void batchSaveHouses_WithoutTenantId_ThrowsException() {
        // 准备
        List<House> houseList = List.of(createTestHouse());

        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> houseService.batchSaveHouses(houseList, null));
        assertEquals("租户ID不能为空", exception.getMessage());
    }

    @Test
    void batchUpdateStatus_Success() {
        // 准备
        House house1 = saveAndGetHouse();
        House house2 = saveAndGetHouse();

        List<Long> ids = Arrays.asList(house1.getId(), house2.getId());

        // 执行 - 批量更新状态为已售
        boolean result = houseService.batchUpdateStatus(ids, "SOLD", DEFAULT_TENANT_ID);

        // 验证
        assertTrue(result);

        // 验证状态已更新
        House updated1 = houseService.getHouseById(house1.getId(), DEFAULT_TENANT_ID);
        House updated2 = houseService.getHouseById(house2.getId(), DEFAULT_TENANT_ID);
        assertEquals("SOLD", updated1.getStatus());
        assertEquals("SOLD", updated2.getStatus());

        // 验证其他字段未改变
        assertEquals(house1.getHouseNo(), updated1.getHouseNo());
        assertEquals(house1.getPrice(), updated1.getPrice());
    }

    @Test
    void batchUpdateStatus_WithoutTenantId_ThrowsException() {
        // 准备
        List<Long> ids = Arrays.asList(1L, 2L);

        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> houseService.batchUpdateStatus(ids, "SOLD", null));
        assertEquals("租户ID不能为空", exception.getMessage());
    }

    @Test
    void batchUpdateStatus_EmptyIds_ThrowsException() {
        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> houseService.batchUpdateStatus(Collections.emptyList(), "SOLD", DEFAULT_TENANT_ID));
        assertEquals("房源ID列表不能为空", exception.getMessage());
    }

    @Test
    void batchUpdateStatus_WithoutStatus_ThrowsException() {
        // 准备
        List<Long> ids = Arrays.asList(1L, 2L);

        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> houseService.batchUpdateStatus(ids, null, DEFAULT_TENANT_ID));
        assertEquals("目标状态不能为空", exception.getMessage());
    }

    @Test
    void batchUpdateStatus_CrossTenantRecords_ThrowsException() {
        // 准备 - 在不同租户下创建记录
        House tenant1House = createTestHouse();
        tenant1House.setTenantId(DEFAULT_TENANT_ID);
        houseService.saveHouse(tenant1House, DEFAULT_TENANT_ID);

        House tenant2House = createTestHouse();
        tenant2House.setTenantId(OTHER_TENANT_ID);
        houseService.saveHouse(tenant2House, OTHER_TENANT_ID);

        List<Long> ids = Arrays.asList(tenant1House.getId(), tenant2House.getId());

        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> houseService.batchUpdateStatus(ids, "SOLD", DEFAULT_TENANT_ID));
        assertEquals("存在跨租户的房源ID，无法批量更新", exception.getMessage());
    }

    @Test
    void batchRemoveHouses_Success() {
        // 准备
        House house1 = saveAndGetHouse();
        House house2 = saveAndGetHouse();

        // 执行
        boolean result = houseService.batchRemoveHouses(
                Arrays.asList(house1.getId(), house2.getId()), DEFAULT_TENANT_ID);

        // 验证
        assertTrue(result);

        // 验证记录已删除
        assertNull(houseService.getHouseById(house1.getId(), DEFAULT_TENANT_ID));
        assertNull(houseService.getHouseById(house2.getId(), DEFAULT_TENANT_ID));
    }

    @Test
    void batchRemoveHouses_WithoutTenantId_ThrowsException() {
        // 准备
        List<Long> ids = Arrays.asList(1L, 2L);

        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> houseService.batchRemoveHouses(ids, null));
        assertEquals("租户ID不能为空", exception.getMessage());
    }

    @Test
    void batchRemoveHouses_EmptyIds_ThrowsException() {
        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> houseService.batchRemoveHouses(Collections.emptyList(), DEFAULT_TENANT_ID));
        assertEquals("房源ID列表不能为空", exception.getMessage());
    }

    @Test
    void batchRemoveHouses_CrossTenantRecords_ThrowsException() {
        // 准备 - 在不同租户下创建记录
        House tenant1House = createTestHouse();
        tenant1House.setTenantId(DEFAULT_TENANT_ID);
        houseService.saveHouse(tenant1House, DEFAULT_TENANT_ID);

        House tenant2House = createTestHouse();
        tenant2House.setTenantId(OTHER_TENANT_ID);
        houseService.saveHouse(tenant2House, OTHER_TENANT_ID);

        List<Long> ids = Arrays.asList(tenant1House.getId(), tenant2House.getId());

        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> houseService.batchRemoveHouses(ids, DEFAULT_TENANT_ID));
        assertEquals("存在跨租户的房源ID，无法批量删除", exception.getMessage());
    }

    @Test
    void testTransactionTypeValidation() {
        // 测试所有有效的交易类型
        String[] validTransactionTypes = {"SALE", "RENT", "BOTH"};

        for (String type : validTransactionTypes) {
            House house = createTestHouse();
            house.setTransactionType(type);

            boolean result = houseService.saveHouse(house, DEFAULT_TENANT_ID);
            assertTrue(result, "交易类型为 " + type + " 的记录应该保存成功");
            assertEquals(type, house.getTransactionType());
        }
    }

    @Test
    void testStatusValidation() {
        // 测试所有有效的房源状态
        String[] validStatuses = {"ON_SALE", "RESERVED", "SOLD", "OFF_SHELF"};

        for (String status : validStatuses) {
            House house = createTestHouse();
            house.setStatus(status);

            boolean result = houseService.saveHouse(house, DEFAULT_TENANT_ID);
            assertTrue(result, "状态为 " + status + " 的记录应该保存成功");
            assertEquals(status, house.getStatus());
        }
    }

    @Test
    void testDecorationValidation() {
        // 测试所有有效的装修类型
        String[] validDecorations = {"UNFINISHED", "SIMPLE", "DELUXE"};

        for (String decoration : validDecorations) {
            House house = createTestHouse();
            house.setDecoration(decoration);

            boolean result = houseService.saveHouse(house, DEFAULT_TENANT_ID);
            assertTrue(result, "装修类型为 " + decoration + " 的记录应该保存成功");
            assertEquals(decoration, house.getDecoration());
        }
    }

    @Test
    void testMortgageStatusValidation() {
        // 测试所有有效的抵押状态
        String[] validMortgageStatuses = {"NONE", "MORTGAGED"};

        for (String status : validMortgageStatuses) {
            House house = createTestHouse();
            house.setMortgageStatus(status);

            if ("MORTGAGED".equals(status)) {
                house.setMortgageDetails("测试抵押详情");
            }

            boolean result = houseService.saveHouse(house, DEFAULT_TENANT_ID);
            assertTrue(result, "抵押状态为 " + status + " 的记录应该保存成功");
            assertEquals(status, house.getMortgageStatus());
        }
    }

    @Test
    void testOrderByCreateTimeDesc() {
        // 准备 - 创建多个记录
        for (int i = 0; i < 3; i++) {
            saveAndGetHouse();
        }

        // 执行 - 查询所有记录
        Map<String, Object> queryParams = new HashMap<>();
        List<House> result = houseService.listByConditions(queryParams, DEFAULT_TENANT_ID);

        // 验证按创建时间倒序排列
        assertNotNull(result);
        assertTrue(result.size() >= 3);

        // 检查是否按创建时间倒序排列
        for (int i = 0; i < result.size() - 1; i++) {
            assertTrue(result.get(i).getCreateTime().isAfter(result.get(i + 1).getCreateTime()) ||
                    result.get(i).getCreateTime().isEqual(result.get(i + 1).getCreateTime()));
        }
    }

    @Test
    void testHouseNoUniquenessWithinTenant() {
        // 测试同一个租户内房源编号必须唯一
        String uniqueHouseNo = generateUniqueHouseNo();

        House house1 = createTestHouse();
        house1.setHouseNo(uniqueHouseNo);
        houseService.saveHouse(house1, DEFAULT_TENANT_ID);

        // 相同租户，相同房源编号 - 应该失败
        House house2 = createTestHouse();
        house2.setHouseNo(uniqueHouseNo);

        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> houseService.saveHouse(house2, DEFAULT_TENANT_ID));
        assertTrue(exception.getMessage().contains("当前租户下房源编号已存在"));

        // 不同租户，相同房源编号 - 应该成功
        House house3 = createTestHouse();
        house3.setHouseNo(uniqueHouseNo);

        boolean result = houseService.saveHouse(house3, OTHER_TENANT_ID);
        assertTrue(result);
        assertEquals(OTHER_TENANT_ID, house3.getTenantId());
    }
}
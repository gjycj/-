package COM.House.Deed.Pavilion.Service;

import COM.House.Deed.Pavilion.DTO.HouseQueryDTO;
import COM.House.Deed.Pavilion.DTO.HouseUpdateDTO;
import COM.House.Deed.Pavilion.Entity.Agent;
import COM.House.Deed.Pavilion.Entity.Building;
import COM.House.Deed.Pavilion.Entity.House;
import COM.House.Deed.Pavilion.Entity.Enum.HouseStatusEnum;
import COM.House.Deed.Pavilion.Mapper.AgentMapper;
import COM.House.Deed.Pavilion.Mapper.BuildingMapper;
import COM.House.Deed.Pavilion.Mapper.HouseMapper;
import COM.House.Deed.Pavilion.Utils.PageResult;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class HouseService {

    @Resource
    private HouseMapper houseMapper;
    @Resource
    private BuildingMapper buildingMapper;
    @Resource
    private AgentMapper agentMapper;
    // 在HouseService中注入状态日志Service
    @Resource
    private HouseStatusLogService houseStatusLogService;

    /**
     * 新增房源
     *
     * @param house 房源信息（包含所属楼栋ID、门牌号等必填字段）
     * @return 新增的房源ID
     */
    @Transactional
    public Long addHouse(House house) {
        // 1. 校验所属楼栋是否存在（核心关联校验）
        Long buildingId = house.getBuildingId();
        Building building = buildingMapper.selectById(buildingId);
        if (building == null) {
            throw new RuntimeException("所属楼栋不存在（楼栋ID：" + buildingId + "），无法新增房源");
        }

        // 2. 校验负责经纪人是否存在
        Long agentId = house.getAgentId();
        Agent agent = agentMapper.selectById(agentId);
        if (agent == null) {
            throw new RuntimeException("负责经纪人不存在（经纪人ID：" + agentId + "），无法新增房源");
        }

        // 3. 校验同一楼栋下门牌号是否重复
        String houseNumber = house.getHouseNumber();
        House existHouse = houseMapper.selectByBuildingIdAndHouseNumber(buildingId, houseNumber, null);
        if (existHouse != null) {
            throw new RuntimeException("该楼栋下已存在门牌号为【" + houseNumber + "】的房源");
        }

        // 4. 基础参数校验
        if (houseNumber == null || houseNumber.trim().isEmpty()) {
            throw new RuntimeException("门牌号不能为空");
        }
        if (house.getLayout() == null || house.getLayout().trim().isEmpty()) {
            throw new RuntimeException("户型信息不能为空");
        }
        if (house.getArea() == null || house.getArea().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("建筑面积必须大于0平方米");
        }
        if (house.getFloor() == null || house.getFloor() <= 0) {
            throw new RuntimeException("所在楼层必须大于0");
        }
        if (house.getHouseType() == null) {
            throw new RuntimeException("房源类型不能为空");
        }
        if (house.getPrice() == null || house.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("价格必须大于0");
        }

        // 5. 校验创建人/更新人ID
        if (house.getCreatedBy() == null || house.getUpdatedBy() == null) {
            throw new RuntimeException("创建人ID和更新人ID不能为空");
        }
        if (!house.getCreatedBy().equals(house.getUpdatedBy())) {
            throw new RuntimeException("创建人ID与更新人ID必须一致");
        }

        // 6. 设置时间和默认值
        LocalDateTime now = LocalDateTime.now();
        house.setCreatedAt(now);
        house.setUpdatedAt(now);
        if (house.getStatus() == null) {
            house.setStatus(HouseStatusEnum.PENDING);
        }
        if (house.getIsValid() == null) {
            house.setIsValid(1);
        }

        // 7. 执行插入
        int rows = houseMapper.insert(house);
        if (rows != 1) {
            throw new RuntimeException("新增房源失败，请重试");
        }

        // 8. 返回新增房源ID
        return house.getId();
    }


    /**
     * 根据ID更新房源信息（适配完整DTO，包含所有可更新字段）
     *
     * @param updateDTO 包含房源ID、所有可更新字段及状态变更原因
     */
    @Transactional
    public void updateHouseById(HouseUpdateDTO updateDTO) {
        // 1. 基础参数校验
        Long houseId = updateDTO.getId();
        if (houseId <= 0) {
            throw new RuntimeException("房源ID无效（必须为正整数）");
        }
        if (updateDTO.getUpdatedBy() <= 0) {
            throw new RuntimeException("操作人ID无效（必须为正整数）");
        }

        // 2. 校验房源存在性
        House existing = houseMapper.selectById(houseId);
        if (existing == null) {
            throw new RuntimeException("房源不存在（房源ID：" + houseId + "），无法更新");
        }

        // 3. 状态变更判断与校验（核心逻辑）
        Integer oldStatus = existing.getStatus().getCode(); // 原状态编码
        Integer newStatus = updateDTO.getStatusCode();       // 新状态编码
        // 状态变更时必须提供原因
        if (newStatus != null && !newStatus.equals(oldStatus)) {
            if (updateDTO.getStatusChangeReason() == null || updateDTO.getStatusChangeReason().trim().isEmpty()) {
                throw new RuntimeException("状态变更时必须填写变更原因");
            }
        }

        // 4. 经纪人ID校验（若传入）
        Long agentId = updateDTO.getAgentId();
        if (agentId != null) {
            if (agentId <= 0) {
                throw new RuntimeException("经纪人ID无效（必须为正整数）");
            }
            Agent agent = agentMapper.selectById(agentId);
            if (agent == null) {
                throw new RuntimeException("经纪人不存在（ID：" + agentId + "），无法关联");
            }
        }

        // 5. 楼栋ID校验（若传入）
        Long newBuildingId = updateDTO.getBuildingId();
        if (newBuildingId != null) {
            if (newBuildingId <= 0) {
                throw new RuntimeException("新楼栋ID无效（必须为正整数）");
            }
            Building newBuilding = buildingMapper.selectById(newBuildingId);
            if (newBuilding == null) {
                throw new RuntimeException("新楼栋不存在（楼栋ID：" + newBuildingId + "），无法更新");
            }
        }

        // 6. 门牌号唯一性校验（若传入）
        String newHouseNumber = updateDTO.getHouseNumber();
        if (newHouseNumber != null && !newHouseNumber.trim().isEmpty()) {
            Long buildingId = newBuildingId != null ? newBuildingId : existing.getBuildingId();
            House existHouse = houseMapper.selectByBuildingIdAndHouseNumber(buildingId, newHouseNumber, houseId);
            if (existHouse != null) {
                throw new RuntimeException("该楼栋下已存在门牌号为【" + newHouseNumber + "】的房源");
            }
        }

        // 7. DTO转换为实体（映射所有可更新字段）
        House house = new House();
        house.setId(houseId);
        house.setBuildingId(updateDTO.getBuildingId());
        house.setHouseNumber(updateDTO.getHouseNumber());
        house.setLayout(updateDTO.getLayout());
        house.setArea(updateDTO.getArea());
        house.setInnerArea(updateDTO.getInnerArea());
        house.setFloor(updateDTO.getFloor());
        house.setTotalFloor(updateDTO.getTotalFloor());
        house.setOrientation(updateDTO.getOrientation());
        house.setDecoration(updateDTO.getDecoration());
        // 房屋类型：假设House实体中houseType是枚举，这里设置code
        if (updateDTO.getHouseTypeCode() != null) {
            house.setHouseTypeByCode(updateDTO.getHouseTypeCode()); // 需在House实体中实现该方法
        }
        house.setPrice(updateDTO.getPrice());
        // 状态：假设House实体中status是枚举，这里设置code
        if (updateDTO.getStatusCode() != null) {
            house.setStatusByCode(updateDTO.getStatusCode()); // 需在House实体中实现该方法
        }
        house.setOwnerPrice(updateDTO.getOwnerPrice());
        house.setDescription(updateDTO.getDescription());
        house.setAgentId(updateDTO.getAgentId());
        house.setIsValid(updateDTO.getIsValid());
        house.setUpdatedBy(updateDTO.getUpdatedBy());
        house.setUpdatedAt(LocalDateTime.now());

        // 8. 执行更新
        int rows = houseMapper.updateById(house);
        if (rows != 1) {
            throw new RuntimeException("更新房源失败，请重试");
        }

        // 9. 状态变更时记录日志（核心逻辑）
        if (newStatus != null && !newStatus.equals(oldStatus)) {
            houseStatusLogService.createStatusLog(
                    houseId,
                    oldStatus,          // 原状态编码
                    newStatus,          // 新状态编码
                    updateDTO.getStatusChangeReason().trim(),  // 变更原因
                    updateDTO.getUpdatedBy()                   // 操作人
            );
        }
    }


    /**
     * 根据ID查询房源详情
     *
     * @param id 房源ID
     * @return 房源完整信息
     */
    public House getHouseById(Long id) {
        // 1. 校验ID合法性
        if (id == null || id <= 0) {
            throw new RuntimeException("房源ID无效（必须为正整数）");
        }

        // 2. 执行查询
        House house = houseMapper.selectById(id);
        if (house == null) {
            throw new RuntimeException("未找到ID为" + id + "的房源");
        }

        return house;
    }

    /**
     * 多条件分页查询房源
     *
     * @param queryDTO 包含查询条件（楼栋ID、价格范围、户型等）和分页参数
     * @return 分页的房源列表
     */
    public PageResult<House> getHousesByCondition(HouseQueryDTO queryDTO) {
        // 1. 校验分页参数
        if (queryDTO.getPageNum() < 1) {
            queryDTO.setPageNum(1);
        }
        if (queryDTO.getPageSize() < 1 || queryDTO.getPageSize() > 100) {
            queryDTO.setPageSize(10); // 限制最大每页100条
        }

        // 2. 若条件包含楼栋ID，校验该楼栋是否存在
        Long buildingId = queryDTO.getBuildingId();
        if (buildingId != null) {
            if (buildingId <= 0) {
                throw new RuntimeException("楼栋ID无效（必须为正整数）");
            }
            Building building = buildingMapper.selectById(buildingId);
            if (building == null) {
                throw new RuntimeException("楼栋不存在（楼栋ID：" + buildingId + "）");
            }
        }

        // 3. 开启分页并执行多条件查询
        PageHelper.startPage(queryDTO.getPageNum(), queryDTO.getPageSize());
        Page<House> housePage = houseMapper.selectByCondition(queryDTO);

        // 4. 封装分页结果
        return getHousePageResult(housePage);
    }

    /**
     * 根据楼栋ID分页查询房源列表
     *
     * @param buildingId 所属楼栋ID
     * @param pageNum    页码（默认1）
     * @param pageSize   每页条数（默认10）
     * @return 分页的房源列表
     */
    public PageResult<House> getHousesByBuildingId(Long buildingId, Integer pageNum, Integer pageSize) {
        // 1. 校验楼栋ID合法性及存在性
        if (buildingId == null || buildingId <= 0) {
            throw new RuntimeException("楼栋ID无效（必须为正整数）");
        }
        Building building = buildingMapper.selectById(buildingId);
        if (building == null) {
            throw new RuntimeException("楼栋不存在（楼栋ID：" + buildingId + "），无法查询房源");
        }

        // 2. 校验分页参数
        if (pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize < 1 || pageSize > 100) {
            pageSize = 10;
        }

        // 3. 开启分页并执行查询
        PageHelper.startPage(pageNum, pageSize);
        Page<House> housePage = houseMapper.selectByBuildingId(buildingId);

        // 4. 封装分页结果
        return getHousePageResult(housePage);
    }

    /**
     * 封装房源分页结果
     */
    private PageResult<House> getHousePageResult(Page<House> housePage) {
        PageResult<House> pageResult = new PageResult<>();
        pageResult.setList(housePage.getResult());
        pageResult.setTotal(housePage.getTotal());
        pageResult.setPages(housePage.getPages());
        pageResult.setPageNum(housePage.getPageNum());
        pageResult.setPageSize(housePage.getPageSize());
        return pageResult;
    }
}

package COM.House.Deed.Pavilion.Service;

import COM.House.Deed.Pavilion.DTO.BuildingQueryDTO;
import COM.House.Deed.Pavilion.Entity.Building;
import COM.House.Deed.Pavilion.Entity.Property;
import COM.House.Deed.Pavilion.Mapper.BuildingMapper;
import COM.House.Deed.Pavilion.Mapper.PropertyMapper;
import COM.House.Deed.Pavilion.Utils.PageResult;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BuildingService {
    @Resource
    private BuildingMapper buildingMapper;
    @Resource
    private PropertyMapper propertyMapper;

    /**
     * 根据ID更新楼栋信息
     *
     * @param building 包含ID和待更新字段的楼栋信息
     */
    @Transactional
    public void updateBuildingById(Building building) {
        // 1. 校验楼栋ID合法性
        Long buildingId = building.getId();
        if (buildingId == null || buildingId <= 0) {
            throw new RuntimeException("楼栋ID无效（必须为正整数）");
        }

        // 2. 校验楼栋是否存在
        Building existing = buildingMapper.selectById(buildingId);
        if (existing == null) {
            throw new RuntimeException("楼栋不存在（楼栋ID：" + buildingId + "），无法更新");
        }

        // 3. 若更新了所属楼盘ID，校验新楼盘是否存在
        Long newPropertyId = building.getPropertyId();
        if (newPropertyId != null) { // 仅当传递了新的propertyId时才校验
            if (newPropertyId <= 0) {
                throw new RuntimeException("新楼盘ID无效（必须为正整数）");
            }
            Property newProperty = propertyMapper.selectById(newPropertyId);
            if (newProperty == null) {
                throw new RuntimeException("新楼盘不存在（楼盘ID：" + newPropertyId + "），无法更新");
            }
        }

        // 4. 若未传递更新人ID，默认使用原更新人ID（可选，根据业务调整）
        if (building.getUpdatedBy() == null) {
            building.setUpdatedBy(existing.getUpdatedBy());
        }

        // 5. 执行更新（XML中已通过NOW()自动更新updated_at）
        int rows = buildingMapper.updateById(building);
        if (rows != 1) {
            throw new RuntimeException("更新楼栋失败，请重试");
        }
    }

    /**
     * 多条件分页查询楼栋
     *
     * @param queryDTO 包含查询条件（楼盘ID、名称、层数范围等）和分页参数
     * @return 分页的楼栋列表
     */
    public PageResult<Building> getBuildingsByCondition(BuildingQueryDTO queryDTO) {
        // 1. 校验分页参数
        if (queryDTO.getPageNum() < 1) {
            queryDTO.setPageNum(1);
        }
        if (queryDTO.getPageSize() < 1 || queryDTO.getPageSize() > 100) {
            queryDTO.setPageSize(10); // 限制最大每页100条
        }

        // 2. 若条件包含楼盘ID，校验该楼盘是否存在
        Long propertyId = queryDTO.getPropertyId();
        if (propertyId != null) {
            if (propertyId <= 0) {
                throw new RuntimeException("楼盘ID无效（必须为正整数）");
            }
            Property property = propertyMapper.selectById(propertyId);
            if (property == null) {
                throw new RuntimeException("楼盘不存在（楼盘ID：" + propertyId + "）");
            }
        }

        // 3. 开启分页并执行多条件查询
        PageHelper.startPage(queryDTO.getPageNum(), queryDTO.getPageSize());
        Page<Building> buildingPage = buildingMapper.selectByCondition(queryDTO);

        // 4. 封装分页结果
        return getBuildingPageResult(buildingPage);
    }

    private PageResult<Building> getBuildingPageResult(Page<Building> buildingPage) {
        PageResult<Building> pageResult = new PageResult<>();
        pageResult.setList(buildingPage.getResult());
        pageResult.setTotal(buildingPage.getTotal());
        pageResult.setPages(buildingPage.getPages());
        pageResult.setPageNum(buildingPage.getPageNum());
        pageResult.setPageSize(buildingPage.getPageSize());

        return pageResult;
    }

    /**
     * 新增楼栋
     *
     * @param building 楼栋信息（包含所属楼盘ID、名称等必填字段）
     * @return 新增的楼栋ID
     */
    @Transactional // 开启事务，确保数据一致性
    public Long addBuilding(Building building) {
        // 1. 校验所属楼盘是否存在（核心关联校验）
        Long propertyId = building.getPropertyId();
        Property property = propertyMapper.selectById(propertyId);
        if (property == null) {
            throw new RuntimeException("所属楼盘不存在（楼盘ID：" + propertyId + "），无法新增楼栋");
        }

        // 2. 校验楼栋名称在同一楼盘下是否重复（可选，根据业务需求添加）
        // 新代码（正确，用列表判断）
        List<Building> existList = buildingMapper.selectByNameAndPropertyId(building.getName(), propertyId);
        // 只要列表不为空，说明存在重复（不管有多少条）
        if (existList != null && !existList.isEmpty()) {
            throw new RuntimeException("该楼盘下已存在名称为【" + building.getName() + "】的楼栋");
        }

        // 3. 设置时间字段（若数据库未配置默认值，手动赋值）
        LocalDateTime now = LocalDateTime.now();
        building.setCreatedAt(now);
        building.setUpdatedAt(now);

        // 确保创建人ID（createdBy）和更新人ID（updatedBy）这两个关键字段不为空
        if (building.getCreatedBy() == null || building.getUpdatedBy() == null) {
            throw new RuntimeException("创建人ID和更新人ID不能为空");
        }

        // 4. 校验创建人ID和更新人ID一致性
        if (!building.getCreatedBy().equals(building.getUpdatedBy())) {
            throw new RuntimeException("创建人ID与更新人ID必须一致");
        }

        // 5. 执行插入
        int rows = buildingMapper.insert(building);
        if (rows != 1) {
            throw new RuntimeException("新增楼栋失败，请重试");
        }

        // 6. 返回新增的楼栋ID（MyBatis自动回填自增主键）
        return building.getId();
    }

    /**
     * 根据ID查询楼栋详情
     *
     * @param id 楼栋ID
     * @return 楼栋完整信息（含所属楼盘ID）
     */
    public Building getBuildingById(Long id) {
        // 1. 校验ID合法性
        if (id == null || id <= 0) {
            throw new RuntimeException("楼栋ID无效（必须为正整数）");
        }

        // 2. 执行查询
        Building building = buildingMapper.selectById(id);
        if (building == null) {
            throw new RuntimeException("未找到ID为" + id + "的楼栋");
        }

        // 3. 可选：关联查询所属楼盘的基本信息（如楼盘名称，需扩展实体类）
        Property property = propertyMapper.selectById(building.getPropertyId());
        if (property != null) {
            building.setProperty(property); // 需在Building实体类中添加propertyName字段
        }

        return building;
    }

    /**
     * 根据楼盘ID分页查询楼栋列表
     *
     * @param propertyId 所属楼盘ID
     * @param pageNum    页码（默认1）
     * @param pageSize   每页条数（默认10）
     * @return 分页的楼栋列表
     */
    public PageResult<Building> getBuildingsByPropertyId(Long propertyId, Integer pageNum, Integer pageSize) {
        // 1. 校验楼盘ID合法性及存在性
        if (propertyId == null || propertyId <= 0) {
            throw new RuntimeException("楼盘ID无效（必须为正整数）");
        }
        // 校验楼盘是否存在（避免查询不存在的楼盘下的楼栋）
        Property property = propertyMapper.selectById(propertyId);
        if (property == null) {
            throw new RuntimeException("楼盘不存在（楼盘ID：" + propertyId + "），无法查询楼栋");
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
        Page<Building> buildingPage = buildingMapper.selectByPropertyId(propertyId);

        // 4. 封装分页结果
        return getBuildingPageResult(buildingPage);
    }
}
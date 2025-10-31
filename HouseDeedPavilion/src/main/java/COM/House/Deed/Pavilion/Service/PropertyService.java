package COM.House.Deed.Pavilion.Service;

import COM.House.Deed.Pavilion.DTO.PropertyQueryDTO;
import COM.House.Deed.Pavilion.Entity.Property;
import COM.House.Deed.Pavilion.Mapper.PropertyMapper;
import COM.House.Deed.Pavilion.Utils.PageResult;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class PropertyService {

    @Resource
    private PropertyMapper propertyMapper;

    /**
     * 新增楼盘
     * @param property 楼盘信息（包含创建人ID等）
     * @return 新增的楼盘ID
     */
    @Transactional // 开启事务，确保数据一致性
    public Long addProperty(Property property) {
        // 1. 业务校验：示例-检查楼盘名称是否已存在（根据实际需求添加）
        // （需在Mapper中添加selectByName方法，此处简化）
        // Property exist = propertyMapper.selectByName(property.getName());
        // if (exist != null) {
        //     throw new RuntimeException("楼盘名称已存在");
        // }

        // 2. 设置时间字段（若数据库未配置默认值，手动赋值）
        LocalDateTime now = LocalDateTime.now();
        property.setCreatedAt(now); // 创建时间
        property.setUpdatedAt(now); // 更新时间（新增时与创建时间一致）

        // 3. 确保创建人ID和更新人ID一致（新增时）
        if (!property.getCreatedBy().equals(property.getUpdatedBy())) {
            throw new RuntimeException("创建人ID与更新人ID必须一致");
        }

        // 4. 执行插入
        int rows = propertyMapper.insert(property);
        if (rows != 1) {
            throw new RuntimeException("新增楼盘失败，请重试");
        }

        // 5. 返回新增的楼盘ID（MyBatis会自动回填自增主键到property对象的id字段）
        return property.getId();
    }

    /**
     * 分页查询楼盘
     * @param pageNum 页码（从1开始）
     * @param pageSize 每页条数
     * @param propertyType 物业类型（可选，为null时查询全部）
     * @return 分页结果（包含数据和分页信息）
     */
    public PageResult<Property> getPropertyByPage(Integer pageNum, Integer pageSize, String propertyType) {
        // 开启分页
        PageHelper.startPage(pageNum, pageSize);
        // 执行查询
        Page<Property> propertyPage = propertyMapper.selectByPage(propertyType);

        // 封装分页结果到PageResult
        return getPropertyPageResult(propertyPage);
    }

    /**
     * 更新楼盘信息
     * @param property 包含ID和待更新字段的楼盘信息
     */
    @Transactional
    public void updateProperty(Property property) {
        // 1. 校验楼盘是否存在
        Property existing = propertyMapper.selectById(property.getId());
        if (existing == null) {
            throw new RuntimeException("楼盘不存在，无法更新");
        }

        // 2. 若未传递更新人ID，默认使用原更新人ID（可选，根据业务调整）
        if (property.getUpdatedBy() == null) {
            property.setUpdatedBy(existing.getUpdatedBy());
        }

        // 3. 执行更新（SQL中已通过NOW()自动更新updated_at，无需手动设置）
        int rows = propertyMapper.updateById(property);
        if (rows != 1) {
            throw new RuntimeException("更新楼盘失败，请重试");
        }
    }

    /**
     * 根据多条件分页查询楼盘
     * @param queryDTO 查询条件和分页参数
     * @return 分页结果
     */
    public PageResult<Property> getPropertyByCondition(PropertyQueryDTO queryDTO) {
        // 1. 校验分页参数（确保页码和每页条数为正数）
        if (queryDTO.getPageNum() < 1) {
            queryDTO.setPageNum(1);
        }
        if (queryDTO.getPageSize() < 1 || queryDTO.getPageSize() > 100) {
            queryDTO.setPageSize(10); // 限制最大每页100条
        }

        // 2. 开启分页并执行条件查询
        PageHelper.startPage(queryDTO.getPageNum(), queryDTO.getPageSize());
        Page<Property> propertyPage = propertyMapper.selectByCondition(queryDTO);

        // 3. 封装分页结果
        return getPropertyPageResult(propertyPage);
    }

    /**
     * 封装楼盘分页结果（复用逻辑）
     */
    private PageResult<Property> getPropertyPageResult(Page<Property> propertyPage) {
        PageResult<Property> pageResult = new PageResult<>();
        pageResult.setList(propertyPage.getResult());
        pageResult.setTotal(propertyPage.getTotal());
        pageResult.setPages(propertyPage.getPages());
        pageResult.setPageNum(propertyPage.getPageNum());
        pageResult.setPageSize(propertyPage.getPageSize());

        return pageResult;
    }

    /**
     * 根据ID查询楼盘详情
     * @param id 楼盘ID
     * @return 楼盘完整信息
     */
    public Property getPropertyById(Long id) {
        // 校验ID合法性
        if (id == null || id <= 0) {
            throw new RuntimeException("楼盘ID无效");
        }

        // 执行查询
        Property property = propertyMapper.selectById(id);
        if (property == null) {
            throw new RuntimeException("未找到ID为" + id + "的楼盘");
        }

        return property;
    }
}
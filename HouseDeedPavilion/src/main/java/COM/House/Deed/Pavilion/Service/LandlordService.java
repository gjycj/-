package COM.House.Deed.Pavilion.Service;

import COM.House.Deed.Pavilion.DTO.LandlordQueryDTO;
import COM.House.Deed.Pavilion.Entity.Landlord;
import COM.House.Deed.Pavilion.Mapper.LandlordMapper;
import COM.House.Deed.Pavilion.Utils.PageResult;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class LandlordService {

    @Resource
    private LandlordMapper landlordMapper;

    /**
     * 新增房东信息
     *
     * @param landlord 房东信息（包含姓名、电话等必填字段）
     * @return 新增的房东ID
     */
    @Transactional
    public Long addLandlord(Landlord landlord) {
        // 1. 校验必填字段
        if (landlord.getName() == null || landlord.getName().trim().isEmpty()) {
            throw new RuntimeException("房东姓名不能为空");
        }
        if (landlord.getPhone() == null || landlord.getPhone().trim().isEmpty()) {
            throw new RuntimeException("联系电话不能为空");
        }
        if (landlord.getCreatedBy() == null || landlord.getCreatedBy() <= 0) {
            throw new RuntimeException("创建人ID无效（必须为正整数）");
        }
        if (landlord.getUpdatedBy() == null || landlord.getUpdatedBy() <= 0) {
            throw new RuntimeException("更新人ID无效（必须为正整数）");
        }

        // 2. 校验手机号唯一性（核心业务校验）
        Landlord existLandlord = landlordMapper.selectByPhone(landlord.getPhone(), null);
        if (existLandlord != null) {
            throw new RuntimeException("联系电话【" + landlord.getPhone() + "】已被占用，无法新增");
        }

        // 3. 设置时间字段
        LocalDateTime now = LocalDateTime.now();
        landlord.setCreatedAt(now);
        landlord.setUpdatedAt(now);

        // 4. 执行插入
        int rows = landlordMapper.insert(landlord);
        if (rows != 1) {
            throw new RuntimeException("新增房东失败，请重试");
        }

        return landlord.getId();
    }

    /**
     * 根据ID查询房东详情
     *
     * @param id 房东ID
     * @return 房东完整信息
     */
    public Landlord getLandlordById(Long id) {
        // 1. 校验ID合法性
        if (id == null || id <= 0) {
            throw new RuntimeException("房东ID无效（必须为正整数）");
        }

        // 2. 执行查询并校验存在性
        Landlord landlord = landlordMapper.selectById(id);
        if (landlord == null) {
            throw new RuntimeException("未找到ID为" + id + "的房东");
        }

        return landlord;
    }

    /**
     * 根据ID更新房东信息
     *
     * @param landlord 包含ID和待更新字段的房东信息
     */
    @Transactional
    public void updateLandlordById(Landlord landlord) {
        // 1. 校验ID合法性
        Long id = landlord.getId();
        if (id == null || id <= 0) {
            throw new RuntimeException("房东ID无效（必须为正整数）");
        }

        // 2. 校验房东是否存在
        Landlord existing = landlordMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("房东不存在（ID：" + id + "），无法更新");
        }

        // 3. 若更新手机号，校验新手机号唯一性
        String newPhone = landlord.getPhone();
        if (newPhone != null && !newPhone.trim().isEmpty()) {
            Landlord conflictLandlord = landlordMapper.selectByPhone(newPhone, id);
            if (conflictLandlord != null) {
                throw new RuntimeException("联系电话【" + newPhone + "】已被占用，无法更新");
            }
        }

        // 4. 校验更新人ID有效性
        if (landlord.getUpdatedBy() != null && landlord.getUpdatedBy() <= 0) {
            throw new RuntimeException("更新人ID无效（必须为正整数）");
        }

        // 5. 设置更新时间
        landlord.setUpdatedAt(LocalDateTime.now());

        // 6. 执行更新
        int rows = landlordMapper.updateById(landlord);
        if (rows != 1) {
            throw new RuntimeException("更新房东信息失败，请重试");
        }
    }

    /**
     * 多条件分页查询房东
     *
     * @param queryDTO 包含查询条件和分页参数
     * @return 分页的房东列表
     */
    public PageResult<Landlord> getLandlordsByCondition(LandlordQueryDTO queryDTO) {
        // 1. 校验分页参数
        if (queryDTO.getPageNum() < 1) {
            queryDTO.setPageNum(1);
        }
        if (queryDTO.getPageSize() < 1 || queryDTO.getPageSize() > 100) {
            queryDTO.setPageSize(10); // 限制最大每页100条
        }

        // 2. 开启分页并执行查询
        PageHelper.startPage(queryDTO.getPageNum(), queryDTO.getPageSize());
        Page<Landlord> landlordPage = landlordMapper.selectByCondition(queryDTO);

        // 3. 封装分页结果
        return getLandlordPageResult(landlordPage);
    }

    /**
     * 根据姓名模糊查询房东（分页）
     *
     * @param name 房东姓名（模糊匹配）
     * @param pageNum 页码（默认1）
     * @param pageSize 每页条数（默认10）
     * @return 分页的房东列表
     */
    public PageResult<Landlord> getLandlordsByName(String name, Integer pageNum, Integer pageSize) {
        // 1. 校验分页参数
        if (pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize < 1 || pageSize > 100) {
            pageSize = 10;
        }

        // 2. 处理空姓名（查询全部）
        if (name == null || name.trim().isEmpty()) {
            name = ""; // 避免模糊查询为NULL
        }

        // 3. 开启分页并执行查询
        PageHelper.startPage(pageNum, pageSize);
        Page<Landlord> landlordPage = landlordMapper.selectByName(name);

        // 4. 封装分页结果
        return getLandlordPageResult(landlordPage);
    }

    /**
     * 封装房东分页结果（复用逻辑）
     */
    private PageResult<Landlord> getLandlordPageResult(Page<Landlord> landlordPage) {
        PageResult<Landlord> pageResult = new PageResult<>();
        pageResult.setList(landlordPage.getResult());
        pageResult.setTotal(landlordPage.getTotal());
        pageResult.setPages(landlordPage.getPages());
        pageResult.setPageNum(landlordPage.getPageNum());
        pageResult.setPageSize(landlordPage.getPageSize());

        return pageResult;
    }
}

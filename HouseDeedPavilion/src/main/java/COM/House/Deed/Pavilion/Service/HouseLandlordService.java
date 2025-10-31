package COM.House.Deed.Pavilion.Service;

import COM.House.Deed.Pavilion.DTO.HouseLandlordQueryDTO;
import COM.House.Deed.Pavilion.Entity.House;
import COM.House.Deed.Pavilion.Entity.HouseLandlord;
import COM.House.Deed.Pavilion.Entity.Landlord;
import COM.House.Deed.Pavilion.Mapper.HouseLandlordMapper;
import COM.House.Deed.Pavilion.Mapper.HouseMapper;
import COM.House.Deed.Pavilion.Mapper.LandlordMapper;
import COM.House.Deed.Pavilion.Utils.PageResult;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class HouseLandlordService {

    @Resource
    private HouseLandlordMapper houseLandlordMapper;

    @Resource
    private HouseMapper houseMapper;

    @Resource
    private LandlordMapper landlordMapper;

    // 在原有HouseLandlordService中注入备份Service
    @Resource
    private HouseLandlordBackupService houseLandlordBackupService;

    /**
     * 新增房源-房东关联关系
     *
     * @param houseLandlord 关联信息（包含房源ID、房东ID等必填字段）
     * @return 新增的关联记录ID
     */
    @Transactional
    public Long addHouseLandlord(HouseLandlord houseLandlord) {
        // 1. 校验核心参数
        Long houseId = houseLandlord.getHouseId();
        Long landlordId = houseLandlord.getLandlordId();
        Integer isMain = houseLandlord.getIsMain();

        if (houseId == null || houseId <= 0) {
            throw new RuntimeException("房源ID无效（必须为正整数）");
        }
        if (landlordId == null || landlordId <= 0) {
            throw new RuntimeException("房东ID无效（必须为正整数）");
        }
        if (isMain == null || (isMain != 0 && isMain != 1)) {
            throw new RuntimeException("是否主要房东必须为0或1（0-否，1-是）");
        }
        if (houseLandlord.getCreatedBy() == null || houseLandlord.getCreatedBy() <= 0) {
            throw new RuntimeException("创建人ID无效（必须为正整数）");
        }

        // 2. 校验房源和房东是否存在（外键关联校验）
        House house = houseMapper.selectById(houseId);
        if (house == null) {
            throw new RuntimeException("房源不存在（ID：" + houseId + "），无法关联");
        }
        Landlord landlord = landlordMapper.selectById(landlordId);
        if (landlord == null) {
            throw new RuntimeException("房东不存在（ID：" + landlordId + "），无法关联");
        }

        // 3. 校验关联关系是否已存在（唯一键约束）
        HouseLandlord existing = houseLandlordMapper.selectByHouseAndLandlord(houseId, landlordId);
        if (existing != null) {
            throw new RuntimeException("房源（ID：" + houseId + "）与房东（ID：" + landlordId + "）的关联关系已存在");
        }

        // 4. 业务规则：同一房源只能有一个主要房东
        if (isMain == 1) {
            // 查询该房源已有的主要房东
            HouseLandlordQueryDTO queryDTO = new HouseLandlordQueryDTO();
            queryDTO.setHouseId(houseId);
            queryDTO.setIsMain(1);
            Page<HouseLandlord> mainLandlords = houseLandlordMapper.selectByCondition(queryDTO);

            if (!mainLandlords.isEmpty()) {
                // 将已有主要房东更新为非主要
                HouseLandlord oldMain = mainLandlords.get(0);
                oldMain.setIsMain(0);
                // 注意：此处需确保HouseLandlord实体包含更新所需字段（如id）
                int updateRows = houseLandlordMapper.updateById(oldMain);
                if (updateRows != 1) {
                    throw new RuntimeException("更新原有主要房东失败，请重试");
                }
            }
        }

        // 5. 设置创建时间
        houseLandlord.setCreatedAt(LocalDateTime.now());

        // 6. 执行插入
        int rows = houseLandlordMapper.insert(houseLandlord);
        if (rows != 1) {
            throw new RuntimeException("新增房源-房东关联失败，请重试");
        }

        return houseLandlord.getId();
    }

    /**
     * 根据ID删除关联关系
     *
     * @param id 关联记录ID
     */
    @Transactional
    public void deleteById(Long id) {
        // 1. 校验ID有效性
        if (id == null || id <= 0) {
            throw new RuntimeException("关联记录ID无效（必须为正整数）");
        }

        // 2. 校验关联记录是否存在
        HouseLandlord existing = houseLandlordMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("关联记录不存在（ID：" + id + "），无法删除");
        }

        // 3. 新增：创建备份记录（核心调整）
        // 操作人ID从现有上下文中获取（示例中直接使用existing的createdBy，实际应从登录用户获取）
        houseLandlordBackupService.createBackup(existing, "手动删除关联关系", existing.getCreatedBy());

        // 4. 执行删除
        int rows = houseLandlordMapper.deleteById(id);
        if (rows != 1) {
            throw new RuntimeException("删除关联关系失败，请重试");
        }
    }

    /**
     * 根据房源ID和房东ID删除关联关系
     *
     * @param houseId    房源ID
     * @param landlordId 房东ID
     */
    @Transactional
    public void deleteByHouseAndLandlord(Long houseId, Long landlordId) {
        // 1. 校验参数有效性
        if (houseId == null || houseId <= 0) {
            throw new RuntimeException("房源ID无效（必须为正整数）");
        }
        if (landlordId == null || landlordId <= 0) {
            throw new RuntimeException("房东ID无效（必须为正整数）");
        }

        // 2. 校验关联关系是否存在
        HouseLandlord existing = houseLandlordMapper.selectByHouseAndLandlord(houseId, landlordId);
        if (existing == null) {
            throw new RuntimeException("房源（ID：" + houseId + "）与房东（ID：" + landlordId + "）无关联关系，无法删除");
        }

        // 3. 新增：创建备份记录（核心调整）
        // 操作人ID从现有上下文中获取（示例中直接使用existing的createdBy，实际应从登录用户获取）
        houseLandlordBackupService.createBackup(existing, "手动解除房源-房东关联", existing.getCreatedBy());

        // 4. 执行删除
        int rows = houseLandlordMapper.deleteByHouseAndLandlord(houseId, landlordId);
        if (rows != 1) {
            throw new RuntimeException("删除关联关系失败，请重试");
        }
    }

    /**
     * 根据房源ID查询关联的房东列表
     *
     * @param houseId 房源ID
     * @return 房东信息列表（包含姓名、电话等详情）
     */
    public List<Landlord> getLandlordsByHouseId(Long houseId) {
        // 1. 校验房源ID
        if (houseId == null || houseId <= 0) {
            throw new RuntimeException("房源ID无效（必须为正整数）");
        }

        // 2. 校验房源是否存在
        House house = houseMapper.selectById(houseId);
        if (house == null) {
            throw new RuntimeException("房源不存在（ID：" + houseId + "）");
        }

        // 3. 查询关联的房东列表
        return houseLandlordMapper.selectLandlordsByHouseId(houseId);
    }

    /**
     * 多条件分页查询关联关系
     *
     * @param queryDTO 包含查询条件和分页参数
     * @return 分页的关联记录列表
     */
    public PageResult<HouseLandlord> getHouseLandlordsByCondition(HouseLandlordQueryDTO queryDTO) {
        // 1. 处理空参数
        if (queryDTO == null) {
            queryDTO = new HouseLandlordQueryDTO();
        }

        // 2. 校验分页参数
        if (queryDTO.getPageNum() < 1) {
            queryDTO.setPageNum(1);
        }
        if (queryDTO.getPageSize() < 1 || queryDTO.getPageSize() > 100) {
            queryDTO.setPageSize(10);
        }

        // 3. 执行分页查询
        PageHelper.startPage(queryDTO.getPageNum(), queryDTO.getPageSize());
        Page<HouseLandlord> relationPage = houseLandlordMapper.selectByCondition(queryDTO);

        // 4. 封装分页结果
        return getHouseLandlordPageResult(relationPage);
    }

    /**
     * 封装房东-房源关联分页结果（复用逻辑）
     */
    private PageResult<HouseLandlord> getHouseLandlordPageResult(Page<HouseLandlord> relationPage) {
        PageResult<HouseLandlord> pageResult = new PageResult<>();
        pageResult.setList(relationPage.getResult());
        pageResult.setTotal(relationPage.getTotal());
        pageResult.setPages(relationPage.getPages());
        pageResult.setPageNum(relationPage.getPageNum());
        pageResult.setPageSize(relationPage.getPageSize());

        return pageResult;
    }

    /**
     * 根据房源ID查询关联的所有房东ID（仅返回ID列表）
     *
     * @param houseId 房源ID
     * @return 房东ID列表（无关联时返回空列表）
     */
    public List<Long> getLandlordIdsByHouseId(Long houseId) {
        // 1. 校验房源ID有效性
        if (houseId == null || houseId <= 0) {
            throw new RuntimeException("房源ID无效（必须为正整数）");
        }

        // 2. 校验房源是否存在（避免查询不存在的房源）
        House house = houseMapper.selectById(houseId);
        if (house == null) {
            throw new RuntimeException("房源不存在（ID：" + houseId + "）");
        }

        // 3. 查询并返回房东ID列表（Mapper确保返回非null列表）
        return houseLandlordMapper.selectLandlordIdsByHouseId(houseId);
    }


}

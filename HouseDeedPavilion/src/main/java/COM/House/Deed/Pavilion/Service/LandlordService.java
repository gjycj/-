package COM.House.Deed.Pavilion.Service;

import COM.House.Deed.Pavilion.Constant.MessageConstant;
import COM.House.Deed.Pavilion.Constant.PageConstant;
import COM.House.Deed.Pavilion.Converter.LandlordConverter;
import COM.House.Deed.Pavilion.DTO.LandlordQueryDTO;
import COM.House.Deed.Pavilion.Entity.Landlord;
import COM.House.Deed.Pavilion.Entity.LandlordBackup;
import COM.House.Deed.Pavilion.Exception.BusinessException;
import COM.House.Deed.Pavilion.Mapper.HouseLandlordMapper;
import COM.House.Deed.Pavilion.Mapper.LandlordBackupMapper;
import COM.House.Deed.Pavilion.Mapper.LandlordMapper;
import COM.House.Deed.Pavilion.Utils.PageResult;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Isolation;

import java.time.LocalDateTime;

@Service
public class LandlordService {

    @Resource
    private LandlordMapper landlordMapper;
    @Resource
    private LandlordBackupMapper backupMapper;
    @Resource
    private LandlordConverter landlordConverter; // 实体转换工具

    /**
     * 新增房东信息
     *
     * @param landlord 房东信息（包含姓名、电话等必填字段）
     * @return 新增的房东ID
     */
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.DEFAULT, rollbackFor = Exception.class)
    public Long addLandlord(Landlord landlord) {
        // 1. 校验必填字段
        validateLandlordRequiredFields(landlord, true);

        // 2. 校验手机号唯一性（核心业务校验）
        Landlord existLandlord = landlordMapper.selectByPhone(landlord.getPhone(), null);
        if (existLandlord != null) {
            throw new BusinessException(String.format(MessageConstant.PHONE_ALREADY_USED, landlord.getPhone()));
        }

        // 3. 设置时间字段（可考虑通过MyBatis拦截器自动填充）
        LocalDateTime now = LocalDateTime.now();
        landlord.setCreatedAt(now);
        landlord.setUpdatedAt(now);

        // 4. 执行插入
        int rows = landlordMapper.insert(landlord);
        if (rows != 1) {
            throw new BusinessException(MessageConstant.ADD_LANDLORD_FAIL);
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
        validateId(id);

        // 2. 执行查询并校验存在性
        Landlord landlord = landlordMapper.selectById(id);
        if (landlord == null) {
            throw new BusinessException(String.format(MessageConstant.LANDLORD_NOT_FOUND, id));
        }

        return landlord;
    }

    /**
     * 根据ID更新房东信息
     *
     * @param landlord 包含ID和待更新字段的房东信息
     */
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.DEFAULT, rollbackFor = Exception.class)
    public void updateLandlordById(Landlord landlord) {
        // 1. 校验ID合法性
        Long id = landlord.getId();
        validateId(id);

        // 2. 校验房东是否存在
        Landlord existing = landlordMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(String.format(MessageConstant.LANDLORD_NOT_EXIST_FOR_UPDATE, id));
        }

        // 3. 若更新手机号，校验新手机号唯一性
        String newPhone = landlord.getPhone();
        if (newPhone != null && !newPhone.trim().isEmpty()) {
            Landlord conflictLandlord = landlordMapper.selectByPhone(newPhone, id);
            if (conflictLandlord != null) {
                throw new BusinessException(String.format(MessageConstant.PHONE_ALREADY_USED_FOR_UPDATE, newPhone));
            }
        }

        // 4. 校验更新人ID有效性
        if (landlord.getUpdatedBy() != null && landlord.getUpdatedBy() <= 0) {
            throw new BusinessException(MessageConstant.UPDATED_BY_INVALID);
        }

        // 5. 设置更新时间（可考虑通过MyBatis拦截器自动填充）
        landlord.setUpdatedAt(LocalDateTime.now());

        // 6. 执行更新
        int rows = landlordMapper.updateById(landlord);
        if (rows != 1) {
            throw new BusinessException(MessageConstant.UPDATE_LANDLORD_FAIL);
        }
    }

    /**
     * 多条件分页查询房东
     *
     * @param queryDTO 包含查询条件和分页参数
     * @return 分页的房东列表
     */
    public PageResult<Landlord> getLandlordsByCondition(LandlordQueryDTO queryDTO) {
        // 1. 校验并修正分页参数
        validateAndCorrectPageParams(queryDTO::setPageNum, queryDTO::setPageSize, queryDTO.getPageNum(), queryDTO.getPageSize());

        // 2. 开启分页并执行查询
        PageHelper.startPage(queryDTO.getPageNum(), queryDTO.getPageSize());
        Page<Landlord> landlordPage = landlordMapper.selectByCondition(queryDTO);

        // 3. 封装分页结果
        return getLandlordPageResult(landlordPage);
    }

    /**
     * 根据姓名模糊查询房东（分页）
     *
     * @param name     房东姓名（模糊匹配）
     * @param pageNum  页码（默认1）
     * @param pageSize 每页条数（默认10）
     * @return 分页的房东列表
     */
    public PageResult<Landlord> getLandlordsByName(String name, Integer pageNum, Integer pageSize) {
        // 1. 校验并修正分页参数
        Integer[] correctedParams = validateAndCorrectPageParams(pageNum, pageSize);
        pageNum = correctedParams[0];
        pageSize = correctedParams[1];

        // 2. 处理空姓名（查询全部）
        String queryName = (name == null || name.trim().isEmpty()) ? "" : name.trim();

        // 3. 开启分页并执行查询
        PageHelper.startPage(pageNum, pageSize);
        Page<Landlord> landlordPage = landlordMapper.selectByName(queryName);

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

    /**
     * 校验房东必填字段（新增时全量校验，更新时按需校验）
     *
     * @param landlord 房东实体
     * @param isAdd    是否为新增操作
     */
    private void validateLandlordRequiredFields(Landlord landlord, boolean isAdd) {
        // 姓名校验
        if (landlord.getName() == null || landlord.getName().trim().isEmpty()) {
            throw new BusinessException(MessageConstant.LANDLORD_NAME_EMPTY);
        }
        // 电话校验
        if (landlord.getPhone() == null || landlord.getPhone().trim().isEmpty()) {
            throw new BusinessException(MessageConstant.LANDLORD_PHONE_EMPTY);
        }
        // 新增时必须校验创建人
        if (isAdd && (landlord.getCreatedBy() == null || landlord.getCreatedBy() <= 0)) {
            throw new BusinessException(MessageConstant.CREATED_BY_INVALID);
        }
        // 新增/更新时必须校验更新人
        if (landlord.getUpdatedBy() == null || landlord.getUpdatedBy() <= 0) {
            throw new BusinessException(MessageConstant.UPDATED_BY_INVALID);
        }
    }

    /**
     * 校验ID合法性
     *
     * @param id 待校验ID
     */
    private void validateId(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(MessageConstant.LANDLORD_ID_INVALID);
        }
    }

    /**
     * 校验并修正分页参数（用于DTO对象）
     *
     * @param pageNumSetter  pageNum设置器
     * @param pageSizeSetter pageSize设置器
     * @param pageNum        原始页码
     * @param pageSize       原始页大小
     */
    private void validateAndCorrectPageParams(
            java.util.function.Consumer<Integer> pageNumSetter,
            java.util.function.Consumer<Integer> pageSizeSetter,
            Integer pageNum,
            Integer pageSize
    ) {
        // 修正页码
        int correctedPageNum = (pageNum == null || pageNum < PageConstant.MIN_PAGE_NUM) ? PageConstant.MIN_PAGE_NUM : pageNum;
        // 修正页大小
        int correctedPageSize = (pageSize == null || pageSize < PageConstant.MIN_PAGE_SIZE || pageSize > PageConstant.MAX_PAGE_SIZE)
                ? PageConstant.DEFAULT_PAGE_SIZE : pageSize;

        pageNumSetter.accept(correctedPageNum);
        pageSizeSetter.accept(correctedPageSize);
    }

    /**
     * 校验并修正分页参数（用于独立参数）
     *
     * @param pageNum  原始页码
     * @param pageSize 原始页大小
     * @return 修正后的[pageNum, pageSize]
     */
    private Integer[] validateAndCorrectPageParams(Integer pageNum, Integer pageSize) {
        int correctedPageNum = (pageNum == null || pageNum < PageConstant.MIN_PAGE_NUM) ? PageConstant.MIN_PAGE_NUM : pageNum;
        int correctedPageSize = (pageSize == null || pageSize < PageConstant.MIN_PAGE_SIZE || pageSize > PageConstant.MAX_PAGE_SIZE)
                ? PageConstant.DEFAULT_PAGE_SIZE : pageSize;
        return new Integer[]{correctedPageNum, correctedPageSize};
    }

    /**
     * 删除房东（含关联校验+自动备份）
     * 流程：校验ID → 检查房东存在性 → 检查房源关联 → 备份数据 → 删除主表记录
     */
    @Transactional(rollbackFor = Exception.class) // 事务保证：备份和删除要么同时成功，要么同时回滚
    public void deleteLandlordById(Long id, Long operatorId, String deleteReason) {
        // 1. 校验房东ID有效性
        if (id == null || id <= 0) {
            throw new BusinessException(MessageConstant.LANDLORD_ID_INVALID);
        }

        // 2. 检查房东是否存在
        Landlord original = landlordMapper.selectById(id);
        if (original == null) {
            throw new BusinessException(String.format(MessageConstant.LANDLORD_NOT_FOUND, id));
        }

        // 3. 检查是否有关联房源（核心校验，防止删除被引用的数据）
        int relatedHouseCount = landlordMapper.countHouseRelByLandlordId(id);
        if (relatedHouseCount > 0) {
            throw new BusinessException(String.format(MessageConstant.LANDLORD_HAS_RELATED_HOUSE, relatedHouseCount));
        }

        // 4. 自动备份到landlord_backup表
        LandlordBackup backup = landlordConverter.convertToBackup(original);
        // 补充备份特有字段（转换工具只复制原字段，备份时间/删除人等在这里设置）
        backup.setBackupTime(LocalDateTime.now());
        backup.setDeletedBy(operatorId); // 假设删除人是最后更新人，也可通过参数传入
        backup.setDeleteReason(deleteReason); // 实际场景可由前端传入删除原因

        int backupRows = backupMapper.insert(backup);
        if (backupRows != 1) {
            throw new BusinessException(MessageConstant.BACKUP_LANDLORD_FAIL);
        }

        // 5. 删除主表landlord中的记录
        int deleteRows = landlordMapper.deleteById(id);
        if (deleteRows != 1) {
            throw new BusinessException(MessageConstant.DELETE_LANDLORD_FAIL);
        }
    }

}
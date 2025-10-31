package COM.House.Deed.Pavilion.Service;

import COM.House.Deed.Pavilion.Entity.Customer;
import COM.House.Deed.Pavilion.Entity.House;
import COM.House.Deed.Pavilion.Entity.VisitRecordBackup;
import COM.House.Deed.Pavilion.Mapper.*;
import COM.House.Deed.Pavilion.Utils.PageResult;
import COM.House.Deed.Pavilion.Utils.VisitRecordConverter;
import COM.House.Deed.Pavilion.DTO.VisitRecordAddDTO;
import COM.House.Deed.Pavilion.DTO.VisitRecordQueryDTO;
import COM.House.Deed.Pavilion.DTO.VisitRecordUpdateDTO;
import COM.House.Deed.Pavilion.Entity.VisitRecord;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 带看记录服务类（包含外键关联校验）
 */
@Service
public class VisitRecordService {

    @Resource
    private VisitRecordMapper visitRecordMapper;

    // 关联表Mapper（用于校验外键存在性）
    @Resource
    private HouseMapper houseMapper;
    @Resource
    private CustomerMapper customerMapper;
    @Resource
    private AgentMapper agentMapper;

    // 新增：注入备份表Mapper
    @Resource
    private VisitRecordBackupMapper visitRecordBackupMapper;

    /**
     * 新增带看记录（校验房源、客户、经纪人存在性）
     */
    @Transactional
    public Long addVisitRecord(VisitRecordAddDTO addDTO) {

        // 1. 校验关联的房源、客户、经纪人是否存在
        validateRelatedEntityExists(
                addDTO.getHouseId(),
                addDTO.getCustomerId(),
                addDTO.getAgentId()
        );

        // 2. 校验带看时间有效性（不能早于当前时间太多，可根据业务调整）
        if (addDTO.getVisitTime().isBefore(java.time.LocalDateTime.now().minusYears(1))) {
            throw new RuntimeException("带看时间不能早于1年前");
        }
        // 3. DTO转实体
        VisitRecord visitRecord = VisitRecordConverter.convertAddDTOToEntity(addDTO);
        System.out.println(visitRecord);
        // 4. 执行插入
        int rows = visitRecordMapper.insert(visitRecord);
        if (rows != 1) {
            throw new RuntimeException("新增带看记录失败，请重试");
        }

        return visitRecord.getId();
    }

    /**
     * 根据ID查询带看记录
     */
    public VisitRecord getVisitRecordById(Long id) {
        // 1. 校验ID
        if (id == null || id <= 0) {
            throw new RuntimeException("带看记录ID无效（必须为正整数）");
        }

        // 2. 查询并校验存在性
        VisitRecord visitRecord = visitRecordMapper.selectById(id);
        if (visitRecord == null) {
            throw new RuntimeException("未找到ID为" + id + "的带看记录");
        }

        return visitRecord;
    }

    /**
     * 多条件分页查询带看记录
     */
    public PageResult<VisitRecord> getVisitRecordByCondition(VisitRecordQueryDTO queryDTO) {
        // 1. 处理空参数
        if (queryDTO == null) {
            queryDTO = new VisitRecordQueryDTO();
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
        Page<VisitRecord> visitRecordPage = visitRecordMapper.selectByCondition(queryDTO);

        // 4. 封装分页结果
        return getVisitRecordPageResult(visitRecordPage);
    }

    private PageResult<VisitRecord> getVisitRecordPageResult(Page<VisitRecord> visitRecordPage) {
        PageResult<VisitRecord> pageResult = new PageResult<>();
        pageResult.setList(visitRecordPage.getResult());
        pageResult.setPages(visitRecordPage.getPages());
        pageResult.setTotal(visitRecordPage.getTotal());
        pageResult.setPageSize(visitRecordPage.getPageSize());
        pageResult.setPageNum(visitRecordPage.getPageNum());

        return pageResult;
    }

    /**
     * 更新带看记录（如需更新关联ID，需重新校验存在性）
     */
    @Transactional
    public void updateVisitRecord(VisitRecordUpdateDTO updateDTO) {
        Long recordId = updateDTO.getId();
        // 1. 校验记录ID
        if (recordId == null || recordId <= 0) {
            throw new RuntimeException("带看记录ID无效（必须为正整数）");
        }

        // 2. 校验记录是否存在
        VisitRecord existing = visitRecordMapper.selectById(recordId);
        if (existing == null) {
            throw new RuntimeException("带看记录不存在（ID：" + recordId + "），无法更新");
        }

        // 3. 若更新关联ID（房源/客户/经纪人），需重新校验存在性
        if (updateDTO.getHouseId() != null && !updateDTO.getHouseId().equals(existing.getHouseId())) {
            validateHouseExists(updateDTO.getHouseId());
        }
        if (updateDTO.getCustomerId() != null && !updateDTO.getCustomerId().equals(existing.getCustomerId())) {
            validateCustomerExists(updateDTO.getCustomerId());
        }
        if (updateDTO.getAgentId() != null && !updateDTO.getAgentId().equals(existing.getAgentId())) {
            validateAgentExists(updateDTO.getAgentId());
        }

        // 4. 校验带看时间有效性（如更新）
        if (updateDTO.getVisitTime() != null && updateDTO.getVisitTime().isBefore(java.time.LocalDateTime.now().minusYears(1))) {
            throw new RuntimeException("带看时间不能早于1年前");
        }

        // 5. DTO转实体
        VisitRecord visitRecord = VisitRecordConverter.convertUpdateDTOToEntity(updateDTO);

        // 6. 执行更新
        int rows = visitRecordMapper.updateById(visitRecord);
        if (rows != 1) {
            throw new RuntimeException("更新带看记录失败，请重试");
        }
    }

    /**
     * 根据ID删除带看记录（删除前自动备份到visit_record_backup表）
     *
     * @param id 带看记录ID
     * @param operatorId 执行删除操作的用户ID（新增参数）
     */
    @Transactional
    public void deleteVisitRecord(Long id, Long operatorId) {
        // 1. 校验ID有效性
        if (id == null || id <= 0) {
            throw new RuntimeException("带看记录ID无效（必须为正整数）");
        }

        // 2. 校验操作人ID有效性
        if (operatorId == null || operatorId <= 0) {
            throw new RuntimeException("操作人ID无效（必须为正整数）");
        }

        // 3. 校验带看记录是否存在
        VisitRecord record = visitRecordMapper.selectById(id);
        if (record == null) {
            throw new RuntimeException("未找到ID为" + id + "的带看记录，无法删除");
        }

        // 4. 备份带看记录到备份表
        VisitRecordBackup backup = VisitRecordConverter.convertToBackup(record, operatorId);
        int backupRows = visitRecordBackupMapper.insert(backup);
        if (backupRows != 1) {
            throw new RuntimeException("带看记录备份失败，无法执行删除操作");
        }

        // 5. 执行删除（仅当备份成功后）
        int deleteRows = visitRecordMapper.deleteById(id);
        if (deleteRows != 1) {
            throw new RuntimeException("删除带看记录失败，请重试");
        }
    }


    /**
     * 一次性校验房源、客户、经纪人存在性
     */
    private void validateRelatedEntityExists(Long houseId, Long customerId, Long agentId) {
        validateHouseExists(houseId);
        validateCustomerExists(customerId);
        validateAgentExists(agentId);
    }

    /**
     * 校验房源存在性
     */
    private void validateHouseExists(Long houseId) {
        House house = houseMapper.selectById(houseId);
        if (house == null) {
            throw new RuntimeException("关联的房源不存在（ID：" + houseId + "）");
        }
    }

    /**
     * 校验客户存在性
     */
    private void validateCustomerExists(Long customerId) {
        Customer customer = customerMapper.selectById(customerId);
        if (customer == null) {
            throw new RuntimeException("关联的客户不存在（ID：" + customerId + "）");
        }
    }

    /**
     * 校验经纪人存在性
     */
    private void validateAgentExists(Long agentId) {
        // 假设Agent实体和Mapper已存在，此处调用其查询方法
        Object agent = agentMapper.selectById(agentId);
        if (agent == null) {
            throw new RuntimeException("关联的经纪人不存在（ID：" + agentId + "）");
        }
    }
}

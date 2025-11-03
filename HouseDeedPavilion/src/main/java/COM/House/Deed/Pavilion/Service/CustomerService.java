package COM.House.Deed.Pavilion.Service;

import COM.House.Deed.Pavilion.DTO.CustomerAddDTO;
import COM.House.Deed.Pavilion.DTO.CustomerQueryDTO;
import COM.House.Deed.Pavilion.DTO.CustomerUpdateDTO;
import COM.House.Deed.Pavilion.Entity.Customer;
import COM.House.Deed.Pavilion.Entity.CustomerBackup;
import COM.House.Deed.Pavilion.Mapper.CustomerBackupMapper;
import COM.House.Deed.Pavilion.Mapper.CustomerMapper;
import COM.House.Deed.Pavilion.Converter.CustomerConverter;
import COM.House.Deed.Pavilion.Utils.PageResult;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 客户信息服务类（已封装性别校验逻辑）
 */
@Service
public class CustomerService {

    @Resource
    private CustomerMapper customerMapper;

    @Resource
    private CustomerBackupMapper customerBackupMapper;

    /**
     * 新增客户信息
     */
    @Transactional
    public Long addCustomer(CustomerAddDTO addDTO) {
        // 1. 校验手机号唯一性
        Customer existByPhone = customerMapper.selectByPhone(addDTO.getPhone(), null);
        if (existByPhone != null) {
            throw new RuntimeException("手机号【" + addDTO.getPhone() + "】已被其他客户使用");
        }

        // 2. 校验性别合法性（调用封装的校验方法）
        validateGender(addDTO.getGender());

        // 3. 校验状态合法性
        if (addDTO.getStatus() != null && (addDTO.getStatus() < 1 || addDTO.getStatus() > 3)) {
            throw new RuntimeException("客户状态无效（必须为1-活跃、2-已成交或3-休眠）");
        }

        // 4. 转换DTO为实体
        Customer customer = CustomerConverter.convertAddDTOToEntity(addDTO);

        // 5. 执行插入
        int rows = customerMapper.insert(customer);
        if (rows != 1) {
            throw new RuntimeException("新增客户失败，请重试");
        }

        return customer.getId();
    }

    /**
     * 根据ID查询客户信息
     */
    public Customer getCustomerById(Long id) {
        if (id == null || id <= 0) {
            throw new RuntimeException("客户ID无效（必须为正整数）");
        }

        Customer customer = customerMapper.selectById(id);
        if (customer == null) {
            throw new RuntimeException("未找到ID为" + id + "的客户");
        }

        return customer;
    }

    /**
     * 多条件分页查询客户信息
     */
    public PageResult<Customer> getCustomerByCondition(CustomerQueryDTO queryDTO) {
        if (queryDTO == null) {
            queryDTO = new CustomerQueryDTO();
        }

        if (queryDTO.getPageNum() < 1) {
            queryDTO.setPageNum(1);
        }
        if (queryDTO.getPageSize() < 1 || queryDTO.getPageSize() > 100) {
            queryDTO.setPageSize(10);
        }

        PageHelper.startPage(queryDTO.getPageNum(), queryDTO.getPageSize());
        Page<Customer> customerPage = customerMapper.selectByCondition(queryDTO);

        return getHousePageResult(customerPage);
    }

    /**
     * 封装分页结果
     */
    private PageResult<Customer> getHousePageResult(Page<Customer> customerPage) {
        PageResult<Customer> pageResult = new PageResult<>();
        pageResult.setList(customerPage.getResult());
        pageResult.setTotal(customerPage.getTotal());
        pageResult.setPages(customerPage.getPages());
        pageResult.setPageNum(customerPage.getPageNum());
        pageResult.setPageSize(customerPage.getPageSize());
        return pageResult;
    }

    /**
     * 更新客户信息
     */
    @Transactional
    public void updateCustomer(CustomerUpdateDTO updateDTO) {
        Long customerId = updateDTO.getId();
        if (customerId == null || customerId <= 0) {
            throw new RuntimeException("客户ID无效（必须为正整数）");
        }

        Customer existing = customerMapper.selectById(customerId);
        if (existing == null) {
            throw new RuntimeException("客户不存在（ID：" + customerId + "），无法更新");
        }

        // 校验手机号唯一性（若更新）
        String newPhone = updateDTO.getPhone();
        if (newPhone != null && !newPhone.equals(existing.getPhone())) {
            Customer existByPhone = customerMapper.selectByPhone(newPhone, customerId);
            if (existByPhone != null) {
                throw new RuntimeException("手机号【" + newPhone + "】已被其他客户使用");
            }
        }

        // 校验性别合法性（调用封装的校验方法）
        validateGender(updateDTO.getGender());

        // 校验状态合法性
        if (updateDTO.getStatus() != null && (updateDTO.getStatus() < 1 || updateDTO.getStatus() > 3)) {
            throw new RuntimeException("客户状态无效（必须为1-活跃、2-已成交或3-休眠）");
        }

        // 转换DTO为实体
        Customer customer = CustomerConverter.convertUpdateDTOToEntity(updateDTO);

        // 执行更新
        int rows = customerMapper.updateById(customer);
        if (rows != 1) {
            throw new RuntimeException("更新客户失败，请重试");
        }
    }

    /**
     * 根据ID删除客户信息（自动备份）
     */
    @Transactional
    public void deleteCustomer(Long id, Long operatorId) {
        if (id == null || id <= 0) {
            throw new RuntimeException("客户ID无效（必须为正整数）");
        }

        if (operatorId == null || operatorId <= 0) {
            throw new RuntimeException("操作人ID无效（必须为正整数）");
        }

        Customer customer = customerMapper.selectById(id);
        if (customer == null) {
            throw new RuntimeException("未找到ID为" + id + "的客户，无法删除");
        }

        // 备份客户数据
        CustomerBackup backup = CustomerConverter.convertToBackup(customer, operatorId);
        int backupRows = customerBackupMapper.insert(backup);
        if (backupRows != 1) {
            throw new RuntimeException("客户数据备份失败，无法执行删除操作");
        }

        // 执行删除
        int deleteRows = customerMapper.deleteById(id);
        if (deleteRows != 1) {
            throw new RuntimeException("删除客户失败，请重试");
        }
    }


    /**
     * 校验性别合法性（1-男，2-女，null时不校验）
     * @param gender 性别值（可为null）
     */
    private void validateGender(Integer gender) {
        // 仅当gender不为null时才校验范围
        if (gender != null && (gender < 1 || gender > 2)) {
            throw new RuntimeException("性别无效（必须为1-男或2-女）");
        }
    }
}

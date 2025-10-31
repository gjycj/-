package COM.House.Deed.Pavilion.Mapper;

import COM.House.Deed.Pavilion.DTO.CustomerQueryDTO;
import COM.House.Deed.Pavilion.Entity.Customer;
import com.github.pagehelper.Page;
import org.apache.ibatis.annotations.Param;

/**
 * 客户信息Mapper接口（操作customer表）
 */
public interface CustomerMapper {

    /**
     * 新增客户信息
     *
     * @param customer 客户信息
     * @return 影响行数（1-成功，0-失败）
     */
    int insert(Customer customer);

    /**
     * 根据ID查询客户信息
     *
     * @param id 客户ID
     * @return 客户信息（null-未查询到）
     */
    Customer selectById(Long id);

    /**
     * 根据手机号查询客户信息（用于校验手机号唯一性）
     *
     * @param phone 联系电话
     * @param excludeId 排除的客户ID（更新时用，避免与自身冲突）
     * @return 客户信息（null-未查询到）
     */
    Customer selectByPhone(@Param("phone") String phone, @Param("excludeId") Long excludeId);

    /**
     * 多条件分页查询客户信息
     *
     * @param queryDTO 查询条件
     * @return 分页的客户信息列表
     */
    Page<Customer> selectByCondition(CustomerQueryDTO queryDTO);

    /**
     * 根据ID更新客户信息
     *
     * @param customer 包含ID和待更新字段的客户信息
     * @return 影响行数（1-成功，0-失败）
     */
    int updateById(Customer customer);

    /**
     * 根据ID删除客户信息（逻辑删除或物理删除，根据业务需求调整）
     *
     * @param id 客户ID
     * @return 影响行数（1-成功，0-失败）
     */
    int deleteById(Long id);
}

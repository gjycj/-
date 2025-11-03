package COM.House.Deed.Pavilion.Converter;

import COM.House.Deed.Pavilion.DTO.CustomerAddDTO;
import COM.House.Deed.Pavilion.DTO.CustomerUpdateDTO;
import COM.House.Deed.Pavilion.Entity.Customer;
import COM.House.Deed.Pavilion.Entity.CustomerBackup;

/**
 * 客户相关的DTO与实体转换工具类
 */
public class CustomerConverter {

    /**
     * 将CustomerAddDTO转换为Customer实体（新增场景）
     */
    public static Customer convertAddDTOToEntity(CustomerAddDTO addDTO) {
        Customer customer = new Customer();
        customer.setName(addDTO.getName());
        customer.setPhone(addDTO.getPhone());
        customer.setGender(addDTO.getGender());
        customer.setDemand(addDTO.getDemand());
        customer.setSource(addDTO.getSource());
        customer.setStatus(addDTO.getStatus());
        customer.setRemark(addDTO.getRemark());
        customer.setCreatedBy(addDTO.getCreatedBy());
        customer.setUpdatedBy(addDTO.getCreatedBy()); // 新增时更新人=创建人
        return customer;
    }

    /**
     * 将CustomerUpdateDTO转换为Customer实体（更新场景）
     */
    public static Customer convertUpdateDTOToEntity(CustomerUpdateDTO updateDTO) {
        Customer customer = new Customer();
        customer.setId(updateDTO.getId());
        customer.setName(updateDTO.getName());
        customer.setPhone(updateDTO.getPhone());
        customer.setGender(updateDTO.getGender());
        customer.setDemand(updateDTO.getDemand());
        customer.setSource(updateDTO.getSource());
        customer.setStatus(updateDTO.getStatus());
        customer.setRemark(updateDTO.getRemark());
        customer.setUpdatedBy(updateDTO.getUpdatedBy());
        return customer;
    }

    /**
     * 将Customer实体转换为CustomerBackup实体（删除备份场景）
     */
    public static CustomerBackup convertToBackup(Customer customer, Long operatorId) {
        CustomerBackup backup = new CustomerBackup();
        backup.setId(customer.getId());
        backup.setName(customer.getName());
        backup.setPhone(customer.getPhone());
        backup.setGender(customer.getGender());
        backup.setDemand(customer.getDemand());
        backup.setSource(customer.getSource());
        backup.setStatus(customer.getStatus());
        backup.setRemark(customer.getRemark());
        backup.setCreatedAt(customer.getCreatedAt());
        backup.setUpdatedAt(customer.getUpdatedAt());
        backup.setCreatedBy(customer.getCreatedBy());
        backup.setUpdatedBy(customer.getUpdatedBy());
        backup.setBackupBy(operatorId);
        return backup;
    }
}

package COM.House.Deed.Pavilion.Converter;

import COM.House.Deed.Pavilion.Entity.Landlord;
import COM.House.Deed.Pavilion.Entity.LandlordBackup;
import org.springframework.stereotype.Component;

/**
 * 房东实体与备份实体的转换工具类
 * 负责在删除房东时将原实体转为备份实体，以及从备份恢复时将备份实体转回原实体
 */
@Component
public class LandlordConverter {

    /**
     * 将原房东实体转换为备份实体（删除房东时调用）
     * 复制原实体的所有业务字段，设置关联的原房东ID，忽略备份特有字段（由Service层填充）
     */
    public LandlordBackup convertToBackup(Landlord originalLandlord) {
        if (originalLandlord == null) {
            return null;
        }

        LandlordBackup backup = new LandlordBackup();

        // 1. 关联原房东ID（核心关联字段）
        backup.setOriginalId(originalLandlord.getId());

        // 2. 复制原房东的业务字段（与数据库表字段一一对应）
        backup.setName(originalLandlord.getName());
        backup.setPhone(originalLandlord.getPhone());
        backup.setIdCard(originalLandlord.getIdCard());
        backup.setGender(originalLandlord.getGender());
        backup.setAddress(originalLandlord.getAddress());
        backup.setRemark(originalLandlord.getRemark());

        // 3. 复制原房东的创建/更新历史信息（保留删除前的原始操作记录）
        backup.setCreatedAt(originalLandlord.getCreatedAt());
        backup.setUpdatedAt(originalLandlord.getUpdatedAt());
        backup.setCreatedBy(originalLandlord.getCreatedBy());
        backup.setUpdatedBy(originalLandlord.getUpdatedBy());

        // 备份特有字段（backupTime/deleteReason/deletedBy）由Service层在创建备份时设置
        return backup;
    }

    /**
     * 将备份实体转换为原房东实体（恢复房东时调用）
     * 提取备份中的业务字段，重置ID和操作时间（使用当前操作信息）
     */
    public Landlord convertToOriginal(LandlordBackup backup) {
        if (backup == null) {
            return null;
        }

        Landlord original = new Landlord();

        // 1. 复制备份中的业务字段（恢复原始数据）
        original.setName(backup.getName());
        original.setPhone(backup.getPhone());
        original.setIdCard(backup.getIdCard());
        original.setGender(backup.getGender());
        original.setAddress(backup.getAddress());
        original.setRemark(backup.getRemark());

        // 2. 重置ID（恢复时生成新的主键，避免与现有数据冲突）
        original.setId(null);

        // 3. 操作时间和操作人由Service层在恢复时设置（使用当前时间和操作人）
        // 此处不设置，留给Service层处理：
        // original.setCreatedAt(LocalDateTime.now());
        // original.setUpdatedAt(LocalDateTime.now());
        // original.setCreatedBy(operatorId);
        // original.setUpdatedBy(operatorId);

        return original;
    }
}
package COM.House.Deed.Pavilion.Converter;

import COM.House.Deed.Pavilion.Entity.Property;
import COM.House.Deed.Pavilion.Entity.PropertyBackup;
import org.springframework.stereotype.Component;

/**
 * 楼盘实体与备份实体的转换工具类
 * 负责Property与PropertyBackup之间的字段映射
 */
@Component
public class PropertyConverter {

    /**
     * 原楼盘实体 → 备份实体（删除楼盘时调用，用于创建备份）
     * 复制原楼盘所有业务字段，并设置关联的原楼盘ID（originalId）
     */
    public PropertyBackup convertToBackup(Property original) {
        PropertyBackup backup = new PropertyBackup();

        // 1. 设置关联的原楼盘ID（核心关联字段）
        backup.setOriginalId(original.getId());

        // 2. 复制原楼盘的所有业务字段（与property表字段一一对应）
        backup.setName(original.getName());
        backup.setAddress(original.getAddress());
        backup.setDeveloper(original.getDeveloper());
        backup.setPropertyType(original.getPropertyType());
        backup.setCompletionYear(original.getCompletionYear());
        backup.setPropertyCompany(original.getPropertyCompany());
        backup.setPropertyFee(original.getPropertyFee());
        backup.setDescription(original.getDescription());
        backup.setLongitude(original.getLongitude());
        backup.setLatitude(original.getLatitude());

        // 3. 复制原楼盘的创建/更新信息（保留历史操作记录）
        backup.setCreatedAt(original.getCreatedAt());
        backup.setUpdatedAt(original.getUpdatedAt());
        backup.setCreatedBy(original.getCreatedBy());
        backup.setUpdatedBy(original.getUpdatedBy());

        // 注意：备份专用的特有字段（backupTime/deleteReason/deletedBy）由Service层设置，此处不处理
        return backup;
    }

    /**
     * 备份实体 → 原楼盘实体（恢复楼盘时调用，用于从备份重建原数据）
     * 仅复制备份中的原楼盘业务字段，忽略备份备份特有字段
     */
    public Property convertToOriginal(PropertyBackup backup) {
        Property original = new Property();

        // 复制备份中保留的原楼盘业务字段（与property表字段一一对应）
        original.setName(backup.getName());
        original.setAddress(backup.getAddress());
        original.setDeveloper(backup.getDeveloper());
        original.setPropertyType(backup.getPropertyType());
        original.setCompletionYear(backup.getCompletionYear());
        original.setPropertyCompany(backup.getPropertyCompany());
        original.setPropertyFee(backup.getPropertyFee());
        original.setDescription(backup.getDescription());
        original.setLongitude(backup.getLongitude());
        original.setLatitude(backup.getLatitude());

        // 注意：原楼盘的创建/更新时间、操作人在恢复时由Service层重新设置（取当前操作信息）
        // 此处不复制backup的createdAt/updatedAt/createdBy/updatedBy，避免历史信息覆盖新操作
        return original;
    }
}
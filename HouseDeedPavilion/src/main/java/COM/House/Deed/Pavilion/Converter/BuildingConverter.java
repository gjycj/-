package COM.House.Deed.Pavilion.Converter;

import COM.House.Deed.Pavilion.Entity.Building;
import COM.House.Deed.Pavilion.Entity.BuildingBackup;
import org.springframework.stereotype.Component;

/**
 * 楼栋实体与备份实体的转换工具类
 */
@Component
public class BuildingConverter {

    /**
     * 原楼栋实体 → 备份实体（复制原字段+设置originalId）
     */
    public BuildingBackup convertToBackup(Building original) {
        BuildingBackup backup = new BuildingBackup();
        // 1. 关联原楼栋ID
        backup.setOriginalId(original.getId());
        // 2. 复制原楼栋业务字段
        backup.setPropertyId(original.getPropertyId());
        backup.setName(original.getName());
        backup.setTotalFloor(original.getTotalFloor());
        backup.setUnitCount(original.getUnitCount());
        backup.setDescription(original.getDescription());
        backup.setCreatedAt(original.getCreatedAt());
        backup.setUpdatedAt(original.getUpdatedAt());
        backup.setCreatedBy(original.getCreatedBy());
        backup.setUpdatedBy(original.getUpdatedBy());
        // 备份专用字段（backupTime/deleteReason/deletedBy在Service中设置）
        return backup;
    }

    /**
     * 备份实体 → 原楼栋实体（恢复时使用）
     */
    public Building convertToOriginal(BuildingBackup backup) {
        Building original = new Building();
        // 复制备份中的原楼栋业务字段（忽略备份专用字段）
        original.setPropertyId(backup.getPropertyId());
        original.setName(backup.getName());
        original.setTotalFloor(backup.getTotalFloor());
        original.setUnitCount(backup.getUnitCount());
        original.setDescription(backup.getDescription());
        // 注意：创建/更新时间、操作人在恢复时由Service重新设置
        return original;
    }
}
package COM.House.Deed.Pavilion.Converter;

import COM.House.Deed.Pavilion.Entity.House;
import COM.House.Deed.Pavilion.Entity.HouseBackup;

import java.time.LocalDateTime;

/**
 * 房源相关实体转换工具类
 * 负责DTO与House实体、House与HouseBackup实体的转换
 */
public class HouseConverter {

    /**
     * 将House实体转换为HouseBackup实体（用于房源删除备份场景）
     * 完整复制原实体字段，并补充备份专用信息
     *
     * @param original     原房源实体
     * @param operatorId   执行删除的操作人ID
     * @param deleteReason 删除原因
     * @return 转换后的HouseBackup实体
     */
    public static HouseBackup convertHouseToBackup(House original, Long operatorId, String deleteReason) {
        if (original == null) {
            return null;
        }

        HouseBackup backup = new HouseBackup();
        // 备份标识（关联原房源ID）
        backup.setOriginalId(original.getId());

        // 原房源所有字段完整复制
        backup.setBuildingId(original.getBuildingId());
        backup.setHouseNumber(original.getHouseNumber());
        backup.setLayout(original.getLayout());
        backup.setArea(original.getArea());
        backup.setInnerArea(original.getInnerArea());
        backup.setFloor(original.getFloor());
        backup.setTotalFloor(original.getTotalFloor());
        backup.setOrientation(original.getOrientation());
        backup.setDecoration(original.getDecoration());

        // 枚举类型直接复用（原实体已为枚举对象）
        backup.setHouseType(original.getHouseType());
        backup.setStatus(original.getStatus());

        // 价格与描述信息
        backup.setPrice(original.getPrice());
        backup.setOwnerPrice(original.getOwnerPrice());
        backup.setDescription(original.getDescription());

        // 关联信息
        backup.setAgentId(original.getAgentId());

        // 原实体审计字段
        backup.setIsValid(original.getIsValid());
        backup.setCreatedAt(original.getCreatedAt());
        backup.setUpdatedAt(original.getUpdatedAt());
        backup.setCreatedBy(original.getCreatedBy());
        backup.setUpdatedBy(original.getUpdatedBy());

        // 备份专用字段
        backup.setDeleteReason(deleteReason);
        backup.setDeletedAt(LocalDateTime.now()); // 删除时间为当前时间
        backup.setDeletedBy(operatorId);          // 执行删除的操作人
        backup.setBackupTime(LocalDateTime.now());// 备份生成时间

        return backup;
    }

    /**
     * 将HouseBackup备份实体转换为House实体（用于房源恢复）
     * 从备份中提取原房源信息，重置部分审计字段
     *
     * @param backup 房源备份实体
     * @return 转换后的House实体（可直接用于恢复到主表）
     */
    public static House convertBackupToHouse(HouseBackup backup) {
        if (backup == null) {
            return null;
        }

        House house = new House();
        // 1. 基础属性从备份复制（复用原房源的核心信息）
        house.setId(backup.getOriginalId()); // 恢复时使用原房源ID（关键）
        house.setBuildingId(backup.getBuildingId());
        house.setHouseNumber(backup.getHouseNumber());
        house.setLayout(backup.getLayout());
        house.setArea(backup.getArea());
        house.setInnerArea(backup.getInnerArea()); // 补充套内面积
        house.setFloor(backup.getFloor());
        house.setTotalFloor(backup.getTotalFloor()); // 补充总楼层
        house.setOrientation(backup.getOrientation()); // 补充朝向
        house.setDecoration(backup.getDecoration()); // 补充装修情况
        house.setHouseType(backup.getHouseType()); // 枚举类型直接复用
        house.setPrice(backup.getPrice());
        house.setOwnerPrice(backup.getOwnerPrice()); // 补充业主底价
        house.setStatus(backup.getStatus()); // 枚举类型直接复用
        house.setDescription(backup.getDescription());
        house.setAgentId(backup.getAgentId()); // 补充经纪人ID

        // 2. 审计字段处理（恢复时的特殊逻辑）
        house.setIsValid(1); // 恢复后设为有效状态
        house.setCreatedAt(backup.getCreatedAt()); // 保留原创建时间
        house.setCreatedBy(backup.getCreatedBy()); // 保留原创建人
        house.setUpdatedAt(LocalDateTime.now()); // 更新时间为恢复操作时间
        // updatedBy由恢复操作人在Service层设置（此处不处理）

        return house;
    }



}
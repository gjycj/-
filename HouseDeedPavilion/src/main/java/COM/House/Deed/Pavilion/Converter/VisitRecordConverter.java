package COM.House.Deed.Pavilion.Converter;

import COM.House.Deed.Pavilion.DTO.VisitRecordAddDTO;
import COM.House.Deed.Pavilion.DTO.VisitRecordUpdateDTO;
import COM.House.Deed.Pavilion.Entity.VisitRecord;
import COM.House.Deed.Pavilion.Entity.VisitRecordBackup;

/**
 * 带看记录DTO与实体的转换工具类
 */
public class VisitRecordConverter {

    /**
     * 将VisitRecordAddDTO转换为VisitRecord实体（新增场景）
     */
    public static VisitRecord convertAddDTOToEntity(VisitRecordAddDTO addDTO) {
        VisitRecord visitRecord = new VisitRecord();
        visitRecord.setHouseId(addDTO.getHouseId());
        visitRecord.setCustomerId(addDTO.getCustomerId());
        visitRecord.setAgentId(addDTO.getAgentId());
        visitRecord.setVisitTime(addDTO.getVisitTime());
        visitRecord.setFeedback(addDTO.getFeedback());
        visitRecord.setNextPlan(addDTO.getNextPlan());
        visitRecord.setCreatedBy(addDTO.getCreatedBy());
        visitRecord.setUpdatedBy(addDTO.getCreatedBy()); // 新增时更新人=创建人
        return visitRecord;
    }

    /**
     * 将VisitRecordUpdateDTO转换为VisitRecord实体（更新场景）
     */
    public static VisitRecord convertUpdateDTOToEntity(VisitRecordUpdateDTO updateDTO) {
        VisitRecord visitRecord = new VisitRecord();
        visitRecord.setId(updateDTO.getId());
        visitRecord.setHouseId(updateDTO.getHouseId());
        visitRecord.setCustomerId(updateDTO.getCustomerId());
        visitRecord.setAgentId(updateDTO.getAgentId());
        visitRecord.setVisitTime(updateDTO.getVisitTime());
        visitRecord.setFeedback(updateDTO.getFeedback());
        visitRecord.setNextPlan(updateDTO.getNextPlan());
        visitRecord.setUpdatedBy(updateDTO.getUpdatedBy());
        return visitRecord;
    }

    /**
     * 新增：将VisitRecord实体转换为VisitRecordBackup实体（删除备份场景）
     */
    public static VisitRecordBackup convertToBackup(VisitRecord record, Long operatorId) {
        VisitRecordBackup backup = new VisitRecordBackup();
        // 复制原记录的所有字段
        backup.setId(record.getId());
        backup.setHouseId(record.getHouseId());
        backup.setCustomerId(record.getCustomerId());
        backup.setAgentId(record.getAgentId());
        backup.setVisitTime(record.getVisitTime());
        backup.setFeedback(record.getFeedback());
        backup.setNextPlan(record.getNextPlan());
        backup.setCreatedAt(record.getCreatedAt());
        backup.setUpdatedAt(record.getUpdatedAt());
        backup.setCreatedBy(record.getCreatedBy());
        backup.setUpdatedBy(record.getUpdatedBy());
        // 设置备份人（执行删除的用户）
        backup.setBackupBy(operatorId);
        return backup;
    }
}

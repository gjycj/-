package com.house.deed.pavilion.module.house.vo;

import com.house.deed.pavilion.module.contract.entity.Contract;
import com.house.deed.pavilion.module.maintenanceOrder.entity.MaintenanceOrder;
import com.house.deed.pavilion.module.visitRecord.entity.VisitRecord;
import lombok.Data;

import java.util.List;

@Data
public class HouseLifecycleVO {
    private Long houseId;
    private List<VisitRecord> visitRecords; // 带看记录
    private List<Contract> contracts; // 合同记录
    private List<MaintenanceOrder> maintenanceOrders; // 维修工单
}
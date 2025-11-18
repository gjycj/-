package com.house.deed.pavilion.module.customer.vo;

import com.house.deed.pavilion.module.contract.entity.Contract;
import com.house.deed.pavilion.module.customerFollowUp.entity.CustomerFollowUp;
import com.house.deed.pavilion.module.visitRecord.entity.VisitRecord;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CustomerFullFlowVO {
    // 客户基本信息
    private Long customerId;
    private String customerName;
    private String phone;
    private String status;
    private LocalDateTime createTime;

    // 关联数据
    private List<VisitRecord> visitRecords;       // 带看记录
    private List<Contract> contracts;             // 合同记录
    private List<CustomerFollowUp> followUps;     // 跟进记录
}
package com.house.deed.pavilion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/*
房源录入 → 客户管理 → 带看记录 → 合同签订 → 房屋交接 → 后续维护（维修/纠纷）
       ↓          ↓           ↓           ↓           ↓
  房源状态管理  客户跟进记录  带看-合同关联  合同状态流转  维修工单/投诉纠纷
*/
@SpringBootApplication
public class HouseApplication {

    public static void main(String[] args) {
         SpringApplication.run(HouseApplication.class, args);
    }
}
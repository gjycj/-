package com.house.deed.pavilion;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

//房源录入 → 客户管理 → 带看记录 → 合同签订 → 后续维护（维修、交接等）
@SpringBootApplication
@MapperScan("com.house.deed.pavilion.module.**.mapper")
public class HouseApplication {

    public static void main(String[] args) {
         SpringApplication.run(HouseApplication.class, args);
    }
}
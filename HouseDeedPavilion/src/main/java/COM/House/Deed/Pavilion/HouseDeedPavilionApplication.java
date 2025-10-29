package COM.House.Deed.Pavilion;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("COM.House.Deed.Pavilion.Mapper")
public class HouseDeedPavilionApplication {

    public static void main(String[] args) {
        SpringApplication.run(HouseDeedPavilionApplication.class, args);
    }

}

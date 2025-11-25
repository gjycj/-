import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.engine.VelocityTemplateEngine;
import java.util.Collections;

public class CodeGenerator {

    // 数据库连接配置（请根据实际情况修改）
    private static final String DB_URL = "jdbc:mysql://192.168.1.4:3306/house_deed_pavilion?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true";
    private static final String DB_USERNAME = "root"; // 如 root
    private static final String DB_PASSWORD = "123456"; // 如 123456

    // 项目基础配置（请根据实际项目修改）
    private static final String PROJECT_PATH = System.getProperty("user.dir"); // 项目根路径
    private static final String PACKAGE_NAME = "com.house.deed.pavilion"; // 基础包名（如 com.example.demo）
    private static final String AUTHOR = "yuquanxi"; // 代码作者（注释用）
    private static final String TABLE_PREFIX = ""; // 表前缀（如 t_，生成实体时会去掉前缀，无则留空）

    public static void main(String[] args) {
        // 1. 数据源配置
        FastAutoGenerator.create(DB_URL, DB_USERNAME, DB_PASSWORD)
                // 2. 全局配置
                .globalConfig(builder -> {
                    builder.outputDir(PROJECT_PATH + "/src/main/java") // 代码输出目录（java 代码）
                            .author(AUTHOR) // 作者名
                            .enableSwagger() // 开启 Swagger 注释（需引入 swagger 依赖，可选）
                            .fileOverride() // 覆盖已有文件
                            .disableOpenDir(); // 生成后不打开文件夹
                })
                // 3. 包配置（标准目录结构）
                .packageConfig(builder -> {
                    builder.parent(PACKAGE_NAME) // 基础包名
                            .entity("entity") // 实体类包名
                            .mapper("mapper") // Mapper 接口包名
                            .service("service") // Service 接口包名
                            .serviceImpl("service.impl") // Service 实现类包名
                            .controller("controller") // Controller 包名
                            .xml("mapper.xml") // Mapper XML 文件包名（resources 目录下）
                            .pathInfo(Collections.singletonMap(OutputFile.xml,
                                    PROJECT_PATH + "/src/main/resources/mapper")); // XML 文件输出目录
                })
                // 4. 策略配置（生成规则）
                .strategyConfig(builder -> {
                    builder.addInclude(
                                    "agent",
                                    "agent_backup",
                                    "agent_performance",
                                    "building",
                                    "commission_rule",
                                    "complaint_dispute",
                                    "contract",
                                    "contract_attachment",
                                    "contract_lease_terms",
                                    "customer",
                                    "customer_backup",
                                    "customer_follow_up",
                                    "customer_history_deal",
                                    "dispute_handle_log",
                                    "electronic_sign",
                                    "flyway_schema_history",
                                    "house",
                                    "house_backup",
                                    "house_handover",
                                    "house_image",
                                    "house_landlord",
                                    "house_maintain_plan",
                                    "house_price_log",
                                    "house_status_log",
                                    "house_tag",
                                    "landlord",
                                    "landlord_entrust",
                                    "loan_info",
                                    "loan_material",
                                    "maintenance_order",
                                    "operation_log",
                                    "property",
                                    "region",
                                    "store",
                                    "tag",
                                    "tenant",
                                    "tenant_config",
                                    "transaction_fee",
                                    "visit_record"
                            ) // 需要生成的表名（多个表用逗号分隔）
                            .addTablePrefix(TABLE_PREFIX) // 去掉表前缀
                            // 实体类策略
                            .entityBuilder()
                            .enableLombok() // 开启 Lombok 注解（无需手动写 getter/setter）
                            .enableTableFieldAnnotation() // 为字段添加 @TableField 注解
                            .idType(IdType.AUTO) // 主键自增
                            .logicDeleteColumnName("is_deleted") // 逻辑删除字段名（可选，需数据库有该字段）
                            .logicDeletePropertyName("deleted") // 逻辑删除属性名
                            // Mapper 策略
                            .mapperBuilder()
                            .enableBaseResultMap() // 开启 BaseResultMap（XML 中生成结果映射）
                            .enableBaseColumnList() // 开启 BaseColumnList（XML 中生成查询字段列表）
                            .superClass(com.baomidou.mybatisplus.core.mapper.BaseMapper.class) // 继承 BaseMapper
                            // Service 策略
                            .serviceBuilder()
                            .formatServiceFileName("%sService") // Service 接口命名格式（如 UserService）
                            .formatServiceImplFileName("%sServiceImpl") // Service 实现类命名格式（如 UserServiceImpl）
                            // Controller 策略
                            .controllerBuilder()
                            .enableRestStyle() // 开启 RestController 注解（RESTful 风格）
                            .enableHyphenStyle(); // 接口路径驼峰转连字符（如 userInfo -> user-info）
                })
                // 5. 模板引擎配置（默认 Velocity）
                .templateEngine(new VelocityTemplateEngine())
                // 6. 执行生成
                .execute();
    }
}
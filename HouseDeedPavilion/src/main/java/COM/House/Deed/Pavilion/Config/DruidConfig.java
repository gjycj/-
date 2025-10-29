package COM.House.Deed.Pavilion.Config;


import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.support.jakarta.StatViewServlet;
import com.alibaba.druid.support.jakarta.WebStatFilter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class DruidConfig {

    // 将配置文件中的spring.datasource属性绑定到DruidDataSource
    @Bean
    @ConfigurationProperties("spring.datasource")
    public DataSource druidDataSource() {
        return new DruidDataSource();
    }

    // 配置Druid监控界面的Servlet
    @Bean
    public ServletRegistrationBean<StatViewServlet> statViewServlet() {
        ServletRegistrationBean<StatViewServlet> bean =
                new ServletRegistrationBean<>(new StatViewServlet(), "/druid/*");
        Map<String, String> initParams = new HashMap<>();
        initParams.put("loginUsername", "admin"); // 监控页面登录账号
        initParams.put("loginPassword", "admin"); // 监控页面登录密码
        initParams.put("allow", ""); // 默认允许所有访问
        // initParams.put("deny", "192.168.1.100"); // 配置黑名单
        bean.setInitParameters(initParams);
        return bean;
    }

    // 配置Druid的Web统计Filter
    @Bean
    public FilterRegistrationBean<WebStatFilter> webStatFilter() {
        FilterRegistrationBean<WebStatFilter> bean =
                new FilterRegistrationBean<>(new WebStatFilter());
        bean.setUrlPatterns(List.of("/*"));
        Map<String, String> initParams = new HashMap<>();
        initParams.put("exclusions", "*.js,*.gif,*.jpg,*.png,*.css,*.ico,/druid/*");
        bean.setInitParameters(initParams);
        return bean;
    }
}
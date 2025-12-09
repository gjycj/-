package com.house.deed.pavilion.config;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

/**
 * MyBatis Plus 测试基类，解决 TableInfoCache 初始化问题
 */
@ExtendWith(MockitoExtension.class)
public abstract class BaseMyBatisPlusTest {

    @Mock
    protected SqlSessionFactory sqlSessionFactory;

    @BeforeEach
    void initMyBatisPlus() {
        // 创建一个模拟的 MybatisConfiguration
        MybatisConfiguration configuration = new MybatisConfiguration();
        GlobalConfig globalConfig = GlobalConfigUtils.defaults();

        // 设置元对象处理器
        globalConfig.setMetaObjectHandler(new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                // 设置创建时间
                if (metaObject.hasSetter("createTime")) {
                    metaObject.setValue("createTime", LocalDateTime.now());
                }
                if (metaObject.hasSetter("updateTime")) {
                    metaObject.setValue("updateTime", LocalDateTime.now());
                }
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                // 设置更新时间
                if (metaObject.hasSetter("updateTime")) {
                    metaObject.setValue("updateTime", LocalDateTime.now());
                }
            }
        });

        GlobalConfigUtils.setGlobalConfig(configuration, globalConfig);
        configuration.addInterceptor(new MybatisPlusInterceptor());
    }

    /**
     * 为 Service 注入必要的依赖
     */
    protected void injectMyBatisPlusDependencies(Object service, Object mapper) {
        try {
            // 设置 baseMapper
            ReflectionTestUtils.setField(service, "baseMapper", mapper);
            ReflectionTestUtils.setField(service, "mapperClass", mapper.getClass());
            ReflectionTestUtils.setField(service, "sqlSessionFactory", sqlSessionFactory);
        } catch (Exception e) {
            // 忽略，可能有些字段不存在
        }
    }
}
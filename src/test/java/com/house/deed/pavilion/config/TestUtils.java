package com.house.deed.pavilion.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import static org.mockito.ArgumentMatchers.any;

public class TestUtils {

    /**
     * 安全的 any() 匹配器，避免 MyBatis Plus Lambda 缓存问题
     */
    public static <T> LambdaQueryWrapper<T> anyLambdaQueryWrapper() {
        return any(LambdaQueryWrapper.class);
    }
}

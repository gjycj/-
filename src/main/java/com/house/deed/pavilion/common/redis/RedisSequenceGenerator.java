package com.house.deed.pavilion.common.redis;

import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * 序列号生成器（基于Redis）
 */
@Component
public class RedisSequenceGenerator {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 获取每日自增序号
     * @param prefix 业务前缀（如"contract"）
     * @param tenantId 租户ID
     * @return 当日序号（1开始，每日重置）
     */
    public int getDailySequence(String prefix, Long tenantId) {
        // 键格式：seq:{租户ID}:{业务}:{日期}
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String key = String.format("seq:%d:%s:%s", tenantId, prefix, date);

        // 自增并设置24小时过期（确保每日重置）
        Long sequence = stringRedisTemplate.opsForValue().increment(key);
        if (sequence != null && sequence == 1) {
            stringRedisTemplate.expire(key, 24, TimeUnit.HOURS);
        }
        return sequence.intValue();
    }
}
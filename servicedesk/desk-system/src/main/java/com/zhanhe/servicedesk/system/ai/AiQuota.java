package com.zhanhe.servicedesk.system.ai;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 每日调用配额（Redis 原子计数）。
 *
 * <p>为什么需要：AI 接口是花钱的，且可被脚本刷。没有配额等于把账单交给别人。
 * 用 INCR 保证并发下不超发；首次计数时设置 25 小时过期，避免脏 key 堆积。
 * Redis 不可用时选择「放行」——配额是成本保护，不是业务正确性依赖。</p>
 */
@Component
public class AiQuota
{
    private static final Logger log = LoggerFactory.getLogger(AiQuota.class);

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final StringRedisTemplate redisTemplate;

    private final AiSettings settings;

    public AiQuota(StringRedisTemplate redisTemplate, AiSettings settings)
    {
        this.redisTemplate = redisTemplate;
        this.settings = settings;
    }

    /**
     * @return true 表示允许本次调用
     */
    public boolean tryAcquire(String userName)
    {
        int limit = settings.snapshot().getDailyLimit();
        if (limit <= 0)
        {
            return true;
        }
        String key = "ai:quota:" + (userName == null || userName.isBlank() ? "anonymous" : userName)
                + ":" + LocalDate.now().format(DAY);
        try
        {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L)
            {
                redisTemplate.expire(key, Duration.ofHours(25));
            }
            return count == null || count <= limit;
        }
        catch (Exception e)
        {
            log.warn("[ai] quota check failed, allow by default: {}", e.toString());
            return true;
        }
    }

    public int remaining(String userName)
    {
        int limit = settings.snapshot().getDailyLimit();
        if (limit <= 0)
        {
            return -1;
        }
        String key = "ai:quota:" + (userName == null || userName.isBlank() ? "anonymous" : userName)
                + ":" + LocalDate.now().format(DAY);
        try
        {
            String value = redisTemplate.opsForValue().get(key);
            int used = value == null ? 0 : Integer.parseInt(value);
            return Math.max(0, limit - used);
        }
        catch (Exception e)
        {
            return -1;
        }
    }
}

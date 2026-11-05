package com.zhanhe.servicedesk.system.ai;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;

/**
 * 简易熔断器。
 *
 * <p>解决的问题：厂商侧故障时，如果每个请求都老老实实等满 8 秒超时，
 * 用户体验会被"每次都卡 8 秒再降级"毁掉。连续失败到阈值后直接短路，
 * 一段时间内不再尝试，等窗口过去自动恢复（半开）。</p>
 */
@Component
public class AiBreaker
{
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);

    private volatile long openUntilMillis = 0L;

    private final AiProperties properties;

    public AiBreaker(AiProperties properties)
    {
        this.properties = properties;
    }

    public boolean isOpen()
    {
        return System.currentTimeMillis() < openUntilMillis;
    }

    public void recordSuccess()
    {
        consecutiveFailures.set(0);
        openUntilMillis = 0L;
    }

    public void recordFailure()
    {
        int failures = consecutiveFailures.incrementAndGet();
        if (failures >= Math.max(1, properties.getBreakerThreshold()))
        {
            openUntilMillis = System.currentTimeMillis() + Math.max(1, properties.getBreakerSeconds()) * 1000L;
            consecutiveFailures.set(0);
        }
    }

    public long openRemainSeconds()
    {
        long remain = openUntilMillis - System.currentTimeMillis();
        return remain > 0 ? remain / 1000 : 0;
    }
}

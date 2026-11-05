package com.zhanhe.servicedesk.system.ai;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * 熔断器：连续失败到阈值后短路，成功一次即恢复。
 */
class AiBreakerTest
{
    private AiBreaker breaker(int threshold, int seconds)
    {
        AiProperties properties = new AiProperties();
        properties.setBreakerThreshold(threshold);
        properties.setBreakerSeconds(seconds);
        return new AiBreaker(properties);
    }

    @Test
    void opensAfterConfiguredFailures()
    {
        AiBreaker breaker = breaker(2, 60);
        assertFalse(breaker.isOpen());
        breaker.recordFailure();
        assertFalse(breaker.isOpen(), "未达阈值不应熔断");
        breaker.recordFailure();
        assertTrue(breaker.isOpen(), "达到阈值应熔断");
        assertTrue(breaker.openRemainSeconds() > 0);
    }

    @Test
    void successResetsFailures()
    {
        AiBreaker breaker = breaker(1, 60);
        breaker.recordFailure();
        assertTrue(breaker.isOpen());
        breaker.recordSuccess();
        assertFalse(breaker.isOpen());
    }
}

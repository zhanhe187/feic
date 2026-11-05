package com.zhanhe.servicedesk.system.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PriorityRuleTest
{
    @Test
    void blockingImpactSuggestsHigh()
    {
        assertEquals(PriorityRule.PRIORITY_HIGH, PriorityRule.suggest(SlotSchema.IMPACT_BLOCKING, "无法工作"));
    }

    @Test
    void multiImpactSuggestsMedium()
    {
        assertEquals(PriorityRule.PRIORITY_MEDIUM, PriorityRule.suggest(SlotSchema.IMPACT_MULTI, "同事们都说连不上"));
    }

    @Test
    void singleImpactSuggestsLow()
    {
        assertEquals(PriorityRule.PRIORITY_LOW, PriorityRule.suggest(SlotSchema.IMPACT_SINGLE, "我的键盘不好用"));
    }

    @Test
    void unknownImpactSuggestsLowToAvoidInflation()
    {
        // 信息不足时宁可低估由 IT 上调，也不要一味拉高造成优先级通胀
        assertEquals(PriorityRule.PRIORITY_LOW, PriorityRule.suggest(null, ""));
        assertEquals(PriorityRule.PRIORITY_LOW, PriorityRule.suggest("not-a-value", "很急很急"));
    }

    @Test
    void priorityWhitelist()
    {
        assertTrue(PriorityRule.isValid("urgent"));
        assertFalse(PriorityRule.isValid("critical"));
    }
}

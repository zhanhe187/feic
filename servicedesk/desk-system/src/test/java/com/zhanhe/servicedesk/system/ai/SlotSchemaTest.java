package com.zhanhe.servicedesk.system.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SlotSchemaTest
{
    @Test
    void impactWhitelistFallsBackToUnknown()
    {
        assertEquals(SlotSchema.IMPACT_BLOCKING, SlotSchema.normalizeImpact("BLOCKING"));
        assertEquals(SlotSchema.IMPACT_UNKNOWN, SlotSchema.normalizeImpact("urgent"));
        assertEquals(SlotSchema.IMPACT_UNKNOWN, SlotSchema.normalizeImpact(null));
    }

    @Test
    void typeWhitelistAcceptsChineseAndEnglish()
    {
        assertEquals(SlotSchema.TYPE_DEVICE, SlotSchema.normalizeType("设备故障"));
        assertEquals(SlotSchema.TYPE_NETWORK, SlotSchema.normalizeType("network"));
        assertEquals(SlotSchema.TYPE_OTHER, SlotSchema.normalizeType("unknown-type"));
        assertEquals(SlotSchema.TYPE_OTHER, SlotSchema.normalizeType(null));
    }

    @Test
    void sourceWhitelistRejectsIllegalValue()
    {
        assertEquals(SlotSchema.SOURCE_LLM, SlotSchema.normalizeSource("LLM"));
        assertNull(SlotSchema.normalizeSource("hacker"));
        assertNull(SlotSchema.normalizeSource(null));
    }

    @Test
    void truncateKeepsCodePointsIntact()
    {
        assertEquals("ab", SlotSchema.truncate("  ab  ", 10));
        assertNull(SlotSchema.truncate("   ", 10));
        String cut = SlotSchema.truncate("你好世界abc", 2);
        assertEquals(2, cut.codePointCount(0, cut.length()));
    }

    @Test
    void valuesMatchFrontendContract()
    {
        assertTrue(SlotSchema.IMPACT_SCOPES.contains("single"));
        assertTrue(SlotSchema.TICKET_TYPES.contains("网络问题"));
        assertFalse(SlotSchema.TICKET_TYPES.contains("网络"));
    }
}

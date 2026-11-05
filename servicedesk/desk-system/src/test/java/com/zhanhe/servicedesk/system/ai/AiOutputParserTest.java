package com.zhanhe.servicedesk.system.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * 模型输出的解析与「防编造」校验。
 * 这是 AI 落库前的最后一道闸门，用例覆盖：正常、带围栏、编造值、非法 JSON、缺字段。
 */
class AiOutputParserTest
{
    private final AiOutputParser parser = new AiOutputParser();

    private static final String SOURCE = "电脑开不了机。我这台 ThinkPad T14 按电源键没反应，重启了好几次都不行，现在没法干活了";

    @Test
    void parsesNormalJsonAndDerivesPriorityFromRule()
    {
        String raw = "{\"device\":\"ThinkPad T14\",\"symptom\":\"按电源键没反应\","
                + "\"impact_scope\":\"blocking\",\"tried_actions\":\"重启了好几次都不行\","
                + "\"ticket_type\":\"设备故障\",\"type_basis\":\"电源无反应\",\"priority\":\"urgent\"}";
        IntakeResult result = parser.parse(raw, SOURCE);

        assertEquals(SlotSchema.SOURCE_LLM, result.getSource());
        assertEquals("ThinkPad T14", result.getDeviceInfo());
        assertEquals(SlotSchema.IMPACT_BLOCKING, result.getImpactScope());
        assertEquals(SlotSchema.TYPE_DEVICE, result.getSuggestedType());
        // 模型即便返回了 priority 也不采纳，优先级一律由规则推导
        assertEquals(PriorityRule.PRIORITY_HIGH, result.getSuggestedPriority());
        assertTrue(result.getRejected().isEmpty());
    }

    @Test
    void stripsMarkdownFence()
    {
        String raw = "```json\n{\"device\":\"ThinkPad T14\",\"impact_scope\":\"single\"}\n```";
        IntakeResult result = parser.parse(raw, SOURCE);
        assertEquals("ThinkPad T14", result.getDeviceInfo());
        assertEquals(SlotSchema.IMPACT_SINGLE, result.getImpactScope());
    }

    @Test
    void dropsFabricatedValueAndRecordsRejected()
    {
        String raw = "{\"device\":\"Dell XPS 15 顶配\",\"symptom\":\"按电源键没反应\"}";
        IntakeResult result = parser.parse(raw, SOURCE);

        assertNull(result.getDeviceInfo(), "原文没有 Dell XPS，必须丢弃");
        assertTrue(result.getRejected().contains("device"));
        assertNotNull(result.getSymptom());
    }

    @Test
    void rejectsInvalidJson()
    {
        assertThrows(AiUnavailableException.class, () -> parser.parse("抱歉，我无法处理", SOURCE));
        assertThrows(AiUnavailableException.class, () -> parser.parse("{device: thinkpad}", SOURCE));
        assertThrows(AiUnavailableException.class, () -> parser.parse(null, SOURCE));
    }

    @Test
    void toleratesMissingFields()
    {
        IntakeResult result = parser.parse("{}", SOURCE);
        assertNull(result.getDeviceInfo());
        assertNull(result.getSymptom());
        assertEquals(SlotSchema.IMPACT_UNKNOWN, result.getImpactScope());
        assertEquals(SlotSchema.TYPE_OTHER, result.getSuggestedType());
        assertEquals(PriorityRule.PRIORITY_LOW, result.getSuggestedPriority());
    }

    @Test
    void unknownImpactValueFallsBackToUnknown()
    {
        IntakeResult result = parser.parse("{\"impact_scope\":\"super-urgent\"}", SOURCE);
        assertEquals(SlotSchema.IMPACT_UNKNOWN, result.getImpactScope());
        assertFalse(SlotSchema.IMPACT_SCOPES.contains("super-urgent"));
    }
}

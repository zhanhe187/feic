package com.zhanhe.servicedesk.system.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * 规则引擎的回归用例：保证「没有大模型时」结构化抽取依然可用，
 * 而且抽取出的每个值都能在原文找到（否则前端确认框里会出现幻觉内容）。
 */
class RuleIntakeEngineTest
{
    private final RuleIntakeEngine engine = new RuleIntakeEngine();

    @Test
    void extractsHardwareTicket()
    {
        String title = "电脑开不了机";
        String description = "我这台 ThinkPad T14 今天早上按电源键没反应，灯闪一下就灭，重启了好几次都不行，现在没法干活了";
        IntakeResult result = engine.run(title, description);

        assertEquals(SlotSchema.SOURCE_RULE, result.getSource());
        assertEquals(SlotSchema.TYPE_DEVICE, result.getSuggestedType());
        assertEquals(SlotSchema.IMPACT_BLOCKING, result.getImpactScope());
        assertEquals(PriorityRule.PRIORITY_HIGH, result.getSuggestedPriority());
        assertNotNull(result.getDeviceInfo());
        assertTrue(result.getDeviceInfo().toLowerCase().contains("thinkpad"));
        assertTrue(EvidenceValidator.hasEvidence(description, result.getSymptom()));
        assertTrue(EvidenceValidator.hasEvidence(description, result.getTriedActions()));
    }

    @Test
    void extractsNetworkTicketAsMultiImpact()
    {
        String description = "公司网络断了，我们组都连不上网，路由器也重启过了";
        IntakeResult result = engine.run("网络故障", description);

        assertEquals(SlotSchema.TYPE_NETWORK, result.getSuggestedType());
        assertEquals(SlotSchema.IMPACT_MULTI, result.getImpactScope());
        assertEquals(PriorityRule.PRIORITY_MEDIUM, result.getSuggestedPriority());
        assertTrue(EvidenceValidator.hasEvidence(description, result.getSymptom()));
    }

    @Test
    void extractsAccountTicket()
    {
        String description = "我的账号被锁定了，密码输了好几次都不对，登录不进去";
        IntakeResult result = engine.run("账号登录问题", description);

        assertEquals(SlotSchema.TYPE_ACCOUNT, result.getSuggestedType());
        assertEquals(SlotSchema.IMPACT_SINGLE, result.getImpactScope());
    }

    @Test
    void softwareRequestWinsOverIncidentalPermissionKeyword()
    {
        // "提示权限不足"里含"权限"，但主要诉求是装软件，按命中次数应判为软件安装
        String description = "需要安装 IntelliJ IDEA，装不了，提示权限不足";
        IntakeResult result = engine.run("安装开发工具", description);

        assertEquals(SlotSchema.TYPE_SOFTWARE, result.getSuggestedType());
        assertTrue(result.getTypeBasis().contains("安装") || result.getTypeBasis().contains("装不了"));
    }

    @Test
    void blankDescriptionFallsBackToOtherWithoutCrash()
    {
        IntakeResult result = engine.run("", "");
        assertEquals(SlotSchema.TYPE_OTHER, result.getSuggestedType());
        assertEquals(SlotSchema.IMPACT_UNKNOWN, result.getImpactScope());
        assertEquals(null, result.getDeviceInfo());
    }
}

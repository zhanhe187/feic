package com.zhanhe.servicedesk.system.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EvidenceValidatorTest
{
    @Test
    void acceptsValuePresentInSource()
    {
        String source = "我这台 ThinkPad T14 按电源键没反应";
        assertTrue(EvidenceValidator.hasEvidence(source, "ThinkPad T14"));
        assertTrue(EvidenceValidator.hasEvidence(source, "按电源键没反应"));
    }

    @Test
    void rejectsFabricatedValue()
    {
        String source = "我这台 ThinkPad T14 按电源键没反应";
        assertFalse(EvidenceValidator.hasEvidence(source, "Dell XPS 15"));
        assertFalse(EvidenceValidator.hasEvidence(source, "主板烧毁"));
    }

    @Test
    void ignoresWhitespaceAndFullWidthDifferences()
    {
        String source = "账号　被锁定 了，登录失败";
        assertTrue(EvidenceValidator.hasEvidence(source, "账号被锁定"));
        assertTrue(EvidenceValidator.hasEvidence(source, "登录失败"));
    }

    @Test
    void rejectsTooShortValue()
    {
        // 单字无法排除误判，一律视为无证据
        assertFalse(EvidenceValidator.hasEvidence("今天电脑坏了", "坏"));
    }

    @Test
    void normalizesCaseForAscii()
    {
        assertTrue(EvidenceValidator.hasEvidence("VPN 连不上", "vpn"));
    }

    @Test
    void handlesNullInputs()
    {
        assertFalse(EvidenceValidator.hasEvidence(null, "abc"));
        assertFalse(EvidenceValidator.hasEvidence("abc", null));
        assertEquals("", EvidenceValidator.normalize(null));
    }
}

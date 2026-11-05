package com.zhanhe.servicedesk.system.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * 审计摘要：只保留哈希与长度，绝不落原文。
 */
class AiAuditServiceTest
{
    @Test
    void digestHidesRawTextButKeepsLength()
    {
        String raw = "我的手机 15273207113 收不到验证码";
        String digest = AiAuditService.digest(raw);

        assertTrue(digest.matches("[0-9a-f]{16}:\\d+"), "格式应为 16 位十六进制 + 原长度");
        assertTrue(digest.endsWith(":" + raw.length()));
        assertTrue(!digest.contains("15273207113"), "摘要不得包含原文");
    }

    @Test
    void digestIsStableForSameInput()
    {
        assertEquals(AiAuditService.digest("abc"), AiAuditService.digest("abc"));
    }

    @Test
    void digestHandlesNull()
    {
        assertNull(AiAuditService.digest(null));
    }
}

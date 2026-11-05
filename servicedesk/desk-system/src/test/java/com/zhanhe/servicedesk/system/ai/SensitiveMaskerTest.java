package com.zhanhe.servicedesk.system.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SensitiveMaskerTest
{
    @Test
    void masksPhoneNumber()
    {
        String masked = SensitiveMasker.mask("我手机 15273207113 收不到验证码");
        assertFalse(masked.contains("15273207113"));
        assertTrue(masked.contains("***"));
    }

    @Test
    void masksEmailAndIdCard()
    {
        String masked = SensitiveMasker.mask("邮箱 2732581930@qq.com，身份证 610122199001011256");
        assertFalse(masked.contains("2732581930"));
        assertFalse(masked.contains("610122199001011256"));
    }

    @Test
    void masksPasswordButKeepsPrefix()
    {
        String masked = SensitiveMasker.mask("我的密码是 abc123 一直登不上");
        assertFalse(masked.contains("abc123"));
        assertTrue(masked.contains("密码是"));
        assertTrue(masked.contains("一直登不上"));
    }

    @Test
    void keepsNormalTicketTextUnchanged()
    {
        String text = "ThinkPad T14 按电源键没反应，重启也没用";
        assertEquals(text, SensitiveMasker.mask(text));
    }

    @Test
    void doesNotBreakOrderNumbers()
    {
        // 长短接近但不是手机号的编号不应被打码（正则限制了 1[3-9] 开头 11 位）
        String text = "工号 1234567 的电脑坏了";
        assertEquals(text, SensitiveMasker.mask(text));
    }
}

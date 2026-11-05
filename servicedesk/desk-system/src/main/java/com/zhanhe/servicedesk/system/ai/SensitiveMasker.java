package com.zhanhe.servicedesk.system.ai;

import java.util.regex.Pattern;

/**
 * 发送给大模型前的脱敏处理。
 *
 * <p>工单描述是员工随手写的，很容易夹带手机号、邮箱、身份证甚至口令。
 * 这些内容一旦发给外部厂商就构成数据外泄，因此在组装 payload 前统一打码；
 * 同时保证「打码后仍保留语义」（例如"我的密码是 abc 改不了" → "我的密码是 *** 改不了"），
 * 不影响模型理解问题。</p>
 */
public final class SensitiveMasker
{
    private static final Pattern PHONE = Pattern.compile("(?<!\\d)1[3-9]\\d{9}(?!\\d)");

    private static final Pattern ID_CARD = Pattern.compile("(?<!\\d)\\d{17}[\\dXx](?!\\d)");

    private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");

    private static final Pattern PASSWORD = Pattern.compile("((?:密码|口令|验证码|passwd|password)\\s*(?:是|为|:|：)?\\s*)\\S+",
            Pattern.CASE_INSENSITIVE);

    private SensitiveMasker()
    {
    }

    public static String mask(String text)
    {
        if (text == null || text.isEmpty())
        {
            return text;
        }
        String out = ID_CARD.matcher(text).replaceAll("***");
        out = PHONE.matcher(out).replaceAll("***");
        out = EMAIL.matcher(out).replaceAll("***");
        out = PASSWORD.matcher(out).replaceAll("$1***");
        return out;
    }
}

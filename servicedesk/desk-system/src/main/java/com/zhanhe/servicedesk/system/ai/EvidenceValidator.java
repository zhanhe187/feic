package com.zhanhe.servicedesk.system.ai;

/**
 * 证据校验：模型抽出来的值必须在它看到的原文里真实存在，否则丢弃。
 *
 * <p>这是防「模型编造」最有效也最便宜的一道闸门：不需要二次调用模型，纯字符串比对。
 * 归一化处理了空白、全半角、大小写，避免因排版差异误杀。</p>
 */
public final class EvidenceValidator
{
    private EvidenceValidator()
    {
    }

    /**
     * 判断 value 是否可以作为 source 中的一段证据。
     *
     * @param source 模型实际看到的文本（已脱敏）
     * @param value  模型抽取的值
     * @return true 表示有证据
     */
    public static boolean hasEvidence(String source, String value)
    {
        if (source == null || value == null)
        {
            return false;
        }
        String v = normalize(value);
        // 过短的值（1 个字）无法排除误判，视为无证据
        if (v.codePointCount(0, v.length()) < 2)
        {
            return false;
        }
        return normalize(source).contains(v);
    }

    /**
     * 归一化：全角转半角 + 去所有空白 + 转小写。
     */
    public static String normalize(String text)
    {
        if (text == null)
        {
            return "";
        }
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++)
        {
            char c = text.charAt(i);
            if (c == '\u3000')
            {
                continue;
            }
            if (c >= '\uFF01' && c <= '\uFF5E')
            {
                c = (char) (c - 0xFEE0);
            }
            if (Character.isWhitespace(c))
            {
                continue;
            }
            sb.append(Character.toLowerCase(c));
        }
        return sb.toString();
    }
}

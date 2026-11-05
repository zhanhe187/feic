package com.zhanhe.servicedesk.system.ai;

/**
 * 优先级推导规则。
 *
 * <p>刻意的设计选择：<b>优先级不交给模型决定</b>。
 * 模型对"紧急"没有责任，很容易被"很急"两个字带偏，造成优先级通胀；
 * 这里只让模型给出可核查的「影响面」，映射关系由确定性规则负责，员工最终确认。</p>
 */
public final class PriorityRule
{
    public static final String PRIORITY_LOW = "low";

    public static final String PRIORITY_MEDIUM = "medium";

    public static final String PRIORITY_HIGH = "high";

    public static final String PRIORITY_URGENT = "urgent";

    private PriorityRule()
    {
    }

    /**
     * 影响面 + 少量关键词 → 建议优先级。
     *
     * <p>blocking：业务中断 → high；multi：影响多人 → medium；single：仅本人 → low；
     * unknown：信息不足 → low（宁可低估由 IT 上调，也不要一味拉高）。</p>
     */
    public static String suggest(String impactScope, String text)
    {
        String impact = SlotSchema.normalizeImpact(impactScope);
        if (SlotSchema.IMPACT_BLOCKING.equals(impact))
        {
            return PRIORITY_HIGH;
        }
        if (SlotSchema.IMPACT_MULTI.equals(impact))
        {
            return PRIORITY_MEDIUM;
        }
        return PRIORITY_LOW;
    }

    public static boolean isValid(String priority)
    {
        return PRIORITY_LOW.equals(priority) || PRIORITY_MEDIUM.equals(priority)
                || PRIORITY_HIGH.equals(priority) || PRIORITY_URGENT.equals(priority);
    }
}

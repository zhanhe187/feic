package com.zhanhe.servicedesk.system.ai;

import java.util.Arrays;
import java.util.List;

/**
 * 工单结构化槽位的「契约」：长度上限、枚举白名单、归一化。
 *
 * <p>所有模型输出在落库前都必须过这里，模型说 ≠ 可信。
 * 白名单思想：宁可退化成"unknown/其他"，也不让模型自由发挥出脏数据。</p>
 */
public final class SlotSchema
{
    public static final int MAX_DEVICE = 120;

    public static final int MAX_SYMPTOM = 500;

    public static final int MAX_TRIED = 500;

    public static final String IMPACT_SINGLE = "single";

    public static final String IMPACT_MULTI = "multi";

    public static final String IMPACT_BLOCKING = "blocking";

    public static final String IMPACT_UNKNOWN = "unknown";

    public static final List<String> IMPACT_SCOPES = Arrays.asList(
            IMPACT_SINGLE, IMPACT_MULTI, IMPACT_BLOCKING, IMPACT_UNKNOWN);

    public static final String TYPE_DEVICE = "设备故障";

    public static final String TYPE_ACCOUNT = "账号权限";

    public static final String TYPE_NETWORK = "网络问题";

    public static final String TYPE_SOFTWARE = "软件安装";

    public static final String TYPE_OTHER = "其他";

    public static final List<String> TICKET_TYPES = Arrays.asList(
            TYPE_DEVICE, TYPE_ACCOUNT, TYPE_NETWORK, TYPE_SOFTWARE, TYPE_OTHER);

    public static final String SOURCE_LLM = "llm";

    public static final String SOURCE_RULE = "rule";

    public static final String SOURCE_MANUAL = "manual";

    public static final List<String> AI_SOURCES = Arrays.asList(SOURCE_LLM, SOURCE_RULE, SOURCE_MANUAL);

    private SlotSchema()
    {
    }

    /** 影响面白名单校验：不在白名单一律归为 unknown */
    public static String normalizeImpact(String value)
    {
        if (value == null)
        {
            return IMPACT_UNKNOWN;
        }
        String v = value.trim().toLowerCase();
        return IMPACT_SCOPES.contains(v) ? v : IMPACT_UNKNOWN;
    }

    /** 工单类型白名单校验：不在白名单归为"其他" */
    public static String normalizeType(String value)
    {
        if (value == null)
        {
            return TYPE_OTHER;
        }
        String v = value.trim();
        if (TICKET_TYPES.contains(v))
        {
            return v;
        }
        // 容忍模型返回英文枚举
        switch (v.toLowerCase())
        {
            case "device":
            case "hardware":
                return TYPE_DEVICE;
            case "account":
            case "permission":
                return TYPE_ACCOUNT;
            case "network":
                return TYPE_NETWORK;
            case "software":
                return TYPE_SOFTWARE;
            default:
                return TYPE_OTHER;
        }
    }

    /** 来源白名单：非法值置空，避免脏数据写进 ai_source */
    public static String normalizeSource(String value)
    {
        if (value == null)
        {
            return null;
        }
        String v = value.trim().toLowerCase();
        return AI_SOURCES.contains(v) ? v : null;
    }

    /** 截断到指定长度（按码点截断，避免切坏中文/emoji） */
    public static String truncate(String value, int max)
    {
        if (value == null)
        {
            return null;
        }
        String v = value.trim();
        if (v.isEmpty())
        {
            return null;
        }
        if (v.codePointCount(0, v.length()) <= max)
        {
            return v;
        }
        int end = v.offsetByCodePoints(0, max);
        return v.substring(0, end);
    }
}

package com.zhanhe.servicedesk.system.ai;

import org.springframework.stereotype.Component;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

/**
 * 模型输出解析器：把「应该是 JSON」的文本变成 {@link IntakeResult}，并对每个槽位做证据校验。
 *
 * <p>容错顺序：去 Markdown 围栏 → 截取第一个 { 到最后一个 } → 解析 → 逐字段白名单/长度/证据校验。
 * 任何一步失败都抛 {@link AiUnavailableException}，由上层降级，绝不把半成品写进数据库。</p>
 */
@Component
public class AiOutputParser
{
    public IntakeResult parse(String rawText, String maskedSource)
    {
        String json = extractJson(rawText);
        if (json == null)
        {
            throw new AiUnavailableException("模型输出中未找到 JSON 对象");
        }
        JSONObject obj;
        try
        {
            obj = JSON.parseObject(json);
        }
        catch (Exception e)
        {
            throw new AiUnavailableException("模型输出不是合法 JSON: " + e.getMessage(), e);
        }
        if (obj == null)
        {
            throw new AiUnavailableException("模型输出 JSON 为空");
        }

        IntakeResult result = new IntakeResult();
        result.setSource(SlotSchema.SOURCE_LLM);

        result.setDeviceInfo(acceptEvidence(obj.getString("device"), maskedSource, "device", SlotSchema.MAX_DEVICE, result));
        result.setSymptom(acceptEvidence(obj.getString("symptom"), maskedSource, "symptom", SlotSchema.MAX_SYMPTOM, result));
        result.setTriedActions(acceptEvidence(obj.getString("tried_actions"), maskedSource, "tried_actions",
                SlotSchema.MAX_TRIED, result));

        // 枚举字段走白名单，不要求原文证据（它们本来就是分类标签）
        result.setImpactScope(SlotSchema.normalizeImpact(obj.getString("impact_scope")));
        result.setSuggestedType(SlotSchema.normalizeType(obj.getString("ticket_type")));
        result.setTypeBasis(SlotSchema.truncate(obj.getString("type_basis"), 60));
        // 优先级由规则推导，模型不参与（见 PriorityRule 注释）
        result.setSuggestedPriority(PriorityRule.suggest(result.getImpactScope(), maskedSource));
        return result;
    }

    /**
     * 值必须能截断到合法长度、且在原文中找得到证据，否则丢弃并记录 rejected。
     */
    private String acceptEvidence(String value, String source, String field, int max, IntakeResult result)
    {
        String v = SlotSchema.truncate(value, max);
        if (v == null)
        {
            return null;
        }
        if (!EvidenceValidator.hasEvidence(source, v))
        {
            result.getRejected().add(field);
            return null;
        }
        return v;
    }

    /**
     * 从可能带 Markdown 围栏/前后废话的文本中截出 JSON 主体。
     */
    static String extractJson(String rawText)
    {
        if (rawText == null)
        {
            return null;
        }
        String text = rawText.trim();
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start)
        {
            return null;
        }
        return text.substring(start, end + 1);
    }
}

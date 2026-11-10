package com.zhanhe.servicedesk.web.controller.it;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zhanhe.servicedesk.common.core.controller.BaseController;
import com.zhanhe.servicedesk.common.core.domain.AjaxResult;
import com.zhanhe.servicedesk.common.utils.SecurityUtils;
import com.zhanhe.servicedesk.common.utils.StringUtils;
import com.zhanhe.servicedesk.system.service.IItAiConfigService;

/**
 * AI 设置：唯一配置入口，数据存 {@code it_ai_config}，保存即生效。
 *
 * <p>Key 在页面只显示掩码；提交上来的仍是掩码时视为「不修改」。
 * 同时回传 keyLength，方便确认到底有没有写进去。</p>
 */
@RestController
@RequestMapping("/it/ai/config")
public class ItAiConfigController extends BaseController
{
    /** 页面展示顺序与说明；缺的键在保存时会自动补插 */
    private static final String[][] FIELDS = {
            { "ai.enabled", "总开关", "false 时全部走规则引擎" },
            { "ai.provider", "提供方", "rule=规则引擎；openai=调用大模型" },
            { "ai.base_url", "接口地址", "OpenAI 兼容地址，通常以 /v1 结尾" },
            { "ai.api_key", "API Key", "留空不调用模型" },
            { "ai.model", "模型名", "例如 deepseek-chat / gpt-4o-mini" },
            { "ai.timeout_seconds", "超时秒数", "同步交互，超时即降级，不重试" },
            { "ai.daily_limit", "每人每日配额", "<=0 表示不限制" },
            { "ai.breaker_threshold", "熔断阈值", "连续失败多少次后短路" },
            { "ai.breaker_seconds", "熔断秒数", "短路持续多久" },
    };

    private static final String MASK = "******";

    private final IItAiConfigService configService;

    public ItAiConfigController(IItAiConfigService configService)
    {
        this.configService = configService;
    }

    @PreAuthorize("@ss.hasPermi('it:ai:config')")
    @GetMapping
    public AjaxResult get()
    {
        Map<String, String> values = configService.asMap();
        List<Map<String, String>> items = new ArrayList<>();
        for (String[] field : FIELDS)
        {
            String key = field[0];
            String value = values.get(key);
            boolean secret = isSecret(key);
            Map<String, String> item = new LinkedHashMap<>();
            item.put("key", key);
            item.put("label", field[1]);
            item.put("hint", field[2]);
            item.put("value", secret ? (StringUtils.isEmpty(value) ? "" : MASK) : (value == null ? "" : value));
            item.put("keyLength", secret && value != null ? String.valueOf(value.length()) : "0");
            items.add(item);
        }
        AjaxResult ajax = AjaxResult.success(items);
        ajax.put("runtime", runtimeSummary(values));
        return ajax;
    }

    @PreAuthorize("@ss.hasPermi('it:ai:config')")
    @PutMapping
    public AjaxResult save(@RequestBody Map<String, String> body)
    {
        if (body == null || body.isEmpty())
        {
            return AjaxResult.warn("没有需要保存的内容");
        }
        Map<String, String> cleaned = new LinkedHashMap<>();
        for (String[] field : FIELDS)
        {
            String key = field[0];
            if (!body.containsKey(key))
            {
                continue;
            }
            String value = body.get(key);
            if (value == null)
            {
                value = "";
            }
            value = value.trim();
            if (isSecret(key) && MASK.equals(value))
            {
                continue;
            }
            cleaned.put(key, value);
        }
        String operator;
        try
        {
            operator = SecurityUtils.getUsername();
        }
        catch (Exception e)
        {
            operator = "admin";
        }
        int updated = configService.save(cleaned, operator);
        Map<String, String> values = configService.asMap();
        AjaxResult ajax = AjaxResult.success();
        ajax.put("updated", updated);
        ajax.put("runtime", runtimeSummary(values));
        String key = values.get("ai.api_key");
        ajax.put("keyLength", key == null ? 0 : key.length());
        return ajax;
    }

    /** 给页面一个「当前到底走模型还是规则」的结论，省得管理员自己拼条件 */
    private Map<String, String> runtimeSummary(Map<String, String> values)
    {
        boolean on = "true".equalsIgnoreCase(values.get("ai.enabled"));
        boolean useLlm = on && "openai".equalsIgnoreCase(values.get("ai.provider"))
                && StringUtils.isNotEmpty(values.get("ai.api_key"));
        Map<String, String> summary = new LinkedHashMap<>();
        summary.put("mode", useLlm ? "llm" : "rule");
        summary.put("text", useLlm ? "当前走大模型（OpenAI 兼容接口）" : "当前走规则引擎");
        String model = values.get("ai.model");
        summary.put("model", StringUtils.isEmpty(model) ? "-" : model);
        return summary;
    }

    private boolean isSecret(String key)
    {
        return "ai.api_key".equals(key);
    }
}

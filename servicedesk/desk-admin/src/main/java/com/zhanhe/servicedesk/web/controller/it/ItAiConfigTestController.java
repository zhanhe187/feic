package com.zhanhe.servicedesk.web.controller.it;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zhanhe.servicedesk.common.core.controller.BaseController;
import com.zhanhe.servicedesk.common.core.domain.AjaxResult;
import com.zhanhe.servicedesk.system.ai.AiLlmClient;
import com.zhanhe.servicedesk.system.ai.AiSettings;

/**
 * AI 连通性自检：用一次最小调用验证「Key / 地址 / 模型」是否真的可用。
 *
 * <p>为什么单独做一个接口：配置页只能说明「参数写进去了」，
 * 不能说明「模型真的答得回来」。运维判断接没接上，应该点一次就有结论，
 * 而不是去工单页试、再翻日志。</p>
 *
 * <p>这次调用不写业务数据、不占用每日配额，只回显耗时与错误原因。</p>
 */
@RestController
@RequestMapping("/it/ai/config")
public class ItAiConfigTestController extends BaseController
{
    private final AiSettings settings;

    private final AiLlmClient llmClient;

    public ItAiConfigTestController(AiSettings settings, AiLlmClient llmClient)
    {
        this.settings = settings;
        this.llmClient = llmClient;
    }

    @PreAuthorize("@ss.hasPermi('it:ai:config')")
    @PostMapping("/test")
    public AjaxResult test()
    {
        AiSettings.Snapshot snapshot = settings.snapshot();
        Map<String, String> info = new LinkedHashMap<>();
        info.put("provider", snapshot.getProvider());
        info.put("model", snapshot.getModel());
        info.put("baseUrl", snapshot.getBaseUrl());
        info.put("keyConfigured", snapshot.getApiKey() == null || snapshot.getApiKey().isEmpty() ? "false" : "true");
        if (!snapshot.useLlm())
        {
            AjaxResult ajax = AjaxResult.warn("当前未启用大模型：请把提供方设为 openai 并填写 API Key");
            ajax.put("detail", info);
            ajax.put("mode", "rule");
            return ajax;
        }
        long start = System.currentTimeMillis();
        try
        {
            String reply = llmClient.chat(
                    "你是一个连通性检测器。只输出一行 JSON：{\"ok\":true}",
                    "<ticket><title>ping</title><description>连通性测试</description></ticket>");
            long cost = System.currentTimeMillis() - start;
            AjaxResult ajax = AjaxResult.success("接入成功，模型已返回内容");
            ajax.put("mode", "llm");
            ajax.put("costMs", cost);
            ajax.put("detail", info);
            ajax.put("reply", reply == null ? "" : reply.substring(0, Math.min(200, reply.length())));
            return ajax;
        }
        catch (Exception e)
        {
            long cost = System.currentTimeMillis() - start;
            AjaxResult ajax = AjaxResult.error("接入失败：" + e.getMessage());
            ajax.put("mode", "llm-error");
            ajax.put("costMs", cost);
            ajax.put("detail", info);
            return ajax;
        }
    }
}

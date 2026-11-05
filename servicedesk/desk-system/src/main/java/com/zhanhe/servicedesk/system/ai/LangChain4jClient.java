package com.zhanhe.servicedesk.system.ai;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;

/**
 * LangChain4j 实现（OpenAI 兼容协议，喂 DeepSeek / 通义 / 智谱 / 本地 Ollama 都行）。
 *
 * <p>关键参数：</p>
 * <ul>
 *   <li>timeout：默认 8s。工单提交是同步交互，宁可降级也不能让员工干等。</li>
 *   <li>maxRetries=0：<b>刻意不重试</b>。带重试时最坏耗时是 超时×2=16s，对提交按钮是不可接受的体感；
 *       瞬时抖动的成本由「规则引擎降级 + 熔断」吸收，而不是让用户多等一倍。</li>
 *   <li>responseFormat=json_object：用框架能力约束输出为 JSON，减少解析失败。</li>
 *   <li>logRequests/logResponses=false：工单里可能有敏感内容，不能落到应用日志。</li>
 * </ul>
 */
@Component
public class LangChain4jClient implements AiLlmClient
{
    private static final Logger log = LoggerFactory.getLogger(LangChain4jClient.class);

    private final AiProperties properties;

    private volatile OpenAiChatModel model;

    public LangChain4jClient(AiProperties properties)
    {
        this.properties = properties;
    }

    @Override
    public String chat(String systemPrompt, String userPayload)
    {
        if (!properties.useLlm())
        {
            throw new AiUnavailableException("LLM 未启用或缺少 API Key");
        }
        try
        {
            ChatResponse response = model().chat(
                    SystemMessage.from(systemPrompt),
                    UserMessage.from(userPayload));
            String text = response == null || response.aiMessage() == null ? null : response.aiMessage().text();
            if (text == null || text.isBlank())
            {
                throw new AiUnavailableException("模型返回空内容");
            }
            return text;
        }
        catch (AiUnavailableException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            // 超时/网络/鉴权/协议错误统一收敛，由上层降级处理
            log.warn("[ai] llm call failed: {}", e.toString());
            throw new AiUnavailableException(e.getMessage(), e);
        }
    }

    @Override
    public String modelName()
    {
        return properties.getModel();
    }

    /** 懒加载：没配 Key 时根本不会构造客户端 */
    private OpenAiChatModel model()
    {
        OpenAiChatModel current = model;
        if (current != null)
        {
            return current;
        }
        synchronized (this)
        {
            if (model == null)
            {
                model = OpenAiChatModel.builder()
                        .baseUrl(trimTrailingSlash(properties.getBaseUrl()))
                        .apiKey(properties.getApiKey())
                        .modelName(properties.getModel())
                        .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                        .maxRetries(0)
                        .responseFormat("json_object")
                        .logRequests(false)
                        .logResponses(false)
                        .build();
            }
            return model;
        }
    }

    private static String trimTrailingSlash(String url)
    {
        if (url == null)
        {
            return "";
        }
        String u = url.trim();
        while (u.endsWith("/"))
        {
            u = u.substring(0, u.length() - 1);
        }
        return u;
    }
}

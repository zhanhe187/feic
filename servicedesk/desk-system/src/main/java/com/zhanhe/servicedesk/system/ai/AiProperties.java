package com.zhanhe.servicedesk.system.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 接入配置。取值来自环境变量（见 deploy/.env.example），yml 中用 ${AI_*:默认值} 占位。
 *
 * <p>设计约束：默认 provider=rule，即不发任何网络请求也能提供完整的结构化抽取能力，
 * 保证「没 Key、断网、厂商故障」三种情况都不影响工单主流程。</p>
 */
@Component
@ConfigurationProperties(prefix = "ai")
public class AiProperties
{
    /** 总开关；关闭后所有 AI 能力退化为规则引擎 */
    private boolean enabled = true;

    /** rule | openai */
    private String provider = "rule";

    /** OpenAI 兼容接口地址，如 https://api.deepseek.com/v1 */
    private String baseUrl = "http://127.0.0.1:8899/v1";

    /** 密钥，只从环境变量读，不写进代码与日志 */
    private String apiKey = "";

    private String model = "deepseek-chat";

    private int timeoutSeconds = 8;

    /** 每人每日调用上限，<=0 表示不限制 */
    private int dailyLimit = 200;

    /** 连续失败多少次后熔断 */
    private int breakerThreshold = 3;

    /** 熔断持续秒数 */
    private int breakerSeconds = 300;

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public String getProvider()
    {
        return provider;
    }

    public void setProvider(String provider)
    {
        this.provider = provider;
    }

    public String getBaseUrl()
    {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl)
    {
        this.baseUrl = baseUrl;
    }

    public String getApiKey()
    {
        return apiKey;
    }

    public void setApiKey(String apiKey)
    {
        this.apiKey = apiKey;
    }

    public String getModel()
    {
        return model;
    }

    public void setModel(String model)
    {
        this.model = model;
    }

    public int getTimeoutSeconds()
    {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds)
    {
        this.timeoutSeconds = timeoutSeconds;
    }

    public int getDailyLimit()
    {
        return dailyLimit;
    }

    public void setDailyLimit(int dailyLimit)
    {
        this.dailyLimit = dailyLimit;
    }

    public int getBreakerThreshold()
    {
        return breakerThreshold;
    }

    public void setBreakerThreshold(int breakerThreshold)
    {
        this.breakerThreshold = breakerThreshold;
    }

    public int getBreakerSeconds()
    {
        return breakerSeconds;
    }

    public void setBreakerSeconds(int breakerSeconds)
    {
        this.breakerSeconds = breakerSeconds;
    }

    /** 是否走大模型分支（配置为 openai 且填了 Key） */
    public boolean useLlm()
    {
        return enabled && "openai".equalsIgnoreCase(provider) && apiKey != null && !apiKey.isBlank();
    }
}

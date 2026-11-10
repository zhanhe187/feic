package com.zhanhe.servicedesk.system.ai;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.zhanhe.servicedesk.common.utils.StringUtils;
import com.zhanhe.servicedesk.system.service.IItAiConfigService;

/**
 * AI 运行时配置：读 {@code it_ai_config} 表，读不到再回落 application.yml 的默认值。
 *
 * <p>为什么单独一张表：AI 参数有自己的页面与生效方式（改完即时生效、Key 需掩码），
 * 和「系统参数」混在一起会让两边都难维护。yml 只作为兜底默认值，不再是配置入口。</p>
 *
 * <p>每次调用读一次（表很小），换来「后台改完即生效」，不引入额外的缓存失效问题。</p>
 */
@Component
public class AiSettings
{
    private static final String K_ENABLED = "ai.enabled";

    private static final String K_PROVIDER = "ai.provider";

    private static final String K_BASE_URL = "ai.base_url";

    private static final String K_API_KEY = "ai.api_key";

    private static final String K_MODEL = "ai.model";

    private static final String K_TIMEOUT = "ai.timeout_seconds";

    private static final String K_DAILY_LIMIT = "ai.daily_limit";

    private static final String K_BREAKER_THRESHOLD = "ai.breaker_threshold";

    private static final String K_BREAKER_SECONDS = "ai.breaker_seconds";

    private final AiProperties defaults;

    private final IItAiConfigService configService;

    @Autowired
    public AiSettings(AiProperties defaults, IItAiConfigService configService)
    {
        this.defaults = defaults;
        this.configService = configService;
    }

    /** 单测用：只读 yml 默认值 */
    public AiSettings(AiProperties defaults)
    {
        this(defaults, null);
    }

    /** 取当前生效的一份配置快照（一次调用内保持不变） */
    public Snapshot snapshot()
    {
        return new Snapshot(
                bool(K_ENABLED, defaults.isEnabled()),
                str(K_PROVIDER, defaults.getProvider()),
                str(K_BASE_URL, defaults.getBaseUrl()),
                str(K_API_KEY, defaults.getApiKey()),
                str(K_MODEL, defaults.getModel()),
                num(K_TIMEOUT, defaults.getTimeoutSeconds()),
                num(K_DAILY_LIMIT, defaults.getDailyLimit()),
                num(K_BREAKER_THRESHOLD, defaults.getBreakerThreshold()),
                num(K_BREAKER_SECONDS, defaults.getBreakerSeconds()));
    }

    private String str(String key, String fallback)
    {
        if (configService == null)
        {
            return fallback;
        }
        try
        {
            String value = configService.getValue(key);
            return StringUtils.isEmpty(value) ? fallback : value.trim();
        }
        catch (Exception e)
        {
            // 配置表不可用时不能让 AI 整体不可用，回落默认值
            return fallback;
        }
    }

    private boolean bool(String key, boolean fallback)
    {
        String value = str(key, null);
        if (StringUtils.isEmpty(value))
        {
            return fallback;
        }
        return "true".equalsIgnoreCase(value) || "1".equals(value) || "Y".equalsIgnoreCase(value);
    }

    private int num(String key, int fallback)
    {
        String value = str(key, null);
        if (StringUtils.isEmpty(value))
        {
            return fallback;
        }
        try
        {
            return Integer.parseInt(value.trim());
        }
        catch (NumberFormatException e)
        {
            return fallback;
        }
    }

    /** 一次识别过程中使用的配置快照 */
    public static final class Snapshot
    {
        private final boolean enabled;

        private final String provider;

        private final String baseUrl;

        private final String apiKey;

        private final String model;

        private final int timeoutSeconds;

        private final int dailyLimit;

        private final int breakerThreshold;

        private final int breakerSeconds;

        Snapshot(boolean enabled, String provider, String baseUrl, String apiKey, String model,
                int timeoutSeconds, int dailyLimit, int breakerThreshold, int breakerSeconds)
        {
            this.enabled = enabled;
            this.provider = provider;
            this.baseUrl = baseUrl;
            this.apiKey = apiKey;
            this.model = model;
            this.timeoutSeconds = Math.max(1, timeoutSeconds);
            this.dailyLimit = dailyLimit;
            this.breakerThreshold = Math.max(1, breakerThreshold);
            this.breakerSeconds = Math.max(1, breakerSeconds);
        }

        /** 只有「开关打开 + provider=openai + 填了 Key」才走模型，三者缺一即走规则引擎 */
        public boolean useLlm()
        {
            return enabled && "openai".equalsIgnoreCase(provider) && StringUtils.isNotEmpty(apiKey);
        }

        /** 用于判断是否需要重建模型客户端：前三项变化必须重建 */
        public String fingerprint()
        {
            return baseUrl + "|" + apiKey + "|" + model + "|" + timeoutSeconds;
        }

        public boolean isEnabled()
        {
            return enabled;
        }

        public String getProvider()
        {
            return provider;
        }

        public String getBaseUrl()
        {
            return baseUrl;
        }

        public String getApiKey()
        {
            return apiKey;
        }

        public String getModel()
        {
            return model;
        }

        public int getTimeoutSeconds()
        {
            return timeoutSeconds;
        }

        public int getDailyLimit()
        {
            return dailyLimit;
        }

        public int getBreakerThreshold()
        {
            return breakerThreshold;
        }

        public int getBreakerSeconds()
        {
            return breakerSeconds;
        }
    }
}

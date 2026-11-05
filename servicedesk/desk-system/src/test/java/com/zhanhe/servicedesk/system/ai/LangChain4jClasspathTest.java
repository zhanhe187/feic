package com.zhanhe.servicedesk.system.ai;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import dev.langchain4j.model.openai.OpenAiChatModel;

/**
 * P0 依赖闸门：只验证 LangChain4j 1.13.0 能在本项目（Spring Boot 4.0.6 / Java 17 / Jackson2 2.21.2）中
 * 成功解析并完成类加载与对象构造，不发起任何网络请求。
 */
class LangChain4jClasspathTest {

    @Test
    void canBuildOpenAiCompatibleModelWithoutNetwork() {
        OpenAiChatModel model = OpenAiChatModel.builder()
                .baseUrl("http://127.0.0.1:8899/v1")
                .apiKey("test-key")
                .modelName("deepseek-chat")
                .timeout(Duration.ofSeconds(3))
                .maxRetries(0)
                .responseFormat("json_object")
                .build();
        assertNotNull(model);
    }
}

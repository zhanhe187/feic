package com.zhanhe.servicedesk.system.ai;

/**
 * 模型调用抽象。业务层只依赖这个接口，换框架/换厂商只改实现类。
 *
 * <p>当前实现：{@link LangChain4jClient}（LangChain4j + OpenAI 兼容协议）。
 * 之所以没有直接用 Spring AI：它要求 Spring Boot 4.1.x，而本项目底座是 4.0.6，
 * 为了一个 AI 能力去升级整个框架底座不划算（详见根 pom 的版本说明）。</p>
 */
public interface AiLlmClient
{
    /**
     * 发起一次对话补全。
     *
     * @param systemPrompt 系统提示词（角色、输出契约、防注入声明）
     * @param userPayload  已经脱敏的用户内容（带分隔符包裹）
     * @return 模型原始文本（约定为 JSON）
     * @throws AiUnavailableException 无 Key / 超时 / 网络错误 / 返回空
     */
    String chat(String systemPrompt, String userPayload);

    String modelName();
}

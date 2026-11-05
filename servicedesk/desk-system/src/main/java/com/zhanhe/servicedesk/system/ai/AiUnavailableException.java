package com.zhanhe.servicedesk.system.ai;

/**
 * 模型不可用（无 Key、超时、5xx、返回空、输出无法解析等）。
 * 调用方必须捕获并降级到规则引擎，不允许把异常抛到业务主链路。
 */
public class AiUnavailableException extends RuntimeException
{
    private static final long serialVersionUID = 1L;

    public AiUnavailableException(String message)
    {
        super(message);
    }

    public AiUnavailableException(String message, Throwable cause)
    {
        super(message, cause);
    }
}

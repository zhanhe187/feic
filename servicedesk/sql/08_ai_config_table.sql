-- AI 配置独立表：不再占用 sys_config（参数设置页）
-- 可重复执行

CREATE TABLE IF NOT EXISTS it_ai_config (
    config_key   varchar(64)  NOT NULL COMMENT '配置键',
    config_value varchar(500) DEFAULT NULL COMMENT '配置值',
    remark       varchar(255) DEFAULT NULL COMMENT '说明',
    update_by    varchar(64)  DEFAULT NULL,
    update_time  datetime     DEFAULT NULL,
    PRIMARY KEY (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 接入配置';

INSERT INTO it_ai_config (config_key, config_value, remark) VALUES
('ai.enabled', 'true', 'false 时全部走规则引擎'),
('ai.provider', 'rule', 'rule=规则引擎；openai=调用大模型'),
('ai.base_url', 'https://api.deepseek.com/v1', 'OpenAI 兼容地址，通常以 /v1 结尾'),
('ai.api_key', '', '留空则不调用模型'),
('ai.model', 'deepseek-chat', '例如 deepseek-chat / gpt-4o-mini'),
('ai.timeout_seconds', '8', '同步交互，超时即降级，不重试'),
('ai.daily_limit', '200', '每人每日调用上限，<=0 表示不限制'),
('ai.breaker_threshold', '3', '连续失败多少次后短路'),
('ai.breaker_seconds', '300', '短路持续多久后自动放行')
ON DUPLICATE KEY UPDATE remark = VALUES(remark);

-- 从参数设置里移除 AI 项（历史遗留）
DELETE FROM sys_config WHERE config_key LIKE 'ai.%';

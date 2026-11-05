package com.zhanhe.servicedesk.system.ai;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.zhanhe.servicedesk.common.utils.SecurityUtils;
import com.zhanhe.servicedesk.system.domain.ItAiLog;
import com.zhanhe.servicedesk.system.mapper.ItAiLogMapper;

/**
 * AI 调用审计。
 *
 * <p>落库的是「摘要」而不是原文：既能回答"这次调用是谁、在什么场景、用了哪个模型、
 * 耗时多少、是否降级、输入输出是否变化"，又不会把工单内容复制到日志表里扩大泄露面。</p>
 *
 * <p>审计失败绝不影响业务：这里全量 try/catch，只打警告日志。</p>
 */
@Service
public class AiAuditService
{
    private static final Logger log = LoggerFactory.getLogger(AiAuditService.class);

    private final ItAiLogMapper aiLogMapper;

    public AiAuditService(ItAiLogMapper aiLogMapper)
    {
        this.aiLogMapper = aiLogMapper;
    }

    public void record(String scene, Long ticketId, String provider, String model, boolean degraded,
            long costMs, String input, String output)
    {
        try
        {
            ItAiLog entity = new ItAiLog();
            entity.setScene(scene);
            entity.setTicketId(ticketId);
            entity.setUserName(currentUserName());
            entity.setProvider(provider);
            entity.setModel(model == null ? "" : model);
            entity.setDegraded(degraded ? "1" : "0");
            entity.setCostMs((int) Math.min(Integer.MAX_VALUE, costMs));
            entity.setInputDigest(digest(input));
            entity.setOutputDigest(digest(output));
            aiLogMapper.insertItAiLog(entity);
        }
        catch (Exception e)
        {
            log.warn("[ai] audit failed: {}", e.toString());
        }
    }

    private String currentUserName()
    {
        try
        {
            return SecurityUtils.getUsername();
        }
        catch (Exception e)
        {
            return "anonymous";
        }
    }

    /** 摘要格式：sha256 前 16 位 + ":" + 原长度，足够做对比与排障，又不泄露内容 */
    static String digest(String text)
    {
        if (text == null)
        {
            return null;
        }
        try
        {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(24);
            for (int i = 0; i < 8; i++)
            {
                sb.append(String.format("%02x", hash[i]));
            }
            sb.append(':').append(text.length());
            return sb.toString();
        }
        catch (Exception e)
        {
            return ":" + text.length();
        }
    }
}

package com.zhanhe.servicedesk.system.service.impl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.zhanhe.servicedesk.common.utils.SecurityUtils;
import com.zhanhe.servicedesk.common.utils.StringUtils;
import com.zhanhe.servicedesk.system.domain.ItMessage;
import com.zhanhe.servicedesk.system.mapper.ItMessageMapper;
import com.zhanhe.servicedesk.system.service.IItMessageService;

/**
 * 站内消息服务。
 *
 * <p>通知只做「该让人知道的事」，不追求多：</p>
 * <ul>
 *   <li>AI 降级 / 熔断 / 配额用尽 → 告警管理员，按天去重，避免同类刷屏；</li>
 *   <li>新工单 → 通知 IT（有处理权限的人），高优先级加前缀；</li>
 *   <li>派单 → 通知处理人；</li>
 *   <li>处理完成 / 驳回 → 通知申请人。</li>
 * </ul>
 *
 * <p>发送失败只记日志，不影响业务主流程——通知是增强，不是依赖。</p>
 */
@Service
public class ItMessageServiceImpl implements IItMessageService
{
    private static final Logger log = LoggerFactory.getLogger(ItMessageServiceImpl.class);

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

    public static final String TYPE_AI_ALERT = "AI_ALERT";

    public static final String TYPE_TICKET = "TICKET";

    public static final String TYPE_ASSIGN = "ASSIGN";

    public static final String TYPE_RESULT = "RESULT";

    private final ItMessageMapper messageMapper;

    private final StringRedisTemplate redisTemplate;

    public ItMessageServiceImpl(ItMessageMapper messageMapper, StringRedisTemplate redisTemplate)
    {
        this.messageMapper = messageMapper;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void send(ItMessage message)
    {
        if (message == null || StringUtils.isEmpty(message.getReceiver()))
        {
            return;
        }
        try
        {
            if (StringUtils.isEmpty(message.getTitle()))
            {
                message.setTitle("系统通知");
            }
            if (StringUtils.isEmpty(message.getMsgType()))
            {
                message.setMsgType("SYSTEM");
            }
            message.setCreateBy(currentUser());
            messageMapper.insertMessage(message);
        }
        catch (Exception e)
        {
            log.warn("[notify] insert failed: {}", e.toString());
        }
    }

    /**
     * AI 异常告警：同一场景每天只发一条，避免模型挂掉时每个请求都推一次。
     */
    @Override
    public void notifyAiAlert(String scene, String detail)
    {
        String day = LocalDate.now().format(DAY);
        String key = "it:notify:ai:" + scene + ":" + day;
        try
        {
            Boolean first = redisTemplate.opsForValue().setIfAbsent(key, "1");
            if (Boolean.FALSE.equals(first))
            {
                return;
            }
            redisTemplate.expire(key, java.time.Duration.ofHours(25));
        }
        catch (Exception e)
        {
            log.warn("[notify] dedupe unavailable, continue: {}", e.toString());
        }
        ItMessage message = new ItMessage();
        message.setReceiver("admin");
        message.setTitle("【AI 告警】" + scene);
        message.setContent(detail);
        message.setMsgType(TYPE_AI_ALERT);
        message.setLink("/it/ticket");
        send(message);
    }

    @Override
    public List<ItMessage> selectByReceiver(String receiver, String readFlag, String msgType, Integer limit)
    {
        return messageMapper.selectMessageList(receiver, readFlag, msgType, limit);
    }

    @Override
    public int countUnread(String receiver)
    {
        return messageMapper.countUnread(receiver);
    }

    @Override
    public int markRead(Long messageId)
    {
        return messageMapper.markRead(currentUser(), messageId);
    }

    @Override
    public int markAllRead()
    {
        return messageMapper.markAllRead(currentUser());
    }

    private String currentUser()
    {
        try
        {
            String name = SecurityUtils.getUsername();
            return StringUtils.isEmpty(name) ? "system" : name;
        }
        catch (Exception e)
        {
            return "system";
        }
    }
}

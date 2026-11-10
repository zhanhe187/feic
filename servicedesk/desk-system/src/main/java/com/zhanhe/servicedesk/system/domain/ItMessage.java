package com.zhanhe.servicedesk.system.domain;

import java.util.Date;

/**
 * 站内消息（通知中心）。
 *
 * <p>为什么单独建表而不是复用 sys_notice：通知公告是「所有人看同一份」，
 * 而派单、处理结果、AI 告警都只该给相关的人看，混在一起等于把别人的工单广播出去。</p>
 */
public class ItMessage
{
    private Long messageId;

    /** 接收人用户名 */
    private String receiver;

    private String title;

    private String content;

    /** AI_ALERT / TICKET / ASSIGN / RESULT / SYSTEM */
    private String msgType;

    private String bizType;

    private Long bizId;

    /** 前端跳转路径，例如 /it/ticket */
    private String link;

    /** 0 未读 1 已读 */
    private String readFlag;

    private String createBy;

    private Date createTime;

    public Long getMessageId()
    {
        return messageId;
    }

    public void setMessageId(Long messageId)
    {
        this.messageId = messageId;
    }

    public String getReceiver()
    {
        return receiver;
    }

    public void setReceiver(String receiver)
    {
        this.receiver = receiver;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public String getMsgType()
    {
        return msgType;
    }

    public void setMsgType(String msgType)
    {
        this.msgType = msgType;
    }

    public String getBizType()
    {
        return bizType;
    }

    public void setBizType(String bizType)
    {
        this.bizType = bizType;
    }

    public Long getBizId()
    {
        return bizId;
    }

    public void setBizId(Long bizId)
    {
        this.bizId = bizId;
    }

    public String getLink()
    {
        return link;
    }

    public void setLink(String link)
    {
        this.link = link;
    }

    public String getReadFlag()
    {
        return readFlag;
    }

    public void setReadFlag(String readFlag)
    {
        this.readFlag = readFlag;
    }

    public String getCreateBy()
    {
        return createBy;
    }

    public void setCreateBy(String createBy)
    {
        this.createBy = createBy;
    }

    public Date getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
    }
}

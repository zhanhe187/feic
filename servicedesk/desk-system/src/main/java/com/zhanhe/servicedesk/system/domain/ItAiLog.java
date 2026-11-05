package com.zhanhe.servicedesk.system.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * AI 调用审计记录。
 * 只落摘要（sha256 前 16 位 + 长度），不落工单原文，避免工单内容被复制到日志表。
 */
public class ItAiLog
{
    private Long aiId;

    /** intake / similar */
    private String scene;

    private Long ticketId;

    private String userName;

    /** llm / rule */
    private String provider;

    private String model;

    /** 0 正常，1 降级 */
    private String degraded;

    private Integer costMs;

    private String inputDigest;

    private String outputDigest;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    public Long getAiId()
    {
        return aiId;
    }

    public void setAiId(Long aiId)
    {
        this.aiId = aiId;
    }

    public String getScene()
    {
        return scene;
    }

    public void setScene(String scene)
    {
        this.scene = scene;
    }

    public Long getTicketId()
    {
        return ticketId;
    }

    public void setTicketId(Long ticketId)
    {
        this.ticketId = ticketId;
    }

    public String getUserName()
    {
        return userName;
    }

    public void setUserName(String userName)
    {
        this.userName = userName;
    }

    public String getProvider()
    {
        return provider;
    }

    public void setProvider(String provider)
    {
        this.provider = provider;
    }

    public String getModel()
    {
        return model;
    }

    public void setModel(String model)
    {
        this.model = model;
    }

    public String getDegraded()
    {
        return degraded;
    }

    public void setDegraded(String degraded)
    {
        this.degraded = degraded;
    }

    public Integer getCostMs()
    {
        return costMs;
    }

    public void setCostMs(Integer costMs)
    {
        this.costMs = costMs;
    }

    public String getInputDigest()
    {
        return inputDigest;
    }

    public void setInputDigest(String inputDigest)
    {
        this.inputDigest = inputDigest;
    }

    public String getOutputDigest()
    {
        return outputDigest;
    }

    public void setOutputDigest(String outputDigest)
    {
        this.outputDigest = outputDigest;
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

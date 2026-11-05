package com.zhanhe.servicedesk.system.ai;

/**
 * 历史工单检索结果（只含检索所需字段，不含申请人等敏感信息）。
 */
public class TicketDoc
{
    private Long ticketId;

    private String ticketNo;

    private String ticketType;

    private String title;

    private String description;

    private String deviceInfo;

    private String symptom;

    private String impactScope;

    /** 最近一次「完成」动作的处理说明，作为解决方案主体 */
    private String processNote;

    /** BM25 原始分 */
    private double score;

    /** 相对相关度：最高分=100，其余按比例，仅用于展示 */
    private int relevance;

    public Long getTicketId()
    {
        return ticketId;
    }

    public void setTicketId(Long ticketId)
    {
        this.ticketId = ticketId;
    }

    public String getTicketNo()
    {
        return ticketNo;
    }

    public void setTicketNo(String ticketNo)
    {
        this.ticketNo = ticketNo;
    }

    public String getTicketType()
    {
        return ticketType;
    }

    public void setTicketType(String ticketType)
    {
        this.ticketType = ticketType;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public String getDeviceInfo()
    {
        return deviceInfo;
    }

    public void setDeviceInfo(String deviceInfo)
    {
        this.deviceInfo = deviceInfo;
    }

    public String getSymptom()
    {
        return symptom;
    }

    public void setSymptom(String symptom)
    {
        this.symptom = symptom;
    }

    public String getImpactScope()
    {
        return impactScope;
    }

    public void setImpactScope(String impactScope)
    {
        this.impactScope = impactScope;
    }

    public String getProcessNote()
    {
        return processNote;
    }

    public void setProcessNote(String processNote)
    {
        this.processNote = processNote;
    }

    public double getScore()
    {
        return score;
    }

    public void setScore(double score)
    {
        this.score = score;
    }

    public int getRelevance()
    {
        return relevance;
    }

    public void setRelevance(int relevance)
    {
        this.relevance = relevance;
    }
}

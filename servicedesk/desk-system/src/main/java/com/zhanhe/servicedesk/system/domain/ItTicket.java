package com.zhanhe.servicedesk.system.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhanhe.servicedesk.common.annotation.Excel;
import com.zhanhe.servicedesk.common.core.domain.BaseEntity;

public class ItTicket extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long ticketId;

    @Excel(name = "Ticket No")
    private String ticketNo;

    @Excel(name = "Applicant")
    private String applicant;

    @Excel(name = "Ticket Type")
    private String ticketType;

    @Excel(name = "Priority")
    private String priority;

    @Excel(name = "Status")
    private String status;

    @Excel(name = "Handler")
    private String handler;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date completeTime;

    private String title;

    private String description;

    /**
     * 以下 5 个字段由「工单描述结构化抽取」写入，AI 只读业务状态，不改 status/handler/applicant。
     */
    @Excel(name = "Device")
    private String deviceInfo;

    @Excel(name = "Symptom")
    private String symptom;

    /** single / multi / blocking / unknown */
    @Excel(name = "Impact")
    private String impactScope;

    @Excel(name = "Tried")
    private String triedActions;

    /** llm / rule / manual / null */
    private String aiSource;

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

    public String getApplicant()
    {
        return applicant;
    }

    public void setApplicant(String applicant)
    {
        this.applicant = applicant;
    }

    public String getTicketType()
    {
        return ticketType;
    }

    public void setTicketType(String ticketType)
    {
        this.ticketType = ticketType;
    }

    public String getPriority()
    {
        return priority;
    }

    public void setPriority(String priority)
    {
        this.priority = priority;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getHandler()
    {
        return handler;
    }

    public void setHandler(String handler)
    {
        this.handler = handler;
    }

    public Date getCompleteTime()
    {
        return completeTime;
    }

    public void setCompleteTime(Date completeTime)
    {
        this.completeTime = completeTime;
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

    public String getTriedActions()
    {
        return triedActions;
    }

    public void setTriedActions(String triedActions)
    {
        this.triedActions = triedActions;
    }

    public String getAiSource()
    {
        return aiSource;
    }

    public void setAiSource(String aiSource)
    {
        this.aiSource = aiSource;
    }
}

package com.zhanhe.servicedesk.system.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhanhe.servicedesk.common.core.domain.BaseEntity;

public class ItTicketRecord extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long recordId;
    private Long ticketId;
    private String processor;
    private String action;
    private String processNote;
    private String processResult;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date processTime;

    public Long getRecordId()
    {
        return recordId;
    }

    public void setRecordId(Long recordId)
    {
        this.recordId = recordId;
    }

    public Long getTicketId()
    {
        return ticketId;
    }

    public void setTicketId(Long ticketId)
    {
        this.ticketId = ticketId;
    }

    public String getProcessor()
    {
        return processor;
    }

    public void setProcessor(String processor)
    {
        this.processor = processor;
    }

    public String getAction()
    {
        return action;
    }

    public void setAction(String action)
    {
        this.action = action;
    }

    public String getProcessNote()
    {
        return processNote;
    }

    public void setProcessNote(String processNote)
    {
        this.processNote = processNote;
    }

    public String getProcessResult()
    {
        return processResult;
    }

    public void setProcessResult(String processResult)
    {
        this.processResult = processResult;
    }

    public Date getProcessTime()
    {
        return processTime;
    }

    public void setProcessTime(Date processTime)
    {
        this.processTime = processTime;
    }
}

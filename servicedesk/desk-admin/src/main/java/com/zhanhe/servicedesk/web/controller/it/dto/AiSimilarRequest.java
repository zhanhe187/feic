package com.zhanhe.servicedesk.web.controller.it.dto;

/**
 * 历史同类工单检索请求。
 * 二选一：传 ticketId（以该工单为查询样本）或传 query（手输关键词）。
 */
public class AiSimilarRequest
{
    private Long ticketId;

    private String query;

    private Integer topK;

    public Long getTicketId()
    {
        return ticketId;
    }

    public void setTicketId(Long ticketId)
    {
        this.ticketId = ticketId;
    }

    public String getQuery()
    {
        return query;
    }

    public void setQuery(String query)
    {
        this.query = query;
    }

    public Integer getTopK()
    {
        return topK;
    }

    public void setTopK(Integer topK)
    {
        this.topK = topK;
    }
}

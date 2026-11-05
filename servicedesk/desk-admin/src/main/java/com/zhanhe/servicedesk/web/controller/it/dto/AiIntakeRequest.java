package com.zhanhe.servicedesk.web.controller.it.dto;

/**
 * 工单信息补全请求。
 */
public class AiIntakeRequest
{
    private String title;

    private String description;

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
}

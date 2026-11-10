package com.zhanhe.servicedesk.system.domain;

import java.util.Date;

/**
 * AI 配置项（独立表 it_ai_config）。
 *
 * <p>不放进 sys_config 的原因：AI 参数有自己的页面与生效方式（改完即时生效、Key 需要掩码），
 * 和「系统参数」混在一起会让参数设置页塞满 AI 项，也让两边的权限很难分开。</p>
 */
public class ItAiConfig
{
    private String configKey;

    private String configValue;

    private String remark;

    private String updateBy;

    private Date updateTime;

    public String getConfigKey()
    {
        return configKey;
    }

    public void setConfigKey(String configKey)
    {
        this.configKey = configKey;
    }

    public String getConfigValue()
    {
        return configValue;
    }

    public void setConfigValue(String configValue)
    {
        this.configValue = configValue;
    }

    public String getRemark()
    {
        return remark;
    }

    public void setRemark(String remark)
    {
        this.remark = remark;
    }

    public String getUpdateBy()
    {
        return updateBy;
    }

    public void setUpdateBy(String updateBy)
    {
        this.updateBy = updateBy;
    }

    public Date getUpdateTime()
    {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime)
    {
        this.updateTime = updateTime;
    }
}

package com.zhanhe.servicedesk.system.ai;

/**
 * 工单描述结构化抽取的结果。
 *
 * <p>注意：这里的字段只是「工单信息」，不包含任何业务状态。
 * status / handler / applicant 一律由原有建单逻辑与状态机维护，AI 无权改动。</p>
 */
public class IntakeResult
{
    /** 设备/系统，如 ThinkPad T14、VPN、邮箱 */
    private String deviceInfo;

    /** 现象或报错原文 */
    private String symptom;

    /** single / multi / blocking / unknown */
    private String impactScope = SlotSchema.IMPACT_UNKNOWN;

    /** 已尝试的操作 */
    private String triedActions;

    /** 建议工单类型，必须命中 SlotSchema.TICKET_TYPES */
    private String suggestedType = SlotSchema.TYPE_OTHER;

    /** 类型判定依据（给人看的一句话） */
    private String typeBasis;

    /** 建议优先级（由规则推导，非模型自由决定） */
    private String suggestedPriority = PriorityRule.PRIORITY_LOW;

    /** 被丢弃的槽位及原因（模型给了值但原文找不到证据） */
    private java.util.List<String> rejected = new java.util.ArrayList<>();

    /** llm | rule */
    private String source = "rule";

    /** 是否发生了降级（模型不可用/失败） */
    private boolean degraded;

    /** 给前端的提示语（额度用尽、熔断、解析失败等），无则可空 */
    private String notice;

    private long costMs;

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

    public String getSuggestedType()
    {
        return suggestedType;
    }

    public void setSuggestedType(String suggestedType)
    {
        this.suggestedType = suggestedType;
    }

    public String getTypeBasis()
    {
        return typeBasis;
    }

    public void setTypeBasis(String typeBasis)
    {
        this.typeBasis = typeBasis;
    }

    public String getSuggestedPriority()
    {
        return suggestedPriority;
    }

    public void setSuggestedPriority(String suggestedPriority)
    {
        this.suggestedPriority = suggestedPriority;
    }

    public java.util.List<String> getRejected()
    {
        return rejected;
    }

    public void setRejected(java.util.List<String> rejected)
    {
        this.rejected = rejected == null ? new java.util.ArrayList<>() : rejected;
    }

    public String getSource()
    {
        return source;
    }

    public void setSource(String source)
    {
        this.source = source;
    }

    public boolean isDegraded()
    {
        return degraded;
    }

    public void setDegraded(boolean degraded)
    {
        this.degraded = degraded;
    }

    public String getNotice()
    {
        return notice;
    }

    public void setNotice(String notice)
    {
        this.notice = notice;
    }

    public long getCostMs()
    {
        return costMs;
    }

    public void setCostMs(long costMs)
    {
        this.costMs = costMs;
    }
}

package com.zhanhe.workforce.controller.emp.dto;

/**
 * 续签/不续签请求体。日期用 yyyy-MM-dd 字符串传，由 Controller 严格解析（拒绝非法日期）。
 */
public class ContractRenewDto {

    private String beginDate;
    private String endDate;
    private String reason;

    public String getBeginDate() {
        return beginDate;
    }

    public void setBeginDate(String beginDate) {
        this.beginDate = beginDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}

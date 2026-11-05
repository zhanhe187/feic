package com.zhanhe.workforce.model;

import java.util.Date;

/**
 * 员工生命周期跟进记录（转正 / 续签 / 不续签）。
 *
 * <p>为什么单独建表而不是只写提醒表的 remark：动作需要可追溯 —— 谁、什么时候、
 * 合同到期日从哪天改到哪天、为什么。提醒表只解决"该提醒了"，这张表才回答"后来怎么处理的"。</p>
 */
public class EmployeeLifecycleAction {

    public static final String ACTION_REGULARIZE = "REGULARIZE";
    public static final String ACTION_RENEW = "RENEW";
    public static final String ACTION_NOT_RENEW = "NOT_RENEW";

    private Integer id;
    private Integer empId;
    private String actionType;
    private Date beforeEndContract;
    private Date afterEndContract;
    private String reason;
    private String operator;
    private Date createTime;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getEmpId() {
        return empId;
    }

    public void setEmpId(Integer empId) {
        this.empId = empId;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public Date getBeforeEndContract() {
        return beforeEndContract;
    }

    public void setBeforeEndContract(Date beforeEndContract) {
        this.beforeEndContract = beforeEndContract;
    }

    public Date getAfterEndContract() {
        return afterEndContract;
    }

    public void setAfterEndContract(Date afterEndContract) {
        this.afterEndContract = afterEndContract;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}

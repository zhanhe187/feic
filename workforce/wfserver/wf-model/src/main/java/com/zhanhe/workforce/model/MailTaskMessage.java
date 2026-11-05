package com.zhanhe.workforce.model;

import java.io.Serializable;

/**
 * 提醒邮件的投递消息体。
 *
 * <p>投递与发信是两件事：生产端只负责把消息丢进队列并落一条发送日志，
 * 真正的 SMTP 发送由消费端完成。消息里同时带 msgId，消费端才能精确回写
 * 对应那一条日志的状态，而不是按员工模糊匹配。
 */
public class MailTaskMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 发送日志主键，与 mail_send_log.msgId 对应 */
    private String msgId;

    /** 员工 id，消费端据此取员工与收件地址 */
    private Integer empId;

    public MailTaskMessage() {
    }

    public MailTaskMessage(String msgId, Integer empId) {
        this.msgId = msgId;
        this.empId = empId;
    }

    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    public Integer getEmpId() {
        return empId;
    }

    public void setEmpId(Integer empId) {
        this.empId = empId;
    }

    @Override
    public String toString() {
        return "MailTaskMessage{msgId='" + msgId + "', empId=" + empId + "}";
    }
}

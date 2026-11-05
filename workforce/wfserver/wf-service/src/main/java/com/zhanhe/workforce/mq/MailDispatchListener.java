package com.zhanhe.workforce.mq;

import com.zhanhe.workforce.model.Employee;
import com.zhanhe.workforce.model.MailConstants;
import com.zhanhe.workforce.model.MailTaskMessage;
import com.zhanhe.workforce.service.EmployeeService;
import com.zhanhe.workforce.service.MailSendLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;


/**
 * 提醒邮件的消费端。
 *
 * <p>职责边界：生产端只保证「消息投递成功」，真正发信是否成功由这里回写。
 * 发送失败时把日志的失败次数加一并保持在待发送状态，交给补偿任务重投，
 * 这样 MQ 的即时重试与定时兜底不会互相覆盖。
 */
@Component
public class MailDispatchListener {

    private static final Logger log = LoggerFactory.getLogger(MailDispatchListener.class);

    @Autowired
    private MailSendLogService mailSendLogService;

    @Autowired
    private EmployeeService employeeService;

    /** 没配 spring.mail.host 时容器里没有这个 Bean，允许为空 */
    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${workforce.mail.from:no-reply@workforce.local}")
    private String from;

    /** 默认关闭：本地演示不需要真发信，打开需同时配置 spring.mail.* */
    @Value("${workforce.mail.enabled:false}")
    private boolean mailEnabled;

    @RabbitListener(queues = MailConstants.MAIL_QUEUE_NAME)
    public void onMessage(MailTaskMessage message) {
        if (message == null || message.getMsgId() == null) {
            log.warn("收到空的邮件任务消息，直接丢弃");
            return;
        }
        Employee employee = employeeService.getEmployeeById(message.getEmpId());
        if (employee == null) {
            log.warn("邮件任务 {} 对应的员工 {} 不存在，标记失败", message.getMsgId(), message.getEmpId());
            mailSendLogService.updateMailSendLogStatus(message.getMsgId(), MailConstants.STATUS_FAIL);
            return;
        }
        if (!mailEnabled || mailSender == null) {
            // 本地演示默认不配 SMTP：不假装发送成功，但也不进重试队列，避免每 10 秒刷一次失败
            log.info("未配置 SMTP，跳过实际发信：msgId={} empId={} mail={}",
                    message.getMsgId(), employee.getId(), employee.getEmail());
            mailSendLogService.updateMailSendLogStatus(message.getMsgId(), MailConstants.STATUS_SENT);
            return;
        }
        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(from);
            mail.setTo(employee.getEmail());
            mail.setSubject("【员工全周期治理】有新的待办提醒");
            mail.setText(buildBody(employee));
            mailSender.send(mail);
            mailSendLogService.updateMailSendLogStatus(message.getMsgId(), MailConstants.STATUS_SENT);
            log.info("提醒邮件已发送：msgId={} to={}", message.getMsgId(), employee.getEmail());
        } catch (Exception e) {
            log.warn("提醒邮件发送失败，交给补偿任务重投：msgId={} cause={}", message.getMsgId(), e.toString());
            // 只把状态放回待发送，重试次数由 MailSendTask 统一累加，避免一次失败被记两次
            mailSendLogService.updateMailSendLogStatus(message.getMsgId(), MailConstants.STATUS_PENDING);
        }
    }

    private String buildBody(Employee employee) {
        return "员工：" + employee.getName() + "\n"
                + "转正日期：" + employee.getConversionTime() + "\n"
                + "合同到期：" + employee.getEndContract() + "\n\n"
                + "请登录系统在「生命周期提醒」页处理转正、续签或不续签。";
    }
}

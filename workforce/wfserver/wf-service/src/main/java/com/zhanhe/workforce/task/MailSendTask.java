package com.zhanhe.workforce.task;

import com.zhanhe.workforce.model.MailConstants;
import com.zhanhe.workforce.model.MailSendLog;
import com.zhanhe.workforce.model.MailTaskMessage;
import com.zhanhe.workforce.service.EmployeeService;
import com.zhanhe.workforce.service.MailSendLogService;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Component
public class MailSendTask {
    @Autowired
    MailSendLogService mailSendLogService;
    @Autowired
    RabbitTemplate rabbitTemplate;
    @Autowired
    EmployeeService employeeService;
    @Scheduled(cron = "0/10 * * * * ?")
    public void mailResendTask() {
        List<MailSendLog> logs = mailSendLogService.getMailSendLogsByStatus();
        if (logs == null || logs.size() == 0) {
            return;
        }
        logs.forEach(mailSendLog->{
            Integer retryCount = mailSendLog.getCount() == null ? 0 : mailSendLog.getCount();
            if (retryCount >= 3) {
                mailSendLogService.updateMailSendLogStatus(mailSendLog.getMsgId(), 2);//直接设置该条消息发送失败
            }else{
                mailSendLogService.updateCount(mailSendLog.getMsgId(), new Date());
                rabbitTemplate.convertAndSend(MailConstants.MAIL_EXCHANGE_NAME, MailConstants.MAIL_ROUTING_KEY_NAME,
                        new MailTaskMessage(mailSendLog.getMsgId(), mailSendLog.getEmpId()),
                        new CorrelationData(mailSendLog.getMsgId()));
            }
        });
    }
}

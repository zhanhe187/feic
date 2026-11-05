package com.zhanhe.workforce.task;

import com.zhanhe.workforce.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EmployeeLifecycleTask {
    @Autowired
    EmployeeService employeeService;

    @Scheduled(cron = "0 0 9 * * ?")
    public void scanLifecycleReminders() {
        employeeService.scanLifecycleReminders();
    }
}

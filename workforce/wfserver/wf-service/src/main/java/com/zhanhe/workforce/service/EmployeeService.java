package com.zhanhe.workforce.service;

import com.zhanhe.workforce.mapper.EmployeeLifecycleActionMapper;
import com.zhanhe.workforce.mapper.EmployeeLifecycleReminderMapper;
import com.zhanhe.workforce.mapper.EmployeeMapper;
import com.zhanhe.workforce.model.Employee;
import com.zhanhe.workforce.model.EmployeeLifecycleAction;
import com.zhanhe.workforce.model.EmployeeLifecycleReminder;
import com.zhanhe.workforce.model.MailConstants;
import com.zhanhe.workforce.model.MailSendLog;
import com.zhanhe.workforce.model.MailTaskMessage;
import com.zhanhe.workforce.model.RespPageBean;
import com.zhanhe.workforce.utils.HrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class EmployeeService {
    @Autowired
    EmployeeMapper employeeMapper;
    @Autowired
    EmployeeLifecycleReminderMapper employeeLifecycleReminderMapper;
    @Autowired
    EmployeeLifecycleActionMapper employeeLifecycleActionMapper;
    @Autowired
    RabbitTemplate rabbitTemplate;
    @Autowired
    MailSendLogService mailSendLogService;
    public final static Logger logger = LoggerFactory.getLogger(EmployeeService.class);
    SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy");
    SimpleDateFormat monthFormat = new SimpleDateFormat("MM");
    DecimalFormat decimalFormat = new DecimalFormat("##.00");

    public RespPageBean getEmployeeByPage(Integer page, Integer size, Employee employee, Date[] beginDateScope) {
        if (page != null && size != null) {
            page = (page - 1) * size;
        }
        List<Employee> data = employeeMapper.getEmployeeByPage(page, size, employee, beginDateScope);
        Long total = employeeMapper.getTotal(employee, beginDateScope);
        RespPageBean bean = new RespPageBean();
        bean.setData(data);
        bean.setTotal(total);
        return bean;
    }

    public Integer addEmp(Employee employee) {
        EmployeeLifecyclePolicy.validateContractDates(employee);
        Date beginContract = employee.getBeginContract();
        Date endContract = employee.getEndContract();
        if (beginContract != null && endContract != null) {
            double month = (Double.parseDouble(yearFormat.format(endContract)) - Double.parseDouble(yearFormat.format(beginContract))) * 12 + (Double.parseDouble(monthFormat.format(endContract)) - Double.parseDouble(monthFormat.format(beginContract)));
            employee.setContractTerm(Double.parseDouble(decimalFormat.format(month / 12)));
        }
        int result = employeeMapper.insertSelective(employee);
        if (result == 1) {
            Employee emp = employeeMapper.getEmployeeById(employee.getId());
            //生成消息的唯一id
            String msgId = UUID.randomUUID().toString();
            MailSendLog mailSendLog = new MailSendLog();
            mailSendLog.setMsgId(msgId);
            mailSendLog.setCreateTime(new Date());
            mailSendLog.setExchange(MailConstants.MAIL_EXCHANGE_NAME);
            mailSendLog.setRouteKey(MailConstants.MAIL_ROUTING_KEY_NAME);
            mailSendLog.setEmpId(emp.getId());
            mailSendLog.setTryTime(new Date(System.currentTimeMillis() + 1000 * 60 * MailConstants.MSG_TIMEOUT));
            mailSendLogService.insert(mailSendLog);
            rabbitTemplate.convertAndSend(MailConstants.MAIL_EXCHANGE_NAME, MailConstants.MAIL_ROUTING_KEY_NAME,
                new MailTaskMessage(msgId, emp.getId()), new CorrelationData(msgId));
        }
        return result;
    }

    public Integer maxWorkID() {
        return employeeMapper.maxWorkID();
    }

    public Integer deleteEmpByEid(Integer id) {
        return employeeMapper.deleteByPrimaryKey(id);
    }

    public Integer updateEmp(Employee employee) {
        EmployeeLifecyclePolicy.validateContractDates(employee);
        return employeeMapper.updateByPrimaryKeySelective(employee);
    }

    public Integer addEmps(List<Employee> list) {
        return employeeMapper.addEmps(list);
    }

    public RespPageBean getEmployeeByPageWithSalary(Integer page, Integer size) {
        if (page != null && size != null) {
            page = (page - 1) * size;
        }
        List<Employee> list = employeeMapper.getEmployeeByPageWithSalary(page, size);
        RespPageBean respPageBean = new RespPageBean();
        respPageBean.setData(list);
        respPageBean.setTotal(employeeMapper.getTotal(null, null));
        return respPageBean;
    }

    public Integer updateEmployeeSalaryById(Integer eid, Integer sid) {
        return employeeMapper.updateEmployeeSalaryById(eid, sid);
    }

    public Employee getEmployeeById(Integer empId) {
        return employeeMapper.getEmployeeById(empId);
    }

    public RespPageBean getProbationEmployees(Integer page, Integer size, Employee employee) {
        if (page != null && size != null) {
            page = (page - 1) * size;
        }
        Date endDate = addDays(new Date(), 7);
        RespPageBean bean = new RespPageBean();
        bean.setData(employeeMapper.getProbationEmployees(page, size, employee, endDate));
        bean.setTotal(employeeMapper.getProbationTotal(employee, endDate));
        return bean;
    }

    public RespPageBean getContractExpireEmployees(Integer page, Integer size, Employee employee) {
        if (page != null && size != null) {
            page = (page - 1) * size;
        }
        Date endDate = addDays(new Date(), 7);
        RespPageBean bean = new RespPageBean();
        bean.setData(employeeMapper.getContractExpireEmployees(page, size, employee, endDate));
        bean.setTotal(employeeMapper.getContractExpireTotal(employee, endDate));
        return bean;
    }

    @Transactional
    public Integer regularizeEmployee(Integer empId) {
        Employee emp = employeeMapper.getEmployeeById(empId);
        if (emp == null) {
            throw new IllegalArgumentException("员工不存在");
        }
        EmployeeLifecyclePolicy.assertCanRegularize(emp);
        Employee update = new Employee();
        update.setId(empId);
        update.setWorkState(EmployeeLifecyclePolicy.STATE_FORMAL);
        update.setLifecycleReminderStatus("DONE");
        int rows = employeeMapper.updateByPrimaryKeySelective(update);
        insertLifecycleAction(empId, EmployeeLifecycleAction.ACTION_REGULARIZE, emp.getEndContract(), emp.getEndContract(),
                "HR 执行转正");
        return rows;
    }

    /**
     * 续签合同：校验 → 更新合同起止与合同期限 → 写跟进记录。
     * 只有「动作 + 留痕 + 提醒状态」三件事一起做，HR 的处理过程才闭环。
     */
    @Transactional
    public Integer renewContract(Integer empId, Date newBegin, Date newEnd, String reason) {
        Employee emp = employeeMapper.getEmployeeById(empId);
        ContractRenewPolicy.assertCanRenew(emp, newBegin, newEnd);
        Employee update = new Employee();
        update.setId(empId);
        update.setBeginContract(newBegin);
        update.setEndContract(newEnd);
        update.setContractTerm(computeContractTerm(newBegin, newEnd));
        update.setLifecycleReminderStatus("DONE");
        int rows = employeeMapper.updateByPrimaryKeySelective(update);
        insertLifecycleAction(empId, EmployeeLifecycleAction.ACTION_RENEW, emp.getEndContract(), newEnd,
                reason == null || reason.trim().isEmpty() ? "HR 执行续签" : reason.trim());
        return rows;
    }

    /**
     * 标记不续签：只写跟进记录并把提醒置为已处理，<b>不改在职状态</b>（离职属于人事处理，不越权）。
     */
    @Transactional
    public Integer markNotRenew(Integer empId, String reason) {
        Employee emp = employeeMapper.getEmployeeById(empId);
        ContractRenewPolicy.assertCanNotRenew(emp, reason);
        Employee update = new Employee();
        update.setId(empId);
        update.setLifecycleReminderStatus("DONE");
        int rows = employeeMapper.updateByPrimaryKeySelective(update);
        insertLifecycleAction(empId, EmployeeLifecycleAction.ACTION_NOT_RENEW, emp.getEndContract(), null, reason.trim());
        return rows;
    }

    public List<EmployeeLifecycleAction> listLifecycleActions(Integer empId) {
        return employeeLifecycleActionMapper.selectByEmpId(empId);
    }

    public EmployeeLifecycleAction latestAction(Integer empId, String actionType) {
        return employeeLifecycleActionMapper.selectLatestByType(empId, actionType);
    }

    private void insertLifecycleAction(Integer empId, String actionType, Date before, Date after, String reason) {
        EmployeeLifecycleAction action = new EmployeeLifecycleAction();
        action.setEmpId(empId);
        action.setActionType(actionType);
        action.setBeforeEndContract(before);
        action.setAfterEndContract(after);
        action.setReason(reason);
        String operator;
        try {
            operator = HrUtils.getCurrentHr().getUsername();
        } catch (Exception e) {
            operator = "system";
        }
        action.setOperator(operator);
        action.setCreateTime(new Date());
        employeeLifecycleActionMapper.insertAction(action);
    }

    /** 与 addEmp 保持同一套口径，避免同一份合同在不同入口算出不同的年限 */
    private Double computeContractTerm(Date begin, Date end) {
        double month = (Double.parseDouble(yearFormat.format(end)) - Double.parseDouble(yearFormat.format(begin))) * 12
                + (Double.parseDouble(monthFormat.format(end)) - Double.parseDouble(monthFormat.format(begin)));
        return Double.parseDouble(decimalFormat.format(month / 12));
    }

    public Integer scanLifecycleReminders() {
        Date endDate = addDays(new Date(), 7);
        List<Employee> employees = employeeMapper.getLifecycleReminderCandidates(endDate);
        int created = 0;
        for (Employee employee : employees) {
            if (needProbationReminder(employee, endDate)) {
                created += createReminderAndSend(employee, "PROBATION", "员工即将转正，请及时跟进");
            }
            if (needContractReminder(employee, endDate)) {
                created += createReminderAndSend(employee, "CONTRACT", "员工合同即将到期，请及时续签或处理");
            }
        }
        return created;
    }

    private int createReminderAndSend(Employee employee, String remindType, String remark) {
        Date now = new Date();
        EmployeeLifecycleReminder reminder = new EmployeeLifecycleReminder();
        reminder.setEmpId(employee.getId());
        reminder.setRemindType(remindType);
        reminder.setRemindStatus("CREATED");
        reminder.setRemark(remark);
        reminder.setRemindDate(now);
        reminder.setCreateTime(now);
        reminder.setUpdateTime(now);
        Integer inserted = employeeLifecycleReminderMapper.insertIgnore(reminder);
        if (inserted != null && inserted > 0) {
            sendLifecycleMail(employee);
            Employee update = new Employee();
            update.setId(employee.getId());
            update.setLifecycleReminderStatus("SENT");
            employeeMapper.updateByPrimaryKeySelective(update);
            return 1;
        }
        return 0;
    }

    private void sendLifecycleMail(Employee emp) {
        String msgId = UUID.randomUUID().toString();
        MailSendLog mailSendLog = new MailSendLog();
        mailSendLog.setMsgId(msgId);
        mailSendLog.setCreateTime(new Date());
        mailSendLog.setExchange(MailConstants.MAIL_EXCHANGE_NAME);
        mailSendLog.setRouteKey(MailConstants.MAIL_ROUTING_KEY_NAME);
        mailSendLog.setEmpId(emp.getId());
        mailSendLog.setTryTime(new Date(System.currentTimeMillis() + 1000 * 60 * MailConstants.MSG_TIMEOUT));
        mailSendLogService.insert(mailSendLog);
        rabbitTemplate.convertAndSend(MailConstants.MAIL_EXCHANGE_NAME, MailConstants.MAIL_ROUTING_KEY_NAME,
                new MailTaskMessage(msgId, emp.getId()), new CorrelationData(msgId));
    }

    private boolean needProbationReminder(Employee employee, Date endDate) {
        Date today = startOfToday();
        return employee.getConversionTime() != null
                && !EmployeeLifecyclePolicy.STATE_FORMAL.equals(employee.getWorkState())
                && !employee.getConversionTime().before(today)
                && !employee.getConversionTime().after(endDate);
    }

    private boolean needContractReminder(Employee employee, Date endDate) {
        Date today = startOfToday();
        return employee.getEndContract() != null
                && !"离职".equals(employee.getWorkState())
                && !employee.getEndContract().before(today)
                && !employee.getEndContract().after(endDate);
    }

    private Date startOfToday() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    private Date addDays(Date date, int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_MONTH, days);
        return calendar.getTime();
    }
}

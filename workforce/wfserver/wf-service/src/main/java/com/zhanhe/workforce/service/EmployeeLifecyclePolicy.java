package com.zhanhe.workforce.service;

import com.zhanhe.workforce.model.Employee;

public final class EmployeeLifecyclePolicy {
    public static final String STATE_PROBATION = "试用期";
    public static final String STATE_FORMAL = "正式";
    public static final String STATE_ON_JOB = "在职";

    private EmployeeLifecyclePolicy() {
    }

    public static void assertCanRegularize(Employee employee) {
        if (employee == null) {
            throw new IllegalArgumentException("员工信息不能为空");
        }
        String state = employee.getWorkState();
        if (STATE_FORMAL.equals(state)) {
            throw new IllegalStateException("员工已转正，不能重复转正");
        }
        if (state != null && !STATE_PROBATION.equals(state) && !STATE_ON_JOB.equals(state)) {
            throw new IllegalStateException("当前员工状态不允许转正");
        }
    }

    public static void validateContractDates(Employee employee) {
        if (employee == null) {
            throw new IllegalArgumentException("员工信息不能为空");
        }
        if (employee.getBeginContract() != null
                && employee.getEndContract() != null
                && employee.getEndContract().before(employee.getBeginContract())) {
            throw new IllegalArgumentException("合同结束日期不能早于合同开始日期");
        }
        if (employee.getBeginDate() != null
                && employee.getConversionTime() != null
                && employee.getConversionTime().before(employee.getBeginDate())) {
            throw new IllegalArgumentException("转正日期不能早于入职日期");
        }
    }
}

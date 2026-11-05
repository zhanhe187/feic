package com.zhanhe.workforce.service;

import com.zhanhe.workforce.model.Employee;

import java.util.Date;

/**
 * 合同续签 / 不续签的业务规则。
 *
 * <p>放在 Service 层统一收口的原因和转正一样：这些规则不能只靠前端校验，
 * 否则任何人绕过页面直接调接口就能写出互相重叠的合同期、给离职员工续签、
 * 或者不填原因就把人标成不续签。</p>
 */
public final class ContractRenewPolicy {

    /** 提醒窗口：与查询里的「7 天内」保持一致 */
    public static final int REMIND_WINDOW_DAYS = 7;

    /** 冷静期：30 天内已决策不续签的员工不再重复提醒 */
    public static final int COOL_DOWN_DAYS = 30;

    public static final String STATE_LEFT = "离职";

    private ContractRenewPolicy() {
    }

    /**
     * 续签前置校验。
     *
     * @param employee 员工（需含 workState / endContract）
     * @param newBegin 新合同起始日
     * @param newEnd   新合同到期日
     * @throws IllegalArgumentException 参数缺失或日期非法
     * @throws IllegalStateException    业务状态不允许
     */
    public static void assertCanRenew(Employee employee, Date newBegin, Date newEnd) {
        if (employee == null) {
            throw new IllegalArgumentException("员工不存在");
        }
        if (STATE_LEFT.equals(employee.getWorkState())) {
            throw new IllegalStateException("已离职员工不能续签合同");
        }
        if (newBegin == null || newEnd == null) {
            throw new IllegalArgumentException("请填写新合同起止日期");
        }
        if (!newEnd.after(newBegin)) {
            throw new IllegalArgumentException("新合同到期日必须晚于起始日");
        }
        if (employee.getEndContract() != null && newBegin.before(employee.getEndContract())) {
            // 允许与当前到期日同一天续接，但不允许重叠，否则工龄/合同期计算会出现负数
            throw new IllegalStateException("新合同起始日不能早于当前合同到期日");
        }
        Date today = startOfToday();
        if (!newEnd.after(today)) {
            throw new IllegalArgumentException("新合同到期日必须晚于今天");
        }
    }

    /**
     * 不续签校验：原因必填；已离职无需再标。
     */
    public static void assertCanNotRenew(Employee employee, String reason) {
        if (employee == null) {
            throw new IllegalArgumentException("员工不存在");
        }
        if (STATE_LEFT.equals(employee.getWorkState())) {
            throw new IllegalStateException("员工已离职，无需再标记不续签");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("请填写不续签原因");
        }
    }

    private static Date startOfToday() {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
        calendar.set(java.util.Calendar.MINUTE, 0);
        calendar.set(java.util.Calendar.SECOND, 0);
        calendar.set(java.util.Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }
}

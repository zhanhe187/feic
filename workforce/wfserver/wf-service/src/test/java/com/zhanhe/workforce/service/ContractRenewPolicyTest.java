package com.zhanhe.workforce.service;

import com.zhanhe.workforce.model.Employee;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 续签/不续签规则的单测。
 * 这些分支如果没有测试，很容易在上线后被"绕过接口直接改库"或"前端表单校验被跳过"冲垮。
 */
class ContractRenewPolicyTest {

    private Employee employee(String workState, String endContract) {
        Employee employee = new Employee();
        employee.setWorkState(workState);
        if (endContract != null) {
            employee.setEndContract(date(endContract));
        }
        return employee;
    }

    @Test
    void rejectsRenewForResignedEmployee() {
        Employee employee = employee("离职", "2026-12-31");
        assertThrows(IllegalStateException.class,
                () -> ContractRenewPolicy.assertCanRenew(employee, date("2027-01-01"), date("2028-01-01")));
    }

    @Test
    void rejectsRenewWhenEndNotAfterBegin() {
        Employee employee = employee("在职", "2026-12-31");
        assertThrows(IllegalArgumentException.class,
                () -> ContractRenewPolicy.assertCanRenew(employee, date("2027-06-01"), date("2027-01-01")));
    }

    @Test
    void rejectsOverlappingContract() {
        // 新合同起点早于当前到期日 → 合同期重叠，工龄/合同期限会被算成负数
        Employee employee = employee("在职", "2027-03-01");
        assertThrows(IllegalStateException.class,
                () -> ContractRenewPolicy.assertCanRenew(employee, date("2027-01-01"), date("2028-01-01")));
    }

    @Test
    void rejectsExpiredNewEnd() {
        Employee employee = employee("在职", "2026-12-31");
        String past = LocalDate.now().minusDays(10).toString();
        assertThrows(IllegalArgumentException.class,
                () -> ContractRenewPolicy.assertCanRenew(employee, date(past), date(past)));
    }

    @Test
    void allowsNormalRenew() {
        Employee employee = employee("在职", "2026-12-31");
        assertDoesNotThrow(() -> ContractRenewPolicy.assertCanRenew(employee, date("2026-12-31"), date("2027-12-31")));
    }

    @Test
    void notRenewRequiresReason() {
        Employee employee = employee("在职", "2026-12-31");
        assertThrows(IllegalArgumentException.class, () -> ContractRenewPolicy.assertCanNotRenew(employee, "  "));
        assertThrows(IllegalArgumentException.class, () -> ContractRenewPolicy.assertCanNotRenew(employee, null));
        assertDoesNotThrow(() -> ContractRenewPolicy.assertCanNotRenew(employee, "岗位取消"));
    }

    @Test
    void notRenewRejectsResignedEmployee() {
        Employee employee = employee("离职", "2026-12-31");
        assertThrows(IllegalStateException.class, () -> ContractRenewPolicy.assertCanNotRenew(employee, "岗位取消"));
    }

    @Test
    void coolDownConstantMatchesQuery() {
        // 提醒查询里的 30 天冷静期与常量保持一致，避免两处口径漂移
        assertTrue(ContractRenewPolicy.COOL_DOWN_DAYS == 30);
    }

    private Date date(String value) {
        return Date.from(LocalDate.parse(value).atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}

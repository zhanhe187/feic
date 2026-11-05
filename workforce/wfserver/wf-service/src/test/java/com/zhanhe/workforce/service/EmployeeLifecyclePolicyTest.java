package com.zhanhe.workforce.service;

import com.zhanhe.workforce.model.Employee;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EmployeeLifecyclePolicyTest {

    @Test
    void rejectDuplicateRegularize() {
        Employee employee = new Employee();
        employee.setWorkState(EmployeeLifecyclePolicy.STATE_FORMAL);

        assertThrows(IllegalStateException.class, () -> EmployeeLifecyclePolicy.assertCanRegularize(employee));
    }

    @Test
    void allowProbationRegularize() {
        Employee employee = new Employee();
        employee.setWorkState(EmployeeLifecyclePolicy.STATE_PROBATION);

        assertDoesNotThrow(() -> EmployeeLifecyclePolicy.assertCanRegularize(employee));
    }

    @Test
    void rejectContractEndBeforeBegin() {
        Employee employee = new Employee();
        employee.setBeginContract(date("2026-07-20"));
        employee.setEndContract(date("2026-07-19"));

        assertThrows(IllegalArgumentException.class, () -> EmployeeLifecyclePolicy.validateContractDates(employee));
    }

    private Date date(String value) {
        return Date.from(LocalDate.parse(value).atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}

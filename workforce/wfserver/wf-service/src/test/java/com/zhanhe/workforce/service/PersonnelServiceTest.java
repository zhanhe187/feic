package com.zhanhe.workforce.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * 人事从属记录接口的连通性测试（连本地 vhr 库）。
 * 跑法：mvn -pl vhrserver/vhr-service -am test -Dstatistics.it=true
 */
@EnabledIfSystemProperty(named = "statistics.it", matches = "true")
class PersonnelServiceTest
{
    private PersonnelService service()
    {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
        ds.setUrl("jdbc:mysql://127.0.0.1:3306/vhr?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai");
        ds.setUsername("root");
        ds.setPassword("root");
        return new PersonnelService(new JdbcTemplate(ds));
    }

    @Test
    void kindsAreWhitelisted()
    {
        PersonnelService service = service();
        assertTrue(service.supports("ec"));
        assertTrue(service.supports("train"));
        assertTrue(service.supports("adjust"));
        assertTrue(service.supports("move"));
        assertFalse(service.supports("employee"), "员工表不允许通过通用接口读写");
        assertFalse(service.supports("sys_user"));
    }

    @Test
    void listReturnsJoinedRows()
    {
        PersonnelService service = service();
        for (String kind : new String[] { "ec", "train", "adjust", "move" })
        {
            List<Map<String, Object>> rows = service.list(kind, 1, 5, null);
            assertNotNull(rows, kind);
            assertTrue(service.count(kind, null) >= rows.size(), kind);
            if (!rows.isEmpty())
            {
                assertNotNull(rows.get(0).get("empName"), kind + " 应带出员工姓名");
            }
        }
    }

    @Test
    void salaryAuditAndSummaryWork()
    {
        PersonnelService service = service();
        assertEquals(service.salaryCount(null, null), service.salary(1, 100, null, null).size());
        assertTrue(service.audit(10).size() <= 10);
        Map<String, Object> summary = service.summary();
        assertTrue(((Integer) summary.get("employees")) > 0);
        assertNotNull(summary.get("tables"));
        Map<String, Object> month = service.monthAdjust(null);
        assertNotNull(month.get("month"));
    }
}

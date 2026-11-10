package com.zhanhe.workforce.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * 统计口径的连通性测试。
 *
 * <p>默认跳过：需要本地 vhr 库。跑法：
 * {@code mvn -pl wfserver/wf-service -am test -Dstatistics.it=true}</p>
 */
@EnabledIfSystemProperty(named = "statistics.it", matches = "true")
class StatisticsServiceTest
{
    private StatisticsService service()
    {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
        ds.setUrl("jdbc:mysql://127.0.0.1:3306/vhr?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai");
        ds.setUsername("root");
        ds.setPassword("root");
        return new StatisticsService(new JdbcTemplate(ds));
    }

    @Test
    void overviewHasCounts()
    {
        Map<String, Object> data = service().overview();
        assertNotNull(data.get("total"));
        assertTrue(((Integer) data.get("total")) > 0, "员工总数应大于 0");
        assertTrue(data.get("departments") instanceof List);
    }

    @Test
    void scoreAndRecordsReturnLists()
    {
        StatisticsService service = service();
        assertNotNull(service.score().get("rows"));
        Map<String, Object> records = service.records();
        for (String key : new String[] { "appraise", "ec", "adjust", "remove", "train" })
        {
            assertTrue(records.get(key) instanceof List, key + " 应为列表");
        }
        assertTrue(service.personnel().size() >= 4, "人事统计应有四组分布");
    }
}

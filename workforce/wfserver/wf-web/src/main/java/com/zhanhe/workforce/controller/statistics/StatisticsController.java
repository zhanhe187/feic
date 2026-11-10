package com.zhanhe.workforce.controller.statistics;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zhanhe.workforce.service.StatisticsService;

/**
 * 统计管理接口：综合信息、员工积分、人事信息、人事记录。
 *
 * <p>只读聚合，不做写操作；数据量在千级，直接查库即可。</p>
 */
@RestController
@RequestMapping("/statistics")
public class StatisticsController
{
    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService)
    {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/overview")
    public Map<String, Object> overview()
    {
        return statisticsService.overview();
    }

    @GetMapping("/score")
    public Map<String, Object> score()
    {
        return statisticsService.score();
    }

    @GetMapping("/personnel")
    public Map<String, Object> personnel()
    {
        return statisticsService.personnel();
    }

    @GetMapping("/records")
    public Map<String, Object> records()
    {
        return statisticsService.records();
    }
}

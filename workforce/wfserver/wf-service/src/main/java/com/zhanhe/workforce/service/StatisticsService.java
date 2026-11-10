package com.zhanhe.workforce.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 统计口径服务。
 *
 * <p>原项目的「统计管理」四个页面是空壳，这里把它们做成真实统计。
 * 统计直接走聚合 SQL，不引入缓存：数据量在千级，实时算比维护缓存更省心。</p>
 */
@Service
public class StatisticsService
{
    private final JdbcTemplate jdbc;

    public StatisticsService(JdbcTemplate jdbc)
    {
        this.jdbc = jdbc;
    }

    /** 综合信息：总量、状态分布、部门与职称分布、三类待办 */
    public Map<String, Object> overview()
    {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", count("select count(*) from employee"));
        result.put("probation", count("select count(*) from employee where workState = '试用期'"));
        result.put("formal", count("select count(*) from employee where workState = '正式'"));
        result.put("onJob", count("select count(*) from employee where workState = '在职'"));
        result.put("leaving", count("select count(*) from employee where workState = '离职'"));
        result.put("departments", top("select d.name as name, count(e.id) as cnt from employee e "
                + "join department d on d.id = e.departmentId group by d.name order by cnt desc", 10));
        result.put("jobLevels", top("select j.name as name, count(e.id) as cnt from employee e "
                + "join joblevel j on j.id = e.jobLevelId group by j.name order by cnt desc", 10));
        result.put("pendingProbation", count("select count(*) from employee where conversionTime >= curdate() "
                + "and conversionTime <= date_add(curdate(), interval 7 day) "
                + "and (workState is null or workState in ('试用期','在职'))"));
        result.put("pendingContract", count("select count(*) from employee where endContract >= curdate() "
                + "and endContract <= date_add(curdate(), interval 7 day) and workState <> '离职'"));
        result.put("newThisMonth", count("select count(*) from employee where date_format(beginDate, '%Y-%m') "
                + "= date_format(curdate(), '%Y-%m')"));
        return result;
    }

    /** 员工积分：奖惩累计得分排行 */
    public Map<String, Object> score()
    {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> rows = new ArrayList<>();
        try
        {
            rows = jdbc.queryForList("select e.id as id, e.name as name, e.workID as workID, "
                    + "coalesce(sum(ec.ecPoint), 0) as point, count(ec.id) as times "
                    + "from employee e left join employeeec ec on ec.eid = e.id "
                    + "group by e.id, e.name, e.workID having count(ec.id) > 0 "
                    + "order by point desc, e.id asc limit 20");
        }
        catch (Exception ignored)
        {
            // 奖惩表为空时也返回空列表，不报错
        }
        result.put("rows", rows);
        result.put("avg", count("select coalesce(round(avg(t.point), 1), 0) from ("
                + "select e.id, coalesce(sum(ec.ecPoint), 0) as point from employee e "
                + "join employeeec ec on ec.eid = e.id group by e.id) t"));
        return result;
    }

    /** 人事信息：按部门、职称、政治面貌、学历分布 */
    public Map<String, Object> personnel()
    {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("departments", top("select d.name as name, count(e.id) as cnt from employee e "
                + "join department d on d.id = e.departmentId group by d.name order by cnt desc", 20));
        result.put("jobLevels", top("select j.name as name, count(e.id) as cnt from employee e "
                + "join joblevel j on j.id = e.jobLevelId group by j.name order by cnt desc", 20));
        result.put("politics", top("select p.name as name, count(e.id) as cnt from employee e "
                + "join politicsstatus p on p.id = e.politicId group by p.name order by cnt desc", 10));
        result.put("degrees", top("select coalesce(e.tiptopDegree,'未填写') as name, count(*) as cnt "
                + "from employee e group by e.tiptopDegree order by cnt desc", 10));
        return result;
    }

    /** 人事记录：最近发生的考评、奖惩、调薪、调动、培训 */
    public Map<String, Object> records()
    {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("appraise", safeQuery("select a.id, e.name, a.appDate as happenDate, a.appResult as title, "
                + "a.appContent as detail from appraise a join employee e on e.id = a.eid "
                + "order by a.appDate desc, a.id desc limit 10"));
        result.put("ec", safeQuery("select ec.id, e.name, ec.ecDate as happenDate, ec.ecReason as title, "
                + "concat(ec.ecPoint, ' 分') as detail from employeeec ec join employee e on e.id = ec.eid "
                + "order by ec.ecDate desc, ec.id desc limit 10"));
        result.put("adjust", safeQuery("select a.id, e.name, a.asDate as happenDate, a.reason as title, "
                + "concat(a.beforeSalary, ' -> ', a.afterSalary) as detail from adjustsalary a "
                + "join employee e on e.id = a.eid order by a.asDate desc, a.id desc limit 10"));
        result.put("remove", safeQuery("select r.id, e.name, r.removeDate as happenDate, r.reason as title, "
                + "d.name as detail from employeeremove r join employee e on e.id = r.eid "
                + "left join department d on d.id = r.afterDepId order by r.removeDate desc, r.id desc limit 10"));
        result.put("train", safeQuery("select t.id, e.name, t.trainDate as happenDate, t.trainContent as title, "
                + "t.remark as detail from employeetrain t join employee e on e.id = t.eid "
                + "order by t.trainDate desc, t.id desc limit 10"));
        return result;
    }

    private Integer count(String sql)
    {
        try
        {
            Long v = jdbc.queryForObject(sql, Long.class);
            return v == null ? 0 : v.intValue();
        }
        catch (Exception e)
        {
            return 0;
        }
    }

    private List<Map<String, Object>> top(String sql, int limit)
    {
        return safeQuery(sql + " limit " + limit);
    }

    private List<Map<String, Object>> safeQuery(String sql)
    {
        try
        {
            return jdbc.queryForList(sql);
        }
        catch (Exception e)
        {
            return new ArrayList<>();
        }
    }
}

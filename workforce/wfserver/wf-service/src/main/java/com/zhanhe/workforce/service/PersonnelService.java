package com.zhanhe.workforce.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 人事从属记录的通用读写：奖惩、培训、调薪、调动。
 *
 * <p>这四张表结构都是「谁 + 时间 + 事项 + 备注」，与其写四套 Mapper，
 * 不如用一张白名单表描述它们，由同一个服务处理增删改查。
 * 表名与字段名全部来自白名单，不接收前端传来的标识，避免 SQL 注入。</p>
 */
@Service
public class PersonnelService
{
    public static final class Kind
    {
        final String table;

        final String orderBy;

        final String[] fields;

        Kind(String table, String orderBy, String... fields)
        {
            this.table = table;
            this.orderBy = orderBy;
            this.fields = fields;
        }
    }

    private static final Map<String, Kind> KINDS = new LinkedHashMap<>();

    static
    {
        KINDS.put("ec", new Kind("employeeec", "ecDate",
                "eid", "ecDate", "ecReason", "ecPoint", "ecType", "remark"));
        KINDS.put("train", new Kind("employeetrain", "trainDate",
                "eid", "trainDate", "trainContent", "remark"));
        KINDS.put("adjust", new Kind("adjustsalary", "asDate",
                "eid", "asDate", "beforeSalary", "afterSalary", "reason", "remark"));
        KINDS.put("move", new Kind("employeeremove", "removeDate",
                "eid", "afterDepId", "afterJobId", "removeDate", "reason", "remark"));
    }

    private final JdbcTemplate jdbc;

    public PersonnelService(JdbcTemplate jdbc)
    {
        this.jdbc = jdbc;
    }

    public boolean supports(String kind)
    {
        return KINDS.containsKey(kind);
    }

    /** 列表：带上员工姓名与部门，页面直接可读 */
    public List<Map<String, Object>> list(String kind, Integer page, Integer size, String name)
    {
        Kind k = KINDS.get(kind);
        if (k == null)
        {
            return new ArrayList<>();
        }
        int offset = (page == null || size == null) ? 0 : Math.max(0, (page - 1) * size);
        StringBuilder sql = new StringBuilder("select t.id as id, t.eid as eid, e.name as empName, "
                + "d.name as depName, t.* from " + k.table + " t "
                + "join employee e on e.id = t.eid left join department d on d.id = e.departmentId ");
        List<Object> args = new ArrayList<>();
        if (name != null && !name.trim().isEmpty())
        {
            sql.append("where e.name like ? ");
            args.add("%" + name.trim() + "%");
        }
        sql.append("order by t.").append(k.orderBy).append(" desc, t.id desc limit ? offset ?");
        args.add(size == null ? 20 : size);
        args.add(offset);
        return jdbc.queryForList(sql.toString(), args.toArray());
    }

    public int count(String kind, String name)
    {
        Kind k = KINDS.get(kind);
        if (k == null)
        {
            return 0;
        }
        if (name == null || name.trim().isEmpty())
        {
            Long v = jdbc.queryForObject("select count(*) from " + k.table, Long.class);
            return v == null ? 0 : v.intValue();
        }
        Long v = jdbc.queryForObject("select count(*) from " + k.table + " t join employee e on e.id = t.eid "
                + "where e.name like ?", Long.class, "%" + name.trim() + "%");
        return v == null ? 0 : v.intValue();
    }

    public int create(String kind, Map<String, Object> body)
    {
        Kind k = KINDS.get(kind);
        if (k == null)
        {
            return 0;
        }
        List<String> cols = new ArrayList<>();
        List<Object> args = new ArrayList<>();
        for (String f : k.fields)
        {
            if (body.containsKey(f))
            {
                cols.add(f);
                args.add(body.get(f));
            }
        }
        if (cols.isEmpty())
        {
            return 0;
        }
        String placeholders = String.join(", ", java.util.Collections.nCopies(cols.size(), "?"));
        String sql = "insert into " + k.table + " (" + String.join(", ", cols) + ") values (" + placeholders + ")";
        return jdbc.update(sql, args.toArray());
    }

    public int update(String kind, Long id, Map<String, Object> body)
    {
        Kind k = KINDS.get(kind);
        if (k == null || id == null)
        {
            return 0;
        }
        List<String> sets = new ArrayList<>();
        List<Object> args = new ArrayList<>();
        for (String f : k.fields)
        {
            if (body.containsKey(f))
            {
                sets.add(f + " = ?");
                args.add(body.get(f));
            }
        }
        if (sets.isEmpty())
        {
            return 0;
        }
        args.add(id);
        return jdbc.update("update " + k.table + " set " + String.join(", ", sets) + " where id = ?", args.toArray());
    }

    public int delete(String kind, Long id)
    {
        Kind k = KINDS.get(kind);
        if (k == null || id == null)
        {
            return 0;
        }
        return jdbc.update("delete from " + k.table + " where id = ?", id);
    }

    /** 工资表：按员工与月份查应发/实发构成 */
    public List<Map<String, Object>> salary(Integer page, Integer size, String name, String month)
    {
        int offset = (page == null || size == null) ? 0 : Math.max(0, (page - 1) * size);
        StringBuilder sql = new StringBuilder("select es.id, e.name as empName, e.workID, d.name as depName, "
                + "s.name as sobName, s.basicSalary, s.bonus, s.lunchSalary, s.trafficSalary, s.allSalary, "
                + "s.createDate from empsalary es join employee e on e.id = es.eid "
                + "left join department d on d.id = e.departmentId left join salary s on s.id = es.sid where 1 = 1 ");
        List<Object> args = new ArrayList<>();
        if (name != null && !name.trim().isEmpty())
        {
            sql.append("and e.name like ? ");
            args.add("%" + name.trim() + "%");
        }
        if (month != null && !month.trim().isEmpty())
        {
            sql.append("and date_format(s.createDate, '%Y-%m') = ? ");
            args.add(month.trim());
        }
        sql.append("order by es.id desc limit ? offset ?");
        args.add(size == null ? 20 : size);
        args.add(offset);
        return jdbc.queryForList(sql.toString(), args.toArray());
    }

    public int salaryCount(String name, String month)
    {
        StringBuilder sql = new StringBuilder("select count(*) from empsalary es join employee e on e.id = es.eid "
                + "join salary s on s.id = es.sid where 1 = 1 ");
        List<Object> args = new ArrayList<>();
        if (name != null && !name.trim().isEmpty())
        {
            sql.append("and e.name like ? ");
            args.add("%" + name.trim() + "%");
        }
        if (month != null && !month.trim().isEmpty())
        {
            sql.append("and date_format(s.createDate, '%Y-%m') = ? ");
            args.add(month.trim());
        }
        Long v = jdbc.queryForObject(sql.toString(), Long.class, args.toArray());
        return v == null ? 0 : v.intValue();
    }

    /** 本月调薪汇总：月末处理页用它给出「这个月动了哪些人的薪资」 */
    public Map<String, Object> monthAdjust(String month)
    {
        String m = (month == null || month.trim().isEmpty())
                ? new java.text.SimpleDateFormat("yyyy-MM").format(new java.util.Date())
                : month.trim();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("month", m);
        result.put("rows", jdbc.queryForList("select a.id, e.name as empName, a.asDate, a.beforeSalary, "
                + "a.afterSalary, (a.afterSalary - a.beforeSalary) as delta, a.reason from adjustsalary a "
                + "join employee e on e.id = a.eid where date_format(a.asDate, '%Y-%m') = ? "
                + "order by a.asDate desc", m));
        result.put("total", jdbc.queryForObject("select coalesce(sum(a.afterSalary - a.beforeSalary), 0) "
                + "from adjustsalary a where date_format(a.asDate, '%Y-%m') = ?", Long.class, m));
        return result;
    }

    /** 审计流水：生命周期动作表就是现成的操作留痕 */
    public List<Map<String, Object>> audit(Integer limit)
    {
        return jdbc.queryForList("select a.id, e.name as empName, a.action_type as actionType, a.reason, "
                + "a.operator, a.before_end_contract as beforeEnd, a.after_end_contract as afterEnd, a.create_time as createTime "
                + "from employee_lifecycle_action a join employee e on e.id = a.emp_id "
                + "order by a.create_time desc, a.id desc limit ?", limit == null ? 50 : limit);
    }

    /** 系统配置页用：角色、菜单、账号数量与数据量概览 */
    public Map<String, Object> summary()
    {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("roles", countPlain("select count(*) from role"));
        result.put("menus", countPlain("select count(*) from menu"));
        result.put("accounts", countPlain("select count(*) from hr"));
        result.put("employees", countPlain("select count(*) from employee"));
        result.put("departments", countPlain("select count(*) from department"));
        result.put("reminders", countPlain("select count(*) from employee_lifecycle_reminder"));
        result.put("actions", countPlain("select count(*) from employee_lifecycle_action"));
        result.put("mailLogs", countPlain("select count(*) from mail_send_log"));
        result.put("tables", jdbc.queryForList("select table_name as name, table_rows as approxRows "
                + "from information_schema.tables where table_schema = database() order by table_name"));
        return result;
    }

    private Integer countPlain(String sql)
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
}

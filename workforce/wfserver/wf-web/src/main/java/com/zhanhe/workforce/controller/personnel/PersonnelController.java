package com.zhanhe.workforce.controller.personnel;

import java.util.List;
import java.util.Map;

import com.zhanhe.workforce.model.RespBean;
import com.zhanhe.workforce.model.RespPageBean;
import com.zhanhe.workforce.service.PersonnelService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 人事从属记录接口：奖惩 / 培训 / 调薪 / 调动，以及工资、审计、系统概览。
 *
 * <p>kind 只允许 ec / train / adjust / move 四个值，白名单在服务层，非法值返回空列表。</p>
 */
@RestController
@RequestMapping("/personnel")
public class PersonnelController
{
    private final PersonnelService service;

    public PersonnelController(PersonnelService service)
    {
        this.service = service;
    }

    @GetMapping("/salary/list")
    public RespPageBean salary(@RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String month)
    {
        RespPageBean bean = new RespPageBean();
        bean.setData(service.salary(page, size, name, month));
        bean.setTotal((long) service.salaryCount(name, month));
        return bean;
    }

    @GetMapping("/monthAdjust")
    public Map<String, Object> monthAdjust(@RequestParam(required = false) String month)
    {
        return service.monthAdjust(month);
    }

    @GetMapping("/audit/list")
    public RespPageBean audit(@RequestParam(required = false, defaultValue = "50") Integer limit)
    {
        List<Map<String, Object>> rows = service.audit(limit);
        RespPageBean bean = new RespPageBean();
        bean.setData(rows);
        bean.setTotal((long) rows.size());
        return bean;
    }

    @GetMapping("/summary")
    public Map<String, Object> summary()
    {
        return service.summary();
    }

    @GetMapping("/{kind}")
    public RespPageBean list(@PathVariable String kind,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size,
            @RequestParam(required = false) String name)
    {
        RespPageBean bean = new RespPageBean();
        bean.setData(service.list(kind, page, size, name));
        bean.setTotal((long) service.count(kind, name));
        return bean;
    }

    @PostMapping("/{kind}")
    public RespBean create(@PathVariable String kind, @RequestBody Map<String, Object> body)
    {
        return service.create(kind, body) > 0 ? RespBean.ok("添加成功") : RespBean.error("添加失败");
    }

    @PutMapping("/{kind}/{id}")
    public RespBean update(@PathVariable String kind, @PathVariable Long id, @RequestBody Map<String, Object> body)
    {
        return service.update(kind, id, body) > 0 ? RespBean.ok("更新成功") : RespBean.error("更新失败");
    }

    @DeleteMapping("/{kind}/{id}")
    public RespBean delete(@PathVariable String kind, @PathVariable Long id)
    {
        return service.delete(kind, id) > 0 ? RespBean.ok("删除成功") : RespBean.error("删除失败");
    }
}

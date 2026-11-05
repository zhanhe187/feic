package com.zhanhe.workforce.controller.emp;

import com.zhanhe.workforce.controller.emp.dto.ContractRenewDto;
import com.zhanhe.workforce.model.*;
import com.zhanhe.workforce.service.*;
import com.zhanhe.workforce.utils.POIUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/employee/basic")
public class EmpBasicController {
    @Autowired
    EmployeeService employeeService;
    @Autowired
    NationService nationService;
    @Autowired
    PoliticsstatusService politicsstatusService;
    @Autowired
    JobLevelService jobLevelService;
    @Autowired
    PositionService positionService;
    @Autowired
    DepartmentService departmentService;

    @GetMapping("/")
    public RespPageBean getEmployeeByPage(@RequestParam(defaultValue = "1") Integer page, @RequestParam(defaultValue = "10") Integer size, Employee employee, Date[] beginDateScope) {
        return employeeService.getEmployeeByPage(page, size, employee,beginDateScope);
    }

    @GetMapping("/probation/remind")
    public RespPageBean getProbationEmployees(@RequestParam(defaultValue = "1") Integer page, @RequestParam(defaultValue = "10") Integer size, Employee employee) {
        return employeeService.getProbationEmployees(page, size, employee);
    }

    @GetMapping("/contract/remind")
    public RespPageBean getContractExpireEmployees(@RequestParam(defaultValue = "1") Integer page, @RequestParam(defaultValue = "10") Integer size, Employee employee) {
        return employeeService.getContractExpireEmployees(page, size, employee);
    }

    @PutMapping("/{id}/regularize")
    public RespBean regularizeEmployee(@PathVariable Integer id) {
        try {
            if (employeeService.regularizeEmployee(id) == 1) {
                return RespBean.ok("员工转正成功!");
            }
            return RespBean.error("员工转正失败!");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return RespBean.error(e.getMessage());
        }
    }

    @PostMapping("/lifecycle/scan")
    public RespBean scanLifecycleReminders() {
        Integer created = employeeService.scanLifecycleReminders();
        return RespBean.ok("生命周期提醒扫描完成", created);
    }

    /**
     * 续签合同：把"提醒 → 决策 → 动作 → 留痕"补完整。
     */
    @PutMapping("/{id}/renew")
    public RespBean renewContract(@PathVariable Integer id, @RequestBody ContractRenewDto dto) {
        try {
            Date begin = parseDate(dto.getBeginDate(), "新合同起始日");
            Date end = parseDate(dto.getEndDate(), "新合同到期日");
            if (employeeService.renewContract(id, begin, end, dto.getReason()) == 1) {
                return RespBean.ok("合同已续签");
            }
            return RespBean.error("续签失败");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return RespBean.error(e.getMessage());
        }
    }

    /**
     * 标记不续签：只记录决策与原因，不改在职状态（离职是人事动作，不越权）。
     */
    @PutMapping("/{id}/not-renew")
    public RespBean markNotRenew(@PathVariable Integer id, @RequestBody ContractRenewDto dto) {
        try {
            if (employeeService.markNotRenew(id, dto == null ? null : dto.getReason()) == 1) {
                return RespBean.ok("已标记为不续签");
            }
            return RespBean.error("标记失败");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return RespBean.error(e.getMessage());
        }
    }

    /** 某员工的跟进记录（转正/续签/不续签） */
    @GetMapping("/{id}/lifecycle/actions")
    public List<EmployeeLifecycleAction> lifecycleActions(@PathVariable Integer id) {
        return employeeService.listLifecycleActions(id);
    }

    private Date parseDate(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("请填写" + field);
        }
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        format.setLenient(false);
        try {
            return format.parse(value.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException(field + "格式应为 yyyy-MM-dd");
        }
    }

    @PostMapping("/")
    public RespBean addEmp(@RequestBody Employee employee) {
        if (employeeService.addEmp(employee) == 1) {
            return RespBean.ok("添加成功!");
        }
        return RespBean.error("添加失败!");
    }

    @DeleteMapping("/{id}")
    public RespBean deleteEmpByEid(@PathVariable Integer id) {
        if (employeeService.deleteEmpByEid(id) == 1) {
            return RespBean.ok("删除成功!");
        }
        return RespBean.error("删除失败!");
    }

    @PutMapping("/")
    public RespBean updateEmp(@RequestBody Employee employee) {
        if (employeeService.updateEmp(employee) == 1) {
            return RespBean.ok("更新成功!");
        }
        return RespBean.error("更新失败!");
    }

    @GetMapping("/nations")
    public List<Nation> getAllNations() {
        return nationService.getAllNations();
    }

    @GetMapping("/politicsstatus")
    public List<Politicsstatus> getAllPoliticsstatus() {
        return politicsstatusService.getAllPoliticsstatus();
    }

    @GetMapping("/joblevels")
    public List<JobLevel> getAllJobLevels() {
        return jobLevelService.getAllJobLevels();
    }

    @GetMapping("/positions")
    public List<Position> getAllPositions() {
        return positionService.getAllPositions();
    }

    @GetMapping("/maxWorkID")
    public RespBean maxWorkID() {
        RespBean respBean = RespBean.build().setStatus(200)
                .setObj(String.format("%08d", employeeService.maxWorkID() + 1));
        return respBean;
    }

    @GetMapping("/deps")
    public List<Department> getAllDepartments() {
        return departmentService.getAllDepartments();
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportData() {
        List<Employee> list = (List<Employee>) employeeService.getEmployeeByPage(null, null, new Employee(),null).getData();
        return POIUtils.employee2Excel(list);
    }

    @PostMapping("/import")
    public RespBean importData(MultipartFile file) throws IOException {
        List<Employee> list = POIUtils.excel2Employee(file, nationService.getAllNations(), politicsstatusService.getAllPoliticsstatus(), departmentService.getAllDepartmentsWithOutChildren(), positionService.getAllPositions(), jobLevelService.getAllJobLevels());
        if (employeeService.addEmps(list) == list.size()) {
            return RespBean.ok("上传成功");
        }
        return RespBean.error("上传失败");
    }
}

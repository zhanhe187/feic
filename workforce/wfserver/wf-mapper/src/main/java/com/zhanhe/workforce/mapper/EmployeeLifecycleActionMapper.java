package com.zhanhe.workforce.mapper;

import com.zhanhe.workforce.model.EmployeeLifecycleAction;

import java.util.List;

public interface EmployeeLifecycleActionMapper {

    Integer insertAction(EmployeeLifecycleAction action);

    List<EmployeeLifecycleAction> selectByEmpId(Integer empId);

    /** 最近一次某类动作（用于判断 30 天冷静期内是否已决策） */
    EmployeeLifecycleAction selectLatestByType(Integer empId, String actionType);
}

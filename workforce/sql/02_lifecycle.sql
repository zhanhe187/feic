-- 员工生命周期扩展：提醒表与员工提醒状态字段。
-- 在 sql/01_init.sql 之后执行。

ALTER TABLE employee
    ADD COLUMN lifecycleReminderStatus varchar(20) DEFAULT 'PENDING' COMMENT 'PENDING/SENT/DONE';

-- 原表 workState 只有（在职、离职），而生命周期模块要区分「试用期/正式」，这里扩展枚举取值。
ALTER TABLE employee
    MODIFY COLUMN workState enum('试用期','在职','正式','离职') DEFAULT '在职' COMMENT '在职状态';

CREATE TABLE IF NOT EXISTS employee_lifecycle_reminder (
    id int NOT NULL AUTO_INCREMENT,
    emp_id int NOT NULL,
    remind_type varchar(20) NOT NULL COMMENT 'PROBATION/CONTRACT',
    remind_status varchar(20) NOT NULL DEFAULT 'CREATED',
    remark varchar(255) DEFAULT NULL,
    remind_date date NOT NULL,
    create_time datetime NOT NULL,
    update_time datetime NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_emp_type_date (emp_id, remind_type, remind_date),
    KEY idx_reminder_emp (emp_id),
    KEY idx_reminder_type_date (remind_type, remind_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Employee lifecycle reminder records';

CREATE INDEX idx_employee_lifecycle_probation
    ON employee (workState, conversionTime, departmentId);

CREATE INDEX idx_employee_lifecycle_contract
    ON employee (workState, endContract, departmentId);

CREATE INDEX idx_employee_lifecycle_status
    ON employee (lifecycleReminderStatus);

INSERT INTO menu (id, url, path, component, name, iconCls, keepAlive, requireAuth, parentId, enabled)
SELECT 29, '/employee/basic/**', '/emp/lifecycle', 'EmpLifecycle', '生命周期提醒', 'fa fa-bell-o', NULL, 1, 2, 1
WHERE NOT EXISTS (SELECT 1 FROM menu WHERE id = 29);

INSERT INTO menu_role (mid, rid)
SELECT 29, r.id FROM role r
WHERE r.id IN (1, 2, 3, 4, 6, 14)
  AND NOT EXISTS (
      SELECT 1 FROM menu_role mr WHERE mr.mid = 29 AND mr.rid = r.id
  );

-- Sample data for local demos. Adjust departmentId/jobLevelId/posId if your seed data differs.
UPDATE employee
SET beginDate = DATE_SUB(CURRENT_DATE, INTERVAL 85 DAY),
    conversionTime = DATE_ADD(CURRENT_DATE, INTERVAL 3 DAY),
    beginContract = DATE_SUB(CURRENT_DATE, INTERVAL 85 DAY),
    endContract = DATE_ADD(CURRENT_DATE, INTERVAL 180 DAY),
    workState = '试用期',
    lifecycleReminderStatus = 'PENDING'
WHERE id IN (1, 2, 3);

UPDATE employee
SET beginContract = DATE_SUB(CURRENT_DATE, INTERVAL 355 DAY),
    endContract = DATE_ADD(CURRENT_DATE, INTERVAL 5 DAY),
    lifecycleReminderStatus = 'PENDING'
WHERE id IN (4, 5, 6);

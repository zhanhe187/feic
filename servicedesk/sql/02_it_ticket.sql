-- 资产台账与工单流程：it_asset / it_ticket / it_ticket_record 三张表。
-- 在 sql/01_init.sql 之后执行。

CREATE TABLE IF NOT EXISTS it_asset (
    asset_id bigint(20) NOT NULL AUTO_INCREMENT,
    asset_no varchar(64) NOT NULL,
    asset_type varchar(40) NOT NULL,
    brand_model varchar(100) NOT NULL,
    owner_user varchar(64) DEFAULT '',
    asset_status varchar(20) NOT NULL DEFAULT 'idle',
    receive_time date DEFAULT NULL,
    return_time date DEFAULT NULL,
    create_by varchar(64) DEFAULT '',
    create_time datetime DEFAULT NULL,
    update_by varchar(64) DEFAULT '',
    update_time datetime DEFAULT NULL,
    remark varchar(500) DEFAULT NULL,
    PRIMARY KEY (asset_id),
    UNIQUE KEY uk_it_asset_no (asset_no),
    KEY idx_it_asset_owner (owner_user),
    KEY idx_it_asset_status (asset_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='IT asset ledger';

CREATE TABLE IF NOT EXISTS it_ticket (
    ticket_id bigint(20) NOT NULL AUTO_INCREMENT,
    ticket_no varchar(64) NOT NULL,
    applicant varchar(64) NOT NULL,
    ticket_type varchar(40) NOT NULL,
    priority varchar(20) NOT NULL,
    status varchar(20) NOT NULL,
    handler varchar(64) DEFAULT '',
    title varchar(100) NOT NULL,
    description varchar(1000) DEFAULT NULL,
    complete_time datetime DEFAULT NULL,
    create_by varchar(64) DEFAULT '',
    create_time datetime DEFAULT NULL,
    update_by varchar(64) DEFAULT '',
    update_time datetime DEFAULT NULL,
    remark varchar(500) DEFAULT NULL,
    PRIMARY KEY (ticket_id),
    UNIQUE KEY uk_it_ticket_no (ticket_no),
    KEY idx_it_ticket_applicant (applicant),
    KEY idx_it_ticket_status_time (status, create_time),
    KEY idx_it_ticket_handler (handler)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='IT service ticket';

CREATE TABLE IF NOT EXISTS it_ticket_record (
    record_id bigint(20) NOT NULL AUTO_INCREMENT,
    ticket_id bigint(20) NOT NULL,
    processor varchar(64) NOT NULL,
    action varchar(30) NOT NULL,
    process_note varchar(1000) DEFAULT NULL,
    process_result varchar(30) NOT NULL,
    process_time datetime NOT NULL,
    create_by varchar(64) DEFAULT '',
    create_time datetime DEFAULT NULL,
    PRIMARY KEY (record_id),
    KEY idx_ticket_record_ticket (ticket_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='IT ticket process record';

INSERT INTO sys_role (role_id, role_name, role_key, role_sort, data_scope, menu_check_strictly,
                      dept_check_strictly, status, del_flag, create_by, create_time, remark)
SELECT 3, 'IT Operator', 'it', 3, '1', 1, 1, '0', '0', 'admin', sysdate(), 'IT ticket handler'
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_key = 'it');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
                      is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2000, 'IT 服务', 0, 5, 'it', NULL, '', '', 1, 0, 'M', '0', '0', '', 'monitor', 'admin', sysdate(), 'IT service directory'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2000);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
                      is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2001, '资产台账', 2000, 1, 'asset', 'it/asset/index', '', '', 1, 0, 'C', '0', '0', 'it:asset:list', 'build', 'admin', sysdate(), 'IT asset ledger'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2001);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
                      is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2002, '工单流程', 2000, 2, 'ticket', 'it/ticket/index', '', '', 1, 0, 'C', '0', '0', 'it:ticket:list', 'message', 'admin', sysdate(), 'IT ticket workflow'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2002);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
                      is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT menu_id, menu_name, parent_id, order_num, '', '', '', '', 1, 0, 'F', '0', '0', perms, '#', 'admin', sysdate(), ''
FROM (
    SELECT 2003 menu_id, 'Asset Query' menu_name, 2001 parent_id, 1 order_num, 'it:asset:query' perms UNION ALL
    SELECT 2004, 'Asset Add', 2001, 2, 'it:asset:add' UNION ALL
    SELECT 2005, 'Asset Edit', 2001, 3, 'it:asset:edit' UNION ALL
    SELECT 2006, 'Asset Delete', 2001, 4, 'it:asset:remove' UNION ALL
    SELECT 2007, 'Asset Export', 2001, 5, 'it:asset:export' UNION ALL
    SELECT 2008, 'Asset Import', 2001, 6, 'it:asset:import' UNION ALL
    SELECT 2009, 'Ticket Query', 2002, 1, 'it:ticket:query' UNION ALL
    SELECT 2010, 'Ticket Add', 2002, 2, 'it:ticket:add' UNION ALL
    SELECT 2011, 'Ticket Handle', 2002, 3, 'it:ticket:handle' UNION ALL
    SELECT 2012, 'Ticket Close', 2002, 4, 'it:ticket:close' UNION ALL
    SELECT 2013, 'Ticket Delete', 2002, 5, 'it:ticket:remove'
) m
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE sys_menu.menu_id = m.menu_id);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 2, menu_id FROM sys_menu WHERE menu_id IN (2000, 2001, 2002, 2003, 2009, 2010);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r
JOIN sys_menu m ON m.menu_id IN (2000, 2001, 2002, 2003, 2009, 2010, 2011, 2012)
WHERE r.role_key = 'it';

INSERT INTO sys_job (job_id, job_name, job_group, invoke_target, cron_expression, misfire_policy,
                     concurrent, status, create_by, create_time, remark)
SELECT 100, 'IT overdue ticket scan', 'SYSTEM', 'itTicketTask.scanOverdueTickets()', '0 0/30 * * * ?', '3',
       '1', '0', 'admin', sysdate(), 'Scan tickets pending for more than 24 hours'
WHERE NOT EXISTS (SELECT 1 FROM sys_job WHERE invoke_target = 'itTicketTask.scanOverdueTickets()');

INSERT IGNORE INTO it_asset(asset_no, asset_type, brand_model, owner_user, asset_status, receive_time, create_by, create_time)
VALUES
('NB-2026-001', 'computer', 'ThinkPad T14', 'common', 'in_use', current_date(), 'admin', sysdate()),
('MON-2026-001', 'monitor', 'Dell U2723QE', 'common', 'in_use', current_date(), 'admin', sysdate()),
('NB-2026-002', 'computer', 'MacBook Pro 14', '', 'idle', null, 'admin', sysdate());

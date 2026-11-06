-- 演示数据：4 张工单 + 处理记录
-- 目的：登录后能看到工单列表；admin 看到全部，普通账号 user 只看到自己提的，用来演示数据权限
-- 可重复执行：先按 ticket_no 清理再插入

USE `servicedesk`;

DELETE FROM it_ticket_record WHERE ticket_id IN (SELECT ticket_id FROM it_ticket WHERE ticket_no LIKE 'IT-DEMO-%');
DELETE FROM it_ticket WHERE ticket_no LIKE 'IT-DEMO-%';

INSERT INTO it_ticket
(ticket_no, applicant, ticket_type, priority, status, handler, title, description,
 complete_time, create_by, create_time, update_by, update_time,
 device_info, symptom, impact_scope, tried_actions, ai_source)
VALUES
('IT-DEMO-0001', 'user', 'hardware', 'high', 'processing', 'admin',
 '三楼打印机连不上，整个部门都没法打印',
 '今天早上开始，三楼东侧那台打印机显示离线，重启过一次还是不行，部门五六个人都在等文件。',
 NULL, 'user', DATE_SUB(NOW(), INTERVAL 3 HOUR), 'admin', DATE_SUB(NOW(), INTERVAL 2 HOUR),
 '三楼东侧打印机', '显示离线，重启无效', 'multi', '重启打印机、检查网线', 'llm'),

('IT-DEMO-0002', 'user', 'software', 'low', 'completed', 'admin',
 '设计软件激活失败，提示许可证过期',
 '打开设计软件提示许可证已过期，重装之后还是一样。',
 DATE_SUB(NOW(), INTERVAL 1 HOUR), 'user', DATE_SUB(NOW(), INTERVAL 6 HOUR), 'admin', DATE_SUB(NOW(), INTERVAL 1 HOUR),
 '设计工作站', '提示许可证过期，重装无效', 'single', '重装软件', 'rule'),

('IT-DEMO-0003', 'admin', 'network', 'medium', 'pending', NULL,
 '会议室无线网络时断时续',
 '四楼大会议室开视频会的时候WiFi 频繁掉线，有线正常。',
 NULL, 'admin', DATE_SUB(NOW(), INTERVAL 30 MINUTE), NULL, NULL,
 '四楼会议室无线AP', 'WiFi 频繁掉线，有线正常', 'multi', '换过信道', 'llm'),

('IT-DEMO-0004', 'admin', 'account', 'high', 'rejected', 'admin',
 '新同事的企业邮箱账号没开通',
 '上周入职的同事登录邮箱提示账号不存在，需要开通。',
 DATE_SUB(NOW(), INTERVAL 2 DAY), 'admin', DATE_SUB(NOW(), INTERVAL 3 DAY), 'admin', DATE_SUB(NOW(), INTERVAL 2 DAY),
 '企业邮箱', '登录提示账号不存在', 'single', '确认过账号拼写', NULL),('IT-DEMO-0005', 'user', 'hardware', 'medium', 'closed', 'admin',
 '打印机显示脱机，重启打印服务后恢复',
 '打印机面板显示脱机，打印队列里任务一直挂着，重启 Print Spooler 后恢复正常。',
 DATE_SUB(NOW(), INTERVAL 5 DAY), 'user', DATE_SUB(NOW(), INTERVAL 6 DAY), 'admin', DATE_SUB(NOW(), INTERVAL 5 DAY),
 '二楼打印机', '显示脱机，队列任务卡住', 'single', '重启打印服务', 'rule');


INSERT INTO it_ticket_record (ticket_id, processor, action, process_note, process_result, process_time, create_by, create_time)
SELECT ticket_id, 'user', 'CREATE', '员工提交工单', 'pending', create_time, 'user', create_time
FROM it_ticket WHERE ticket_no LIKE 'IT-DEMO-%';

INSERT INTO it_ticket_record (ticket_id, processor, action, process_note, process_result, process_time, create_by, create_time)
SELECT ticket_id, 'admin', 'ASSIGN', '派给 IT 值班同学', 'processing', DATE_ADD(create_time, INTERVAL 30 MINUTE), 'admin', DATE_ADD(create_time, INTERVAL 30 MINUTE)
FROM it_ticket WHERE ticket_no IN ('IT-DEMO-0001', 'IT-DEMO-0002');

INSERT INTO it_ticket_record (ticket_id, processor, action, process_note, process_result, process_time, create_by, create_time)
SELECT ticket_id, 'admin', 'PROCESS', '重新安装客户端并绑定授权，已恢复正常', 'completed', update_time, 'admin', update_time
FROM it_ticket WHERE ticket_no = 'IT-DEMO-0002';

INSERT INTO it_ticket_record (ticket_id, processor, action, process_note, process_result, process_time, create_by, create_time)
SELECT ticket_id, 'admin', 'REJECT', '该同事不走企业邮箱，改用共享邮箱，工单关闭', 'rejected', update_time, 'admin', update_time
FROM it_ticket WHERE ticket_no = 'IT-DEMO-0004';

INSERT INTO it_ticket_record (ticket_id, processor, action, process_note, process_result, process_time, create_by, create_time)
SELECT ticket_id, 'admin', 'PROCESS', '重启 Print Spooler 后队列清空，已恢复正常', 'completed', update_time, 'admin', update_time
FROM it_ticket WHERE ticket_no = 'IT-DEMO-0005';

INSERT INTO it_ticket_record (ticket_id, processor, action, process_note, process_result, process_time, create_by, create_time)
SELECT ticket_id, 'admin', 'CLOSE', '员工确认可正常打印，归档', 'closed', DATE_ADD(update_time, INTERVAL 1 HOUR), 'admin', DATE_ADD(update_time, INTERVAL 1 HOUR)
FROM it_ticket WHERE ticket_no = 'IT-DEMO-0005';

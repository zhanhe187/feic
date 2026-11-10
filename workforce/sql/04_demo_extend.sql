-- 员工系统演示数据：把几个原本为空的业务表填上，便于演示统计与列表
-- 只动 1-20 号员工，可重复执行（先按 id 区间清理再插入）

DELETE FROM appraise WHERE id BETWEEN 9001 AND 9030;
DELETE FROM employeeec WHERE id BETWEEN 9001 AND 9030;
DELETE FROM adjustsalary WHERE id BETWEEN 9001 AND 9030;
DELETE FROM employeeremove WHERE id BETWEEN 9001 AND 9030;
DELETE FROM employeetrain WHERE id BETWEEN 9001 AND 9030;

-- 考评
INSERT INTO appraise (id, eid, appDate, appResult, appContent, remark) VALUES
(9001, 1, DATE_SUB(CURDATE(), INTERVAL 30 DAY), '优秀', '季度目标全部达成，客户满意度高', '提前转正建议'),
(9002, 2, DATE_SUB(CURDATE(), INTERVAL 28 DAY), '良好', '按期完成迭代，缺陷率低于团队均值', NULL),
(9003, 3, DATE_SUB(CURDATE(), INTERVAL 26 DAY), '合格', '完成既定任务，沟通需加强', NULL),
(9004, 4, DATE_SUB(CURDATE(), INTERVAL 24 DAY), '良好', '独立负责模块开发，文档完整', NULL),
(9005, 5, DATE_SUB(CURDATE(), INTERVAL 22 DAY), '优秀', '解决线上稳定性问题，减少告警', '建议列入骨干'),
(9006, 6, DATE_SUB(CURDATE(), INTERVAL 20 DAY), '合格', '进度略有延迟，质量稳定', NULL),
(9007, 7, DATE_SUB(CURDATE(), INTERVAL 18 DAY), '良好', '跨部门协作顺畅', NULL),
(9008, 8, DATE_SUB(CURDATE(), INTERVAL 16 DAY), '良好', '交付质量稳定', NULL),
(9009, 9, DATE_SUB(CURDATE(), INTERVAL 14 DAY), '合格', '完成培训并通过考核', NULL),
(9010, 10, DATE_SUB(CURDATE(), INTERVAL 12 DAY), '优秀', '主动补位，承担额外值班任务', NULL);

-- 奖惩
INSERT INTO employeeec (id, eid, ecDate, ecReason, ecPoint, ecType, remark) VALUES
(9001, 1, DATE_SUB(CURDATE(), INTERVAL 40 DAY), '季度优秀员工', 10, 1, NULL),
(9002, 2, DATE_SUB(CURDATE(), INTERVAL 35 DAY), '完成紧急故障排查', 5, 1, NULL),
(9003, 3, DATE_SUB(CURDATE(), INTERVAL 33 DAY), '迟到三次', -3, 2, '已提醒'),
(9004, 5, DATE_SUB(CURDATE(), INTERVAL 30 DAY), '提出流程改进并被采纳', 8, 1, NULL),
(9005, 7, DATE_SUB(CURDATE(), INTERVAL 25 DAY), '文档遗漏导致返工', -2, 2, NULL),
(9006, 9, DATE_SUB(CURDATE(), INTERVAL 20 DAY), '新人导师奖', 6, 1, NULL);

-- 调薪
INSERT INTO adjustsalary (id, eid, asDate, beforeSalary, afterSalary, reason, remark) VALUES
(9001, 1, DATE_SUB(CURDATE(), INTERVAL 90 DAY), 12000, 14000, '转正调薪', NULL),
(9002, 2, DATE_SUB(CURDATE(), INTERVAL 80 DAY), 15000, 16500, '年度调薪', NULL),
(9003, 5, DATE_SUB(CURDATE(), INTERVAL 70 DAY), 18000, 20000, '晋升调薪', '技术骨干'),
(9004, 7, DATE_SUB(CURDATE(), INTERVAL 60 DAY), 11000, 12000, '年度调薪', NULL),
(9005, 9, DATE_SUB(CURDATE(), INTERVAL 50 DAY), 9000, 10000, '绩效调薪', NULL);

-- 人事调动
INSERT INTO employeeremove (id, eid, afterDepId, afterJobId, removeDate, reason, remark) VALUES
(9001, 2, 78, 31, DATE_SUB(CURDATE(), INTERVAL 120 DAY), '业务需要转入市场部', NULL),
(9002, 4, 81, 31, DATE_SUB(CURDATE(), INTERVAL 110 DAY), '区域调整', NULL),
(9003, 6, 8, 9, DATE_SUB(CURDATE(), INTERVAL 100 DAY), '财务岗位补缺', NULL),
(9004, 8, 5, 36, DATE_SUB(CURDATE(), INTERVAL 95 DAY), '晋升为研发经理', NULL),
(9005, 10, 78, 29, DATE_SUB(CURDATE(), INTERVAL 90 DAY), '转岗技术总监助理', NULL);

-- 培训
INSERT INTO employeetrain (id, eid, trainDate, trainContent, remark) VALUES
(9001, 1, DATE_SUB(CURDATE(), INTERVAL 60 DAY), 'Spring 进阶与事务实践', NULL),
(9002, 2, DATE_SUB(CURDATE(), INTERVAL 55 DAY), 'MySQL 索引与慢查询优化', NULL),
(9003, 3, DATE_SUB(CURDATE(), INTERVAL 50 DAY), '信息安全与合规', NULL),
(9004, 4, DATE_SUB(CURDATE(), INTERVAL 45 DAY), '需求分析与沟通', NULL),
(9005, 5, DATE_SUB(CURDATE(), INTERVAL 40 DAY), '分布式事务与消息队列', NULL),
(9006, 6, DATE_SUB(CURDATE(), INTERVAL 35 DAY), '新人入职培训', NULL);

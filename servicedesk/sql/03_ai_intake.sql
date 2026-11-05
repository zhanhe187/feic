-- ============================================================================
-- AI 能力升级（ServiceDesk IT 工单平台）
-- 设计说明：
--   1. 只做一件事——把工单的自由文本描述结构化为 4 个职位槽位（设备/现象/影响面/已尝试），
--      供 IT 排序筛选与管理员统计；AI 不参与任何状态流转与权限判断。
--   2. 不建知识库表：历史同类工单直接查 it_ticket + it_ticket_record（引用真实工单号，零失真）。
--   3. 每次模型调用写 it_ai_log 审计（只存摘要 digest，不存原文，避免泄露工单内容）。
-- 幂等：可重复执行。
-- ============================================================================

-- 工单表：AI 结构化槽位 + 来源标记
-- ai_source 取值：llm（大模型抽取）/ rule（规则引擎抽取）/ manual（人工填写或修改后提交）/ NULL（未使用）
SET @ddl := (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'it_ticket' AND column_name = 'device_info') = 0,
    'ALTER TABLE it_ticket ADD COLUMN device_info varchar(120) DEFAULT NULL COMMENT ''设备/系统''',
    'DO 0'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl := (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'it_ticket' AND column_name = 'symptom') = 0,
    'ALTER TABLE it_ticket ADD COLUMN symptom varchar(500) DEFAULT NULL COMMENT ''现象/报错原文''',
    'DO 0'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl := (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'it_ticket' AND column_name = 'impact_scope') = 0,
    'ALTER TABLE it_ticket ADD COLUMN impact_scope varchar(20) DEFAULT NULL COMMENT ''影响面: single/multi/blocking/unknown''',
    'DO 0'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl := (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'it_ticket' AND column_name = 'tried_actions') = 0,
    'ALTER TABLE it_ticket ADD COLUMN tried_actions varchar(500) DEFAULT NULL COMMENT ''已尝试的操作''',
    'DO 0'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl := (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'it_ticket' AND column_name = 'ai_source') = 0,
    'ALTER TABLE it_ticket ADD COLUMN ai_source varchar(10) DEFAULT NULL COMMENT ''AI 来源: llm/rule/manual''',
    'DO 0'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 影响面用于 IT 侧排序筛选，建普通索引即可（枚举值区分度低，不建联合索引避免维护成本）
SET @ddl := (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.statistics
      WHERE table_schema = DATABASE() AND table_name = 'it_ticket' AND index_name = 'idx_it_ticket_impact') = 0,
    'ALTER TABLE it_ticket ADD KEY idx_it_ticket_impact (impact_scope)',
    'DO 0'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- AI 调用审计（不存原文，只存摘要，避免工单内容外泄到日志表）
CREATE TABLE IF NOT EXISTS it_ai_log (
    ai_id bigint(20) NOT NULL AUTO_INCREMENT,
    scene varchar(30) NOT NULL COMMENT 'intake / similar',
    ticket_id bigint(20) DEFAULT NULL,
    user_name varchar(64) DEFAULT '',
    provider varchar(10) NOT NULL COMMENT 'llm / rule',
    model varchar(60) DEFAULT '',
    degraded char(1) NOT NULL DEFAULT '0' COMMENT '0正常 1降级',
    cost_ms int DEFAULT NULL,
    input_digest varchar(80) DEFAULT NULL,
    output_digest varchar(80) DEFAULT NULL,
    create_time datetime DEFAULT NULL,
    PRIMARY KEY (ai_id),
    KEY idx_it_ai_log_scene_time (scene, create_time),
    KEY idx_it_ai_log_user (user_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 调用审计';

-- 权限：AI 结构化抽取（员工建单时用）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
                      is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2014, '工单AI抽取', 2002, 6, '', '', '', '', 1, 0, 'F', '0', '0', 'it:ai:intake', '#', 'admin', sysdate(), 'AI struct intake'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2014);

-- 权限：历史同类工单检索（仅 IT/管理员，防止普通员工读到他人工单内容）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
                      is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2015, '历史工单检索', 2002, 7, '', '', '', '', 1, 0, 'F', '0', '0', 'it:ai:similar', '#', 'admin', sysdate(), 'AI similar ticket search'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2015);

-- 授权：管理员(1) 与 IT(3) 都能用；普通员工(2) 只授抽取（建单），不授检索
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) SELECT 1, 2014 FROM dual;
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) SELECT 1, 2015 FROM dual;
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) SELECT 3, 2014 FROM dual;
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) SELECT 3, 2015 FROM dual;
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) SELECT 2, 2014 FROM dual;

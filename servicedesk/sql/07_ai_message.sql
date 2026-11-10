-- AI 设置与站内消息
-- 1) it_message：按人投递的站内消息（通知中心）
-- 2) 菜单：AI 设置、消息中心
-- 可重复执行

CREATE TABLE IF NOT EXISTS it_message (
    message_id  bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    receiver    varchar(64)  NOT NULL COMMENT '接收人用户名',
    title       varchar(200) NOT NULL COMMENT '标题',
    content     varchar(1000) DEFAULT NULL COMMENT '内容',
    msg_type    varchar(20)  NOT NULL DEFAULT 'SYSTEM' COMMENT 'AI_ALERT/TICKET/ASSIGN/RESULT/SYSTEM',
    biz_type    varchar(20)  DEFAULT NULL COMMENT '业务类型',
    biz_id      bigint(20)   DEFAULT NULL COMMENT '业务ID，如工单ID',
    link        varchar(200) DEFAULT NULL COMMENT '前端跳转路径',
    read_flag   char(1)      NOT NULL DEFAULT '0' COMMENT '0未读 1已读',
    create_by   varchar(64)  DEFAULT NULL,
    create_time datetime     DEFAULT NULL,
    PRIMARY KEY (message_id),
    KEY idx_receiver_read (receiver, read_flag, create_time),
    KEY idx_biz (biz_type, biz_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内消息';

-- 菜单：IT 运维 -> AI 设置
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 'AI 设置', p.menu_id, 20, 'aiconfig', 'it/aiconfig/index', 1, 0, 'C', '0', '0', 'it:ai:config', 'tool', 'admin', sysdate(), 'AI 供应商、Key、超时、配额与通知开关'
FROM sys_menu p WHERE p.path = 'it' AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'it:ai:config');

-- 菜单：IT 运维 -> 消息中心
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '消息中心', p.menu_id, 21, 'message', 'it/message/index', 1, 0, 'C', '0', '0', 'it:message:list', 'message', 'admin', sysdate(), 'AI 告警、工单与派单通知'
FROM sys_menu p WHERE p.path = 'it' AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'it:message:list');

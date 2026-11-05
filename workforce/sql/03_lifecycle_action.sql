-- ============================================================================
-- 生命周期闭环补全：续签 / 不续签 + 跟进留痕
-- 背景：原扩展只做到「提醒 + 转正」，合同到期没有任何动作，HR 还得去员工管理里手工改日期，
--       改完之后提醒记录也不知道处理结果。本脚本补齐「决策 → 动作 → 留痕」这一段。
-- 幂等：可重复执行。
-- ============================================================================

CREATE TABLE IF NOT EXISTS employee_lifecycle_action (
    id int NOT NULL AUTO_INCREMENT,
    emp_id int NOT NULL,
    action_type varchar(20) NOT NULL COMMENT 'REGULARIZE 转正 / RENEW 续签 / NOT_RENEW 不续签',
    before_end_contract date DEFAULT NULL COMMENT '动作前的合同到期日',
    after_end_contract date DEFAULT NULL COMMENT '动作后的合同到期日（不续签为空）',
    reason varchar(255) DEFAULT NULL COMMENT '原因/说明，不续签必填',
    operator varchar(64) DEFAULT NULL COMMENT '操作人',
    create_time datetime NOT NULL,
    PRIMARY KEY (id),
    KEY idx_action_emp (emp_id),
    KEY idx_action_type_time (action_type, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='员工生命周期跟进记录';

-- 扫描/列表用的辅助索引：按「动作类型 + 时间」判断 30 天冷静期
-- （同一个员工 30 天内已决定不续签，就不再重复提醒，避免天天骚扰 HR）

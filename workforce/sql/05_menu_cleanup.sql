-- 菜单与演示数据清理（可重复执行）
-- 1) 原版「备份恢复数据库」「初始化数据库」是空壳且会破坏数据，演示环境隐藏
UPDATE menu SET enabled = 0 WHERE id IN (27, 28) AND enabled = 1;

-- 2) 演示数据里沿用了原作者昵称，统一改成中性姓名
UPDATE employee SET name = REPLACE(name, '江南一点雨', '张伟')
WHERE name LIKE '%江南一点雨%';

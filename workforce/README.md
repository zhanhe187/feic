# 员工全周期治理系统（workforce）

面向 HR 的员工全周期治理子系统：每天扫描 7 天内待转正、合同快到期的员工，生成待办提醒并通过 RabbitMQ 投递提醒邮件；HR 在待办里办理转正、续签或标记不续签，并留下可追溯的跟进记录。

## 业务痛点

- 转正日期和合同到期日散落在 Excel 与员工档案里，HR 每周手工筛一遍，靠记性盯人，漏一个是常态。
- 没有提前量：往往是合同已经过期才被发现，续签从「提前办」变成「补办」。
- 处理结果不留痕：谁在什么时候、把到期日从哪天改到哪天、为什么改，事后查不到。
- 提醒要么漏、要么重：同一个人被反复提醒，已决策不续签的人还天天出现在待办里。

## 功能清单

| 能力 | 说明 | 入口 |
| --- | --- | --- |
| 员工档案 | 基本信息、部门/职称/职位、入职日期、合同起止、转正日期，支持 Excel 导入导出 | `/employee/basic`，页面 `EmpBasic.vue` |
| 转正办理 | 待转正列表 + 一键转正，状态置「正式」并写跟进记录 | `PUT /employee/basic/{id}/regularize` |
| 合同到期待办 | 7 天内到期列表，含合同起止与年限 | `GET /employee/basic/contract/remind` |
| 到期提醒 | 定时扫描生成提醒（幂等），可手工触发 | `POST /employee/basic/lifecycle/scan` |
| 续签 / 不续签 | 续签改合同起止并重算年限；不续签只留决策，不改在职状态 | `PUT /employee/basic/{id}/renew`、`PUT /employee/basic/{id}/not-renew` |
| 跟进记录 | 按员工查转正/续签/不续签的动作流水，含到期日前后值 | `GET /employee/basic/{id}/lifecycle/actions` |
| 提醒邮件 | 先写 `mail_send_log` 再投 MQ，失败按 10 秒一轮重投，最多 3 次 | `MailSendTask` |

## 技术栈

Spring Boot 2.4.0 / Spring Security（会话登录 + 菜单权限）/ MyBatis 2.1.0 / MySQL 8 / Redis（菜单权限缓存）/ RabbitMQ（提醒邮件）/ Flyway（`V1__workforce.sql` 基线）/ Druid / Vue 2 + Element UI。

## 架构

```
web/                     Vue 2 + Element UI，菜单由 /system/config/menu 动态注册路由
   │  HTTP  /employee/basic/**
wf-web                   Controller、SecurityConfig、全局异常
wf-service               EmployeeService、两个 Policy、定时任务、RabbitConfig
wf-mapper                Mapper 接口 + 同包 XML（不在 src/main/resources/mappers）
wf-model                 Employee / EmployeeLifecycleReminder / EmployeeLifecycleAction / MailSendLog
   │
MySQL(workforce)   Redis(menus_cache)   RabbitMQ(zhanhe.mail.exchange)
```

## 关键设计

- **幂等提醒**：`employee_lifecycle_reminder` 对 `(emp_id, remind_type, remind_date)` 建唯一键，插入用 `insert ignore`，重复即忽略；只有影响行数为 1 才继续发邮件、把员工 `lifecycleReminderStatus` 置 `SENT`。批任务同一天重跑不会重复打扰。
- **索引与查询**：`employee` 上建 `(workState, conversionTime, departmentId)`、`(workState, endContract, departmentId)` 与 `(lifecycleReminderStatus)`。待办查询原来是「只卡上界（今天+7）」，把「今天」补成下界（`>= curdate()`）后，本地数据从 617 条降到 3 条。
- **规则收口**：转正校验在 `EmployeeLifecyclePolicy`，续签/不续签校验在 `ContractRenewPolicy`，Controller 只负责解析参数与转错误信息，前端校验可以被绕过而 Service 不会。
- **事务边界**：转正、续签、不续签都在 `EmployeeService` 的一个方法里先 `updateByPrimaryKeySelective`（合同字段与 `lifecycleReminderStatus` 同一条 UPDATE）再 `insertAction`；**当前代码尚未加 `@Transactional`**（仓库里只有 `HrService`、`MenuService` 用了），第二次写失败会留下部分写入，详见 `docs/workforce.md`。
- **30 天冷静期**：合同待办与扫描候选都用 `not exists` 子查询排除 30 天内已有 `NOT_RENEW` 决策的员工，`ContractRenewPolicy.COOL_DOWN_DAYS` 与之对应。
- **通知重试**：先落 `mail_send_log`（status=0，tryTime=now+1 分钟）再投递；`MailSendTask` 每 10 秒捞 `status=0 and tryTime < sysdate()`，重试计数为 null 时按 0 处理（修复过空指针），到 3 次直接置 status=2。

## 数据表

| 表 | 关键字段 | 说明 |
| --- | --- | --- |
| `employee` | `workState`（试用期/在职/正式/离职）、`conversionTime`、`beginContract`、`endContract`、`contractTerm`、`lifecycleReminderStatus`（PENDING/SENT/DONE） | 原人事表，02 脚本扩展了枚举与提醒状态列 |
| `employee_lifecycle_reminder` | `emp_id`、`remind_type`（PROBATION/CONTRACT）、`remind_status`、`remind_date` | 提醒流水，唯一键做幂等 |
| `employee_lifecycle_action` | `emp_id`、`action_type`（REGULARIZE/RENEW/NOT_RENEW）、`before_end_contract`、`after_end_contract`、`reason`、`operator` | 跟进留痕，回答「后来怎么处理的」 |
| `mail_send_log` | `msgId`、`empId`、`status`（0 投递中/1 成功/2 失败）、`count`、`tryTime` | 发送日志与重试依据 |

## 快速开始

```bash
# 1. 建库并依次导入（02 扩展 employee，03 建动作表）
mysql -uroot -p -e "CREATE DATABASE IF NOT EXISTS workforce DEFAULT CHARSET utf8mb4"
mysql -uroot -p workforce < sql/01_init.sql
mysql -uroot -p workforce < sql/02_lifecycle.sql
mysql -uroot -p workforce < sql/03_lifecycle_action.sql

# 2. 打包（Java 8）
mvn -f wfserver/pom.xml clean package

# 3. 启动，端口 8081
java -jar wfserver/wf-web/target/wf-web-0.0.1-SNAPSHOT.jar

# 4. 前端 dev server，8084 代理到 8081
cd web && npm install && npm run serve
```

`wfserver/wf-web/src/main/resources/application.yml` 的数据源、RabbitMQ、Redis 都可用环境变量覆盖（`MYSQL_HOST`/`MYSQL_USER`/`MYSQL_PASSWORD`、`RABBIT_HOST`、`REDIS_HOST` 等），默认连本机。Flyway 开了 `baseline-on-migrate: true`，手工导过 `01_init.sql` 后启动只写基线记录，不会重复建表。登录后从菜单「生命周期提醒」（`/emp/lifecycle`）进入待办页。

## 目录结构

```
workforce/
├── sql/                  01_init.sql（原表结构与样例数据）、02_lifecycle.sql（提醒表/索引）、03_lifecycle_action.sql（动作表）
├── web/                  Vue 前端，EmpLifecycle.vue 是生命周期待办页
└── wfserver/
    ├── wf-model/         实体、MailConstants、RespBean
    ├── wf-mapper/        Mapper 接口与同名 XML
    ├── wf-service/       service/、task/、config/、utils/
    └── wf-web/           controller/、config/（Security）、resources/db/migration/
```

## 已验证的测试与脚本

- `wf-service` 单测 11 个用例全部通过：`ContractRenewPolicyTest` 8 个（离职拒续签、日期倒挂、合同重叠、到期日早于今天、正常续签、不续签原因必填等），`EmployeeLifecyclePolicyTest` 3 个。命令：`mvn -f wfserver/pom.xml -pl wf-service -am test`。
- `sql/03` 可重复执行（`CREATE TABLE IF NOT EXISTS`）；`sql/02` 是 `ALTER TABLE` + `CREATE INDEX`，重复执行会报「列/索引已存在」，需要在干净库上导入。02 末尾把 1–3 号员工改成 3 天后转正、4–6 号改成 5 天后合同到期，用于本地演示。
- 扫描可用 `POST /employee/basic/lifecycle/scan` 或页面「扫描提醒」按钮触发，返回本次新建提醒条数；同一天连续调用两次，第二次返回 0（幂等）。
- 扫描候选 SQL 在 `employee_lifecycle_action` 上做 30 天冷静期过滤，可用 `EXPLAIN` 确认走 `idx_employee_lifecycle_probation` / `idx_employee_lifecycle_contract`（组合索引的列序问题见 docs）。

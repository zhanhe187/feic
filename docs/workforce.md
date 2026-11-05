# 员工全周期治理系统 · workforce 设计说明

> 范围：员工转正、合同到期两件事的「发现 → 提醒 → 决策 → 留痕」。不做入职、离职、薪酬核算。
> 文中路径均相对 `workforce/` 目录；表名、字段名与 `sql/` 下的脚本一致。

## 1. 问题拆解

HR 的真实动作链只有四步：知道谁快到期、决定怎么办、把结果写回档案、留下为什么这么办。系统的设计围绕这四步展开，对应四张表/四段代码：待办查询（`employee`）、提醒生成（`employee_lifecycle_reminder`）、决策动作（`employee_lifecycle_action`）、通知投递（`mail_send_log`）。超过 7 天的人不进入待办，避免列表变成一张没人看的全量员工表。

## 2. 数据模型

### 2.1 表一览

| 表 | 角色 | 由谁创建 |
| --- | --- | --- |
| `employee` | 人事主表，待办与扫描的唯一数据源 | `sql/01_init.sql` |
| `employee_lifecycle_reminder` | 提醒流水，负责「今天提醒过谁」的幂等 | `sql/02_lifecycle.sql` |
| `employee_lifecycle_action` | 跟进留痕，负责「后来怎么处理的」 | `sql/03_lifecycle_action.sql` |
| `mail_send_log` | 邮件投递日志与重试计数 | `sql/01_init.sql`（Flyway 基线 `V1__workforce.sql` 里也有同名表） |

### 2.2 `employee` 的扩展

- `02_lifecycle.sql` 新增 `lifecycleReminderStatus varchar(20) DEFAULT 'PENDING'`，取值 PENDING / SENT / DONE，页面按它显示标签颜色。
- `workState` 从 `enum('在职','离职')` 扩成 `enum('试用期','在职','正式','离职')`。原枚举没有「试用期」，若不扩展，「待转正」只能靠 `conversionTime` 猜。
- 待办直接查 `employee`，不再 join 提醒表：列表要展示姓名、部门、合同年限，这些只有主表有。`lifecycleReminderStatus` 只做页面标签，**不再参与扫描筛人**（曾经用它拦重复提醒，已从候选 SQL 移除，见 §3.1、§5）。

### 2.3 `employee_lifecycle_reminder`

```sql
UNIQUE KEY uk_emp_type_date (emp_id, remind_type, remind_date)
KEY idx_reminder_emp (emp_id)
KEY idx_reminder_type_date (remind_type, remind_date)
```

唯一键选「员工 + 类型 + 日期」而不是「员工 + 类型」，是因为扫描是每天跑的批任务：只卡前两列等于要求「一个人一辈子只提醒一次」；带上日期后，同一天重跑天然幂等，跨天则自然再提醒一次。插入侧用 `insert ignore`，靠唯一键把重复挡在数据库层，应用层不做「先查再插」（那样仍有并发窗口）。

`remind_date` 是 `date`，Java 侧传 `new Date()` 会被截断到天，所以幂等粒度就是天：同一天同一类型只有一条，跨天则每天各一条。`remind_status` 插入时固定 `CREATED`，之后没有代码更新它——它只是流水标记，既不承担判重（判重靠唯一键），也不承担展示（展示用 `employee.lifecycleReminderStatus`）。

### 2.4 `employee_lifecycle_action`

字段是刻意做成「能独立回答一次决策」的：`action_type`（REGULARIZE / RENEW / NOT_RENEW）、`before_end_contract`、`after_end_contract`、`reason`、`operator`、`create_time`。

- 续签写 `before=旧到期日`、`after=新到期日`，可复核合同期有没有重叠。
- 不续签 `after` 为空，只留原因与操作人。
- 转正不改合同，则两个字段都写当前到期日（保持「一次动作一行、两列可直接对比」的形态）。
- `operator` 取 `HrUtils.getCurrentHr().getUsername()`，定时任务或异步上下文里拿不到登录态时降级为 `system`。

### 2.5 `mail_send_log`

`msgId`（UUID）、`empId`、`status`、`count`、`tryTime`。插入时 `count` 写死 0、`tryTime = now + 1 分钟`。`status` 被两个写入方复用：0 待发送（含「已投递但未被 confirm」）/ 1 已处理（发送成功，或未配置 SMTP 时跳过）/ 2 失败，`MailConstants` 另给了 `STATUS_PENDING` / `STATUS_SENT` / `STATUS_FAIL` 三个语义别名。这张表没有主键也没有唯一索引，`msgId` 重复无法在库层拦截；重试与状态更新都以 `msgId` 为条件。

### 2.6 索引为什么这么建

```sql
CREATE INDEX idx_employee_lifecycle_probation ON employee (workState, conversionTime, departmentId);
CREATE INDEX idx_employee_lifecycle_contract  ON employee (workState, endContract, departmentId);
CREATE INDEX idx_employee_lifecycle_status    ON employee (lifecycleReminderStatus);
```

待办/扫描 SQL 的形状是 `workState 等值/IN` + `conversionTime（或 endContract）between` + 可选 `departmentId =`。把低选择性的 `workState` 放最左，是为了先按状态收窄，再在第二列做范围扫描，避免全表；`order by conversionTime` 仍可能走 filesort（`IN` 命中多段时索引顺序不保证全局有序）。（列序的遗留问题见 §5。）

## 3. 核心流程

### 3.1 每日扫描

`EmployeeLifecycleTask` 用 `@Scheduled(cron = "0 0 9 * * ?")` 每天 9 点调 `EmployeeService.scanLifecycleReminders()`，也留了 `POST /employee/basic/lifecycle/scan` 供手工触发。

1. `endDate = now + 7 天`。
2. `EmployeeMapper.getLifecycleReminderCandidates(endDate)` 一次捞全量候选：满足「转正分支」或「合同分支」之一即可，不再看 `lifecycleReminderStatus`；合同分支内嵌 30 天冷静期子查询。
3. 逐人做二次判定 `needProbationReminder` / `needContractReminder`：日期非空、未转正/未离职、`conversionTime`（或 `endContract`）落在 `[今天 00:00, now+7天]`。SQL 里的 `curdate()` 与 Java 里的 `startOfToday()` 是同一语义的两道下界。
4. 命中则 `createReminderAndSend`。

判重只发生在第 4 步的 `insert ignore`：唯一键 `(emp_id, remind_type, remind_date)` 保证「同一员工、同一类型、同一天」只有一条，所以同一天重跑幂等、隔天自然再提醒。`employee.lifecycleReminderStatus` 既不拦扫描也不拦插入，只在页面上展示。

### 3.2 生成提醒（幂等 + 发信）

`createReminderAndSend` 依次做：组装提醒对象 → `insertIgnore` → 影响行数为 1 才继续 → `sendLifecycleMail` → 把 `employee.lifecycleReminderStatus` 更新为 `SENT`。影响行数为 0 表示当天已有同类型提醒，直接跳过，不发信也不改状态。

### 3.3 投递 MQ 与发送日志

`sendLifecycleMail`：生成 `msgId` → 插 `mail_send_log`（status=0）→ `convertAndSend` 投 `MailTaskMessage(msgId, empId)`，`CorrelationData` 带 `msgId`。消息体只带这两个字段，消费端据此取员工并精确回写对应那条日志，不按员工模糊匹配。`RabbitConfig` 声明 `DirectExchange zhanhe.mail.exchange`、持久化队列 `zhanhe.mail.queue`、routing key `zhanhe.mail.routing.key`，confirm 回调 ack 时把日志置 1（nack 只打日志，不置失败状态）。

消费端是 `mq/MailDispatchListener`，`@RabbitListener(queues = MailConstants.MAIL_QUEUE_NAME)` 收到消息后：按 `empId` 取员工 → 用 `JavaMailSender`（`@Autowired(required=false)`，发件人取 `workforce.mail.from`）发一封 `SimpleMailMessage` → 成功置 1；员工不存在直接置 2；发送抛异常时 `count+1` 并把日志置回 0，交给 `MailSendTask` 重投；容器里没有 `JavaMailSender`（未配置 SMTP）时不假装发送成功，也不进重试队列，只打一条 info 并把日志置 1，避免每 10 秒刷一次失败（空 host 仍可能创建 Bean 的情况见 §5 第 9 条）。

完整链路：落日志 → 投递 DTO → confirm 置 1 → 消费端发 SMTP → 失败 `count+1` 置回 0 → `MailSendTask` 每 10 秒重投 → 计数到 3 置 2。发信依赖 `spring-boot-starter-mail`，SMTP 参数在 `application.yml` 的 `spring.mail.*`（host/port/username/password 全部走环境变量，默认为空），发件人由 `workforce.mail.from` 决定。

### 3.4 失败重试

`MailSendTask` 每 10 秒跑一次：`select * from mail_send_log where status=0 and tryTime < sysdate()`。

- `count` 为 null 时按 0 处理——这正是修过的空指针点，`01_init.sql` 里 `count` 允许 NULL。
- `count >= 3`：直接置 status=2，不再投。
- 否则 `count = count + 1` 后重投，`tryTime` 不推进。所以首次投递后 1 分钟开始，每 10 秒重投一次，第 3 次重投后置失败；节奏由定时任务决定，不是指数退避。
- 消费端发送失败时也会 `count+1` 并置回 0，`count` 被消费端和补偿任务同时递增，`count=3` 时真正打到 SMTP 的次数少于 3 次（见 §5）。

### 3.5 30 天冷静期

合同待办（`getContractExpireEmployees` / `getContractExpireTotal`）与扫描候选里都有：

```sql
and not exists (select 1 from employee_lifecycle_action a
                where a.emp_id = e.id and a.action_type = 'NOT_RENEW'
                  and a.create_time >= date_sub(curdate(), interval 30 day))
```

含义是「这个人 30 天内已经决定不续签了，别再出现在待办里」。30 在 SQL 里是字面量，`ContractRenewPolicy.COOL_DOWN_DAYS` 只是同值常量，两者没有代码级联动。

### 3.6 决策落库

| 动作 | 校验（Policy） | 写库 | 跟进记录 |
| --- | --- | --- | --- |
| 转正 | 不为空；非「正式」；状态为「试用期/在职」或 null | `workState=正式`、`lifecycleReminderStatus=DONE` | REGULARIZE，前后到期日均为当前值，reason「HR 执行转正」 |
| 续签 | 非离职；起止日必填；到期日晚于起始日；新起始日不早于当前到期日（允许同日续接）；新到期日晚于今天 | `beginContract`/`endContract`/`contractTerm`（年差×12+月差再除以 12，两位小数）、`DONE` | RENEW，before=旧到期日，after=新到期日，reason 缺省「HR 执行续签」 |
| 不续签 | 非离职；原因必填（trim 后非空） | 只置 `DONE`，**不改 `workState`** | NOT_RENEW，after 为 null，reason 必填 |

三个写库方法（`regularizeEmployee`、`renewContract`、`markNotRenew`）都标了 `@Transactional`：`employee` 行与 `employee_lifecycle_action` 行同成同败，Controller 通过代理调用，注解生效。发信不在这条事务里——扫描时先插提醒、再写日志投 MQ，由消费端异步发信。

不续签不自动改离职是刻意收窄权限边界：离职涉及工龄、薪酬结算，属于人事动作，提醒模块只记录决策。

## 4. 关键代码位置

| 类 / 文件 | 路径 | 职责 |
| --- | --- | --- |
| `EmpBasicController` | `wfserver/wf-web/src/main/java/com/zhanhe/workforce/controller/emp/EmpBasicController.java` | 待办、转正、续签、不续签、跟进记录、扫描接口 |
| `ContractRenewDto` | `.../controller/emp/dto/ContractRenewDto.java` | 续签/不续签请求体，日期用字符串传、Controller 严格解析 |
| `EmployeeService` | `wfserver/wf-service/src/main/java/com/zhanhe/workforce/service/EmployeeService.java` | 待办查询、扫描、三类办理、发信入队 |
| `EmployeeLifecyclePolicy` | `.../service/EmployeeLifecyclePolicy.java` | 转正前置校验、合同/转正日期合法性 |
| `ContractRenewPolicy` | `.../service/ContractRenewPolicy.java` | 续签/不续签规则、7 天窗口与 30 天冷静期常量 |
| `EmployeeLifecycleTask` | `.../task/EmployeeLifecycleTask.java` | 每日 9 点扫描 |
| `MailSendTask` | `.../task/MailSendTask.java` | 每 10 秒重投，计数到 3 置失败 |
| `RabbitConfig` | `.../config/RabbitConfig.java` | Exchange/Queue/Binding、publisher confirm 回写状态 |
| `MailTaskMessage` | `wfserver/wf-model/src/main/java/com/zhanhe/workforce/model/MailTaskMessage.java` | 消息体：`msgId` + `empId`，`Serializable` |
| `MailDispatchListener` | `wfserver/wf-service/src/main/java/com/zhanhe/workforce/mq/MailDispatchListener.java` | 消费端：取员工 → SMTP 发信 → 回写日志状态 |
| `MailSendLogService` | `.../service/MailSendLogService.java` | 发送日志的增改查 |
| `EmployeeMapper` + `.xml` | `wfserver/wf-mapper/src/main/java/com/zhanhe/workforce/mapper/EmployeeMapper.xml` | 待办/扫描/人数统计 SQL（XML 与接口同目录，不在 `src/main/resources/mappers`） |
| `EmployeeLifecycleReminderMapper` + `.xml` | `.../mapper/EmployeeLifecycleReminderMapper.xml` | 只有一条 `insert ignore` |
| `EmployeeLifecycleActionMapper` + `.xml` | `.../mapper/EmployeeLifecycleActionMapper.xml` | 插入动作、按员工查、按类型查最近一条 |
| `MailSendLogMapper` + `.xml` | `.../mapper/MailSendLogMapper.xml` | 状态更新、计数 +1、捞待重试 |
| `EmpLifecycle.vue` | `web/src/views/emp/EmpLifecycle.vue` | 两个 tab、续签/不续签弹窗、跟进记录弹窗、扫描按钮 |
| 建表脚本 | `sql/01_init.sql`、`sql/02_lifecycle.sql`、`sql/03_lifecycle_action.sql` | 主表、提醒表与索引、动作表 |
| 配置 | `wfserver/wf-web/src/main/resources/application.yml` | 数据源/RabbitMQ/Redis/Flyway/端口 8081 |

## 5. 边界与已知不足

1. **`insert ignore` 会吞掉非重复键错误**：唯一键冲突以外的部分错误（NOT NULL、类型截断等）被降级成 warning，接口拿到 0 行就当「今天已提醒过」静默跳过，既不告警也不重试；排查时只能靠人对数据。
2. **组合索引的列序问题**：`(workState, conversionTime, departmentId)` 里 `conversionTime` 是范围列，范围列之后的 `departmentId` 只能做索引下推过滤，无法缩小扫描范围。更合理的顺序是等值列（`workState`、`departmentId`）在前、范围列（`conversionTime`）在后，或单独建 `(departmentId, workState, conversionTime)`。
3. **冷静期子查询用不上最优索引**：`idx_action_type_time (action_type, create_time)` 的前导列不是 `emp_id`，按员工关联时派不上用场，优化器大概率回退到 `idx_action_emp` 再过滤时间。`(emp_id, action_type, create_time)` 才是这条子查询的形状。
4. **`idx_reminder_emp` 是冗余索引**：`uk_emp_type_date` 的最左前缀已经是 `emp_id`，单列索引可以删。
5. **`lifecycleReminderStatus` 只影响展示**：它已从候选 SQL 移除，判重只剩提醒表唯一键与合同分支的 30 天冷静期。副作用是「只要人还在 7 天窗口内，每天都会再生成一条提醒」——这是每日提醒的本意，但「今天处理完、今天别再提醒」这类需求现在无法表达，只能等 `endContract` 改写或人离开窗口。
6. **提醒表状态从不更新**：`employee_lifecycle_reminder.remind_status` 始终停在 `CREATED`，办理动作只改 `employee.lifecycleReminderStatus`，两表对「是否处理」没有单一事实来源；提醒表也没有读取接口，属于只写日志。
7. **重试计数被两处 +1**：消费端发送失败 `count+1` 并置回 0，`MailSendTask` 重投前又 `count+1`，`count=3` 时真正打到 SMTP 的次数少于 3 次；`tryTime` 从不推进，重投节奏完全由 10 秒任务决定，不是退避。
8. **`status` 一列两种含义、两个写入方**：0 既表示「消息未被 confirm」也表示「待发送」，1 既表示「投递成功」也表示「已处理/跳过」。confirm 与 listener 写同一列：发送成功后 confirm 置 1，listener 发信失败又置回 0；若 confirm 晚到，会把失败状态刷回 1，重试就丢了。
9. **没配 SMTP 也记 1**：`JavaMailSender` 注入不到时只打 info 并把日志置 1，等于「没发出去但记成功」。另外 `application.yml` 把 `spring.mail.host` 默认成空串而不是删掉该键，Spring Boot 自动配置按属性是否存在判断，空串通常也算存在，Bean 仍可能被创建，此时走的是发送失败 → 重试分支；要稳定走跳过分支，需要把整段 `spring.mail` 注释掉。
10. **`mail_send_log` 无索引无主键**：`status=0 and tryTime < sysdate()` 每 10 秒全表扫；`msgId` 重复无法拦截。
11. **`tryTime` 类型不一致**：`sql/01_init.sql` 是 `date`，Flyway 的 `V1__workforce.sql` 是 `datetime`；手工导 `01_init.sql` 后 `tryTime` 被截断到 0 点，首次重试提前。
12. **投递异常会让扫描中断**：`createReminderAndSend` 里 `convertAndSend` 没有 try/catch，broker 不可用时异常冒泡，本轮后续员工不再处理；提醒行已插入，当天重跑被唯一键挡掉，只能靠 `mail_send_log` 补投。
13. **续签没有并发控制**：读员工 → 校验 → 写回，`@Transactional` 默认隔离级别下两个 HR 同时给同一个人续签会后写覆盖先写，可能写出重叠合同期；没有乐观锁或行锁。
14. **时区与前端约定**：JDBC 串写了 `serverTimezone=Asia/Shanghai` 但 JVM 时区未固定；dev server 是 8084（8080 让给别的系统）代理到 8081；`/emp/lifecycle` 由 `sql/02_lifecycle.sql` 插 `menu`（id=29）后才出现，`router.js` 没有静态路由。

## 6. 如何验证

**起服务**：按 README 依次导 `sql/01`→`02`→`03`，`mvn -f wfserver/pom.xml clean package`，`java -jar wfserver/wf-web/target/wf-web-0.0.1-SNAPSHOT.jar`，确认 8081 起来、Flyway 只写 baseline。

**造数据**：`02_lifecycle.sql` 末尾已把 1–3 号设成 3 天后转正、4–6 号设成 5 天后合同到期。也可以直接：

```sql
UPDATE employee SET workState='试用期', conversionTime=DATE_ADD(CURDATE(), INTERVAL 2 DAY),
       lifecycleReminderStatus='PENDING' WHERE id=7;
UPDATE employee SET endContract=DATE_ADD(CURDATE(), INTERVAL 6 DAY),
       lifecycleReminderStatus='PENDING' WHERE id=8;
```

**看待办条数**：`GET /employee/basic/probation/remind?page=1&size=10` 与 `GET /employee/basic/contract/remind?page=1&size=10`，看返回的 `total`；页面「生命周期提醒」两个 tab 的数字应一致。

**验证下界的效果**：把 `EmployeeMapper.xml` 里 `getProbationEmployees` 的 `and e.conversionTime >= curdate()` 注掉，`total` 会从个位数跳到几百条（本地实测 617 → 3）。再用 `EXPLAIN` 对同样的 where 条件确认命中 `idx_employee_lifecycle_probation`。

**验证幂等**：连续两次 `POST /employee/basic/lifecycle/scan`，第一次返回本次新建条数，第二次返回 0；`select count(*) from employee_lifecycle_reminder where remind_date = curdate();` 数量不变。

**验证冷静期**：对某个合同快到期员工调 `PUT /employee/basic/{id}/not-renew`（reason 必填），该员工应从合同待办里消失；把 `employee_lifecycle_action.create_time` 改到 31 天前，会重新出现。

**验证投递与重试**：不配 SMTP 时触发扫描，`mail_send_log` 的新记录先写 0；如果容器里确实没有 `JavaMailSender`，消费端会置 1 并打一条「未配置 SMTP，跳过实际发信」，不进重试队列（空 host 仍可能创建 Bean 的情况见 §5 第 9 条）。配好 `MAIL_HOST`/`MAIL_USER`/`MAIL_PASSWORD`（必要时 `MAIL_PORT`/`MAIL_AUTH`/`MAIL_STARTTLS`）即可真实发信，发送失败时 `count` 递增并保持 0、到 3 后置 2。也可以直接造一条：`update mail_send_log set status=0, count=null, tryTime=now() - interval 1 minute where msgId='...';`，观察 10 秒一轮的补偿，顺带复现「count 为 null」的空指针修复路径。broker 停掉后再触发扫描，`convertAndSend` 的异常会让扫描接口报错（见 §5 第 12 条）。

**验证事务**：用超过 255 字符的 `reason` 调续签，严格模式下动作 INSERT 报错，`employee` 的合同字段应保持不变，说明 `@Transactional` 回滚生效。

**跑单测**：`mvn -f wfserver/pom.xml -pl wf-service -am test`，`ContractRenewPolicyTest` 8 个 + `EmployeeLifecyclePolicyTest` 3 个，共 11 个用例。

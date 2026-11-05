# IT 服务交付系统 · servicedesk

面向企业内部 IT 运维的工单与资产子系统。员工用一句口语描述报故障，系统先把描述拆成设备、现象、影响面、已尝试四个字段，员工确认后提交；工单按 5 个状态流转，由 IT 派单、处理、驳回或关闭，全过程保留处理记录与审计。

## 业务场景

员工遇到电脑开不了机、VPN 连不上、账号被锁时，往往只写一段含糊的描述。IT 接单后要反复确认：哪台设备、什么现象、影响了谁、已经试过什么。

- 员工：创建工单 → 确认系统识别出的结构化字段 → 跟踪状态 → 查看处理结果。
- IT：查看待处理工单 → 分配处理人 → 处理时参考历史同类工单 → 填写处理说明完成或驳回 → 关闭工单。
- 超时的待处理工单由定时任务扫描，写入系统通知，提醒 IT 跟进。
- 工单与资产列表按角色限制：普通员工默认只返回本人数据，IT 与管理员可看全量。

## 功能清单

- **资产台账**：资产新增、编辑、删除、查询、Excel 导入导出，记录资产编号、类型、品牌型号、使用人、状态、领用/归还时间。
- **工单提交**：标题必填，可附描述；服务端强制工单号为 `IT` + 毫秒时间戳，状态置为 `pending`，清空处理人，不信任前端传入的状态字段。
- **工单派发**：`pending → processing`，必须指定处理人，写处理记录。
- **工单处理**：`processing → completed`，填写处理说明，写入 `complete_time`。
- **工单驳回**：`pending/processing → rejected`，填写驳回说明，写入 `complete_time`。
- **工单关闭**：`pending/processing/rejected/completed → closed`，可附关闭说明。
- **处理记录时间线**：创建、派发、处理、驳回、关闭各写一条 `it_ticket_record`，按记录顺序展示。
- **角色数据范围**：无处理权限的用户，工单列表只返回本人申请的单；无资产管理权限的用户，资产列表只返回本人名下的资产。
- **超时提醒**：超过 24 小时仍未处理的 `pending` 工单，写入 `sys_notice`；Quartz 每 30 分钟扫描一次，也可手工触发。
- **AI 分诊**：建单前调用 `/it/ai/ticket/intake`，返回设备、现象、影响面、已尝试、建议类型、建议优先级、被丢弃字段、是否降级；接口不写库，员工确认后随建单提交。
- **历史工单检索**：IT 处理工单时按标题、现象、描述、设备检索已完结工单，返回真实工单号、相关度与最近一次处理说明，可一键引用。
- **审计**：每次 AI 调用写 `it_ai_log`，只落输入/输出摘要（SHA-256 前 16 位十六进制 + 原文长度），不落原文；工单增删改走 `@Log` 写 `sys_oper_log`。

## 技术栈

- 后端：Java 17、Spring Boot 4.0.6、Maven 多模块、Spring Security、JJWT 0.9.1、Redis 会话。
- 持久化：MyBatis（starter 4.0.1）、PageHelper 4.1.0、MySQL 8、Druid 1.2.28。
- 调度与模型：Quartz（`spring-boot-starter-quartz`）、LangChain4j 1.13.0（OpenAI 兼容协议）。
- 前端：Vue 3.5、Element Plus 2.13、Vite 6.4。
- 其他：Fastjson2 2.0.62、Springdoc 3.0.3、POI 4.1.2。

## 架构

```text
web/（Vue 3 + Element Plus）
      │  /dev-api 由 Vite 代理到 http://localhost:8080
      ▼
desk-admin（启动类、Controller、application.yml）
      ▼
desk-framework（Spring Security、JWT + Redis 会话、DataScope/Log 切面、全局异常）
      ▼
desk-system（工单、资产、处理记录、AI 分诊、Mapper）
      ▼
MySQL 8            Redis 7            LangChain4j → OpenAI 兼容模型（可选）
```

- `desk-admin`：启动类、`ItTicketController`/`ItAssetController`/`ItAiController`、`application.yml`、`ItTicketTask`。
- `desk-framework`：Spring Security、`TokenService`、`PermissionService`、`DataScopeAspect`/`LogAspect`、全局异常。
- `desk-system`：工单、资产、处理记录、AI 分诊、Mapper XML、提示词、单测。
- `desk-common` / `desk-quartz`：统一响应与工具类、Quartz 封装与调度。
- `web`：Vue 3 前端，工单页 `src/views/it/ticket/index.vue`、资产页 `src/views/it/asset/index.vue`。

## 关键设计

### 状态机集中校验

5 个状态：`pending`、`processing`、`completed`、`rejected`、`closed`；8 条合法流转写成一张静态流转表 `Map<当前状态, 允许目标状态集合>`，所有改状态的动作先过 `assertTransition`。没有通用 `update status` 接口，避免前端传什么就存什么。非法流转抛 `IllegalStateException`，由全局异常处理器返回错误；Controller 上的 `@Log` 会把这次失败写进 `sys_oper_log`（不会写工单处理记录）。规则是代码里的常量表，不是数据库表，改一条流转只需要改这一处。

### 接口按动作拆开

`PUT /it/ticket/{ticketId}/assign`、`/process`、`/reject`、`/close` 各自校验参数、写各自的处理记录；`assign` 必填处理人，`process` 必须写处理说明、`reject` 必须写驳回原因，`close` 只改状态。新增一条流转要同时改 `TicketStateMachine` 和对应的动作实现。

### JWT + Redis 会话与续期

- JWT 只携带 `login_user_key`（随机会话 UUID）与 `sub`（用户名），HS512 签名，不写角色与权限，也不设 JWT 自身的过期时间。
- `LoginUser`、权限集合、登录 IP/浏览器等存 Redis 键 `login_tokens:{uuid}`，TTL 为 `token.expireTime`（30 分钟）。
- 每次请求由 `JwtAuthenticationTokenFilter` 解析 JWT、从 Redis 取登录态；剩余有效期不足 20 分钟时自动刷新 Redis TTL；退出登录删除该键。
- 因此改权限或禁用账号只影响 Redis 中的会话，不需要等 JWT 过期。

### 功能权限与数据权限两层

| 层 | 落地位置 | 作用 |
|---|---|---|
| 功能权限 | `@PreAuthorize("@ss.hasPermi('it:ticket:handle')")` | 这个接口能不能调；管理员拥有 `*:*:*` |
| 数据范围 | 列表：`selectItTicketList`、`selectItAssetList` 强制 `applicant` / `owner_user`；单条：`assertTicketVisible` 校验 `applicant` 或 `handler` | 无处理/管理权限时，看不到也不允许操作他人工单，越权按「工单不存在」返回 |
| 状态权限 | `TicketStateMachine.assertTransition` | 这一行能不能改到目标状态 |

前端的 `v-hasPermi` 只负责隐藏按钮，不能替代服务端校验。`@DataScope` 切面目前只用于系统用户、部门、角色查询，IT 工单与资产的数据范围是在 Service 里手写条件注入的。

### AI 分诊链路

建单页先调 `POST /it/ai/ticket/intake`，`AiIntakeService` 依次完成：脱敏 → 组装提示词 → 调模型 → 解析 JSON 容错 → 证据校验 → 优先级规则 → 白名单与截断 → 写审计摘要。模型调用失败时改走 `RuleIntakeEngine`，返回同样结构的 `IntakeResult`，并带 `degraded=true` 与提示语。接口只返回建议值，不写库；员工可修改或清空后提交。

模型只被要求输出设备、现象、影响面、已尝试、类型与判定依据；状态、处理人由建单逻辑强制设置，优先级由影响面按规则推导，模型即使返回也不采纳。

### 降级与熔断

`AiLlmClient` 接口隔离模型客户端，实现类 `LangChain4jClient` 使用 OpenAI 兼容协议，超时 8 秒、`maxRetries=0`（不重试），输出格式约束为 `json_object`，请求与响应日志关闭。

以下情况一律改走规则引擎：`provider` 不是 `openai` 或没有 Key、熔断打开、当日配额用尽、模型超时/网络错误/5xx/返回空、输出不是合法 JSON。其中 `provider` 不是 `openai` 或没有 Key 属于默认规则模式，不算降级（`degraded=false`）；熔断、配额、调用或解析失败会带 `degraded=true` 与提示语。熔断阈值默认连续 3 次失败，打开 300 秒，期间直接走规则引擎；窗口结束自动放行，成功一次即清零。熔断状态在单实例内存中，多实例不共享。

### 配额

`AiQuota` 用 Redis `INCR` 做每人每日计数，键为 `ai:quota:{username}:{yyyyMMdd}`，首次计数时设置 25 小时过期；默认上限 200 次/人/日，`daily-limit <= 0` 表示不限。Redis 不可用时放行，配额只做成本保护，不影响业务正确性。

### 脱敏与审计

发送给模型前，`SensitiveMasker` 对手机号、18 位身份证、邮箱、密码/口令/验证码做打码；规则引擎在本地运行，使用原始文本。`AiAuditService` 每次调用写一条 `it_ai_log`，字段包括场景、用户、provider、model、是否降级、耗时、输入/输出摘要；摘要格式为 SHA-256 前 8 字节的十六进制加 `:` 加原文字符数，无法还原原文。审计失败只打日志，不影响主流程。工单提交时再写一条 `intake_commit` 记录，把「这张工单采纳了识别结果」与工单号关联。

### 历史工单 BM25 检索

`KbRetriever` 从 `it_ticket` 读取最近 500 条 `completed/closed` 工单，关联最近一次 `PROCESS` 处理说明作为解决方案；中文按 2-gram、ASCII 按单词切分，字段加权后计算 BM25（`k1=1.2`、`b=0.75`），按最高分归一化为 0–100 的相关度，低于 15 的命中丢弃。前端默认取前 3 条，最多 10 条；只对 `it:ai:similar` 授权，普通员工不能通过该接口读取他人工单。

## 接口清单

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/login` | 账号、密码、验证码登录，返回 token |
| GET | `/getInfo` | 当前用户、角色、权限 |
| GET | `/it/ticket/list` | 工单分页；权限 `it:ticket:list`，非处理角色只看本人 |
| GET | `/it/ticket/{ticketId}` | 工单详情，附带 `records` 处理记录；权限 `it:ticket:query` |
| POST | `/it/ticket` | 创建工单；权限 `it:ticket:add` |
| PUT | `/it/ticket/{ticketId}/assign` | 派发，设置处理人；权限 `it:ticket:handle` |
| PUT | `/it/ticket/{ticketId}/process` | 处理完成，填写说明；权限 `it:ticket:handle` |
| PUT | `/it/ticket/{ticketId}/reject` | 驳回，填写说明；权限 `it:ticket:handle` |
| PUT | `/it/ticket/{ticketId}/close` | 关闭工单；权限 `it:ticket:close` |
| DELETE | `/it/ticket/{ticketIds}` | 批量删除工单；权限 `it:ticket:remove` |
| POST | `/it/ticket/scanOverdue` | 手工扫描超时工单；权限 `it:ticket:handle` |
| GET | `/it/asset/list` | 资产分页；权限 `it:asset:list`，无管理权限只看本人 |
| GET | `/it/asset/{assetId}` | 资产详情；权限 `it:asset:query` |
| POST | `/it/asset` | 新增资产；权限 `it:asset:add` |
| PUT | `/it/asset` | 修改资产；权限 `it:asset:edit` |
| DELETE | `/it/asset/{assetIds}` | 批量删除资产；权限 `it:asset:remove` |
| POST | `/it/asset/export` | 导出资产 Excel；权限 `it:asset:export` |
| POST | `/it/asset/importData` | 导入资产 Excel；权限 `it:asset:import` |
| POST | `/it/asset/importTemplate` | 下载导入模板 |
| POST | `/it/ai/ticket/intake` | 描述结构化抽取，不写库；权限 `it:ai:intake` |
| POST | `/it/ai/kb/similar` | 历史同类工单检索；权限 `it:ai:similar` |
| GET | `/it/ai/quota` | 当前用户今日剩余额度，`-1` 表示不限 |
| GET | `/it/ai/log/{ticketId}` | 某工单最近 AI 调用记录，`limit` 默认 5、最大 20；权限 `it:ticket:query` |

## 数据表

| 表 | 用途 |
|---|---|
| `it_asset` | 资产台账；`asset_no` 唯一，记录类型、品牌型号、使用人、状态、领用/归还时间 |
| `it_ticket` | 工单主表；含 AI 槽位 `device_info`、`symptom`、`impact_scope`、`tried_actions`、`ai_source` |
| `it_ticket_record` | 处理记录；`action` 为 `CREATE/ASSIGN/PROCESS/REJECT/CLOSE`，`process_result` 记录动作后的状态 |
| `it_ai_log` | AI 调用审计；`input_digest`、`output_digest` 存摘要，`degraded` 标记降级 |
| `sys_*` | 用户、角色、菜单、部门、字典、公告、操作日志、登录日志、定时任务等平台表 |
| `QRTZ_*` | Quartz 标准表，共 11 张；当前默认内存 JobStore，不导入也能运行（见快速开始） |

## 快速开始

1. 建库并导入脚本（在仓库根目录执行）：

```bash
mysql -uroot -p -e "CREATE DATABASE IF NOT EXISTS servicedesk DEFAULT CHARACTER SET utf8mb4"
mysql -uroot -p servicedesk -e "source servicedesk/sql/01_init.sql"
mysql -uroot -p servicedesk -e "source servicedesk/sql/02_it_ticket.sql"
mysql -uroot -p servicedesk -e "source servicedesk/sql/03_ai_intake.sql"
```

`05_quartz.sql` 是 Quartz JDBC JobStore 的表；`ScheduleConfig` 当前整段注释，`application.yml` 也没有配置 `spring.quartz.job-store-type`，默认走内存 JobStore，任务在启动时从 `sys_job` 重建，因此不导入也能运行。确实要切 JDBC JobStore 时再执行。

2. 启动 Redis（默认 `localhost:6379`，库 0）。

3. 改数据库与 Redis：编辑 `desk-admin/src/main/resources/application-druid.yml` 的 master 连接，或设置环境变量 `MYSQL_HOST`、`MYSQL_PORT`、`MYSQL_USER`、`MYSQL_PASSWORD`、`MYSQL_DB_RY`；Redis 在 `application.yml` 的 `spring.data.redis`，或设置 `REDIS_HOST`、`REDIS_PORT`、`REDIS_DB`、`REDIS_PASSWORD`。

4. AI 可选。默认 `ai.provider=rule`，不联网也能用完整的规则抽取。要接模型，设置 `AI_PROVIDER=openai`、`AI_API_KEY=...`、`AI_BASE_URL=https://api.deepseek.com/v1`、`AI_MODEL=deepseek-chat`；只填 Key 而 `provider` 仍是 `rule` 时不会调用模型。其余可调项：`AI_TIMEOUT_SECONDS`（8）、`AI_DAILY_LIMIT`（200）、`AI_BREAKER_THRESHOLD`（3）、`AI_BREAKER_SECONDS`（300）。

5. 构建并启动后端：

```bash
mvn clean package -DskipTests
java -jar desk-admin/target/desk-admin.jar
```

默认端口 8080。单测：`mvn -pl desk-system -am test`（11 个测试类、47 个用例，不依赖数据库和 Redis）。

6. 启动前端：

```bash
cd web
npm install
npm run dev
```

开发服务器默认端口 80，`/dev-api` 代理到 `http://localhost:8080`；被占用时改 `web/vite.config.js` 的 `server.port`。打开前端后用 `admin / admin123` 登录（初始密码来自 `01_init.sql` 的 BCrypt 哈希），登录页默认开启图形验证码。

## 目录结构

```text
servicedesk/
├── desk-admin/      启动类、It*Controller、application.yml、ItTicketTask
├── desk-framework/  Security、JWT 会话、切面、全局异常
├── desk-system/     工单/资产/AI 业务、Mapper、prompts、单测
├── desk-common/     统一响应、分页、Excel、Redis、工具类
├── desk-quartz/     Quartz 封装与调度
├── sql/             01_init、02_it_ticket、03_ai_intake、05_quartz
└── web/             Vue 3 + Vite 前端（src/views/it）
```

更细的链路、参数与失败路径见 [docs/servicedesk.md](../docs/servicedesk.md)。

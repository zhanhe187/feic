# 架构说明

两个系统各自独立，不共享库表、不互相调用；公共部分只有本地基础设施与工程约定。

## 1. 部署拓扑

```mermaid
flowchart TB
  subgraph Client
    B1["浏览器 · workforce"]
    B2["浏览器 · servicedesk"]
  end
  subgraph App["应用层"]
    W["workforce · Spring Boot 2.4 :8081"]
    S["servicedesk · Spring Boot 4 :8080"]
  end
  subgraph Infra["基础设施"]
    M[("MySQL 8<br/>servicedesk / workforce")]
    R[("Redis 7")]
    Q["RabbitMQ 3"]
  end
  subgraph Ext["外部依赖"]
    L["模型服务<br/>OpenAI 兼容"]
    SMTP["SMTP"]
  end
  B1 --> W
  B2 --> S
  W --> M
  W --> R
  W --> Q
  Q --> SMTP
  S --> M
  S --> R
  S -. 可选 .-> L
```

- `workforce` 用 Flyway 管理表结构，启动时按版本执行；`servicedesk` 用手工脚本建库，Quartz 自带 `qrtz_*` 表。
- 模型服务是可选依赖：没有 Key 时 `servicedesk` 直接使用规则引擎，功能不缺失。

## 2. servicedesk 请求链路

```mermaid
sequenceDiagram
  participant U as 浏览器
  participant F as Spring Security 过滤器链
  participant C as Controller
  participant T as Service
  participant DB as MySQL
  participant RD as Redis

  U->>F: 请求 + Authorization: Bearer token
  F->>RD: 按会话标识取登录态
  alt 取不到
    RD-->>F: 空
    F-->>U: 401 未登录
  else 取到
    RD-->>F: 用户信息与权限
    F->>C: 放行
    C->>C: @PreAuthorize 校验功能权限
    C->>T: 调用业务方法
    T->>T: 按角色注入数据范围条件
    T->>DB: 查询 / 更新
    DB-->>T: 结果
    T-->>C: 领域结果
    C-->>U: 统一响应体
  end
```

权限是两层的，缺一不可：

| 层 | 决定什么 | 落地位置 |
|---|---|---|
| 功能权限 | 这个接口能不能调 | `@PreAuthorize("@ss.hasPermi('...')")` |
| 数据权限 | 调通之后能看到哪些行 | Service 查询前注入申请人 / 处理人条件 |
| 状态权限 | 这一行能不能改到这个状态 | 状态机校验，非法流转直接抛业务异常 |

## 3. servicedesk 的 AI 分诊链路

```mermaid
sequenceDiagram
  participant U as 员工
  participant API as ItAiController
  participant S as AiIntakeService
  participant Mask as SensitiveMasker
  participant LLM as LangChain4jClient
  participant Rule as RuleIntakeEngine
  participant V as EvidenceValidator
  participant P as PriorityRule
  participant DB as it_ai_log

  U->>API: POST /it/ai/ticket/intake
  API->>S: 口语描述
  S->>S: 配额检查（每日）+ 熔断检查
  S->>Mask: 脱敏
  S->>LLM: 提示词 + 脱敏文本
  alt 成功且 JSON 可解析
    LLM-->>S: 四个槽位
    S->>V: 槽位是否在原文出现
    V-->>S: 保留 / 丢弃
    S->>P: 影响面 → 优先级
  else 无 Key / 超时 / 5xx / 坏 JSON / 配额 / 熔断
    LLM--xS: 异常
    S->>Rule: 关键词计数与从句抽取
    Rule-->>S: 同样结构的槽位
  end
  S->>DB: 落审计摘要（不存原文）
  S-->>API: 槽位 + 是否降级 + 提示语
  API-->>U: 待员工确认的结果
```

关键约束：AI 的输出**只包含信息字段**，不包含状态、处理人、优先级；
落库前服务端再截断长度、走枚举白名单，并强制把状态置为初始值。

## 4. workforce 的提醒链路

```mermaid
sequenceDiagram
  participant T as EmployeeLifecycleTask
  participant DB as MySQL
  participant Q as RabbitMQ
  participant C as 邮件消费端
  participant H as HR

  T->>DB: 扫 7 天窗口内待转正 / 合同到期
  T->>DB: 插入提醒（唯一索引保证当天只一条）
  T->>Q: 投递通知消息
  Q->>C: 消费
  C->>DB: 写发送日志（先落库再发信）
  C->>C: SMTP 发送
  C->>DB: 回写成功 / 失败次数
  H->>DB: 办理转正 / 续签 / 标记不续签
  DB-->>H: 改合同 + 写动作 + 关提醒（同一事务）
```

失败补偿：定时任务每 10 秒扫一次「待发送或失败且次数小于 3」的记录重新投递，
超过 3 次不再重试并保留记录，避免无限重试掩盖配置错误。

## 5. 模块职责

### servicedesk

| 模块 | 职责 |
|---|---|
| `desk-admin` | 启动类、HTTP 接口、登录与静态资源 |
| `desk-framework` | Security 过滤器链、JWT 会话与续期、数据权限切面、操作日志切面、全局异常 |
| `desk-system` | 工单 / 资产 / 处理记录 / AI 分诊的全部业务实现 |
| `desk-common` | 统一响应、分页工具、Excel、字符串与日期工具 |
| `desk-quartz` | 定时任务框架与超时提醒任务 |

### workforce

| 模块 | 职责 |
|---|---|
| `wf-web` | 启动类、Controller、登录过滤器与安全配置 |
| `wf-service` | 生命周期规则、续签规则、提醒与动作、定时扫描、邮件消费 |
| `wf-mapper` | Mapper 接口与 XML，待办查询与扫描 SQL 都在这里 |
| `wf-model` | 实体与常量 |

## 6. 环境变量

两个后端都支持环境变量覆盖配置，缺省值面向本地开发。

| 变量 | 作用 | 缺省 |
|---|---|---|
| `MYSQL_HOST` / `MYSQL_PORT` | 数据库地址 | `localhost` / `3306` |
| `MYSQL_USER` / `MYSQL_PASSWORD` | 数据库账号 | `root` / `root` |
| `MYSQL_DB_RY` | servicedesk 库名 | `servicedesk` |
| `MYSQL_DB_WORKFORCE` | workforce 库名 | `workforce` |
| `REDIS_HOST` / `REDIS_PORT` | Redis | `127.0.0.1` / `6379` |
| `RABBIT_HOST` / `RABBIT_PORT` | RabbitMQ | `127.0.0.1` / `5672` |
| `AI_BASE_URL` / `AI_API_KEY` / `AI_MODEL` | 模型服务 | 空（走规则引擎） |

## 7. 失败与降级矩阵

| 情况 | servicedesk | workforce |
|---|---|---|
| 数据库不可用 | 接口 500，统一异常体 | 同样 500；定时任务本次跳过 |
| Redis 不可用 | 登录态校验失败，返回未登录 | 提醒仍可落库，缓存退化为直查 |
| MQ 不可用 | 不影响工单主流程 | 发送日志留在库中，恢复后由补偿任务重投 |
| 模型不可用 | 降级规则引擎，返回 `degraded=true` | 不涉及 |
| 模型输出越界 | 证据校验丢弃 + 白名单置空 | 不涉及 |

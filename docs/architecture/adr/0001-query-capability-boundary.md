# ADR-0001：建立独立 Query 能力边界

- 状态：已接受
- 日期：2026-07-12
- 决策范围：Agent-ready 计划目标 B–D 的 Query 能力、适配器和契约

## 背景

当前查询能力位于 `server` 的 `DataProviderServiceImpl`，并直接依赖 `BaseService`、实体服务、Mapper、Spring Security、AES/Jackson 和 `DataProviderManager`。该实现同时服务登录查询、预览查询、分享查询、下载任务和部分数据源维护逻辑。

未来 Agent 必须通过稳定、可测试、可审计的业务能力执行查询，不能直接访问 Controller、Mapper、数据库或 Provider 实现。由于 `core` 本身包含 Web、MyBatis、Quartz、POI 和 Selenium 等依赖，让新模块依赖 `core` 不能形成有效隔离。

## 决策

新增纯 Java Maven 模块 `query`。该模块不依赖 `core`、`security`、`server`、Spring、MyBatis、Jackson、AES 或任何 DataProvider 实现。`server` 依赖 `query` 并提供所有基础设施适配器。

### 1. 模块内部结构

```text
query
└── yubi.query
    ├── api          # Use Case、命令、结果和公开异常
    ├── application  # 查询编排
    ├── domain       # 纯值对象和规则
    └── port         # 定义、权限、变量、引擎和审计端口
```

`query` 只使用 JDK 21。测试依赖可以使用 JUnit，生产依赖不得引入框架。

### 2. 公开 Use Case

```java
public interface ExecuteQueryUseCase {
    QueryResult execute(ExecuteQueryCommand command, QueryExecutionContext context);
}

public interface PreviewQueryUseCase {
    QueryResult preview(PreviewQueryCommand command, QueryExecutionContext context);
}
```

`QueryExecutionContext` 由可信适配器创建，包含调用渠道、当前主体引用、组织引用和关联 ID。REST 请求、Agent Tool 参数和模型输出不得直接指定或覆盖主体与组织。

调用渠道至少包含：

- `AUTHENTICATED`：普通登录用户。
- `SHARED`：经过分享令牌验证的身份。
- `SYSTEM`：调度和下载任务。

### 3. 命令与结果

`ExecuteQueryCommand` 包含：

- `viewId`
- 选择列、关键字、计算列、聚合、过滤、分组和排序
- 分页
- Query 变量参数
- 缓存、缓存时长和并发控制开关
- 是否请求返回脚本

它不包含 `vizId`、`vizName`、`vizType`、分享令牌、用户 ID、组织 ID 或数据源配置。

`PreviewQueryCommand` 包含 `sourceId`、脚本、脚本类型、列、变量和预览行数。Preview Use Case 只供数据视图编辑器使用，不注册为 Agent V1 工具。

`QueryResult` 包含列元数据、数据行、分页和可选脚本，不暴露 `Dataframe`。REST、下载和前端适配器负责在 `QueryResult` 与现有传输格式之间转换。

查询 API 使用自己的纯值对象表示列、过滤、聚合和排序。`server` 的 Provider 适配器负责转换为 `yubi.core.data.provider.QueryScript` 和 `ExecuteParam`，避免把 Provider 类型带入 Query API。

### 4. 端口职责

| 端口 | 输入/输出职责 | `server` 适配器来源 |
|---|---|---|
| `QueryDefinitionPort` | 按 ID 返回 View、Source、脚本和 Schema 的只读投影 | View/Source 实体服务、配置解析和 AES 解密 |
| `QueryAccessPolicyPort` | 验证 READ/分享范围，返回允许列、Owner 状态和脚本可见性 | Spring Security、ViewService、RelSubjectColumnsMapper |
| `QueryVariablePort` | 返回系统、组织、View 和请求覆盖后的变量 | 当前用户、VariableService |
| `QueryEnginePort` | 执行纯 Query 计划并返回纯结果 | DataProviderManager 及 DTO 转换 |
| `QueryAuditPort` | 记录渠道、主体、关联 ID、耗时、结果规模和失败分类 | server 审计实现；不得记录令牌、密码或数据源配置 |

端口实现不得把 MyBatis 实体、Spring `Authentication` 或 Provider DTO 返回给 `query`。

### 5. 应用编排顺序

Execute Use Case 固定按以下顺序执行：

1. 校验命令结构和非空查询。
2. 通过 `QueryDefinitionPort` 读取只读定义。
3. 通过 `QueryAccessPolicyPort` 计算执行权限、允许列和脚本可见性。
4. 通过 `QueryVariablePort` 解析变量；Owner 禁用权限变量。
5. 归一化分页并构建纯 Query 计划。
6. 调用 `QueryEnginePort`。
7. 按脚本可见性清理结果。
8. 无论成功或失败都调用 `QueryAuditPort`，审计失败不得覆盖原查询结果或异常。

分页语义保持当前基线：缺失时为第 1 页、1000 行；页码小于 1 时归一为 1；页大小小于 1 时归一为 1000，上限为 `Integer.MAX_VALUE`。

### 6. 身份与分享边界

分享令牌只在 `server` 适配器解密和验证。适配器必须验证令牌类型为 View 且 `vizId` 与命令 `viewId` 一致，然后创建 `SHARED` 上下文。

如果适配现有 `runAs`，身份切换必须使用显式 `try/finally` 作用域，并在结束时调用 `releaseRunAs()`。Query 应用层只接收可信上下文，不知道令牌和 Spring Security 的存在。

目标 C 将公共查询令牌迁移到 `X-YuBi-Share-Token`。分享页面路由不携带执行请求体；页面 JavaScript 负责设置该请求头。

### 7. 异常边界

Query API 定义以下稳定异常类别：

- `QueryValidationException`：命令、分页或字段无效。
- `QueryAccessDeniedException`：主体、组织、View 或列权限拒绝。
- `QueryDefinitionException`：View、Source、配置或 Schema 无法读取。
- `QueryExecutionException`：Provider 执行失败，保留原始 cause。

目标 B 的旧 REST 适配器继续映射为当前 `ResponseData` 和全局异常处理语义。不能吞掉 Provider cause，也不能把权限失败降级为空结果。

### 8. REST 和前端迁移

目标 B 只提取能力，旧 Controller 委托新 Use Case。目标 C 新增：

- `POST /api/v1/queries/execute`
- `POST /api/v1/queries/preview`
- `POST /api/v1/public/queries/execute`

目标 C 完成全部仓库内调用迁移后，目标 D 直接删除三个旧入口和临时 re-export。项目没有生产用户或已知外部调用方，因此不设置发布周期兼容窗口，也不使用 302/307/308 重定向。

数据库结构、View/Dashboard 配置、View model 的历史 Schema 形态和 DataProvider SPI 不变。

## 备选方案

### 让 Query 直接依赖 `core`

拒绝。虽然初始映射代码较少，但会传递 Web、MyBatis、Quartz、POI、Selenium 和实体模型，无法形成 Agent 可复用的能力边界。

### 先拆 `core-api/core-impl`

拒绝作为首步。该方案影响所有后端模块，范围远超查询切片。Query 自有纯值对象和 `server` 映射的成本更可控。

### 仅拆分 `DataProviderServiceImpl` 内部类

拒绝。只能降低单类体积，无法阻止 Agent 或新调用方继续依赖 `server`、实体和 Provider 类型。

### 立即拆微服务

拒绝。当前没有独立部署、扩缩容或团队边界需求，会提前引入网络、认证、分布式事务和运维成本。

## 影响

正向影响：

- REST、调度、下载和 Agent Tool 可以复用同一确定性查询能力。
- 权限、变量、分页和脚本脱敏可以脱离 Spring 容器测试。
- Provider、模型供应商和 Web 框架不会进入 Query 公开 API。
- 后续元数据和写能力可以沿同一端口-适配器方式扩展。

代价：

- 需要维护 Query 值对象与现有实体/Provider DTO 的映射。
- 目标 B 初期文件和接口数量增加。
- 旧实现迁移期间会短暂存在委托层，但目标 D 必须清理。

## 验收约束

- 目标 A 的全部特征测试必须在目标 B–D 持续通过。
- `query` 生产依赖树不得出现 `core`、Spring、MyBatis、Security 或 Provider 实现。
- 任何身份、权限、变量或脚本可见性变化必须单独提出，不得作为重构副作用混入。

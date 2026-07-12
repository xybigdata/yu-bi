# YuBi Agent-ready 架构改造计划

## 1. 目标与边界

YuBi 后续将逐步引入 AI 和 Agent 能力。本计划不以一次性重写为目标，而是先把现有查询、元数据、权限和可视化能力整理为稳定的业务能力接口，再让 Agent 通过受控工具调用这些接口。

已确认的基本决策：

- 保持 Maven 多模块单体和单体部署，不拆微服务。
- 首个能力切片是查询执行，后续再扩展元数据、可视化和写操作。
- `query` 模块不依赖 `core`、`security`、`server`、Spring MVC、MyBatis 或具体 DataProvider 实现。
- Agent 不直接访问 Controller、Mapper、数据库、数据源配置或 Provider 实现。
- 项目当前没有生产用户或已知外部调用方，REST 接口可以在仓库内原子迁移后直接清理，不设置发布版本兼容窗口。
- 数据库结构、已保存的 View/Dashboard 配置和 DataProvider SPI 保持兼容。
- Agent V1 只允许查询已有 View，不接受任意 SQL，不提供写操作。

本计划不包含模型选型、提示词设计或多 Agent 协作。这些决策应在查询和元数据能力稳定后单独完成。

## 2. 目标架构

```mermaid
flowchart TD
    UI["现有前端"] --> REST["REST 适配器"]
    AgentUI["Agent 工作区"] --> Agent["Agent Runtime"]
    Agent --> Tools["Tool 适配器"]
    REST --> QueryAPI["Query 能力 API"]
    Tools --> QueryAPI
    Tools --> MetadataAPI["Metadata 能力 API"]
    QueryAPI --> Policy["权限与变量端口"]
    QueryAPI --> Engine["QueryEnginePort"]
    MetadataAPI --> Catalog["元数据目录端口"]
    Engine --> Provider["DataProvider 适配器"]
    Agent --> Audit["审计、Trace 与评测"]
```

### 2.1 Query 模块边界

`query` 模块只包含纯 Java API、应用服务、领域值对象和端口：

- `ExecuteQueryUseCase`：执行已有 View。
- `PreviewQueryUseCase`：服务现有数据视图编辑器，不暴露为 Agent V1 工具。
- `QueryDefinitionPort`：读取 View 和 Source 的只读投影。
- `QueryAccessPolicyPort`：解析用户、组织、视图和列权限，并决定脚本是否可见。
- `QueryVariablePort`：解析系统、组织、View 和请求变量。
- `QueryEnginePort`：隔离现有 `DataProviderManager`。
- `QueryAuditPort`：记录调用来源、耗时、结果规模和失败状态。

Mapper 查询、Spring Security 上下文、AES 解密、Jackson 配置解析以及现有 Provider DTO 转换由 `server` 中的适配器承担。`query` 不暴露 MyBatis 实体或 `yubi.core.data.provider` 类型。

### 2.2 查询契约

新 REST 入口统一为：

- `POST /api/v1/queries/execute`
- `POST /api/v1/queries/preview`
- `POST /api/v1/public/queries/execute`

公共查询使用 `X-YuBi-Share-Token` 请求头。分享页面 URL 仍只负责加载前端应用，页面 JavaScript 再携带请求头调用公共查询接口。

`ExecuteQueryCommand` 只保留查询实际需要的 View、选择列、聚合、过滤、分组、排序、分页、变量和执行选项。规范字段使用 `concurrencyControlMode`；当前未生效的旧字段 `concurrencyControlModel` 不进入 Agent Tool Schema。

`QueryResult` 返回列元数据、数据行、分页信息和按权限决定是否返回的脚本。用户、组织和权限上下文由服务端认证会话生成，不允许客户端或模型覆盖。

### 2.3 Agent V1 工具

首版只读 Agent 只注册以下工具：

- `search_data_assets`：搜索当前用户可见的数据资产。
- `describe_data_asset`：读取指定 View 的字段、类型和业务描述。
- `execute_view`：通过结构化参数执行已有 View。

任何创建、修改、发布、分享或删除能力均不属于 Agent V1。

## 3. 阶段性独立目标

每个目标必须在独立任务中完成。一个目标未满足退出条件时，不进入下一目标，也不顺带实施后续阶段。

### 目标 A：架构基线与行为特征测试

**状态**：已完成（2026-07-12）

**目的**：在移动查询逻辑前锁定现有行为和安全语义。

**工作内容**：

- 完成架构审查、Query ADR、当前与目标依赖图。
- 为登录查询、分享查询、变量解析、列权限、分页归一化、脚本隐藏和 Provider 异常建立特征测试。
- 记录当前 REST 请求和响应样例，确认哪些字段实际生效。
- 明确持久化配置兼容样例，尤其是 View 和 Dashboard 查询配置。

**退出条件**：

- 特征测试在未重构代码上通过。
- 文档能够解释查询请求从 Controller 到 Provider 的完整数据流。
- 无尚未决定的 Query 模块依赖方向或公开契约。

**完成记录**：

- 架构审查：`docs/architecture/query-current-state-review.md`
- Query ADR：`docs/architecture/adr/0001-query-capability-boundary.md`
- 新增 8 个特征测试，覆盖登录与预览查询、分享令牌、变量、列权限、Owner 规则、分页、脚本隐藏和 Provider 异常。
- `mvn -pl server -am -Dexec.skip=true test`：通过；server 19 项测试通过，reactor 全部成功。
- Query 定向测试：8 项通过。
- `npm run checkTs`、`npm run lint`：通过。
- `npm run test:ci`：201 个测试文件通过，1319 项测试通过，4 项跳过；其中 View 配置迁移与请求构建定向测试 3 个文件、38 项通过。
- `mvn -pl server -am -DskipTests package`：通过；前端主应用/task、后端 Jar 和安装包 assembly 均成功。
- 已知非阻断告警：Mockito 动态 agent、测试环境 Log4j provider、Vite 大 chunk、AntV S2 缺失 sourcemap 提示；前端迁移异常输出来自错误分支测试预期。均未通过放宽测试处理。
- 本目标未修改生产代码，未创建 `query` 模块，未进入目标 B。

### 目标 B：后端 Query 能力模块

**状态**：已完成（2026-07-12）

**依赖**：目标 A。

**目的**：建立与框架和基础设施解耦的查询应用能力，暂不改变前端调用路径。

**工作内容**：

- 新增纯 Java `query` Maven 模块及 Use Case、命令、结果和端口。
- 在 `server` 实现查询定义、权限、变量、审计和 Provider 适配器。
- 将 `DataProviderServiceImpl` 中的查询编排迁入 Query 应用服务；数据源元数据、连接测试等非查询职责暂时保留。
- 旧 Controller 暂时委托新 Use Case，保证阶段结束时现有前端仍可运行。
- 增加模块依赖测试，禁止 `query` 依赖 `core/security/server` 和框架实现。

**退出条件**：

- 原查询特征测试全部通过。
- `query` 的 Maven 依赖树不包含 Spring MVC、MyBatis、Security 或 Provider 实现。
- `DataProviderServiceImpl` 不再负责变量、列权限、分页和执行编排。

**完成记录**：

- 新增纯 Java 21 `query` 模块，按 `api/application/domain/port` 分层提供 `ExecuteQueryUseCase`、`PreviewQueryUseCase`、纯值对象、稳定异常和五个端口；`server` 单向依赖 `query`。
- `DefaultQueryService` 统一承担查询与预览编排、变量覆盖、Owner 权限变量规则、列权限决策接入、分页归一化、引擎执行、脚本可见性、异常分类和尽力审计。端口返回 `null`、结果转换等未预期 `RuntimeException` 也会稳定分类、保留 cause 并审计为失败；`Error` 不包装、不吞掉，但同样不误记为成功。审计事件仅包含渠道、主体/组织引用、关联 ID、资源 ID、耗时、行数和失败类别，审计失败不覆盖查询结果或原异常。
- `server/query` 提供定义安全投影、权限与列权限、系统/组织/View 变量、Provider DTO 转换与调度、审计、可信执行上下文和旧 DTO 兼容映射适配器；Query API 不暴露实体、Spring Authentication、Provider DTO 或数据源配置。View 定义只携带 `sourceId` 和纯查询/权限投影，预览 Source 使用不选择 `config/type` 的最小投影；只有权限通过并进入 `ServerQueryEngineAdapter` 后，才按 `sourceId` 读取完整 Source、解密配置并调用 Provider。
- 上下文工厂不再预读 View/Source；应用服务在单次定义快照上绑定并校验组织。登录/分享 View 执行只读取一次 View，预览在授权前只读取一次无配置投影，授权成功后由引擎读取一次完整 Source，没有使用全局缓存或线程本地传递快照。
- `DataProviderServiceImpl` 的查询方法已收缩为旧 DTO 映射、可信上下文创建、Use Case 委托和旧 `Dataframe` 映射；数据源元数据、连接测试、函数能力及 Source 更新等非查询职责继续保留。旧 Controller、附件下载和调度/下载任务仍经原 `DataProviderService` 路径工作，REST 路径未变。
- 分享查询在 `try/finally` 中执行 `runAs`，成功和异常路径均调用 `releaseRunAs()`；分享令牌仍由旧分享入口校验并绑定 View，没有新增目标 C 的 REST 路径或 `X-YuBi-Share-Token` 契约。
- Query 脚本类型同时覆盖 `SQL` 和 `STRUCT`，旧 View 执行与预览 DTO 均双向映射到 Provider 原类型。
- 查询结果集合只在公开 `QueryResult` 边界冻结一次；内部 `EngineResult` 作为受信任的同步端口传输对象不重复复制，公开 columns、rows 和每一行均保持不可修改，Provider 原始 rows 后续变化不会影响公开结果。
- 兼容层因 Query API 禁止暴露 `Dataframe` 而重建边界响应对象；目标 A 测试不再只比较 ID，而是锁定 ID、名称、可视化字段、列、行、分页、脚本和单元格对象引用等全部可观察字段，避免以削弱断言换取重构通过。
- 测试新增：Query 应用服务 9 项、Query 结果所有权与不可变性 1 项、Query 源码边界 1 项、旧接口委托 2 项、定义安全投影 1 项、授权拒绝顺序 2 项、引擎延迟读取/解密与组织快照校验 2 项、Spring Use Case 装配 1 项、View/Preview STRUCT 2 项、分享异常释放 1 项；目标 A 原 8 项行为特征测试继续通过。
- `mvn -pl query test`：通过，11 项测试全部成功；Maven Enforcer 的 `query-production-boundary` 同时通过。
- 目标 A 与 Query 定向测试：`DataProviderServiceImplCharacterizationTest` 原 6 项及新增 2 项 STRUCT 回归均通过；`ShareServiceImplCharacterizationTest` 原 2 项及新增异常释放测试均通过。
- `mvn -pl server -am -Dexec.skip=true test`：通过；Query 11、Core 23、Security 18、Provider Base 46、JDBC Provider 125、Server 30，共 253 项，0 失败、0 错误，JDBC 既有 6 项跳过。
- `mvn -pl query dependency:tree -Dscope=runtime`：只包含 `yubi:yu-bi-query` 自身，运行时生产依赖为空，不含 core、security、server、Spring、MyBatis、Jackson、AES 或 Provider 实现。
- `mvn -pl server -am -DskipTests package`：通过；前端主应用/task、Query 与后端 Jar、安装包 assembly 均成功。
- 已知非阻断告警：Mockito 动态 agent、测试环境 Log4j provider、GraalVM fallback runtime、Vite 大 chunk 和 npm allow-scripts 提示；均未通过删除测试、吞异常或放宽权限/边界规则处理。
- 本目标未修改前端查询调用路径，未新增或删除 REST 接口，未实现 Agent Runtime、LLM 接入或写操作，未进入目标 C。

### 目标 C：新 REST 契约与前端 Query Feature

**状态**：已完成（2026-07-12）

**依赖**：目标 B。

**目的**：让现有产品界面统一通过 Query 能力接口访问查询服务。

**工作内容**：

- 增加三个新查询入口和 `X-YuBi-Share-Token` 处理。
- 新建前端 `features/query`，集中查询契约、客户端和请求构建逻辑。
- 页面 thunk 继续负责页面状态编排，只把 HTTP 调用和纯请求构建移入 feature。
- 逐批迁移 ChartWorkbench、Dashboard、Viz 和 Share 调用点。
- 迁移期间旧 `app/models`、`app/types` 路径可临时 re-export；目标结束前允许保留，避免把目录迁移与调用迁移混成一次大改。
- 验证普通分享链接、应用内分享 iframe 和跨域预检场景。

**退出条件**：

- 仓库内前端不再调用三个旧 REST 入口。
- 所有查询调用通过 `features/query` 客户端发送。
- 分享查询只通过请求头发送执行令牌。
- 前端类型检查、查询单测、分享页面测试和构建通过。

**完成记录**：

- 新增 `POST /api/v1/queries/execute`、`POST /api/v1/queries/preview` 和 `POST /api/v1/public/queries/execute`。新 Web DTO 经单次 `QueryWebMapper` 映射直接调用目标 B 的 `ExecuteQueryUseCase` / `PreviewQueryUseCase`，响应保持现有 Dataframe 可观察字段兼容；旧 Controller、旧路径和旧 DTO 继续存在。
- 新 REST 为 Query 稳定异常建立明确 HTTP 映射：校验失败 400、权限拒绝 403、定义失败 422、执行失败 502。Query Advice 显式使用最高优先级；真实 Spring MVC + Security 上下文同时加载 Query Advice 与全局 Advice，逐项锁定 400/403/422/502，并确认非 Query 异常仍由全局 Advice 处理为 400。Spring Security 只匿名放行公共查询，登录查询和预览继续要求认证。
- `QueryWebMapper` 在 Web 边界显式校验空请求、所有嵌套对象/路径集合、参数 Map 键值及 Set 元素，并只在 `enumValue` 中把非法枚举转换为稳定的 `QueryValidationException`；对象图映射和 Query 值对象构造不做宽泛异常捕获，内部 NPE/IAE 保持原异常。认证 execute、preview 和 public execute 的空/非法输入均在 Use Case、执行上下文或公共身份切换前返回 400。
- 公共查询只从 `X-YuBi-Share-Token` 读取执行令牌。服务端解密后校验令牌类型为 View 且绑定请求 `viewId`，以 `permissionBy` 身份调用 Query Use Case，并在执行成功、执行异常和身份切换异常路径通过 `finally` 可靠释放身份；回归测试锁定 `runAs` 自身失败时原异常不变、释放恰好一次且不调用后续上下文或 Use Case。缺失、无效类型和错误 View 令牌均拒绝且不回显令牌。
- CORS 只配置新 Query 路径：认证入口允许 `POST/OPTIONS`、`Authorization/Content-Type`，公共入口允许 `POST/OPTIONS`、`Content-Type/X-YuBi-Share-Token`；未向旧分享或其他 API 扩大跨域权限，真实预检处理测试通过。
- 新增 `frontend/src/app/features/query`，集中请求/响应类型、`ChartDataRequestBuilder`、认证/公共/预览客户端和公开入口；客户端复用现有 `request2`，没有新增第二套 HTTP 实例。旧 `app/models/ChartDataRequestBuilder` 和 `app/types/ChartDataRequest` 仅保留最小 re-export。
- ChartWorkbench、Dashboard Board/BoardEditor、Dashboard actions 经公共 fetch 链路、Viz、Share、过滤器公共 fetch、交互 Hook、ChartEditor 和 task 构建调用点均已迁移；页面 thunk 只保留状态、错误和交互编排。迁移架构测试递归扫描 `frontend/src/app` 与 `frontend/src/task.ts` 的全部生产 TS/TSX/JS/JSX，并兼容 MTS/CTS/MJS/CJS，锁定三个旧查询 URL 为 0、新查询 URL 只存在于 `features/query/client.ts`、feature 生产源码不导入 `app/pages`。
- 分享执行令牌只写入专用 Header，不进入执行 URL、query parameter 或请求体。Query 客户端测试、普通分享路由、Chart/Dashboard/Story 分享页面、应用内 iframe 生命周期及迁移架构回归测试均通过；View/Dashboard 历史配置和 `concurrencyControlMode` 测试继续通过。
- 后端新增 Query Web、安全与 CORS 测试 21 项，覆盖认证、DTO 精确映射边界、稳定错误映射、两个 Advice 真实共存、公共 Header、缺失/错误 token、View 绑定、成功/失败身份释放和 CORS 预检；目标 A/B 特征测试继续通过。
- `mvn -pl query -am test`：通过，Query 11 项；`mvn -pl server -am -Dexec.skip=true test`：通过，Query 11、Core 23、Security 18、Provider Base 46、JDBC Provider 125、Server 51，共 274 项，0 失败、0 错误，JDBC 既有 6 项跳过。
- `mvn -pl query dependency:tree -Dscope=runtime`：只包含 Query 模块自身；`npm run checkTs`、`npm run lint`、`npm run build:task`、`npm run build` 均通过；`npm run test:ci` 全量 203 个测试文件、1332 项通过、4 项跳过；`mvn -pl server -am -DskipTests package` 和安装包 assembly 通过。
- 全仓旧前端 URL、新 Header 使用、Query feature 依赖、旧接口并存和目标 D 清理项搜索通过，`git diff --check` 通过。已知非阻断告警仍为 Mockito 动态 agent、测试 Log4j provider、GraalVM fallback、AntV S2 缺失 sourcemap、Vite 大 chunk 和 npm allow-scripts 提示。
- 残余安全策略：公共执行令牌的 `expiryDate` 校验继续沿用旧 `/shares/execute` 基线，目标 C 未新增令牌有效期校验，因此本目标不宣称覆盖完整令牌有效期策略。
- 本目标未删除旧 REST、旧 DTO、旧字段或临时 re-export，未实现元数据、Agent Runtime、模型 SDK 或写操作，未进入目标 D。

### 目标 D：旧接口与过渡代码清理

**状态**：已完成（2026-07-12）

**依赖**：目标 C。

**目的**：在无外部用户的前提下完成原子迁移收尾，不长期维护双实现。

**工作内容**：

- 删除 `/data-provider/execute`、`/data-provider/execute/test` 和 `/shares/execute`。
- 删除旧 Controller 查询方法、旧请求 DTO、查询参数令牌逻辑和临时前端 re-export。
- 删除 `concurrencyControlModel` 拼写，统一为 `concurrencyControlMode`；持久化配置仍保持现有 `concurrencyControlMode` 格式。
- 增加前端导入规则：shared/feature 不得导入 `app/pages/*`，页面不得直接导入其他页面的查询 thunk。
- 全仓搜索确认旧路径、旧字段和旧类型引用归零。

**退出条件**：

- 旧接口返回 404，仓库内无旧路径和 `concurrencyControlModel` 引用。
- View/Dashboard 历史配置兼容测试通过。
- 完整产品查询、分享和下载回归通过。

**完成记录**：

- 已删除三个旧查询 REST 的 Controller 查询方法；`/data-provider/execute`、`/data-provider/execute/test` 与 `/shares/execute` 的 POST 集成验收均返回 404。分享通用 ID 路由显式排除 `execute`，避免旧地址被错误解析为 405。
- 已移除旧查询 DTO、兼容 Mapper 和 DataProvider/Share 查询门面。下载、分享下载和调度保留原有持久化载荷形状，但统一改用 `DownloadQueryRequest`、`DownloadQueryExecutor` 和 Query Use Case；历史 `concurrencyControlMode` 值在下载请求反序列化与分享下载回归中保持兼容。
- 已删除前端临时 re-export，所有调用直接从 `app/features/query` 导入。14 项迁移架构测试使用 TypeScript AST 递归扫描生产扩展名，禁止 feature/shared 导入页面、禁止页面直接导入其他页面的 Query thunk，并覆盖本地/默认导出、绝对、相对、别名、命名空间、动态、无插值模板、require、重导出及全部生产扩展的目录入口解析。
- 已增加生产代码旧工件架构测试、旧入口 404 测试、分享下载回归和前端临时模块/旧字段门禁；生产代码中不保留旧路径、旧字段或旧 DTO。
- 验证通过：`mvn -pl query -am test`（11 项）、`mvn -pl server -am -Dexec.skip=true test`（Server 47 项）、`mvn -pl query dependency:tree -Dscope=runtime`、前端 `checkTs`、`lint`、`test:ci`、`build:task`、`build`，以及 `mvn -pl server -am -DskipTests package`。本目标未进入目标 E。

### 目标 E：元数据与语义能力

**依赖**：目标 D。

**目的**：让 Agent 在查询前能够发现和理解有权限的数据资产。

**工作内容**：

- 建立 `QueryMetadataUseCase` 及搜索、详情和字段描述契约。
- 返回权限过滤后的 View、字段、数据类型、变量和可用函数。
- 不向 Agent 返回数据源密码、连接串、原始加密配置或无管理权限时的脚本。
- 为元数据搜索和描述建立稳定 Tool Schema。

**退出条件**：

- 不同组织、角色和列权限下的元数据结果符合现有授权模型。
- 搜索和详情接口不依赖 Agent Runtime 或具体模型 SDK。

### 目标 F：只读 Agent Runtime

**依赖**：目标 E。

**目的**：在不开放任意 SQL 和写操作的前提下完成第一条端到端 Agent 数据分析链路。

**工作内容**：

- 新增独立 `agent` 模块，包含 Model Gateway、会话、Tool Registry、步骤执行和失败处理。
- 注册 `search_data_assets`、`describe_data_asset`、`execute_view`。
- Tool 适配器直接调用能力 API，不通过 REST，也不访问 Mapper 或 Provider。
- 从认证上下文注入用户和组织，拒绝模型传入的身份覆盖字段。
- 记录会话、请求、用户、组织、工具、脱敏参数摘要、耗时、结果规模和状态。

**退出条件**：

- 使用假模型可重复验证工具选择、权限拒绝、失败恢复和结果截断。
- Agent 无法执行任意 SQL、访问未授权字段或调用写操作。
- 领域模块和 Query 模块不依赖任何模型 SDK。

### 目标 G：评测、可观测与安全加固

**依赖**：目标 F。

**目的**：在扩展 Agent 能力前建立可量化的质量和运行风险基线。

**工作内容**：

- 建立离线评测集，覆盖资产发现、查询参数生成、拒答和越权尝试。
- 增加 Tool 调用 Trace、延迟、失败率、查询行数和资源消耗指标。
- 配置查询分页、超时、并发和结果截断策略。
- 验证日志不包含令牌、数据源配置、密码或未授权数据。

**退出条件**：

- 评测可在 CI 或受控环境中重复运行。
- 每次 Agent 查询都能追踪到具体用户、工具调用和最终结果。
- 安全测试覆盖提示注入、身份伪造和超限查询。

### 目标 H：受控写工具与 Agent 工作区

**依赖**：目标 G。

**目的**：在只读链路稳定后逐步开放创建图表、修改仪表盘等高价值操作。

**工作内容**：

- 每类写能力先抽取独立业务 Use Case，再注册为 Agent Tool。
- 所有写工具必须支持显式审批、参数预览、幂等键、审计和失败回滚。
- 前端 Agent 工作区展示计划、工具执行、数据结果、待审批操作和失败信息。
- 删除、发布、分享等高风险动作单独评审，不随首批写工具默认开放。

**退出条件**：

- 未审批的写操作不会产生业务副作用。
- 重复提交不会创建重复资源。
- 用户可以查看并追溯 Agent 的每次业务变更。

## 4. 验证基线

阶段目标按影响范围执行验证，不能用后续阶段测试替代当前阶段的退出条件。

后端基础验证：

```bash
mvn -pl server -am -Dexec.skip=true test
mvn -pl server -am -DskipTests package
```

目标 B 创建 `query` 模块后，所有后续目标额外执行：

```bash
mvn -pl query -am test
```

前端基础验证：

```bash
cd frontend
npm run checkTs
npm run lint
npm run test:ci
npm run build:task
npm run build
```

发布链路继续执行现有构建体积检查、JDBC 定向测试和 `scripts/check-demo-health.sh`。如果新增架构规则进入 ESLint，CI 必须显式执行 `npm run lint`，不能只运行现有 Stylelint 步骤。

## 5. 执行约束

- 每次只启动一个阶段目标，并将目标名称写入任务描述。
- 每个目标必须从干净工作区开始，以对应退出条件结束。
- 不在 Query 重构中顺带实现 Agent Runtime，也不在 Agent Runtime 中回头重写 Query 边界。
- 不为了消除编译错误让 `query` 直接依赖 `core` 或 `server`。
- 不把模型生成内容直接视为已授权命令；权限和审批始终由确定性代码执行。
- 验证失败时记录真实原因，不删除测试或放宽权限规则来获得通过。

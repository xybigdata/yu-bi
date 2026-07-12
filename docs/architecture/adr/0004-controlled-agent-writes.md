# ADR-0004：受控 Agent 写入与工作区边界

- 状态：已接受
- 日期：2026-07-12
- 最近复核：2026-07-13
- 决策范围：Agent-ready 计划目标 H 的首批可视化写能力、审批、幂等、事务、审计与前端工作区

## 背景

ADR-0001 建立了纯 Java Query 能力边界，ADR-0002 将 Agent V1 限制为三项只读工具，ADR-0003 又锁定了离线评测、固定容量执行和脱敏可观测协议。目标 H 只能在不削弱这些边界的前提下开放第一批写操作。

现有可视化通用更新接口不适合作为 Agent Tool：Controller DTO 可携带组织、权限、发布状态、原始配置和 Widget 删除列表；Dashboard 更新在最终 MANAGE 校验前已经进入 Widget 写步骤；Widget 服务本身没有独立权限检查。模型或工作区不能复用这些宽 DTO，也不能把模型输出直接当作已授权命令。

当前没有生产 `ModelGateway` 或 Agent 会话存储适配器。目标 H 可以交付确定性写 Use Case、受控提议协议、审批服务和工作区，但不能宣称部署后已经具备自然语言模型能力。

## 决策

### 1. 首批能力严格收窄

首批写工具只有：

1. `create_chart`：创建一个绑定已有 View 的未发布基础明细表图表；
2. `rename_dashboard`：只修改已有仪表盘名称。

`create_chart` 只接受 `name/viewId/parentId/description`，图表类型和持久化配置由服务端固定生成。命令不包含组织、主体、权限、状态、原始配置、计算字段、SQL、脚本或 Source。

`rename_dashboard` 只接受 `dashboardId/newName`。它不接受 Dashboard 原始配置、Widget 创建/更新/删除、发布状态或权限。

删除、发布、分享、权限变更、任意 SQL、View/Source/脚本写入和通用 Dashboard patch 不注册，也不能借由首批命令字段间接表达。

### 2. 独立确定性业务能力

新增纯 Java `visualization-write` 模块：

```text
server -> agent -> query
server -> visualization-write
```

`CreateChartUseCase` 与 `RenameDashboardUseCase` 均采用 `prepare -> execute`：

- `prepare` 校验窄命令、可信组织和当前权限，返回安全预览；
- `execute` 只接收服务端持久化且摘要校验通过的 prepared operation，不重新调用 `prepare`，也不在业务锁前读取 View、Dashboard、Folder、权限或名称；
- server 原子写端口先通过受控写专用当前读取得完整锁，再使用锁定快照重验主体、组织、权限、目标状态和名称；
- 仪表盘预览绑定目标状态指纹，批准时锁定目标并重新比较，不一致返回有限 `STALE` 失败；
- 业务 `prepare` 可以在 Tool 生成的最小安全预览上追加当前名称等权威信息，但不能删除或改写既有字段，追加字段和影响项均受数量、长度与类型上限约束；
- 模块端口只使用纯投影和结果，不暴露 Controller DTO、实体、Mapper、Spring、Jackson 或 Provider 类型。

Agent 通过纯 `VisualizationWritePort` 描述业务准备与执行，`server` 组合根把该端口适配到 `visualization-write` 的两个 Use Case。现有 Query 模块不依赖 `agent` 或 `visualization-write`。Agent 生产代码仍不得依赖 server/core/security/Spring/MyBatis/Jackson/AES/Provider 实现或模型 SDK。

### 3. 只读 Registry 与写提议 Registry 分离

`DefaultReadOnlyToolRegistry` 继续精确包含：

- `search_data_assets`
- `describe_data_asset`
- `execute_view`

写能力使用独立 `WriteProposalRegistry`，精确包含 `create_chart` 和 `rename_dashboard`，Schema 必须标记 `WRITE` 与 `requiresApproval=true`。写提议只能创建待审批 operation，不能调用业务 `execute`。批准与拒绝通过独立受信 Use Case/HTTP 入口处理，不复用只读 `BoundedToolExecutor`，避免超时返回后写事务仍继续提交。

若模型协议返回写提议，它与普通只读 `ToolCall` 是不同判别类型；Runtime 只能执行参数规范化、预览和持久化 `PENDING`。当前目标没有把写提议接入只读 Runtime，写 Registry 也不能被 `BoundedToolExecutor` 解析或执行。

工作区的只读运行入口仍只接受一个 `message`，在恢复可信工作区会话后调用可选 `AgentUseCase`。Web 响应重新绑定本次可信 session/request，只投影三项只读 Tool 的计划、步骤和受限结构化结果，并递归剔除 SQL、脚本、Source 配置和凭据类键。缺少生产模型适配器时稳定返回 `503 AGENT_RUNTIME_UNAVAILABLE`，工作区如实显示模型能力未配置，仍可使用同一确定性受控写入口完成引导式操作。

### 4. 服务端会话、审批与不可绕过状态机

工作区会话 ID 和 approval ID 均由服务端生成并持久化。approval 绑定：

- trusted subject、organization 和 workspace session；
- preview request、correlation 和 tool；
- 规范化参数及其 SHA-256 摘要；
- 幂等键摘要、创建时间和有效期；
- 安全预览、最终资源、变更和有限失败码。

持久可见状态为：

```text
PENDING -> SUCCEEDED
PENDING -> REJECTED
PENDING -> EXPIRED
PENDING -> FAILED
```

`PENDING -> approved execution -> SUCCEEDED` 在持有 operation 行锁的单个数据库事务中完成。批准执行使用专属 `REQUIRES_NEW + REPEATABLE_READ` 事务，且在任何普通业务一致性读之前按固定顺序完成 operation、目标、名称空间与授权当前锁定读。这既隔离调用方外层事务，又使 MySQL/InnoDB next-key lock 阻止继承权限语义中“原本缺失的具体资源授权行”被并发插入为降权行。`APPROVED/EXECUTING` 是该事务内的受信转换，不提交为可被其他请求复用的中间状态。未审批、拒绝、过期、未知 approval、会话/主体/组织不匹配和重复批准都在业务写之前终止。

批准请求只携带服务端 approval ID 和工作区会话 Header，不重传模型参数。服务端从 operation 重新加载 prepared command 并校验摘要，UI 或模型不能在批准时换参。

页面重新连接时，服务端在锁定组织行后，按重新授权得到的 subject/organization 恢复尚未过期的活动会话，并优先选择仍持有有效 `PENDING` operation 的会话；不存在时才创建新会话。该恢复不放宽 approval 的 session 绑定，也不要求浏览器持久化 session。并发打开同一组织由组织行锁串行化，不会为同一可信作用域创建重复活动会话。

### 5. 幂等与并发语义

幂等唯一范围为：

```text
(subject, organization, tool, SHA-256(idempotency-key))
```

workspace session 仍是审批绑定和审计字段，但不进入幂等唯一范围，使同一可信用户在会话重连或网络重试后仍可回放原 operation。相同作用域、相同 key、等价规范化参数返回原 operation/结果；相同 key 与不同参数固定冲突；不同用户或组织不能互相复用。

仍有效的 `PENDING` operation 不允许从其他 session 回放；服务端会话恢复使正常页面刷新重新获得原 session。终态或已过期 operation 可以在相同 subject/organization/tool/idempotency 作用域内跨 session 重放，但不能再次执行业务写。

三张新表显式使用 InnoDB。数据库唯一约束处理并发 preview 竞态；MySQL `REPEATABLE READ` 下唯一键冲突后的重放使用 `SELECT ... FOR UPDATE` 当前读，避免旧快照看不到胜出的记录。批准、拒绝和过期使用 operation 行锁；行锁保持到业务、operation 终态和审计一并提交，同一 operation 的并发批准最多执行一次业务写。approval ID 自身也构成执行重放边界。

业务执行的锁顺序固定为：创建图表按 View -> 目标 Folder 或 Organization 根名称空间；重命名仪表盘按 Dashboard -> 关联 Folder -> 父 Folder 或 Organization 根名称空间。锁齐后再锁定当前成员关系和该用户在组织内的 Role/Role-Resource 授权范围，随后检查名称并写入。相同受控名称空间的并发写因此串行化，不以 Folder 的可空父级唯一索引承担根名称唯一性。

Folder、Dashboard 和 Role 生产 Mapper 存在短时二级缓存，单独调整事务隔离级别不能保证普通 Service 查询一定到达数据库。目标 H 为所需 View/Dashboard/Folder/Organization/Role/Role-Resource 查询增加独立、窄范围的 `FOR UPDATE + useCache=false + flushCache=true` 当前读方法；它们在审批事务中清除 MyBatis 一级缓存并使共享二级缓存到提交前不可读，再由既有业务权限服务基于同一事务的当前数据判定。Role 与 Role-Resource 锁定读按稳定索引顺序最多返回 4097 个 ID；超过 4096 个有效授权行时整个审批 fail closed，不继续权限判定或业务写。名称唯一性直接使用专用当前查询，不回退到缓存查询。普通 UI 查询和缓存策略不变。

终态 operation 默认保留 90 天，因此幂等保证的持久窗口也是 90 天；窗口外清理后，同一 key 不再保证返回历史结果。该限制是明确的数据治理语义，不通过前端缓存延长。

### 6. 事务、回滚和审计

首批能力只有本地数据库副作用，不包含文件、消息或其他外部副作用，因此不需要补偿流程。

- 图表、Folder、operation `SUCCEEDED` 和成功审计事件在同一事务提交；
- 仪表盘与 Folder 同步改名、operation `SUCCEEDED` 和成功审计事件在同一事务提交；
- 图表执行先锁定 View 与目标名称空间，仪表盘执行先锁定 Dashboard/关联 Folder/目标名称空间，再以无缓存当前读重验成员、权限、状态和名称；
- 业务异常使整个执行事务回滚，不留下图表、Folder 或半更新名称；
- 回滚完成后使用另一个独立 `REQUIRES_NEW + READ_COMMITTED` 事务把 operation 标记为 `FAILED` 并记录有限失败码；
- 审计是持久账本，不复用尽力日志、旧 `access_log` 或只读 `AgentAuditPort`。

审计可按 subject/session/request/correlation/tool/approval/idempotency 摘要/resource/final status 追溯，但不记录参数值、原始异常、令牌、密码、Source 配置、脚本、SQL 或完整数据结果。参数和 prepared 摘要用于检测应用协议与可信数据库记录之间的不一致；数据库本身属于可信服务端边界，摘要不是用于抵御能够任意改写数据库行的攻击者。

机会式保留维护运行在独立事务中：默认终态 operation 保留 90 天、过期 session 保留 30 天、审计保留 365 天。每次限批把到期 `PENDING` 原子转为 `EXPIRED` 并追加审计，先删除可清理 operation，再删除无 operation 引用的 session，最后清理已无 operation 的审计。维护失败只记录固定告警并等待后续请求重试，不回显数据，也不阻断正常工作区请求。

### 7. 前端工作区

前端新增独立 `features/agent` 客户端和 `agentWorkspace` Redux slice。页面只能通过受控工作区 API 创建会话、预览、列出审批、批准或拒绝；不得导入现有 Viz/Chart/Board 写 thunk 或直接调用 `/viz/*` 写接口。

工作区展示只读计划、Tool 执行、受限数据结果、最终回答，以及受控 Tool 预览、影响范围、待审批状态、审批中、成功变更、拒绝、过期、重复和失败。按钮状态只服从服务端审批投影，不能从模型文本推断。异步响应绑定发起时的组织、session 和工作区代次，列表响应不能覆盖更晚的预览或终态。

路由 `:orgId` 必须先出现在服务端返回的当前成员组织列表中，并与 Redux 当前组织一致，才成为唯一请求和资源导航作用域。URL 组织合法但持久偏好不一致时先清空工作区并封闭全部 Agent 请求和审批控件，再通过既有组织切换流程同步；非成员 URL 固定拒绝。组织切换期间的旧 session、审批和异步响应不能进入新作用域。

session、approval、幂等键、prompt 和用户输入只保存在内存并通过 Header/请求体传递，不进入 URL、`localStorage`、日志或分析事件。Redux DevTools 启用时，全部 `agentWorkspace/*` action 只投影 action type，根状态中的 `agentWorkspace` 子树整体替换为固定占位符；其他 Redux 调试信息保持可用。全局 rejected 日志同样只记录 action type，不输出 action、payload 或 `meta.arg`。生产关闭 DevTools 时继续完全关闭。

## 备选方案

### 把写工具加入 V1 只读 Registry

拒绝。它会放宽 ADR-0002/0003 的精确三工具门禁，并让现有 Runtime 在没有审批阶段的情况下立即执行写调用。

### 暴露通用 DashboardUpdateParam

拒绝。该 DTO 可携带 Widget 删除、状态和原始配置，现有写顺序还存在授权过晚与 Widget 归属缺口，无法证明首批范围不包含删除或发布。

### 只把审批状态放在 Redux 或模型历史

拒绝。浏览器和模型均不是受信执行边界，也不能提供多请求、多实例或并发批准的一致性。

### 用只读 Tool 超时执行器运行写事务

拒绝。Java 中断不能保证底层写入停止，可能产生调用方收到超时而事务随后提交的歧义。

## 影响与残余风险

正向影响：

- 模型输出与已授权命令之间存在持久、显式、不可绕过的审批边界；
- 两项首批写能力可以独立测试权限重验、幂等并发、陈旧预览和事务回滚；
- 只读三工具、Query 边界和现有 REST/DataProvider/View/Dashboard Schema 保持不变；
- 每次业务变更都有不含敏感参数值的可信账本。

代价与残余风险：

- 新增三张最小表用于工作区会话、operation/幂等和 append-only 审计；迁移提供对应回滚脚本；
- 仪表盘内容修改仍不开放；添加图表到仪表盘需要单独解决 Widget 所属、布局生成、关系授权和并发语义；
- 没有生产 `ModelGateway` 和 Agent 会话存储，因此自然语言计划与只读结果仍处于未配置状态，不能伪造为可用；接入真实模型前还必须为模型最终回答建立生产级敏感输出策略；
- 多实例部署依赖共享数据库和数据库行锁；外部审计归档、告警阈值和长期合规留存仍由部署治理；
- 受控写当前读依赖所有实例共享同一 MySQL/InnoDB 数据库、`REPEATABLE_READ` 事务、生产授权唯一索引和 MyBatis 事务工厂；新增 Mapper 方法的 `useCache=false/flushCache=true/FOR UPDATE`、索引顺序和返回上限由架构测试锁定，不能降回 `READ_COMMITTED` 或在普通缓存重构中删除；`LIMIT 4097` 只严格约束 Java 结果物化量，InnoDB 实际扫描和 next-key 锁范围仍由索引与执行计划决定，高授权量用户可能被拒绝，同组织权限维护可能在短事务期间受到更宽的锁等待；
- Agent 图表创建会锁定目标名称空间，但现有 UI 写入口不遵循同一锁协议，且历史 Folder Schema 没有名称唯一约束；跨入口同时创建同名资源仍有残余竞态，本目标不通过修改既有持久化 Schema 扩大修复范围；
- 账本记录提议、重放、批准结果、拒绝、过期和失败；在 operation 建立前被拒绝的未知字段、缺失 Header 或越权请求只产生有限 Web 失败，不逐请求写入受控写账本，边界拒绝率需要由后续低基数安全指标覆盖。

## 验收约束

- 未审批、拒绝、过期、篡改、跨主体/组织/会话和批准时撤权均不得产生业务副作用；
- 同一幂等作用域的重复请求和重复批准不得重复创建或修改资源；
- 写失败必须回滚业务表，并以有限失败码留下可信终态；
- 只读 Registry 仍精确三项，写 Registry 仅精确两项；
- 删除、发布、分享、权限、SQL、Source 和脚本不得出现在写 Schema、Controller 请求或审批 UI；
- 前端页面不得直接调用现有可视化写接口或持久化工作区敏感状态。

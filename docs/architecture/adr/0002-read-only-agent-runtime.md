# ADR-0002：建立只读 Agent Runtime 边界

- 状态：已接受
- 日期：2026-07-12
- 决策范围：Agent-ready 计划目标 F 的进程内 Runtime、Tool、可信上下文、失败和 Trace 契约

## 背景

ADR-0001 已将查询执行和元数据能力收敛到纯 Java `query` 模块，并明确身份、组织、权限、Source 配置和 Provider 实现只能由 `server` 可信适配。目标 F 需要让模型通过进程内 Tool 调用这些能力，但模型输出不能成为已授权命令，也不能把 Controller、Mapper、Spring Security、数据库、AES 或具体模型 SDK 重新带回领域边界。

首版只需要可重复验证的只读分析链路，不需要模型供应商选型、Agent Web 入口、会话数据库表、写操作或前端工作区。

## 决策

### 1. 独立模块和依赖方向

新增纯 Java 21 `agent` 模块：

```text
server -> agent -> query
```

`agent` 按 `api/application/domain/port` 分层，Maven 生产依赖只有 `query`。它不得依赖 `server`、`core`、`security`、Spring、MyBatis、Jackson、AES、DataProvider/Provider DTO、数据库驱动或模型 SDK。为安全投影 Query 返回的 JDBC 时间值，字节码可依赖 JDK `java.sql` 模块，但源码架构门禁将引用精确限制为 `Timestamp/Date/Time` 三种值类型，禁止其他 `java.sql`、通配符和 `javax.sql` API。Maven Enforcer 显式禁止基础设施生产依赖族，源码架构测试作为第二道门禁；`query` 不反向依赖 `agent` 或模型 SDK，ADR-0001 的 Query 边界保持不变。

`ModelGateway` 是供应商无关端口。模型输入为用户消息、步骤序号、只读 Tool Schema 和已脱敏的历史步骤；不包含可信主体、组织、角色、权限或分享身份。模型输出使用 `ToolCall` / `FinalAnswer` 结构化决策和深度不可变的 `StructuredValue`，不使用字符串协议承载 Tool 调用。

### 2. 固定只读工具集合

V1 Registry 必须且只能注册：

1. `search_data_assets`
2. `describe_data_asset`
3. `execute_view`

Registry 构造时验证精确集合、唯一名称和 `readOnly=true`。搜索和详情直接复用 `QueryMetadataToolSchemas`，不复制 Schema；Tool 每次直接调用 `QueryMetadataUseCase`，不经过 REST。

`execute_view` 只允许已有 View 的结构化选择列、聚合、过滤、分组、排序、有界分页和安全 Query 变量。它不接受：

- SQL、脚本、Source、Source 配置或 Preview；
- `FunctionColumn.snippet`、关键字搜索或表达式变量；
- userId、organizationId、roleId、权限覆盖、分享令牌或分享身份；
- 缓存、并发、创建、修改、发布、分享、删除、审批或其他写能力。

过滤值只允许 `STRING/NUMERIC/DATE/BOOLEAN`，别名使用安全标识符。Tool 在任何 Metadata 或 Query 能力调用前按 FilterType 穷尽校验值基数：`EQ/NE/GT/LT/GTE/LTE/LIKE/PREFIX_LIKE/SUFFIX_LIKE/NOT_LIKE/PREFIX_NOT_LIKE/SUFFIX_NOT_LIKE` 恰好 1 个，`IN/NOT_IN` 至少 1 个，`BETWEEN/NOT_BETWEEN` 恰好 2 个，`IS_NULL/NOT_NULL` 恰好 0 个。V1 从过滤 Schema 和输入白名单移除 `aggregateOperator`；聚合过滤只有在后续明确推导聚合结果类型及兼容规则后才能单独决策，当前不会把 `COUNT/COUNT_DISTINCT` 按原字段类型错误校验。Tool 还拒绝其他未知字段和空查询，避免 `DefaultQueryService` 的空查询快速返回把权限拒绝降级为成功。

### 3. 每次调用重新授权

`search_data_assets` 和 `describe_data_asset` 每次调用元数据 Use Case，重新验证可信主体、组织和 READ/列权限。

`execute_view` 每次先以同一可信上下文调用 `describe(assetId, false)`，使用当次授权字段和变量描述校验所有列、聚合、过滤、分组、排序和变量；字段路径按不可变路径片段逐段精确比较，不能通过点号重分段绕过。NUMERIC 过滤值和 Query 变量必须经 `BigDecimal` 严格解析并规范化，BOOLEAN 只接受 `true` / `false`，过滤值类型必须与授权字段类型一致。显式空变量、缺失的安全必填 Query 变量，以及 V1 无法安全满足的必填表达式/非安全类型 Query 变量都会在引擎前明确拒绝。随后调用 `ExecuteQueryUseCase`，再次执行 Query 定义读取、组织绑定、READ/列权限、变量和引擎边界。授权结果不进入会话缓存或 Registry。

这种双重校验既阻止模型直接查询未授权字段，也不改变 Query 模块现有确定性授权职责。

### 4. 可信执行上下文

`AgentRequest` 只包含用户业务消息。`server/agent` 的上下文工厂从当前认证 principal 取得 subject，并通过 `OrgService.listOrganizations()` 验证显式服务器受控组织范围。工厂在模型调用前生成 session、request、correlation ID 和固定 `AUTHENTICATED` 的 `QueryExecutionContext`。

空主体、空组织、非成员组织或组织适配失败必须稳定拒绝，不能让搜索退化为跨组织空结果。Agent 不支持 `SHARED`、`SYSTEM`、`runAs` 或分享令牌上下文。

### 5. 状态机和失败策略

Runtime 使用固定最大步数的顺序状态机。默认最多 8 步，模型参数同时限制请求长度、节点数和嵌套深度。

失败策略固定为：

- 输入校验、未授权字段、Query 校验、权限拒绝、未知工具和模型协议失败立即终止；
- Query 定义或执行失败默认最多允许模型恢复 1 次；
- 未预期的 Runtime/Tool 内部异常固定分类为 `INTERNAL` 并立即终止，不能让模型把内部失败降级为最终成功；
- 模型或 Tool 抛出 `Error` 时先尽力写入失败步骤、终态会话和完成 Trace，再原样重抛；
- 恢复预算耗尽或达到最大步骤后终止，不自动无限重试；
- Query/Tool 原始异常 message 和 cause 不进入模型历史、最终失败或 Trace。

会话和步骤状态均为不可变快照。`AgentSessionStorePort` 只接收脱敏状态快照：保留步骤、状态、失败和结果规模，但清空 Tool payload 与最终答案。目标 F 使用测试内存适配器验证状态转换，不新增数据库表。具体持久化、一致性、保留期限和多实例语义需要后续独立决策。

### 6. 结果限制

默认 Tool 结果上限为 100 项和 64 KiB。`execute_view` 将 Query 页大小限制为“最大返回行数 + 1”，以确定是否存在溢出，再按原始顺序保留稳定前缀。字节预算覆盖模型可见的完整结构化输出，包括 envelope、标识、列元数据、分页和行；文本按紧凑 JSON 等价 UTF-8 大小确定性计量。JDBC `Timestamp` 使用 `LocalDateTime`、JDBC `Date` 使用 `LocalDate`、JDBC `Time` 使用 `LocalTime`、普通 `java.util.Date` 使用 `Instant` 做显式安全投影，避免 JDBC Date/Time 的不支持转换，同时所有文本继续遵守单元格字节上限。超大元数据和单元格使用有界前缀，未知对象使用稳定类型标记且不调用任意对象的 `toString()`。

`describe_data_asset` 先构造始终包含 `id/name/fields/variables/functions` 的最小 Schema 必填 envelope，再把剩余字节确定性分配给 `id/name` 前缀、可选描述/脚本和集合项；合法最小值 128 字节及 256 字节配置均保留全部必填字段，`returnedBytes` 按最终结构精确计量且不超过上限。元数据集合和显式授权的可选脚本文本采用同一完整输出上限。

`ResultSize` 记录观察/返回项数、观察/返回字节、上限和 `truncated`。它同时进入下一轮 `ModelTurn` 历史和最终 `AgentRunResult`，模型与调用方都能看到截断事实；最终结果的上限字段按本次各 Tool 输出上限累计，不丢失实际限制。

### 7. 审计和装配

`AgentAuditPort` 记录会话开始/完成和每一步的：

- session、request、subject、organization、correlation；
- step、已注册工具、耗时、状态和失败分类；
- Schema 识别字段、拒绝字段数量、标量/集合数量和最大深度；
- 结果项数、字节数和截断状态。

Trace 不记录参数值、用户原始消息、模型原始响应、令牌、密码、Source 配置、脚本或完整查询结果。未知工具名统一脱敏为 `<unregistered>`。Trace 或会话存储失败不能覆盖本次确定性结果。

`server` 是唯一组合根。固定 Registry、限制、时钟和审计适配器可直接装配；只有同时存在 `ModelGateway` 和 `AgentSessionStorePort` 时才创建 Runtime，避免在尚未选择模型供应商和持久化方案时让现有应用启动失败。

## 备选方案

### 让模型调用 Query REST

拒绝。它会重复认证和 DTO 协议，弱化进程内能力边界，并增加模型构造身份、分享令牌或 Preview 请求的机会。

### 把 Agent Runtime 放入 Query

拒绝。Query 是确定性业务能力边界，不应依赖模型协议、会话状态或模型 SDK。

### 直接暴露 `ExecuteQueryCommand` 全部字段

拒绝。计算片段、关键字、脚本请求、缓存和宽松分页不属于 V1 最小只读 Tool 契约，也会扩大任意表达式和超限查询风险。

### 目标 F 同时选择模型 SDK 和建立会话表

拒绝。模型供应商与持久化语义尚无足够决策；端口、假模型和测试内存适配器已经能稳定验证本阶段退出条件，新增 SDK 或数据库 Schema 会不必要地扩大范围。

## 影响

正向影响：

- 模型选择、Tool 执行、权限拒绝、恢复和截断可以用假模型确定性测试。
- Tool 无法越过 Query/Metadata 的组织、READ、列权限和变量边界。
- 模型供应商、Web、安全和 Provider 实现不会进入 Agent 或 Query 领域模块。
- 后续模型网关、会话持久化和评测可以围绕稳定端口独立演进。

代价和残余风险：

- `execute_view` 为防止未授权字段参与过滤或聚合，会在执行前额外调用一次 Metadata 详情能力；Query 随后仍会重新授权。
- V1 不支持聚合结果过滤；过滤级 `aggregateOperator` 会在能力调用前拒绝，待聚合结果类型和比较兼容性形成独立稳定契约后再评估。
- 当前没有生产模型网关、持久化适配器或 Agent Web/前端入口，Runtime 只在这些端口由组合根提供后启用。
- Trace 和会话存储采用尽力语义；生产持久化、告警和留存策略属于后续可观测与安全目标。
- `describe_data_asset` 保持目标 E 的既有契约：只有 MANAGE 权限且显式请求时才可能返回已有 View 脚本；脚本不能作为 `execute_view` 输入，也不会进入 Trace。
- Metadata 字段快照和 Query 最终授权是连续的两次能力调用；Agent 不缓存授权且 Query 会在执行时重新授权，但权限并发变化的原子字段授权语义仍由既有 Query/Provider 边界决定，目标 F 不回头改写该边界。

## 验收约束

- Registry 生产代码只能包含三项 V1 工具。
- 每次 Tool 调用都必须使用同一服务器可信上下文重新进入 Query/Metadata 权限边界。
- 任意 SQL、Preview、Source 配置、身份/组织/权限覆盖和写操作输入必须在能力调用前拒绝。
- Agent Maven 生产依赖树只能是 `agent -> query`，Query 生产依赖树仍为空；Agent 字节码仅可额外使用 JDK `java.base` 和受架构门禁限制的 `java.sql` 时间值类型，两者均不得包含模型 SDK 或数据库实现。
- 结果截断必须同时对模型和最终调用方可见，成功与失败 Trace 均不得包含敏感参数值或完整结果。

# ADR-0003：Agent 评测、可观测与资源治理

- 状态：已接受
- 日期：2026-07-12
- 决策范围：Agent-ready 计划目标 G 的离线评测、Tool 指标、Trace 完整性和执行资源限制

## 背景

ADR-0001 建立了纯 Java Query 能力边界，ADR-0002 建立了只读 Agent Runtime、可信上下文、三工具白名单、双重授权、有限状态机和结果截断契约。目标 G 需要在不引入模型供应商、生产会话表、写工具或新 Web 入口的前提下，为这条链路增加可重复质量基线和运行风险治理。

现有 Runtime 已有会话与步骤完成 Trace，但没有独立低基数指标端口，也没有对 Tool 阻塞时间和跨会话并发施加基础设施无关的确定性限制。评测必须离线运行，不能依赖网络、真实模型、真实数据源或会泄露 prompt、参数与结果的失败报告。

## 决策

### 1. 离线评测是测试夹具，不是生产模型接口

评测集使用固定案例 ID、类别、结构化假模型决策、内存 Query/Metadata 能力和预期终态。它实际穿过 V1 Registry、Tool 参数映射、字段授权守卫、Query 命令生成、审计、指标和脱敏会话快照，并由 Maven 测试在 CI 或受控环境重复执行。

V1 数据集包含 9 个案例，覆盖资产发现、查询参数生成、有限码拒答、未注册 `execute_sql` 攻击、`execute_view.sql` 未知字段攻击、提示注入写工具、身份伪造、未授权字段和超限分页。SQL 攻击实际进入 Registry 或 Tool 输入映射，并在 Metadata/Query 能力调用前稳定拒绝；身份伪造同时要求 Metadata 与 Query 能力零调用，不能由假模型直接输出自由文本替代。评分同时检查完整 `AgentRunResult`、最终答案、审计、指标和脱敏快照，并逐项检查令牌、JDBC URL、密码原值和未授权字段四个独立 canary。报告只包含案例 ID、通过数和有限失败原因枚举；反向用例在一次合法 Tool 调用后分别返回四个 canary，证明泄密答案稳定归类为 `SENSITIVE_OUTPUT`，错误拒答和跳过攻击也会失败，且报告不回显用户消息、Tool 参数、模型响应、异常 message/cause、数据行或敏感夹具值。评测不进入生产 API，不新增网络或模型 SDK 依赖。

### 2. Tool 执行使用固定容量和确定性终态

`ToolExecutionPolicy` 独立配置 Agent 查询最大页大小、Tool 超时和最大并发调用数。`BoundedToolExecutor` 使用按需创建的固定上限线程池、固定容量队列和非阻塞并发许可：构造时不预启动线程，达到并发上限立即拒绝，不忙等；超时取消任务并请求中断；调用线程自身被中断时先取消任务、恢复中断标志，再返回稳定失败。许可绑定 `ToolTask.run()` 的实际退出而非 `Future` 完成状态；提交拒绝（包括线程创建或启动抛出的 `Error`）、取消未开始、正常/异常返回、超时、中断和关闭竞态都先取消任务，再通过一次性租约恰好释放。完成后的竞争测试还锁定不会过量释放许可。底层能力若忽略中断，在真正退出前继续占用许可，因此新调用仍立即得到 `CONCURRENCY_LIMIT`，不会扩张为无界线程或任务队列。

超时与并发拒绝分别使用稳定 `TIMEOUT` 和 `CONCURRENCY_LIMIT` 分类，均不可由模型恢复。Runtime 仍是 ADR-0002 的顺序有限状态机，Registry 仍精确只有 `search_data_assets`、`describe_data_asset`、`execute_view`。

`execute_view` 分离调用方请求页大小、公开返回上限和内部探测量。请求页大小在进入 Metadata/Query 能力前按策略校验，公开上限为请求页大小与结果项上限的较小值；首屏未计数查询最多获取公开上限加 1 行作为哨兵，后续页或调用方显式计数保持公开有效页大小并使用内部 `countTotal` 获得可靠 `hasMore`，避免 `pageSize + 1` 改变后续页偏移。已知总数时只按 `total > offset + observedRows` 判断是否还有数据，合法空越界页与总数为零不会误报截断；Provider 总数与行数据不一致不借用正常截断标志表达。内部哨兵和强制计数不进入公开分页元数据，输出严格返回公开上限内的稳定前缀并显式标记 `truncated`；现有 Query REST、Preview、下载及 Query 应用服务的分页兼容语义不改变。

默认配置为：页大小 100、Tool 超时 30 秒、并发 4、结果 100 项和 64 KiB；平台线程并发配置的绝对上限为 64。`server` 组合根从 `yubi.agent.*` 读取配置并负责有界执行器生命周期，外部非默认覆盖值由装配测试精确校验，无效值启动即失败；缺少模型网关或会话存储时既不创建 Runtime，也不创建执行器。

### 3. Trace 与指标职责分离

`AgentAuditPort` 继续承担可追踪 Trace。每个会话开始、每个 Tool/模型/最终回答步骤和会话完成事件都带可信 session、request、subject、organization、correlation、步骤、工具、耗时、结果规模、状态和失败分类。会话完成事件提供最终状态；未知工具继续归一为 `<unregistered>`。

`AgentMetricsPort` 每个 Tool 调用只记录一个有限协议事件，字段仅包含：归一化工具名、状态、失败枚举、耗时、参数节点数、结果项数/字节数和 `execute_view` 查询行数。server 使用窄依赖 `spring-boot-starter-micrometer-metrics` 提供 `MeterRegistry` 并记录调用计数、延迟、参数节点、结果项/字节和查询行数；失败率由有限 `status/failure` 标签的调用计数计算。目标 G 不依赖 Actuator，也不增加 `/actuator` 或其他管理 Web 端点；装配测试同时锁定注册表存在、Actuator Web 自动配置不在运行时类路径以及 POM 不引入 Actuator 依赖。

指标标签只允许三工具名或 `unregistered`、有限状态和失败枚举。用户、组织、session、request、correlation、SQL、字段名、参数值和数据内容都不得成为指标标签。指标失败与 Trace/会话存储失败一样采用尽力语义，不能覆盖业务结果。

### 4. 敏感信息不进入可观测和评测产物

Trace 不记录用户消息、Tool 参数值、模型原始响应、令牌、密码、Source 配置、脚本或完整结果；参数只记录 Schema 识别字段和结构复杂度。会话存储继续清空 Tool payload 和最终答案。指标协议不含任何身份或自由文本值。评测敏感检查包含完整运行结果和最终答案，报告不回显案例输入和运行产物。

subject/session/request/correlation 等可信追踪标识只进入 Trace/审计，不进入指标。目标 G 实际清理的 server、Query 适配器和 Provider 生产文件由显式受控集合逐文件扫描；其中传统日志级别调用只允许固定字符串字面量或其编译期拼接，任何动态参数均拒绝，SLF4J 2 的 `atTrace/atDebug/atInfo/atWarn/atError/atLevel/makeLoggingEventBuilder` fluent builder 入口则全部禁止。因此该门禁不依赖 logger、SQL、Throwable、密码或配置变量名称，能覆盖改名、包装表达式和直接 Throwable 参数；它只证明受控文件集合，不声称对集合之外的全仓代码执行跨过程污点分析。适配器不得记录 Query/Tool 原始异常 message/cause。

## 备选方案

### 使用无界异步执行器并依赖 Provider 超时

拒绝。不同 Provider 的超时语义不一致，而且无法限制多个 Agent 会话对进程线程和排队任务的占用。

### 在指标标签中加入用户或请求 ID

拒绝。这会造成高基数并扩大身份信息暴露面。逐次追踪由 Trace 负责，聚合风险由低基数指标负责。

### 将评测集接入真实模型或真实数据源

拒绝。网络、供应商版本和真实数据会破坏确定性，并让评测输出承担不必要的敏感信息风险。真实模型质量评测需要后续在受控环境基于同一脱敏协议单独决策。

## 影响

正向影响：

- CI 可以稳定检测 Tool 选择、参数映射、拒答、安全攻击和脱敏回归。
- 阻塞、超限并发和大结果都获得明确、可测试的资源上限。
- 单次调用可由 Trace 精确追踪，聚合延迟、失败率、行数和资源规模可由低基数指标观测。
- Agent 与 Query 生产依赖方向保持 `agent -> query`，Micrometer 和 Spring 仍只存在于 server 适配层。

代价和残余风险：

- Java 中断是协作式取消；忽略中断的 Provider 调用会继续占用一个固定工作线程和并发许可，直到自身返回或底层超时，但不会触发无界扩容或提前接纳新调用。
- 当前没有生产模型网关和会话存储，因此 Runtime 仍只在这两个端口同时存在时启用。
- 当前 Micrometer 指标可进入 server 的注册表；具体外部导出、告警阈值、保留期限和多实例聚合由部署环境配置，不在目标 G 内新增公开端点。

## 验收约束

- 离线评测不得使用网络、真实模型、真实数据源或自由文本失败输出。
- Tool Registry 必须继续精确为三项只读工具。
- 超时、并发、分页和截断必须有确定性测试，不得使用无界线程、无界队列、忙等或吞掉调用线程中断。
- 每次 Agent 查询必须能通过可信 user/session/request/correlation、Tool 步骤和会话最终状态追踪。
- 指标不得使用身份、请求、SQL、参数值或数据内容作为标签；Trace、指标、会话快照和评测报告不得包含令牌、Source 配置、密码或未授权数据。
- Query 不依赖 Agent；Agent 不依赖 server/core/security/Spring/MyBatis/Jackson/AES/Provider 实现或模型 SDK。

# Agent 工作入口

本文件仅提供全局路由。进入具体工作前，按任务类型阅读对应文档：

规则优先级为：用户明确要求、`SECURITY.md` 安全规则、Release 规则、一般工程约束。疑似漏洞不得因 Issue 流程而公开披露。

- Issue 操作：`docs/agents/issue-tracker.md`
- 分诊标签：`docs/agents/triage-labels.md`
- 领域术语与架构决策：`docs/agents/domain.md`
- 安全、TDD、验证与本地产物边界：`docs/agents/engineering-workflow.md`
- 版本与 Release：`docs/agents/release.md`

默认使用中文沟通、编写文档和提交说明；代码标识符遵循所在模块的既有风格。更具体目录中的 `AGENTS.md` 可补充模块工程细节，但不得放宽本文件的安全、远端写操作和 Release 授权规则。

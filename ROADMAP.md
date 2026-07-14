# Roadmap

## 近期

- 保持 `JDK 21 + Spring Boot 4 + Node.js 24 + Vite 8` 当前基线稳定。
- 按 [Agent-ready 架构改造计划](./docs/agent-ready-architecture-plan.md) 完成架构基线、Query 能力模块和查询行为测试。
- 将现有查询入口迁移到统一 Query 契约，并在仓库内调用迁移完成后清理旧接口。
- 保持数据库结构、历史 View/Dashboard 配置和 DataProvider SPI 兼容。
- 继续处理测试稳定性、依赖风险和发布链路维护。

## 中期

- 建立权限过滤后的元数据与语义能力，为 Agent 提供数据资产搜索和描述。
- 实现只读 Agent Runtime、Tool Registry、审计和 Trace，首版只执行已有 View。
- 建立 Agent 离线评测、查询资源限制和安全测试。
- 根据 Query Feature 迁移结果逐步收敛前端页面间依赖。

## 长期

- 在显式审批、幂等和审计保护下开放图表与仪表盘写工具。
- 建设 Agent 工作区，呈现计划、工具执行、结果和待审批操作。
- 保持独立发布线，持续完善发布说明、兼容矩阵和运维文档。
- 持续统一仓库、发布产物和产品界面的 `yu-bi` 品牌。
